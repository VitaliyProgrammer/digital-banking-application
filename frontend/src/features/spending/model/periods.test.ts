import { monthOptions, periodRange } from './periods';

describe('spending periods', () => {
  it('offers the current month first and goes back across the new year', () => {
    const months = monthOptions({ year: 2026, month: 2, day: 10 });

    expect(months).toHaveLength(12);
    expect(months[0]).toEqual({ kind: 'month', year: 2026, month: 2 });
    expect(months[2]).toEqual({ kind: 'month', year: 2025, month: 12 });
    expect(months[11]).toEqual({ kind: 'month', year: 2025, month: 3 });
  });

  it('asks for whole months, including the 29th of February in a leap year', () => {
    expect(periodRange({ kind: 'month', year: 2028, month: 2 })).toEqual({
      from: '2028-02-01',
      to: '2028-02-29',
    });
    expect(periodRange({ kind: 'year', year: 2026 })).toEqual({ from: '2026-01-01', to: '2026-12-31' });
  });
});
