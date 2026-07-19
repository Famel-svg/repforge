import type { NavigatorScreenParams } from '@react-navigation/native';

export type MainTabParamList = {
  Home: undefined;
  Sheets: undefined;
  Track: undefined;
  Config: undefined;
};

export type RootStackParamList = {
  Main: NavigatorScreenParams<MainTabParamList> | undefined;
  Sheet: { sheetId: number; sheetName: string };
  Exercise: {
    exerciseId: number;
    exerciseName: string;
    gifUrl: string | null;
  };
  SearchExercise: { sheetId: number };
};

export type RootTabRoute = keyof MainTabParamList;
