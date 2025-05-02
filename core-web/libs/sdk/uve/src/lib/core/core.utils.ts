import {
    UVE_MODE,
    UVEState,
    UVEEventSubscription,
    UVEEventType,
    UVEEventPayloadMap,
    UVEEventHandler
} from '@dotcms/types';

import { __UVE_EVENTS__, __UVE_EVENT_ERROR_FALLBACK__ } from '../../internal/constants';

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
        return undefined;
    }

    const url = new URL(window.location.href);

    const possibleModes = Object.values(UVE_MODE);

    let mode = (url.searchParams.get('mode') as UVE_MODE) ?? UVE_MODE.EDIT;
    const languageId = url.searchParams.get('language_id');
    const persona = url.searchParams.get('personaId');
    const variantName = url.searchParams.get('variantName');
    const experimentId = url.searchParams.get('experimentId');
    const publishDate = url.searchParams.get('publishDate');
    const dotCMSHost = url.searchParams.get('dotCMSHost');

    if (!possibleModes.includes(mode)) {
        mode = UVE_MODE.EDIT;
    }

    return {
        mode,
        languageId,
        persona,
        variantName,
        experimentId,
        publishDate,
        dotCMSHost
    };
}

/**
 * Creates a subscription to a UVE event.
 *
 * @param eventType - The type of event to subscribe to
 * @param callback - The callback function that will be called when the event occurs
 * @returns An event subscription that can be used to unsubscribe
 *
 * @example
 * ```ts
 * // Subscribe to page changes
 * const subscription = createUVESubscription(UVEEventType.CONTENT_CHANGES, (changes) => {
 *   console.log('Content changes:', changes);
 * });
 *
 * // Later, unsubscribe when no longer needed
 * subscription.unsubscribe();
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
