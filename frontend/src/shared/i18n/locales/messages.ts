import type { uk } from './uk';

/** The shape of the Ukrainian texts with any wording: every other language must have exactly the same keys. */
type Shape<T> = { [K in keyof T]: T[K] extends string ? string : Shape<T[K]> };

export type Messages = Shape<typeof uk>;
