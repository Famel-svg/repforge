export type RootStackParamList = {
  Home: undefined;
  Sheet: { sheetId: number; sheetName: string };
  Exercise: {
    exerciseId: number;
    exerciseName: string;
    gifUrl: string | null;
  };
  SearchExercise: { sheetId: number };
};
