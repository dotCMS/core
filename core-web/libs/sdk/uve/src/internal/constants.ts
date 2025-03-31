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
 * End class
 *
 * @internal
 */
export const END_CLASS = 'col-end-';

/**
 * Start class
 *
 * @internal
 */
export const START_CLASS = 'col-start-';

/**
 * Empty container style
 *
 * @internal
 */
export const EMPTY_CONTAINER_STYLE = {
    width: '100%',
    backgroundColor: '#ECF0FD',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    color: '#030E32',
    height: '10rem'
};
