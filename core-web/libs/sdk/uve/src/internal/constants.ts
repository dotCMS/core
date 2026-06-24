import { UVEEventHandler, UVEEventSubscriber, UVEEventType } from '@dotcms/types';

import {
    onAutoBounds,
    onContentChanges,
    onContentletClicked,
    onContentletHovered,
    onIframeScroll,
    onPageReload,
    onScrollToSection
} from './events';

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

    [UVEEventType.IFRAME_SCROLL]: (callback: UVEEventHandler) => {
        return onIframeScroll(callback);
    },

    [UVEEventType.CONTENTLET_HOVERED]: (callback: UVEEventHandler) => {
        return onContentletHovered(callback);
    },

    [UVEEventType.CONTENTLET_CLICKED]: (callback: UVEEventHandler) => {
        return onContentletClicked(callback);
    },

    [UVEEventType.SCROLL_TO_SECTION]: (callback: UVEEventHandler) => {
        return onScrollToSection(callback);
    },

    // SELECTION_CLEARED is editor→SDK only. No public subscriber surface;
    // onContentletClicked listens for the underlying postMessage internally
    // to reset its lastSelectedInode tracker.
    [UVEEventType.SELECTION_CLEARED]: (_callback: UVEEventHandler) => {
        return {
            unsubscribe: () => {
                /* no-op: SELECTION_CLEARED has no consumer-facing subscription */
            },
            event: UVEEventType.SELECTION_CLEARED
        };
    },

    [UVEEventType.AUTO_BOUNDS]: (callback: UVEEventHandler) => {
        return onAutoBounds(callback);
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
 * Empty container style for React
 *
 * @internal
 */
export const EMPTY_CONTAINER_STYLE_REACT = {
    width: '100%',
    backgroundColor: '#ECF0FD',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    color: '#030E32',
    height: '10rem'
};

/**
 * Empty container style for Angular
 *
 * @internal
 */
export const EMPTY_CONTAINER_STYLE_ANGULAR = {
    width: '100%',
    'background-color': '#ECF0FD',
    display: 'flex',
    'justify-content': 'center',
    'align-items': 'center',
    color: '#030E32',
    height: '10rem'
};

/**
 * Custom no component
 *
 * @internal
 */
export const CUSTOM_NO_COMPONENT = 'CustomNoComponent';

/**
 * ID prefix applied to page section wrappers for editor scroll-to-section support.
 * Used by SDK row components and the UVE scroll event handler.
 *
 * @internal
 */
export const DOT_SECTION_ID_PREFIX = 'dot-section-';

/**
 * Window flag set by `@dotcms/analytics` when content analytics is initialized
 * and active on the page.
 *
 * @important This value is intentionally duplicated from `@dotcms/analytics`
 * (`ANALYTICS_WINDOWS_ACTIVE_KEY` in dot-analytics.constants.ts). The SDKs read
 * it in live mode to decide whether to keep the minimal contentlet attributes
 * Analytics depends on. Both constants MUST stay in sync.
 *
 * @internal
 */
export const ANALYTICS_ACTIVE_WINDOW_KEY = '__dotAnalyticsActive__';

/**
 * Event dispatched by `@dotcms/analytics` once analytics is ready. The SDKs
 * listen for it so live-mode contentlets can re-render with the attributes
 * Analytics needs, regardless of initialization order.
 *
 * @important Kept in sync with the `dotcms:analytics:ready` event dispatched by
 * `@dotcms/analytics` (initializeContentAnalytics).
 *
 * @internal
 */
export const ANALYTICS_READY_EVENT = 'dotcms:analytics:ready';
