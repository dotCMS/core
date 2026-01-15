import { UVEEventHandler, UVEEventType } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

import {
    findDotCMSElement,
    findDotCMSVTLData,
    getClosestDotCMSContainerData,
    getDotCMSPageBounds
} from '../lib/dom/dom.utils';

/**
 * Subscribes to content changes in the UVE editor
 *
 * @param {UVEEventHandler} callback - Function to be called when content changes are detected
 * @returns {Object} Object containing unsubscribe function and event type
 * @returns {Function} .unsubscribe - Function to remove the event listener
 * @returns {UVEEventType} .event - The event type being subscribed to
 * @internal
 */
export function onContentChanges(callback: UVEEventHandler) {
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
}

/**
 * Subscribes to page reload events in the UVE editor
 *
 * @param {UVEEventHandler} callback - Function to be called when page reload is triggered
 * @returns {Object} Object containing unsubscribe function and event type
 * @returns {Function} .unsubscribe - Function to remove the event listener
 * @returns {UVEEventType} .event - The event type being subscribed to
 * @internal
 */
export function onPageReload(callback: UVEEventHandler) {
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
}

/**
 * Subscribes to request bounds events in the UVE editor
 *
 * @param {UVEEventHandler} callback - Function to be called when bounds are requested
 * @returns {Object} Object containing unsubscribe function and event type
 * @returns {Function} .unsubscribe - Function to remove the event listener
 * @returns {UVEEventType} .event - The event type being subscribed to
 * @internal
 */
export function onRequestBounds(callback: UVEEventHandler) {
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
}

/**
 * Subscribes to iframe scroll events in the UVE editor
 *
 * @param {UVEEventHandler} callback - Function to be called when iframe scroll occurs
 * @returns {Object} Object containing unsubscribe function and event type
 * @returns {Function} .unsubscribe - Function to remove the event listener
 * @returns {UVEEventType} .event - The event type being subscribed to
 * @internal
 */
export function onIframeScroll(callback: UVEEventHandler) {
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

/**
 * Subscribes to contentlet hover events in the UVE editor
 *
 * @param {UVEEventHandler} callback - Function to be called when a contentlet is hovered
 * @returns {Object} Object containing unsubscribe function and event type
 * @returns {Function} .unsubscribe - Function to remove the event listener
 * @returns {UVEEventType} .event - The event type being subscribed to
 * @internal
 */
export function onContentletHovered(callback: UVEEventHandler) {
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
            onNumberOfPages: foundElement.dataset?.['dotOnNumberOfPages'],
            ...(foundElement.dataset?.['dotStyleProperties'] && {
                styleProperties: JSON.parse(foundElement.dataset['dotStyleProperties'])
            })
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
