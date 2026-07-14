import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { RootTabRoute } from '@/navigation/types';
import { colors, radius, spacing } from '@/theme';

type TabKey = 'home' | 'sheets' | 'track';

type TabItem = {
  icon: string;
  key: TabKey;
  label: string;
  routeName: RootTabRoute;
};

const TABS: TabItem[] = [
  { icon: 'H', key: 'home', label: 'Home', routeName: 'Home' },
  { icon: 'P', key: 'sheets', label: 'Planilhas', routeName: 'Sheets' },
  { icon: 'T', key: 'track', label: 'Track', routeName: 'Track' },
];

type Props = {
  active: TabKey;
  onNavigate: (routeName: RootTabRoute) => void;
};

export function BottomTabBar({ active, onNavigate }: Props) {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.shell, { paddingBottom: Math.max(insets.bottom, spacing.sm) }]}>
      <View style={styles.bar}>
        {TABS.map((tab) => {
          const isActive = tab.key === active;

          return (
            <Pressable
              accessibilityLabel={`Abrir ${tab.label}`}
              accessibilityRole="button"
              accessibilityState={{ selected: isActive }}
              hitSlop={8}
              key={tab.routeName}
              onPress={() => { if (!isActive) onNavigate(tab.routeName); }}
              style={({ pressed }) => [
                styles.item,
                isActive && styles.itemActive,
                pressed && styles.itemPressed,
              ]}
            >
              <View style={[styles.iconBubble, isActive && styles.iconBubbleActive]}>
                <Text style={[styles.iconText, isActive && styles.iconTextActive]}>
                  {tab.icon}
                </Text>
              </View>
              <Text style={[styles.label, isActive && styles.labelActive]}>
                {tab.label}
              </Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  shell: {
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.sm,
    backgroundColor: colors.background,
  },
  bar: {
    minHeight: 64,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.xs,
    backgroundColor: colors.surface,
    gap: spacing.xs,
  },
  item: {
    flex: 1,
    minHeight: 54,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    gap: 3,
  },
  itemActive: {
    backgroundColor: colors.surfaceRaised,
  },
  itemPressed: {
    opacity: 0.72,
  },
  iconBubble: {
    width: 26,
    height: 26,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 13,
    borderWidth: 1,
    borderColor: colors.border,
  },
  iconBubbleActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primary,
  },
  iconText: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '900',
  },
  iconTextActive: {
    color: '#FFFFFF',
  },
  label: {
    color: colors.textMuted,
    fontSize: 11,
    fontWeight: '800',
  },
  labelActive: {
    color: colors.text,
  },
});

