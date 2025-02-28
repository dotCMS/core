import { UVE_MODE, UVEState } from './types';

/**
 * Gets the current state of the Universal Visual Editor (UVE).
 *
 * This function checks if the code is running inside the DotCMS Universal Visual Editor
 * and returns information about its current state, including the editor mode.
 *
 * @export
 * @return {UVEState | undefined} Returns the UVE state object if running inside the editor,
 * undefined otherwise.
 *
 * The state includes:
 * - mode: The current editor mode (preview, edit, live)
 * - languageId: The language ID of the current page setted on the UVE
 * - persona: The persona of the current page setted on the UVE
 * - variantName: The name of the current variant
 * - experimentId: The ID of the current experiment
 * - publishDate: The publish date of the current page setted on the UVE
 *
 * @note The absence of any of these properties means that the value is the default one.
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
    if (typeof window === 'undefined' || window.parent === window || !window.location) {
        return;
    }

    const url = new URL(window.location.href);

    const possibleModes = Object.values(UVE_MODE);

    let mode = (url.searchParams.get('mode') as UVE_MODE) ?? UVE_MODE.EDIT;
    const languageId = url.searchParams.get('language_id');
    const persona = url.searchParams.get('personaId');
    const variantName = url.searchParams.get('variantName');
    const experimentId = url.searchParams.get('experimentId');
    const publishDate = url.searchParams.get('publishDate');

    if (!possibleModes.includes(mode)) {
        mode = UVE_MODE.EDIT;
    }

    return {
        mode,
        languageId,
        persona,
        variantName,
        experimentId,
        publishDate
    };
}
