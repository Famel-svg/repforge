import {
  StyleSheet,
  Text,
  View,
  type StyleProp,
  type ViewStyle,
} from 'react-native';

import { colors, radius, spacing } from '@/theme';

type StatCardProps = {
  hint?: string;
  label: string;
  style?: StyleProp<ViewStyle>;
  tone?: 'default' | 'primary';
  value: number | string;
};

export function StatCard({
  hint,
  label,
  style,
  tone = 'default',
  value,
}: StatCardProps) {
  const isPrimary = tone === 'primary';

  return (
    <View style={[styles.card, isPrimary && styles.primaryCard, style]}>
      <Text style={styles.label}>{label}</Text>
      <Text style={[styles.value, isPrimary && styles.primaryValue]}>
        {value}
      </Text>
      {hint ? <Text style={styles.hint}>{hint}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    minWidth: 0,
    flex: 1,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    backgroundColor: colors.surface,
    gap: spacing.xs,
  },
  primaryCard: {
    borderColor: colors.primary,
    backgroundColor: colors.surfaceRaised,
  },
  label: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '700',
  },
  value: {
    color: colors.text,
    fontSize: 22,
    fontWeight: '900',
  },
  primaryValue: {
    color: colors.primary,
  },
  hint: {
    color: colors.textMuted,
    fontSize: 11,
  },
});
