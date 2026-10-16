package com.repforge.app.presentation

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler

/** Keeps a failed ViewModel write from crashing the app (cancellation never reaches the handler). */
val logExceptions = CoroutineExceptionHandler { _, e -> Log.e("RepForge", "Falha em operação", e) }
