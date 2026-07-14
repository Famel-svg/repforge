import { Image } from 'expo-image';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { colors, radius } from '@/theme';

type Props = {
  uri: string | null;
  size?: number;
};

export function ExerciseImage({ uri, size = 76 }: Props) {
  const [failed, setFailed] = useState(false);

  if (!uri || failed) {
    return (
      <View style={[styles.placeholder, { width: size, height: size }]}>
        <Text style={styles.placeholderText}>RF</Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, { width: size, height: size }]}>
      <Image
        cachePolicy="none"
        contentFit="contain"
        onError={() => setFailed(true)}
        source={{ uri }}
        style={[styles.image, { width: size, height: size }]}
        transition={150}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderRadius: radius.md,
    backgroundColor: '#1a1a2e',
    overflow: 'hidden',
  },
  image: {
    borderRadius: radius.md,
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
