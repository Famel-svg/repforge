import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { useFocusEffect } from '@react-navigation/native';
import type { CompositeScreenProps } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
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
import {
  createSheet,
  deleteSheet,
  duplicateSheet,
  listSheets,
} from '@/db/sheets';
import type { MainTabParamList, RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing, touch } from '@/theme';
import type { Sheet } from '@/types';
import { formatDateTime } from '@/utils/format';

type Props = CompositeScreenProps<
  BottomTabScreenProps<MainTabParamList, 'Sheets'>,
  NativeStackScreenProps<RootStackParamList>
>;

export function SheetsScreen({ navigation }: Props) {
  const db = useSQLiteContext();
  const [sheets, setSheets] = useState<Sheet[]>([]);
  const [name, setName] = useState('');
  const [modalVisible, setModalVisible] = useState(false);
  const [saving, setSaving] = useState(false);
  const [duplicatingSheetId, setDuplicatingSheetId] = useState<number | null>(null);

  const loadSheets = useCallback(async () => {
    try {
      setSheets(await listSheets(db));
    } catch (error) {
      Alert.alert('Erro', error instanceof Error ? error.message : 'Falha ao carregar planilhas.');
    }
  }, [db]);

  useFocusEffect(
    useCallback(() => {
      void loadSheets();
    }, [loadSheets]),
  );

  async function handleCreate() {
    const sheetName = name.trim();
    if (!sheetName || saving) {
      return;
    }

    setSaving(true);
    try {
      await createSheet(db, sheetName);
      setName('');
      setModalVisible(false);
      await loadSheets();
    } catch (error) {
      Alert.alert('Planilha não criada', error instanceof Error ? error.message : 'Tente novamente.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDuplicate(sheet: Sheet) {
    setDuplicatingSheetId(sheet.id);
    try {
      const duplicatedId = await duplicateSheet(db, sheet.id);
      const duplicatedName = `${sheet.name} (cópia)`;
      await loadSheets();
      navigation.navigate('Sheet', {
        sheetId: duplicatedId,
        sheetName: duplicatedName,
      });
    } catch (error) {
      Alert.alert(
        'Planilha não duplicada',
        error instanceof Error ? error.message : 'Tente novamente.',
      );
    } finally {
      setDuplicatingSheetId(null);
    }
  }

  function confirmDuplicate(sheet: Sheet) {
    Alert.alert(
      'Duplicar planilha?',
      `${sheet.name} será copiada com os mesmos exercícios, sem histórico.`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Duplicar',
          onPress: () => {
            void handleDuplicate(sheet);
          },
        },
      ],
    );
  }

  function confirmDelete(sheet: Sheet) {
    Alert.alert(
      'Excluir planilha?',
      `${sheet.name} e todo o histórico serão removidos.`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Excluir',
          style: 'destructive',
          onPress: () => {
            void deleteSheet(db, sheet.id)
              .then(loadSheets)
              .catch((error) => {
                Alert.alert(
                  'Planilha não excluída',
                  error instanceof Error ? error.message : 'Tente novamente.',
                );
              });
          },
        },
      ],
    );
  }

  return (
    <View style={styles.screen}>
      <View style={styles.hero}>
        <Text style={styles.eyebrow}>PLANILHAS</Text>
        <Text style={styles.heroTitle}>Treinos prontos para abrir.</Text>
        <Text style={styles.heroText}>
          Crie ciclos e duplique estruturas boas. Backup agora fica em Config.
        </Text>
      </View>

      <FlatList
        style={styles.listShell}
        contentContainerStyle={sheets.length === 0 ? styles.emptyList : styles.list}
        data={sheets}
        keyExtractor={(item) => String(item.id)}
        ListEmptyComponent={
          <EmptyState
            description="Crie sua primeira planilha e monte a sequência de exercícios."
            title="Nenhuma planilha ainda"
          />
        }
        renderItem={({ item }) => (
          <Pressable
            accessibilityRole="button"
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
            <View style={styles.cardActions}>
              <Pressable
                accessibilityRole="button"
                disabled={duplicatingSheetId === item.id}
                hitSlop={12}
                onPress={() => confirmDuplicate(item)}
                style={[
                  styles.cardAction,
                  duplicatingSheetId === item.id && styles.disabledAction,
                ]}
              >
                <Text style={styles.duplicateText}>
                  {duplicatingSheetId === item.id ? 'Duplicando...' : 'Duplicar'}
                </Text>
              </Pressable>
              <Pressable
                accessibilityRole="button"
                hitSlop={12}
                onPress={() => confirmDelete(item)}
                style={styles.cardAction}
              >
                <Text style={styles.deleteText}>Excluir</Text>
              </Pressable>
            </View>
          </Pressable>
        )}
      />

      <SafeAreaView edges={['bottom']} style={styles.footer}>
        <AppButton label="+ Nova planilha" onPress={() => setModalVisible(true)} />
      </SafeAreaView>

      <Modal
        animationType="fade"
        onRequestClose={() => setModalVisible(false)}
        transparent
        visible={modalVisible}
      >
        <SafeAreaView style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Nova planilha</Text>
            <TextInput
              autoFocus
              maxLength={60}
              onChangeText={setName}
              onSubmitEditing={() => {
                if (name.trim()) {
                  void handleCreate();
                }
              }}
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
                disabled={!name.trim() || saving}
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
  listShell: {
    flex: 1,
  },
  list: {
    padding: spacing.md,
    gap: spacing.sm,
    paddingBottom: spacing.lg,
  },
  emptyList: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.md,
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
  cardActions: {
    alignItems: 'flex-end',
    gap: spacing.xs,
  },
  cardAction: {
    minHeight: touch.min,
    justifyContent: 'center',
    paddingHorizontal: spacing.xs,
  },
  disabledAction: {
    opacity: 0.55,
  },
  duplicateText: {
    color: colors.primary,
    fontSize: 13,
    fontWeight: '700',
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

