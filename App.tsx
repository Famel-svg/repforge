import {
  DarkTheme,
  NavigationContainer,
  type Theme,
} from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SQLiteProvider } from 'expo-sqlite';
import { StatusBar } from 'expo-status-bar';
import { Suspense } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { enableScreens } from 'react-native-screens';

import { migrateDatabase } from '@/db/schema';
import type { RootStackParamList } from '@/navigation/types';
import { ExerciseScreen } from '@/screens/ExerciseScreen';
import { HomeScreen } from '@/screens/HomeScreen';
import { SearchExerciseScreen } from '@/screens/SearchExerciseScreen';
import { SheetScreen } from '@/screens/SheetScreen';
import { SheetsScreen } from '@/screens/SheetsScreen';
import { TrainingTrackScreen } from '@/screens/TrainingTrackScreen';
import { colors } from '@/theme';

enableScreens();

const Stack = createNativeStackNavigator<RootStackParamList>();

const navigationTheme: Theme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: colors.primary,
    background: colors.background,
    card: colors.surface,
    border: colors.border,
    text: colors.text,
    notification: colors.primary,
  },
};

function LoadingScreen() {
  return (
    <View style={styles.loading}>
      <ActivityIndicator color={colors.primary} size="large" />
      <Text style={styles.loadingText}>Preparando sua forja...</Text>
    </View>
  );
}

function AppNavigator() {
  return (
    <NavigationContainer theme={navigationTheme}>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: colors.surface },
          headerTintColor: colors.text,
          headerShadowVisible: false,
          contentStyle: { backgroundColor: colors.background },
        }}
      >
        <Stack.Screen
          name="Home"
          component={HomeScreen}
          options={{ title: 'RepForge' }}
        />
        <Stack.Screen
          name="Sheets"
          component={SheetsScreen}
          options={{ title: 'Planilhas' }}
        />
        <Stack.Screen
          name="Track"
          component={TrainingTrackScreen}
          options={{ title: 'Track de treino' }}
        />
        <Stack.Screen
          name="Sheet"
          component={SheetScreen}
          options={({ route }) => ({ title: route.params.sheetName })}
        />
        <Stack.Screen
          name="Exercise"
          component={ExerciseScreen}
          options={({ route }) => ({ title: route.params.exerciseName })}
        />
        <Stack.Screen
          name="SearchExercise"
          component={SearchExerciseScreen}
          options={{ title: 'Adicionar exercício' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <StatusBar style="light" />
      <Suspense fallback={<LoadingScreen />}>
        <SQLiteProvider
          databaseName="repforge.db"
          onInit={migrateDatabase}
          useSuspense
        >
          <AppNavigator />
        </SQLiteProvider>
      </Suspense>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
    backgroundColor: colors.background,
  },
  loadingText: {
    color: colors.textMuted,
    fontSize: 16,
  },
});
