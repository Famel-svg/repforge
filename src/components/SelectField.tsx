import { useState } from 'react';
import {
  FlatList,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, radius, spacing } from '@/theme';

type Props = {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
};

export function SelectField({ label, value, options, onChange }: Props) {
  const [visible, setVisible] = useState(false);

  function choose(nextValue: string) {
    onChange(nextValue);
    setVisible(false);
  }

  return (
    <>
      <Pressable style={styles.field} onPress={() => setVisible(true)}>
        <Text style={styles.fieldLabel}>{label}</Text>
        <Text style={styles.fieldValue}>{value || 'Todos'}</Text>
      </Pressable>

      <Modal
        animationType="slide"
        onRequestClose={() => setVisible(false)}
        transparent
        visible={visible}
      >
        <SafeAreaView style={styles.overlay}>
          <View style={styles.sheet}>
            <View style={styles.header}>
              <Text style={styles.title}>{label}</Text>
              <Pressable onPress={() => setVisible(false)}>
                <Text style={styles.close}>Fechar</Text>
              </Pressable>
            </View>
            <FlatList
              data={['', ...options]}
              keyExtractor={(item) => item || 'all'}
              renderItem={({ item }) => (
                <Pressable
                  onPress={() => choose(item)}
                  style={[
                    styles.option,
                    item === value && styles.selectedOption,
                  ]}
                >
                  <Text style={styles.optionText}>{item || 'Todos'}</Text>
                </Pressable>
              )}
            />
          </View>
        </SafeAreaView>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  field: {
    flex: 1,
    minHeight: 58,
    justifyContent: 'center',
    gap: 3,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    backgroundColor: colors.surface,
  },
  fieldLabel: {
    color: colors.textMuted,
    fontSize: 12,
  },
  fieldValue: {
    color: colors.text,
    fontSize: 15,
    textTransform: 'capitalize',
  },
  overlay: {
    flex: 1,
    justifyContent: 'flex-end',
    backgroundColor: colors.overlay,
  },
  sheet: {
    maxHeight: '72%',
    borderTopLeftRadius: radius.lg,
    borderTopRightRadius: radius.lg,
    backgroundColor: colors.surface,
    padding: spacing.md,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingBottom: spacing.md,
  },
  title: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '800',
  },
  close: {
    color: colors.primary,
    fontWeight: '700',
  },
  option: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
    paddingVertical: 15,
    paddingHorizontal: spacing.sm,
  },
  selectedOption: {
    backgroundColor: colors.surfaceRaised,
  },
  optionText: {
    color: colors.text,
    fontSize: 16,
    textTransform: 'capitalize',
  },
});
