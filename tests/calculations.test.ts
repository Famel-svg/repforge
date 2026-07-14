import {
  calculateVolume,
  detectPersonalRecords,
  estimateOneRepMax,
} from '@/utils/calculations';
import { describe, expect, it } from 'vitest';

describe('cálculos de treino', () => {
  it('calcula volume como séries vezes repetições vezes carga', () => {
    expect(calculateVolume({ sets: 4, reps: 8, weightKg: 32.5 })).toBe(1040);
  });

  it('estima 1RM usando fórmula de Epley', () => {
    expect(estimateOneRepMax({ reps: 6, weightKg: 100 })).toBeCloseTo(120);
  });

  it('detecta PR por carga', () => {
    expect(
      detectPersonalRecords(
        { sets: 3, reps: 8, weightKg: 90 },
        [
          { sets: 3, reps: 10, weightKg: 80 },
          { sets: 4, reps: 8, weightKg: 85 },
        ],
      ),
    ).toMatchObject({ weight: true });
  });

  it('detecta PR por 1RM estimado mesmo sem maior carga', () => {
    expect(
      detectPersonalRecords(
        { sets: 3, reps: 12, weightKg: 80 },
        [{ sets: 3, reps: 6, weightKg: 90 }],
      ),
    ).toEqual({ weight: false, oneRepMax: true });
  });

  it('não marca PR sem histórico anterior ou em empate', () => {
    expect(detectPersonalRecords({ sets: 3, reps: 8, weightKg: 80 }, [])).toEqual({
      weight: false,
      oneRepMax: false,
    });
    expect(
      detectPersonalRecords(
        { sets: 3, reps: 8, weightKg: 80 },
        [{ sets: 3, reps: 8, weightKg: 80 }],
      ),
    ).toEqual({ weight: false, oneRepMax: false });
  });
});
