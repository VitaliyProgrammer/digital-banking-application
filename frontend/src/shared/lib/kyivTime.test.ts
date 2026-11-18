import { formatLongDate, isEvening, kyivDaysBetween } from './kyivTime';

describe('Kyiv time', () => {
  it('turns the greeting to evening at 17:00 in Kyiv, whatever the time zone of the computer', () => {
    // 14:59 and 15:00 UTC are 16:59 and 17:00 in Kyiv in winter (UTC+2).
    expect(isEvening(new Date('2026-11-18T14:59:00Z'))).toBe(false);
    expect(isEvening(new Date('2026-11-18T15:00:00Z'))).toBe(true);
  });

  it('writes the date the way the approved mockup does', () => {
    expect(formatLongDate(new Date('2026-11-17T10:00:00Z'), 'uk')).toBe('Вівторок, 17 листопада');
    expect(formatLongDate(new Date('2026-11-17T10:00:00Z'), 'en')).toBe('Tuesday 17 November');
  });

  it('counts days by the Kyiv calendar: 23:30 UTC is already tomorrow in Kyiv', () => {
    const evening = new Date('2026-11-17T20:00:00Z');
    const afterKyivMidnight = new Date('2026-11-17T23:30:00Z');

    expect(kyivDaysBetween(evening, afterKyivMidnight)).toBe(1);
  });
});
