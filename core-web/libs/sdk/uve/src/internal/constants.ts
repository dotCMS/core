import {
    findDotElement,
    findVTLData,
    getClosestContainerData,
    scrollIsInBottom,
    setBounds
} from '../lib/editor/internal';
import { sendMessageToEditor } from '../lib/editor/public';
import {
    DotCMSUVEAction,
    UVECallback,
    UVEEvent,
    UVESubscriptionEvent
} from '../lib/types/editor/public';
import { __DOTCMS_UVE_EVENT__ } from '../lib/types/events/internal';

// TODO: WE NEED TO LOOK FOR ALL THE NOTIFY_CLIENT EVENTS AND ADD THEM TO THE UVE_EVENTS CONSTANT WHEN WE MIGRATE THE EDITOR TO THE NEW UVE LIBRARY

/**
 * Events that can be subscribed to in the UVE
 *
 * @internal
 * @type {Record<string, UVEEvent>}
 */
export const __UVE_EVENTS__: Record<string, UVEEvent> = {
    [UVESubscriptionEvent.CHANGES]: (callback: UVECallback) => {
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
            event: UVESubscriptionEvent.CHANGES
        };
    },
    [UVESubscriptionEvent.EDITOR_MESSAGES]: () => {
        const messageCallback = (
            event: MessageEvent<{ name: __DOTCMS_UVE_EVENT__; direction: 'up' | 'down' }>
        ) => {
            const ACTIONS_NOTIFICATION: { [K in __DOTCMS_UVE_EVENT__]?: () => void } = {
                [__DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE]: () => {
                    window.location.reload();
                },
                [__DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS]: () => {
                    setBounds();
                },
                [__DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME]: () => {
                    const direction = event.data.direction;

                    if (
                        (window.scrollY === 0 && direction === 'up') ||
                        (scrollIsInBottom() && direction === 'down')
                    ) {
                        // If the iframe scroll is at the top or bottom, do not send anything.
                        // This avoids losing the scrollend event.
                        return;
                    }

                    const scrollY = direction === 'up' ? -120 : 120;
                    window.scrollBy({ left: 0, top: scrollY, behavior: 'smooth' });
                }
            };

            ACTIONS_NOTIFICATION[event.data.name]?.();
        };

        window.addEventListener('message', messageCallback);

        return {
            unsubscribe: () => {
                window.removeEventListener('message', messageCallback);
            },
            event: UVESubscriptionEvent.EDITOR_MESSAGES
        };
    },
    [UVESubscriptionEvent.CONTENTLET_HOVER]: () => {
        const pointerMoveCallback = (event: PointerEvent) => {
            const foundElement = findDotElement(event.target as HTMLElement);

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

            const vtlFiles = findVTLData(foundElement);
            const contentletPayload = {
                container:
                    // Here extract dot-container from contentlet if it is Headless
                    // or search in parent container if it is VTL
                    foundElement.dataset?.['dotContainer']
                        ? JSON.parse(foundElement.dataset?.['dotContainer'])
                        : getClosestContainerData(foundElement),
                contentlet: isContainer ? contentletForEmptyContainer : contentlet,
                vtlFiles
            };

            sendMessageToEditor({
                action: DotCMSUVEAction.SET_CONTENTLET,
                payload: {
                    x,
                    y,
                    width,
                    height,
                    payload: contentletPayload
                }
            });
        };

        document.addEventListener('pointermove', pointerMoveCallback);

        return {
            unsubscribe: () => {
                document.removeEventListener('pointermove', pointerMoveCallback);
            },
            event: UVESubscriptionEvent.CONTENTLET_HOVER
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
