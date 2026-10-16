package com.repforge.app.data

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

private val Context.proPreferences by preferencesDataStore("repforge_pro")
private val cachedPro = booleanPreferencesKey("is_pro")
const val PRO_PRODUCT_ID = "repforge_pro_lifetime"
private const val UNAVAILABLE = "Google Play indisponível neste aparelho. Tente novamente mais tarde."

/** Lifetime unlock via Play Billing. Never throws: without Play it stays at the cached (offline) state plus [error]. */
class ProStore(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val lock = Mutex()
    private var details: ProductDetails? = null

    private val _isPro = MutableStateFlow(false)
    private val _price = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    val isPro: StateFlow<Boolean> = _isPro
    val price: StateFlow<String?> = _price
    val error: StateFlow<String?> = _error

    private val client: BillingClient? = runCatching {
        BillingClient.newBuilder(app)
            .setListener { result, purchases ->
                when (result.responseCode) {
                    BillingClient.BillingResponseCode.OK -> scope.launch { handle(purchases.orEmpty()) }
                    BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restore()
                    else -> _error.value = "Não foi possível concluir a compra (${result.responseCode})."
                }
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
    }.getOrNull()

    init {
        scope.launch {
            runCatching { app.proPreferences.data.first()[cachedPro] }.getOrNull()?.let { _isPro.value = it }
            refresh()
        }
    }

    fun restore() {
        scope.launch { refresh() }
    }

    fun purchase(activity: Activity) {
        scope.launch {
            _error.value = null
            if (details == null) refresh()
            val d = details ?: return@launch run { _error.value = _error.value ?: UNAVAILABLE }
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
                .build()
            val result = runCatching { client?.launchBillingFlow(activity, params) }.getOrNull()
            if (result?.responseCode != BillingClient.BillingResponseCode.OK) _error.value = UNAVAILABLE
        }
    }

    private suspend fun ready(): Boolean {
        val c = client ?: return false
        lock.withLock {
            repeat(3) { attempt ->
                if (c.isReady) return true
                val ok = runCatching {
                    suspendCancellableCoroutine { cont ->
                        c.startConnection(object : BillingClientStateListener {
                            override fun onBillingSetupFinished(result: BillingResult) {
                                if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                            }
                            override fun onBillingServiceDisconnected() = Unit
                        })
                    }
                }.getOrDefault(false)
                if (ok) return true
                delay(1_500L shl attempt)
            }
        }
        return false
    }

    private suspend fun refresh() {
        val c = client
        if (c == null || !ready()) return run { _error.value = UNAVAILABLE }
        try {
            val product = QueryProductDetailsParams.Product.newBuilder().setProductId(PRO_PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build()
            val found = c.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build())
                .productDetailsList?.firstOrNull()
            details = found
            _price.value = found?.oneTimePurchaseOfferDetails?.formattedPrice
            val owned = c.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
            if (owned.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _error.value = null
                handle(owned.purchasesList, authoritative = true)
            } else _error.value = UNAVAILABLE
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = UNAVAILABLE
        }
    }

    /** Unacknowledged purchases are refunded by Google after 3 days, so retry with backoff until the result is OK. */
    private suspend fun acknowledge(purchase: Purchase) {
        repeat(3) { attempt ->
            val code = try {
                client?.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())?.responseCode
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            if (code == BillingClient.BillingResponseCode.OK) return
            if (attempt < 2) delay(1_000L shl attempt)
        }
    }

    private suspend fun handle(purchases: List<Purchase>, authoritative: Boolean = false) {
        val mine = purchases.filter { PRO_PRODUCT_ID in it.products }
        val owned = mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (mine.any { it.purchaseState == Purchase.PurchaseState.PENDING }) _error.value = "Compra pendente; o Pro será liberado quando o pagamento for confirmado."
        owned.filter { !it.isAcknowledged }.forEach { acknowledge(it) }
        // A successful query is authoritative (covers refunds); a purchase event only ever grants.
        if (owned.isNotEmpty() || authoritative) {
            _isPro.value = owned.isNotEmpty()
            runCatching { app.proPreferences.edit { it[cachedPro] = owned.isNotEmpty() } }
        }
    }
}
