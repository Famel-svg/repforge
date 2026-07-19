import type { BottomTabBarProps } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { MainTabParamList } from '@/navigation/types';
import { colors, radius, spacing, touch } from '@/theme';

type TabVisual = {
  icon: keyof typeof Ionicons.glyphMap;
  iconFocused: keyof typeof Ionicons.glyphMap;
  label: string;
};

const TAB_VISUALS: Record<keyof MainTabParamList, TabVisual> = {
  Home: {
    icon: 'home-outline',
    iconFocused: 'home',
    label: 'Home',
  },
  Sheets: {
    icon: 'list-outline',
    iconFocused: 'list',
    label: 'Planilhas',
  },
  Track: {
    icon: 'flame-outline',
    iconFocused: 'flame',
    label: 'Track',
  },
  Config: {
    icon: 'settings-outline',
    iconFocused: 'settings',
    label: 'Config',
  },
};

export function BottomTabBar({ state, descriptors, navigation }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.shell, { paddingBottom: Math.max(insets.bottom, spacing.sm) }]}>
      <View style={styles.bar}>
        {state.routes.map((route, index) => {
          const focused = state.index === index;
          const { options } = descriptors[route.key];
          const tabKey = route.name as keyof MainTabParamList;
          const visual = TAB_VISUALS[tabKey];
          const label =
            typeof options.tabBarLabel === 'string'
              ? options.tabBarLabel
              : (visual?.label ?? route.name);
          const iconName = focused
            ? (visual?.iconFocused ?? 'ellipse')
            : (visual?.icon ?? 'ellipse-outline');

          return (
            <Pressable
              accessibilityLabel={options.tabBarAccessibilityLabel ?? `Abrir ${label}`}
              accessibilityRole="button"
              accessibilityState={{ selected: focused }}
              hitSlop={8}
              key={route.key}
              onPress={() => {
                const event = navigation.emit({
                  type: 'tabPress',
                  target: route.key,
                  canPreventDefault: true,
                });

                if (!focused && !event.defaultPrevented) {
                  navigation.navigate(route.name, route.params);
                }
              }}
              style={({ pressed }) => [
                styles.item,
                focused && styles.itemActive,
                pressed && styles.itemPressed,
              ]}
            >
              <View style={[styles.iconBubble, focused && styles.iconBubbleActive]}>
                <Ionicons
                  color={focused ? '#FFFFFF' : colors.textMuted}
                  name={iconName}
                  size={18}
                />
              </View>
              <Text style={[styles.label, focused && styles.labelActive]}>{label}</Text>
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
    minHeight: touch.min,
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
    width: 28,
    height: 28,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
  },
  iconBubbleActive: {
    borderColor: colors.primary,
    backgroundColor: colors.primary,
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
