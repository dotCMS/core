import { UVEEventHandler, UVEEventType } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

import { DOT_SECTION_ID_PREFIX } from './constants';

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
 * Listens for scroll-to-section requests from the UVE editor.
 *
 * Queries `#dot-section-{n}` first, then falls back to `#section-{n}`.
 * If the element is found, calls the callback with `{ sectionIndex, offsetTop }`.
 * If not found, the callback is not invoked.
 *
 * @param {UVEEventHandler} callback - Receives `{ sectionIndex: number; offsetTop: number }`.
 * @internal
 */
export function onScrollToSection(callback: UVEEventHandler) {
    const messageCallback = (event: MessageEvent) => {
        if (event.data.name !== __DOTCMS_UVE_EVENT__.UVE_SCROLL_TO_SECTION) {
            return;
        }

        const sectionIndex: number = event.data.sectionIndex;
        const el = (document.querySelector(`#${DOT_SECTION_ID_PREFIX}${sectionIndex}`) ??
            document.querySelector(`#section-${sectionIndex}`)) as HTMLElement | null;

        if (!el) {
            return;
        }

        callback({ sectionIndex, offsetTop: el.offsetTop });
    };

    window.addEventListener('message', messageCallback);

    return {
        unsubscribe: () => {
            window.removeEventListener('message', messageCallback);
        },
        event: UVEEventType.SCROLL_TO_SECTION
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
                dotStyleProperties: JSON.parse(foundElement.dataset['dotStyleProperties'])
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

/**
 * Subscribes to contentlet click events in the UVE editor.
 *
 * The editor's hover overlay is `pointer-events: none` so wheel events pass
 * through to the iframe. We detect the user's selection click here instead and
 * post it back to the editor.
 *
 * @param {UVEEventHandler} callback - Function to be called when a contentlet is clicked
 * @returns {Object} Object containing unsubscribe function and event type
 * @internal
 */
export function onContentletClicked(callback: UVEEventHandler) {
    // Track the last selected contentlet so a second click on the same one
    // lets the page's native click through (links, accordions, etc.). The
    // first click is "select"; subsequent clicks on the selected contentlet
    // are "interact with the page".
    let lastSelectedInode: string | undefined;

    const clickCallback = (event: MouseEvent) => {
        const foundElement = findDotCMSElement(event.target as HTMLElement);

        if (!foundElement) return;

        const isContainer = foundElement.dataset?.['dotObject'] === 'container';
        // Only emit for contentlet clicks; an empty container click is a no-op
        // for selection purposes (there's nothing to select).
        if (isContainer) return;

        const inode = foundElement.dataset?.['dotInode'];

        // If the user is clicking the already-selected contentlet, let the
        // page handle the click natively (link navigation, button handlers,
        // form submission). The editor selection toolbar already exposes the
        // edit/delete/etc actions; the contentlet's own UI should still work.
        if (inode && inode === lastSelectedInode) {
            return;
        }

        // First click on this contentlet (or a different one) — select it in
        // the editor and block the page's natural click. Capture phase +
        // preventDefault + stopPropagation suppresses both the default action
        // and any subscribers further down the tree.
        event.preventDefault();
        event.stopPropagation();
        lastSelectedInode = inode;

        const { x, y, width, height } = foundElement.getBoundingClientRect();

        const contentlet = {
            identifier: foundElement.dataset?.['dotIdentifier'],
            title: foundElement.dataset?.['dotTitle'],
            inode: foundElement.dataset?.['dotInode'],
            contentType: foundElement.dataset?.['dotType'],
            baseType: foundElement.dataset?.['dotBasetype'],
            widgetTitle: foundElement.dataset?.['dotWidgetTitle'],
            onNumberOfPages: foundElement.dataset?.['dotOnNumberOfPages'],
            ...(foundElement.dataset?.['dotStyleProperties'] && {
                dotStyleProperties: JSON.parse(foundElement.dataset['dotStyleProperties'])
            })
        };

        const vtlFiles = findDotCMSVTLData(foundElement);

        callback({
            x,
            y,
            width,
            height,
            payload: {
                container: foundElement.dataset?.['dotContainer']
                    ? JSON.parse(foundElement.dataset?.['dotContainer'])
                    : getClosestDotCMSContainerData(foundElement),
                contentlet,
                vtlFiles
            }
        });
    };

    // Capture phase so we run BEFORE the page's own click handlers and can
    // preventDefault/stopPropagation effectively.
    document.addEventListener('click', clickCallback, { capture: true });

    return {
        unsubscribe: () => {
            document.removeEventListener('click', clickCallback, { capture: true });
        },
        event: UVEEventType.CONTENTLET_CLICKED
    };
}
