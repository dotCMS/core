/**
 * Checks if the code is running inside the DotCMS Universal Visual Editor (UVE).
 *
 * @returns {boolean} Returns true if running inside the editor, false otherwise.
 *
 * @example
 * ```ts
 * if (isInsideUVE()) {
 *   // Running inside UVE editor
 *   console.log('Inside editor');
 * } else {
 *   // Running in normal mode
 *   console.log('Outside editor');
 * }
 * ```
 */
export function isInsideUVE(): boolean {
    if (typeof window === 'undefined' || window.parent === window || !window.location) {
        return false;
    }

    return true;
}
