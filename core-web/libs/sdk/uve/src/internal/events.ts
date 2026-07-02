import { UVEEventHandler, UVEEventType } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

import { DOT_SECTION_ID_PREFIX } from './constants';
import { TEMP_EMPTY_CONTENTLET, TEMP_EMPTY_CONTENTLET_TYPE } from './contentlet-sentinel.constants';

import {
    findDotCMSElement,
    findDotCMSVTLData,
    getClosestDotCMSContainerData,
    getDotCMSPageBounds,
    getNativeEventBinder,
    readContentletDataset
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

    // Native binder so this parent→iframe receiver survives the iframe's
    // document.open()/write()/close() rewrites under Zone.js. See getNativeEventBinder.
    const nativeWindow = getNativeEventBinder(window);
    nativeWindow.addEventListener('message', messageCallback);

    return {
        unsubscribe: () => {
            nativeWindow.removeEventListener('message', messageCallback);
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

    // Native binder so this parent→iframe receiver survives the iframe's
    // document.open()/write()/close() rewrites under Zone.js. See getNativeEventBinder.
    const nativeWindow = getNativeEventBinder(window);
    nativeWindow.addEventListener('message', messageCallback);

    return {
        unsubscribe: () => {
            nativeWindow.removeEventListener('message', messageCallback);
        },
        event: UVEEventType.PAGE_RELOAD
    };
}

const AUTO_BOUNDS_DEBOUNCE_MS = 100;

/**
 * The single bounds-sync channel. Observes the iframe document and
 * every `[data-dot-object="container"]` with a single ResizeObserver,
 * debounces the trailing edge by {@link AUTO_BOUNDS_DEBOUNCE_MS}ms, and
 * emits the full `getDotCMSPageBounds(...)` payload whenever the layout
 * settles. Also listens on `scroll` (since scrolling moves contentlets
 * without changing layout) and on `UVE_FLUSH_BOUNDS` (the editor's
 * "give me bounds NOW, skip the debounce" message used during drag).
 *
 * Re-runs `querySelectorAll` and the observer wiring whenever a
 * MutationObserver detects child changes that touch container nodes,
 * so containers that mount/unmount after page-load are picked up
 * automatically.
 *
 * @internal
 */
export function onAutoBounds(callback: UVEEventHandler) {
    let debounceTimer: ReturnType<typeof setTimeout> | null = null;
    let observed: HTMLDivElement[] = [];

    const emit = () => {
        const containers = Array.from(
            document.querySelectorAll('[data-dot-object="container"]')
        ) as HTMLDivElement[];
        callback(getDotCMSPageBounds(containers));
    };

    const scheduleEmit = () => {
        if (debounceTimer !== null) {
            clearTimeout(debounceTimer);
        }
        debounceTimer = setTimeout(() => {
            debounceTimer = null;
            emit();
        }, AUTO_BOUNDS_DEBOUNCE_MS);
    };

    const resizeObserver = new ResizeObserver(() => {
        scheduleEmit();
    });

    const observeAll = () => {
        // Tear down previous observations before re-wiring.
        for (const el of observed) {
            resizeObserver.unobserve(el);
        }
        observed = Array.from(
            document.querySelectorAll('[data-dot-object="container"]')
        ) as HTMLDivElement[];
        resizeObserver.observe(document.documentElement);
        for (const container of observed) {
            resizeObserver.observe(container);
        }
    };

    observeAll();

    // Containers can mount/unmount after the page first paints (route
    // changes in headless apps, lazy-loaded sections, etc.). Re-wire only
    // when a node carrying [data-dot-object="container"] is added or
    // removed — ignoring text/attribute churn keeps this observer cheap on
    // busy pages.
    const containsContainerNode = (nodes: NodeList) => {
        for (let i = 0; i < nodes.length; i++) {
            const node = nodes[i];
            if (node.nodeType !== Node.ELEMENT_NODE) {
                continue;
            }
            const el = node as Element;
            if (
                el.matches?.('[data-dot-object="container"]') ||
                el.querySelector?.('[data-dot-object="container"]')
            ) {
                return true;
            }
        }
        return false;
    };
    const mutationObserver = new MutationObserver((mutations) => {
        for (const m of mutations) {
            if (m.type !== 'childList') continue;
            if (containsContainerNode(m.addedNodes) || containsContainerNode(m.removedNodes)) {
                observeAll();
                scheduleEmit();
                return;
            }
        }
    });
    // The SDK script can run from <head> before <body> exists. Fall back to
    // <html> in that case — childList+subtree on the documentElement still
    // catches container nodes that mount once <body> arrives.
    mutationObserver.observe(document.body ?? document.documentElement, {
        childList: true,
        subtree: true
    });

    // Scrolling inside the iframe doesn't change layout, so ResizeObserver
    // doesn't fire, but every contentlet's viewport-relative position
    // (getBoundingClientRect) does change. Re-emit bounds after each
    // scroll burst settles so the editor's pinned selected overlay
    // re-anchors to the on-screen position.
    // Bind through Zone.js's native (untracked) listener so this scroll-driven
    // re-anchor survives the iframe's document.open()/write()/close() rewrites.
    // Without it, controls hide on scroll but the SET_BOUNDS that brings them
    // back never fires under Zone.js. See getNativeEventBinder for the rationale.
    const nativeWindow = getNativeEventBinder(window);
    const onScroll = () => scheduleEmit();
    nativeWindow.addEventListener('scroll', onScroll, { passive: true });

    // Flush channel: the editor occasionally needs an immediate snapshot
    // of bounds (drag enter, where the dropzone has to know container
    // rectangles before the user moves another pixel). Bypass the
    // debounce timer and emit synchronously.
    const onFlush = (event: MessageEvent) => {
        if (event?.data?.name !== __DOTCMS_UVE_EVENT__.UVE_FLUSH_BOUNDS) return;
        if (debounceTimer !== null) {
            clearTimeout(debounceTimer);
            debounceTimer = null;
        }
        emit();
    };
    // Reuse the native binder (declared above for `scroll`) so this
    // parent→iframe flush receiver survives the iframe rewrites too — this is
    // the channel that supplies the dropzone's bounds during drag-and-drop.
    nativeWindow.addEventListener('message', onFlush);

    return {
        unsubscribe: () => {
            if (debounceTimer !== null) {
                clearTimeout(debounceTimer);
                debounceTimer = null;
            }
            resizeObserver.disconnect();
            mutationObserver.disconnect();
            nativeWindow.removeEventListener('scroll', onScroll);
            nativeWindow.removeEventListener('message', onFlush);
            observed = [];
        },
        event: UVEEventType.AUTO_BOUNDS
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

    // Native binder so this parent→iframe receiver survives the iframe's
    // document.open()/write()/close() rewrites under Zone.js — this drives the
    // edge auto-scroll while dragging near the iframe top/bottom.
    const nativeWindow = getNativeEventBinder(window);
    nativeWindow.addEventListener('message', messageCallback);

    return {
        unsubscribe: () => {
            nativeWindow.removeEventListener('message', messageCallback);
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

    // Native binder so this parent→iframe receiver survives the iframe's
    // document.open()/write()/close() rewrites under Zone.js. See getNativeEventBinder.
    const nativeWindow = getNativeEventBinder(window);
    nativeWindow.addEventListener('message', messageCallback);

    return {
        unsubscribe: () => {
            nativeWindow.removeEventListener('message', messageCallback);
        },
        event: UVEEventType.SCROLL_TO_SECTION
    };
}

/**
 * Subscribes to contentlet hover events in the UVE editor.
 *
 * The callback is invoked with a payload while the pointer is over a
 * DotCMS element, and once with `null` when the pointer leaves the last
 * reported element (transitions onto dead space). The editor uses the
 * `null` signal to clear the hover overlay so it doesn't linger over
 * areas that no longer have a contentlet under the pointer.
 *
 * @param {UVEEventHandler} callback - Function to be called when hover state changes
 * @returns {Object} Object containing unsubscribe function and event type
 * @returns {Function} .unsubscribe - Function to remove the event listener
 * @returns {UVEEventType} .event - The event type being subscribed to
 * @internal
 */
export function onContentletHovered(callback: UVEEventHandler) {
    let hasHover = false;

    const pointerMoveCallback = (event: PointerEvent) => {
        const foundElement = findDotCMSElement(event.target as HTMLElement);

        if (!foundElement) {
            // Transitioning from a hovered contentlet to dead space — emit
            // a single null so the editor can clear its hover overlay.
            // Subsequent moves over dead space are no-ops.
            if (hasHover) {
                hasHover = false;
                callback(null);
            }
            return;
        }

        const { x, y, width, height } = foundElement.getBoundingClientRect();

        const isContainer = foundElement.dataset?.['dotObject'] === 'container';

        const contentletForEmptyContainer = {
            identifier: TEMP_EMPTY_CONTENTLET,
            title: TEMP_EMPTY_CONTENTLET,
            contentType: TEMP_EMPTY_CONTENTLET_TYPE,
            inode: 'TEMPY_EMPTY_CONTENTLET_INODE',
            widgetTitle: TEMP_EMPTY_CONTENTLET,
            baseType: TEMP_EMPTY_CONTENTLET,
            onNumberOfPages: 1
        };

        const contentlet = readContentletDataset(foundElement);

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

        hasHover = true;
        callback(contentletHoveredPayload);
    };

    // We intentionally do not fire null on document `pointerleave`: the
    // editor's hover toolbar lives in the parent window (outside the
    // iframe), so leaving the iframe usually means the user is heading
    // for the toolbar. Killing the overlay there would yank the toolbar
    // away just as the user reaches for it. Dead-space-inside-iframe
    // is already covered by the `pointermove` null branch above.
    //
    // Bind to `document.documentElement` (the <html> node) rather than
    // `document`. UVE renders traditional pages by reusing one iframe and
    // rewriting it via `doc.open()/write()/close()` on each in-editor
    // navigation. When Zone.js is loaded inside that iframe it runs in
    // global-events mode (one native gateway listener + a JS-level task
    // list stored on the node); `document.open()` tears down the native
    // gateway but the task list survives on the persistent `document`
    // node, so Zone sees "already registered" on re-init and skips
    // re-binding the native listener — pointermove silently goes dead.
    // `documentElement` is a fresh node after each `write()`, so it
    // carries no stale task list and re-binds cleanly. `pointermove`
    // bubbles to <html>, so behavior is identical on no-Zone pages.
    document.documentElement.addEventListener('pointermove', pointerMoveCallback);

    return {
        unsubscribe: () => {
            document.documentElement.removeEventListener('pointermove', pointerMoveCallback);
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

        const contentlet = readContentletDataset(foundElement);

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

    // The editor clears its selection on canvas resize / scroll. When that
    // happens, our lastSelectedInode is stale: a click on what used to be the
    // selected contentlet would be treated as a passthrough (page click) even
    // though the editor no longer has it selected. Listen for the
    // UVE_SELECTION_CLEARED message and reset the tracker.
    const selectionClearedCallback = (event: MessageEvent) => {
        if (event?.data?.name === __DOTCMS_UVE_EVENT__.UVE_SELECTION_CLEARED) {
            lastSelectedInode = undefined;
        }
    };

    // Capture phase so we run BEFORE the page's own click handlers and can
    // preventDefault/stopPropagation effectively.
    //
    // Bind to `document.documentElement` rather than `document` for the same
    // reason as the hover listener above: on Zone.js-loading traditional
    // pages the persistent `document` node keeps a stale Zone task list
    // across `doc.open()/write()/close()` iframe rewrites, so re-binding on
    // it is silently skipped after the first in-editor navigation. The
    // <html> node is recreated on each rewrite and re-binds cleanly. Capture
    // phase on <html> still runs before the page's own handlers, so
    // preventDefault/stopPropagation behave identically.
    document.documentElement.addEventListener('click', clickCallback, { capture: true });
    // Native binder so this parent→iframe receiver survives the iframe's
    // document.open()/write()/close() rewrites under Zone.js. See getNativeEventBinder.
    const nativeWindow = getNativeEventBinder(window);
    nativeWindow.addEventListener('message', selectionClearedCallback);

    return {
        unsubscribe: () => {
            document.documentElement.removeEventListener('click', clickCallback, {
                capture: true
            });
            nativeWindow.removeEventListener('message', selectionClearedCallback);
        },
        event: UVEEventType.CONTENTLET_CLICKED
    };
}
