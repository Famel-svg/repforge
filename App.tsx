import { Ionicons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
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

import { BottomTabBar } from '@/components/BottomTabBar';
import { migrateDatabase } from '@/db/schema';
import type { MainTabParamList, RootStackParamList } from '@/navigation/types';
import { ConfigScreen } from '@/screens/ConfigScreen';
import { ExerciseScreen } from '@/screens/ExerciseScreen';
import { HomeScreen } from '@/screens/HomeScreen';
import { SearchExerciseScreen } from '@/screens/SearchExerciseScreen';
import { SheetScreen } from '@/screens/SheetScreen';
import { SheetsScreen } from '@/screens/SheetsScreen';
import { TrainingTrackScreen } from '@/screens/TrainingTrackScreen';
import { colors } from '@/theme';

enableScreens();

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<MainTabParamList>();

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

function MainTabs() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        headerStyle: { backgroundColor: colors.surface },
        headerTintColor: colors.text,
        headerShadowVisible: false,
        sceneStyle: { backgroundColor: colors.background },
      }}
      tabBar={(props) => <BottomTabBar {...props} />}
    >
      <Tab.Screen
        component={HomeScreen}
        name="Home"
        options={{
          title: 'RepForge',
          tabBarIcon: ({ color, size }) => (
            <Ionicons color={color} name="home" size={size} />
          ),
        }}
      />
      <Tab.Screen
        component={SheetsScreen}
        name="Sheets"
        options={{
          title: 'Planilhas',
          tabBarIcon: ({ color, size }) => (
            <Ionicons color={color} name="list" size={size} />
          ),
        }}
      />
      <Tab.Screen
        component={TrainingTrackScreen}
        name="Track"
        options={{
          title: 'Track de treino',
          tabBarIcon: ({ color, size }) => (
            <Ionicons color={color} name="flame" size={size} />
          ),
        }}
      />
      <Tab.Screen
        component={ConfigScreen}
        name="Config"
        options={{
          title: 'Config',
          tabBarIcon: ({ color, size }) => (
            <Ionicons color={color} name="settings" size={size} />
          ),
        }}
      />
    </Tab.Navigator>
  );
}

function AppNavigator() {
  return (
    <NavigationContainer theme={navigationTheme}>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
          headerStyle: { backgroundColor: colors.surface },
          headerTintColor: colors.text,
          headerShadowVisible: false,
          contentStyle: { backgroundColor: colors.background },
        }}
      >
        <Stack.Screen
          component={MainTabs}
          name="Main"
        />
        <Stack.Screen
          component={SheetScreen}
          name="Sheet"
          options={({ route }) => ({ title: route.params.sheetName })}
        />
        <Stack.Screen
          component={ExerciseScreen}
          name="Exercise"
          options={({ route }) => ({ title: route.params.exerciseName })}
        />
        <Stack.Screen
          component={SearchExerciseScreen}
          name="SearchExercise"
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
