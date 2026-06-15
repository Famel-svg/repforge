import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useFocusEffect } from '@react-navigation/native';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import {
  Alert,
  FlatList,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppButton } from '@/components/AppButton';
import { EmptyState } from '@/components/EmptyState';
import { createSheet, deleteSheet, listSheets } from '@/db/sheets';
import type { RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing } from '@/theme';
import type { Sheet } from '@/types';
import { exportBackupFile, pickAndImportBackup } from '@/utils/backupFile';
import { formatDateTime } from '@/utils/format';

type Props = NativeStackScreenProps<RootStackParamList, 'Home'>;

export function HomeScreen({ navigation }: Props) {
  const db = useSQLiteContext();
  const [sheets, setSheets] = useState<Sheet[]>([]);
  const [name, setName] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [saving, setSaving] = useState(false);
  const [backupBusy, setBackupBusy] = useState(false);

  const loadSheets = useCallback(async () => {
    try {
      setSheets(await listSheets(db));
    } catch (error) {
      Alert.alert('Erro', error instanceof Error ? error.message : 'Falha ao carregar fichas.');
    }
  }, [db]);

  useFocusEffect(
    useCallback(() => {
      void loadSheets();
    }, [loadSheets]),
  );

  async function handleCreate() {
    setSaving(true);
    try {
      await createSheet(db, name);
      setName('');
      setModalVisible(false);
      await loadSheets();
    } catch (error) {
      Alert.alert('Ficha não criada', error instanceof Error ? error.message : 'Tente novamente.');
    } finally {
      setSaving(false);
    }
  }

  function confirmDelete(sheet: Sheet) {
    Alert.alert(
      'Excluir ficha?',
      `${sheet.name} e todo o histórico serão removidos.`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Excluir',
          style: 'destructive',
          onPress: () => {
            void deleteSheet(db, sheet.id).then(loadSheets);
          },
        },
      ],
    );
  }

  async function handleExport() {
    setBackupBusy(true);
    try {
      await exportBackupFile(db);
    } catch (error) {
      Alert.alert('Exportação falhou', error instanceof Error ? error.message : 'Tente novamente.');
    } finally {
      setBackupBusy(false);
    }
  }

  async function handleImport() {
    setBackupBusy(true);
    try {
      const count = await pickAndImportBackup(db);
      if (count !== null) {
        await loadSheets();
        Alert.alert('Backup importado', `${count} ficha(s) adicionada(s).`);
      }
    } catch (error) {
      Alert.alert('Importação falhou', error instanceof Error ? error.message : 'Banco não alterado.');
    } finally {
      setBackupBusy(false);
    }
  }

  return (
    <View style={styles.screen}>
      <View style={styles.hero}>
        <Text style={styles.eyebrow}>PROGRESSÃO LOCAL</Text>
        <Text style={styles.heroTitle}>Forje seu próximo recorde.</Text>
        <Text style={styles.heroText}>
          Fichas, cargas e histórico disponíveis mesmo sem internet.
        </Text>
      </View>

      <View style={styles.actions}>
        <AppButton
          compact
          disabled={backupBusy}
          label="Importar"
          onPress={() => void handleImport()}
          style={styles.actionButton}
          variant="secondary"
        />
        <AppButton
          compact
          disabled={backupBusy}
          label="Exportar"
          onPress={() => void handleExport()}
          style={styles.actionButton}
          variant="secondary"
        />
      </View>

      <FlatList
        contentContainerStyle={sheets.length === 0 ? styles.emptyList : styles.list}
        data={sheets}
        keyExtractor={(item) => String(item.id)}
        ListEmptyComponent={
          <EmptyState
            description="Crie sua primeira ficha e monte a sequência de exercícios."
            title="Nenhuma ficha ainda"
          />
        }
        renderItem={({ item }) => (
          <Pressable
            onPress={() =>
              navigation.navigate('Sheet', {
                sheetId: item.id,
                sheetName: item.name,
              })
            }
            style={({ pressed }) => [styles.card, pressed && styles.pressed]}
          >
            <View style={styles.cardBody}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              <Text style={styles.cardDate}>
                Atualizada em {formatDateTime(item.updatedAt)}
              </Text>
            </View>
            <Pressable
              hitSlop={12}
              onPress={() => confirmDelete(item)}
              style={styles.deleteButton}
            >
              <Text style={styles.deleteText}>Excluir</Text>
            </Pressable>
          </Pressable>
        )}
      />

      <View style={styles.footer}>
        <AppButton label="+ Nova ficha" onPress={() => setModalVisible(true)} />
      </View>

      <Modal
        animationType="fade"
        onRequestClose={() => setModalVisible(false)}
        transparent
        visible={modalVisible}
      >
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Nova ficha</Text>
            <TextInput
              autoFocus
              maxLength={60}
              onChangeText={setName}
              onSubmitEditing={() => void handleCreate()}
              placeholder="Ex.: Peito e tríceps"
              placeholderTextColor={colors.textMuted}
              returnKeyType="done"
              style={styles.input}
              value={name}
            />
            <View style={styles.modalActions}>
              <AppButton
                label="Cancelar"
                onPress={() => setModalVisible(false)}
                style={styles.modalButton}
                variant="secondary"
              />
              <AppButton
                disabled={!name.trim()}
                label="Criar"
                loading={saving}
                onPress={() => void handleCreate()}
                style={styles.modalButton}
              />
            </View>
          </View>
        </SafeAreaView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  hero: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.lg,
    paddingBottom: spacing.md,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1.4,
  },
  heroTitle: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '900',
    marginTop: spacing.xs,
  },
  heroText: {
    color: colors.textMuted,
    lineHeight: 21,
    marginTop: spacing.sm,
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
    paddingHorizontal: spacing.md,
    paddingBottom: spacing.sm,
  },
  actionButton: {
    flex: 1,
  },
  list: {
    padding: spacing.md,
    gap: spacing.sm,
  },
  emptyList: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    backgroundColor: colors.surface,
  },
  pressed: {
    opacity: 0.75,
  },
  cardBody: {
    flex: 1,
    gap: spacing.xs,
  },
  cardTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '800',
  },
  cardDate: {
    color: colors.textMuted,
    fontSize: 13,
  },
  deleteButton: {
    padding: spacing.sm,
  },
  deleteText: {
    color: colors.danger,
    fontSize: 13,
    fontWeight: '700',
  },
  footer: {
    padding: spacing.md,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
  },
  modalOverlay: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.md,
    backgroundColor: colors.overlay,
  },
  modalCard: {
    width: '100%',
    maxWidth: 420,
    borderRadius: radius.lg,
    padding: spacing.lg,
    backgroundColor: colors.surface,
    gap: spacing.md,
  },
  modalTitle: {
    color: colors.text,
    fontSize: 22,
    fontWeight: '900',
  },
  input: {
    minHeight: 52,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    color: colors.text,
    backgroundColor: colors.background,
    fontSize: 16,
  },
  modalActions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  modalButton: {
    flex: 1,
  },
});
