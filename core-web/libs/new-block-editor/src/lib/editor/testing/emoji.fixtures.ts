/**
 * Shared character sample for the emoji-conversion specs (#37340, AC-002).
 *
 * The defect spans 1907 of the 1949 characters catalogued by `@tiptap/extension-emoji`, so
 * asserting three literals would not cover the class. Asserting all 1907 is slow and breaks on
 * every extension upgrade. The spec settled on a REPRESENTATIVE sample — see
 * `specs/37340-emoji-text-node/spec.md` "Resolved Decisions" #3.
 */

/**
 * Characters Unicode classifies as emoji but which have NO `Emoji_Presentation` property, so
 * they render as ordinary typography. Authors type these as punctuation and have no reason to
 * expect them to become emoji. 219 exist; these are the ones that turn up in real copy.
 */
export const TEXT_PRESENTATION_SAMPLE = [
    '©', // © copyright   — reported by the customer
    '®', // ® registered  — reported by the customer
    '™', // ™ tm          — reported by the customer
    '‼', // ‼ bangbang
    '⁉', // ⁉ interrobang
    '✔', // ✔ heavy_check_mark
    '✖', // ✖ heavy_multiplication_x
    '✂', // ✂ scissors
    '✏', // ✏ pencil2
    '⚠', // ⚠ warning
    'ℹ', // ℹ information_source
    '♻', // ♻ recycle
    '▪', // ▪ black_small_square
    '↔', // ↔ left_right_arrow
    '➡', // ➡ arrow_right
    '♀', // ♀ female_sign
    '✝', // ✝ latin_cross
    '☯', // ☯ yin_yang
    '⚙', // ⚙ gear
    '✉', // ✉ envelope
    '〰', // 〰 wavy_dash
    '✳', // ✳ eight_spoked_asterisk
    '♾', // ♾ infinity
    '⚕' // ⚕ medical_symbol
] as const;

/** Ordinary pictographic emoji — these must behave identically to the symbols above. */
export const PICTOGRAPHIC_SAMPLE = ['🚀', '🙂', '🎉'] as const;

/**
 * Multi-codepoint cases. These exercise the paths where a naive per-character fix would split a
 * grapheme: a variation selector, a ZWJ sequence, and a regional-indicator flag pair.
 */
export const MULTI_CODEPOINT_SAMPLE = [
    '©️', // ©️  copyright + VS16
    '❤️', // ❤️  heart + VS16
    '👨‍👩‍👧‍👦', // 👨‍👩‍👧‍👦 ZWJ family sequence
    '🇺🇸' // regional-indicator flag pair
] as const;

/** Every character the AC-002 assertions run over. */
export const EMOJI_CHARACTER_SAMPLE = [
    ...TEXT_PRESENTATION_SAMPLE,
    ...PICTOGRAPHIC_SAMPLE,
    ...MULTI_CODEPOINT_SAMPLE
] as const;

/**
 * Characters verified NOT to be in the emoji set — they must be unaffected either way, and
 * guard against an over-broad fix that starts touching ordinary punctuation.
 */
export const NON_EMOJI_CONTROL_SAMPLE = [
    '½', // ½ vulgar fraction
    '—', // — em dash
    'é', // é e-acute
    '→' // → rightwards arrow (NOT the emoji ➡)
] as const;
