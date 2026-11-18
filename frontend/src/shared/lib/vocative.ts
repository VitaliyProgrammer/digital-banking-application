/**
 * The name in the vocative case for addressing the client in Ukrainian: Іван → Іване, Олена → Олено.
 * The declension library is loaded only when needed, so it does not weigh on the first page load.
 * A name it cannot decline (e.g. written in Latin letters) is returned as it is.
 */
export async function toVocative(givenName: string): Promise<string> {
  try {
    const { detectGender, inVocative } = await import('shevchenko');
    const gender = await detectGender({ givenName });
    if (!gender) {
      return givenName;
    }
    const { givenName: vocative } = await inVocative({ gender, givenName });
    return vocative || givenName;
  } catch {
    return givenName;
  }
}
