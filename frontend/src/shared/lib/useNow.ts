import { useEffect, useState } from 'react';

const ONE_MINUTE = 60_000;

/** The current time, updated every minute: enough for a greeting that changes at 17:00 and a date at midnight. */
export function useNow(): Date {
  const [now, setNow] = useState(() => new Date());

  useEffect(() => {
    const timer = setInterval(() => setNow(new Date()), ONE_MINUTE);
    return () => clearInterval(timer);
  }, []);

  return now;
}
