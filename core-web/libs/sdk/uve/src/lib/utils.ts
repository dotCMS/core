import { UVEState, UVE_MODE } from './types';

/**
 * Gets the current state of the Universal Visual Editor (UVE).
 *
 * This function checks if the code is running inside the DotCMS Universal Visual Editor
 * and returns information about its current state, including the editor mode.
 *
 * @export
 * @return {UVEState | undefined} Returns the UVE state object if running inside the editor,
 * undefined otherwise. The state includes:
 * - mode: The current editor mode (preview, edit, live)
 *
 * @example
 * ```ts
 * const editorState = getUVEState();
 * if (editorState?.mode === 'edit') {
 *   // Enable editing features
 * }
 * ```
 */
export function getUVEState(): UVEState | undefined {
    if (typeof window === 'undefined' || window.parent === window) {
        return;
    }

    const url = new URL(window.location.href);

    // TODO: Return everything from the QP
    const mode = (url.searchParams.get('editorMode') as UVE_MODE) ?? UVE_MODE.UNKNOWN;

    if (mode === UVE_MODE.UNKNOWN) {
        console.warn("Couldn't identify the current mode of UVE, please contact customer support.");
    }

    return {
        mode
    };
}
