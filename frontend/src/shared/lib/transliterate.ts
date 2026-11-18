/*
 * The official Ukrainian transliteration (Cabinet of Ministers resolution No. 55, 2010), the one used in
 * passports and on bank cards: Іван Петренко → IVAN PETRENKO, Юлія → YULIIA, Згурський → ZGHURSKYI.
 */

const LETTERS: Record<string, string> = {
  а: 'a',
  б: 'b',
  в: 'v',
  г: 'h',
  ґ: 'g',
  д: 'd',
  е: 'e',
  є: 'ie',
  ж: 'zh',
  з: 'z',
  и: 'y',
  і: 'i',
  ї: 'i',
  й: 'i',
  к: 'k',
  л: 'l',
  м: 'm',
  н: 'n',
  о: 'o',
  п: 'p',
  р: 'r',
  с: 's',
  т: 't',
  у: 'u',
  ф: 'f',
  х: 'kh',
  ц: 'ts',
  ч: 'ch',
  ш: 'sh',
  щ: 'shch',
  ь: '',
  ю: 'iu',
  я: 'ia',
  "'": '',
  ʼ: '',
  '’': '',
};

/** At the start of a word these letters are written differently: Єва → Yeva, Яна → Yana. */
const AT_WORD_START: Record<string, string> = { є: 'ye', ї: 'yi', й: 'y', ю: 'yu', я: 'ya' };

function transliterateWord(word: string): string {
  const lower = word.toLowerCase().replace(/зг/g, 'zgh');
  let result = '';
  [...lower].forEach((letter, index) => {
    if (index === 0 && letter in AT_WORD_START) {
      result += AT_WORD_START[letter];
    } else {
      result += LETTERS[letter] ?? letter;
    }
  });
  return result;
}

/** The name as it is embossed on the card: Latin capitals. */
export function cardholderName(firstName: string, lastName: string): string {
  return `${firstName} ${lastName}`.trim().split(/\s+/).map(transliterateWord).join(' ').toUpperCase();
}
