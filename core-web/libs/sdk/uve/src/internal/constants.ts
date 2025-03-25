import {
    findDotCMSElement,
    findDotCMSVTLData,
    getClosestDotCMSContainerData,
    getDotCMSPageBounds
} from '../lib/dom/dom.utils';
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
                const positionData = getDotCMSPageBounds(containers);

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
    },

    [UVEEventType.CONTENTLET_HOVERED]: (callback: UVEEventHandler) => {
        const pointerMoveCallback = (event: PointerEvent) => {
            const foundElement = findDotCMSElement(event.target as HTMLElement);

            if (!foundElement) return;

            const { x, y, width, height } = foundElement.getBoundingClientRect();

            const isContainer = foundElement.dataset?.['dotObject'] === 'container';

            const contentletForEmptyContainer = {
                identifier: 'TEMP_EMPTY_CONTENTLET',
                title: 'TEMP_EMPTY_CONTENTLET',
                contentType: 'TEMP_EMPTY_CONTENTLET_TYPE',
                inode: 'TEMPY_EMPTY_CONTENTLET_INODE',
                widgetTitle: 'TEMP_EMPTY_CONTENTLET',
                baseType: 'TEMP_EMPTY_CONTENTLET',
                onNumberOfPages: 1
            };

            const contentlet = {
                identifier: foundElement.dataset?.['dotIdentifier'],
                title: foundElement.dataset?.['dotTitle'],
                inode: foundElement.dataset?.['dotInode'],
                contentType: foundElement.dataset?.['dotType'],
                baseType: foundElement.dataset?.['dotBasetype'],
                widgetTitle: foundElement.dataset?.['dotWidgetTitle'],
                onNumberOfPages: foundElement.dataset?.['dotOnNumberOfPages']
            };

            const vtlFiles = findDotCMSVTLData(foundElement);
            const contentletPayload = {
                container:
                    // Here extract dot-container from contentlet if it is Headless
                    // or search in parent container if it is VTL
                    foundElement.dataset?.['dotContainer']
                        ? JSON.parse(foundElement.dataset?.['dotContainer'])
                        : getClosestDotCMSContainerData(foundElement),
                contentlet: isContainer ? contentletForEmptyContainer : contentlet,
                vtlFiles
            };

            const contentletHoveredPayload = {
                x,
                y,
                width,
                height,
                payload: contentletPayload
            };

            callback(contentletHoveredPayload);
        };

        document.addEventListener('pointermove', pointerMoveCallback);

        return {
            unsubscribe: () => {
                document.removeEventListener('pointermove', pointerMoveCallback);
            },
            event: UVEEventType.CONTENTLET_HOVERED
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
