import { clampRestSeconds, formatRestTime } from '@/utils/timer';
import { describe, expect, it } from 'vitest';

describe('timer de descanso', () => {
  it('formata segundos como mm:ss', () => {
    expect(formatRestTime(90)).toBe('1:30');
    expect(formatRestTime(5)).toBe('0:05');
    expect(formatRestTime(-10)).toBe('0:00');
  });

  it('limita o tempo configurável', () => {
    expect(clampRestSeconds(0)).toBe(15);
    expect(clampRestSeconds(999)).toBe(300);
    expect(clampRestSeconds(Number.NaN)).toBe(90);
  });
});
