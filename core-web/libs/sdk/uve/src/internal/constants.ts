import {
    onContentChanges,
    onContentletHovered,
    onIframeScroll,
    onPageReload,
    onRequestBounds
} from './events';

import { UVEEventHandler, UVEEventSubscriber, UVEEventType } from '../lib/types/editor/public';

/**
 * Events that can be subscribed to in the UVE
 *
 * @internal
 * @type {Record<UVEEventType, UVEEventSubscriber>}
 */
export const __UVE_EVENTS__: Record<UVEEventType, UVEEventSubscriber> = {
    [UVEEventType.CONTENT_CHANGES]: (callback: UVEEventHandler) => {
        return onContentChanges(callback);
    },
    [UVEEventType.PAGE_RELOAD]: (callback: UVEEventHandler) => {
        return onPageReload(callback);
    },

    [UVEEventType.REQUEST_BOUNDS]: (callback: UVEEventHandler) => {
        return onRequestBounds(callback);
    },

    [UVEEventType.IFRAME_SCROLL]: (callback: UVEEventHandler) => {
        return onIframeScroll(callback);
    },

    [UVEEventType.CONTENTLET_HOVERED]: (callback: UVEEventHandler) => {
        return onContentletHovered(callback);
    }
};

/**
 * Default UVE event
 *
 * @param {string} event - The event to subscribe to.
 * @internal
 */
export const __UVE_EVENT_ERROR_FALLBACK__ = (event: string) => {
    return {
        unsubscribe: () => {
            /* do nothing */
        },
        event
    };
};

/**
 * Development mode
 *
 * @internal
 */
export const DEVELOPMENT_MODE = 'development';

/**
 * Production mode
 *
 * @internal
 */
export const PRODUCTION_MODE = 'production';

/**
 * End class map
 *
 * @internal
 */
export const END_CLASS_MAP: Record<number, string> = {
    1: 'col-end-1',
    2: 'col-end-2',
    3: 'col-end-3',
    4: 'col-end-4',
    5: 'col-end-5',
    6: 'col-end-6',
    7: 'col-end-7',
    8: 'col-end-8',
    9: 'col-end-9',
    10: 'col-end-10',
    11: 'col-end-11',
    12: 'col-end-12',
    13: 'col-end-13'
};

/**
 * Start class map
 *
 * @internal
 */
export const START_CLASS_MAP: Record<number, string> = {
    1: 'col-start-1',
    2: 'col-start-2',
    3: 'col-start-3',
    4: 'col-start-4',
    5: 'col-start-5',
    6: 'col-start-6',
    7: 'col-start-7',
    8: 'col-start-8',
    9: 'col-start-9',
    10: 'col-start-10',
    11: 'col-start-11',
    12: 'col-start-12'
};
