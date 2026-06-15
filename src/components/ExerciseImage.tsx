import { Image, StyleSheet, Text, View } from 'react-native';

import { colors, radius } from '@/theme';

type Props = {
  uri: string | null;
  size?: number;
};

export function ExerciseImage({ uri, size = 76 }: Props) {
  if (!uri) {
    return (
      <View style={[styles.placeholder, { width: size, height: size }]}>
        <Text style={styles.placeholderText}>RF</Text>
      </View>
    );
  }

  return (
    <Image
      resizeMode="cover"
      source={{ uri }}
      style={[styles.image, { width: size, height: size }]}
    />
  );
}

const styles = StyleSheet.create({
  image: {
    borderRadius: radius.md,
    backgroundColor: '#FFFFFF',
  },
  placeholder: {
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    backgroundColor: colors.surfaceRaised,
  },
  placeholderText: {
    color: colors.primary,
    fontWeight: '900',
  },
});
