import type { StatementEntry } from '../api/accountsApi';
import { groupByKyivDay } from './statementDays';

function entry(transactionId: string, createdAt: string): StatementEntry {
  return {
    transactionId,
    type: 'DEPOSIT',
    direction: 'CREDIT',
    amount: 100,
    balanceAfter: 100,
    description: null,
    createdAt,
  };
}

describe('groupByKyivDay', () => {
  it('keeps the order of the statement and starts a new day at Kyiv midnight', () => {
    const days = groupByKyivDay([
      // 22:30 UTC on the 17th is 00:30 on the 18th in Kyiv (UTC+2).
      entry('t3', '2026-11-17T22:30:00Z'),
      entry('t2', '2026-11-17T21:30:00Z'),
      entry('t1', '2026-11-17T08:00:00Z'),
    ]);

    expect(days.map((day) => day.key)).toEqual(['2026-11-18', '2026-11-17']);
    expect(days[1]?.entries.map((item) => item.transactionId)).toEqual(['t2', 't1']);
  });

  it('has no days for an empty statement', () => {
    expect(groupByKyivDay([])).toEqual([]);
  });
});
