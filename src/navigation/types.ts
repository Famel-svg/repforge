export type RootTabRoute = 'Home' | 'Sheets' | 'Track';

export type RootStackParamList = {
  Home: undefined;
  Sheets: undefined;
  Track: undefined;
  Sheet: { sheetId: number; sheetName: string };
  Exercise: {
    exerciseId: number;
    exerciseName: string;
    gifUrl: string | null;
  };
  SearchExercise: { sheetId: number };
};
