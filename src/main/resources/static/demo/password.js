// Password helpers shared by the demo pages: generate, show/hide, copy. Nothing here ever leaves the browser.
'use strict';

/** No look-alikes (0/O, 1/l/I): a generated password may still have to be typed in by hand once. */
const PASSWORD_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789-_!@#$%*?';

const PASSWORD_LENGTH = 20;

/**
 * A strong password from the browser's cryptographic generator (Math.random is predictable, never use it for
 * secrets). Bytes that would make some characters more likely than others are thrown away (no modulo bias).
 */
function generatePassword(length = PASSWORD_LENGTH) {
    const limit = 256 - (256 % PASSWORD_ALPHABET.length);
    let password = '';
    while (password.length < length) {
        for (const byte of crypto.getRandomValues(new Uint8Array(length * 2))) {
            if (byte < limit && password.length < length) {
                password += PASSWORD_ALPHABET[byte % PASSWORD_ALPHABET.length];
            }
        }
    }
    return password;
}

/** Puts a fresh password into the field and shows it, so the user sees what they are about to save. */
function fillGeneratedPassword(inputId) {
    const input = document.getElementById(inputId);
    input.value = generatePassword();
    input.type = 'text';
    input.dispatchEvent(new Event('input', { bubbles: true }));
}

function togglePasswordVisibility(inputId) {
    const input = document.getElementById(inputId);
    input.type = input.type === 'password' ? 'text' : 'password';
}

async function copyPassword(inputId, onDone) {
    const input = document.getElementById(inputId);
    try {
        await navigator.clipboard.writeText(input.value);
        onDone?.(typeof t === 'function' ? t('password.copied') : 'Password copied');
    } catch {
        input.type = 'text';
        input.select();
        onDone?.(typeof t === 'function' ? t('password.pressCtrlC') : 'Press Ctrl+C to copy');
    }
}
