import { getPageElementBound } from '../lib/editor/internal';
import { UVEEventHandler, UVEEventSubscriber, UVEEventType } from '../lib/types/editor/public';
import { __DOTCMS_UVE_EVENT__ } from '../lib/types/events/internal';

/**
 * Events that can be subscribed to in the UVE
 *
 * @internal
 * @type {Record<UVEEventType, UVEEventSubscriber>}
 */
export const __UVE_EVENTS__: Record<UVEEventType, UVEEventSubscriber> = {
    [UVEEventType.CONTENT_CHANGES]: (callback: UVEEventHandler) => {
        const messageCallback = (event: MessageEvent) => {
            if (event.data.name === __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA) {
                callback(event.data.payload);
            }
        };

        window.addEventListener('message', messageCallback);

        return {
            unsubscribe: () => {
                window.removeEventListener('message', messageCallback);
            },
            event: UVEEventType.CONTENT_CHANGES
        };
    },
    [UVEEventType.PAGE_RELOAD]: (callback: UVEEventHandler) => {
        const messageCallback = (event: MessageEvent) => {
            if (event.data.name === __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE) {
                callback();
            }
        };

        window.addEventListener('message', messageCallback);

        return {
            unsubscribe: () => {
                window.removeEventListener('message', messageCallback);
            },
            event: UVEEventType.PAGE_RELOAD
        };
    },

    [UVEEventType.REQUEST_BOUNDS]: (callback: UVEEventHandler) => {
        const messageCallback = (event: MessageEvent) => {
            if (event.data.name === __DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS) {
                const containers = Array.from(
                    document.querySelectorAll('[data-dot-object="container"]')
                ) as HTMLDivElement[];
                const positionData = getPageElementBound(containers);

                callback(positionData);
            }
        };

        window.addEventListener('message', messageCallback);

        return {
            unsubscribe: () => {
                window.removeEventListener('message', messageCallback);
            },
            event: UVEEventType.REQUEST_BOUNDS
        };
    },

    [UVEEventType.IFRAME_SCROLL]: (callback: UVEEventHandler) => {
        const messageCallback = (event: MessageEvent) => {
            if (event.data.name === __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME) {
                const direction = event.data.direction;

                callback(direction);
            }
        };

        window.addEventListener('message', messageCallback);

        return {
            unsubscribe: () => {
                window.removeEventListener('message', messageCallback);
            },
            event: UVEEventType.IFRAME_SCROLL
        };
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
