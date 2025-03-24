import { __UVE_EVENTS__, __UVE_EVENT_ERROR_FALLBACK__ } from '../../internal/constants';
import {
    UVE_MODE,
    UVEEventHandler,
    UVEState,
    UVEEventSubscription,
    UVEEventType,
    UVEEventPayloadMap
} from '../types/editor/public';

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

/**
 * Creates a subscription to a UVE event.
 *
 * This function allows subscribing to various UVE events like editor messages, content changes,
 * and contentlet hover events. It checks if the code is running inside the UVE and if the
 * requested event exists before creating the subscription.
 *
 * @export
 * @param {string} eventType - The event to subscribe to (e.g. 'editor-messages', 'changes', 'contentlet-hover')
 * @param {UVEEventHandler} [callback=() => ({})] - The callback function to execute when the event occurs
 * @return {UVEEventSubscription} An object containing the unsubscribe function and event name
 * @example
 * ```ts
 * // Subscribe with callback
 * const subscription = createUVESubscription('changes', (payload) => {
 *   console.log('Page data changed:', payload);
 * });
 *
 * // Subscribe without callback
 * const editorSubscription = createUVESubscription('editor-messages');
 *
 * // Later when done
 * subscription.unsubscribe();
 * editorSubscription.unsubscribe();
 * ```
 */
export function createUVESubscription<T extends UVEEventType>(
    eventType: T,
    callback: (
        payload: UVEEventPayloadMap[T] extends undefined ? void : UVEEventPayloadMap[T]
    ) => void
): UVEEventSubscription {
    if (!getUVEState()) {
        console.warn('UVE Subscription: Not running inside UVE');

        return __UVE_EVENT_ERROR_FALLBACK__(eventType);
    }

    const eventCallback = __UVE_EVENTS__[eventType];

    if (!eventCallback) {
        console.error(`UVE Subscription: Event ${eventType} not found`);

        return __UVE_EVENT_ERROR_FALLBACK__(eventType);
    }

    return eventCallback(callback as UVEEventHandler);
}
