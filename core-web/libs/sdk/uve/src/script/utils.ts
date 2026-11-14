import {
    DotCMSIframeScrollPayload,
    DotCMSPageResponse,
    DotCMSUVEAction,
    UVEEventType
} from '@dotcms/types';

import { createUVESubscription } from '../lib/core/core.utils';
import { computeScrollIsInBottom, getNativeEventBinder } from '../lib/dom/dom.utils';
import { setBounds } from '../lib/editor/internal';
import { initInlineEditing, sendMessageToUVE } from '../lib/editor/public';

/** One frame at 60fps, in ms: the unit the editor's scroll velocity is expressed in. */
const FRAME_MS = 1000 / 60;

/** The fastest the page scrolls during a drag, in px per 60fps frame. */
const MAX_SCROLL_VELOCITY = 40;

/**
 * The longest frame a scroll step accounts for, so a frame delayed by a busy or hidden tab does
 * not jump the page.
 */
const MAX_FRAME_MS = 4 * FRAME_MS;

/** A velocity the page can use: a finite number, clamped to the maximum; anything else is 0. */
function toScrollVelocity(velocity: unknown): number {
    if (typeof velocity !== 'number' || !Number.isFinite(velocity)) {
        return 0;
    }

    return Math.max(-MAX_SCROLL_VELOCITY, Math.min(MAX_SCROLL_VELOCITY, velocity));
}

/**
 * Scrolls the page continuously during a drag, at the velocity the editor sets, one instant step
 * per animation frame scaled by how long the frame took. It stops at velocity 0, at the top when
 * scrolling up, at the bottom when scrolling down, and on `stop()`.
 */
function createDragScroller() {
    let velocity = 0;
    let frameId: number | null = null;
    let lastFrameTime: number | null = null;

    const halt = () => {
        if (frameId !== null) {
            cancelAnimationFrame(frameId);
        }

        frameId = null;
        lastFrameTime = null;
    };

    const step = (time: number) => {
        frameId = null;

        if (
            velocity === 0 ||
            (velocity < 0 && window.scrollY <= 0) ||
            (velocity > 0 && computeScrollIsInBottom())
        ) {
            halt();

            return;
        }

        const elapsed = lastFrameTime === null ? FRAME_MS : time - lastFrameTime;

        lastFrameTime = time;
        window.scrollBy({
            left: 0,
            top: velocity * (Math.min(elapsed, MAX_FRAME_MS) / FRAME_MS),
            behavior: 'instant'
        });
        // Report where the page is now, in this frame. The scroll event's own report comes a
        // frame later, and the editor shifts bounds that arrive in between from the last report.
        sendMessageToUVE<DotCMSIframeScrollPayload>({
            action: DotCMSUVEAction.IFRAME_SCROLL,
            payload: { scrollY: window.scrollY }
        });
        frameId = requestAnimationFrame(step);
    };

    return {
        setVelocity: (next: unknown) => {
            velocity = toScrollVelocity(next);

            if (velocity === 0) {
                halt();

                return;
            }

            if (frameId === null) {
                frameId = requestAnimationFrame(step);
            }
        },
        stop: () => {
            velocity = 0;
            halt();
        }
    };
}

function escapeCssContentValue(value: string): string {
    return value
        .replace(/\\/g, '\\\\')
        .replace(/'/g, "\\'")
        .replace(/\r\n|\r|\n/g, '\\a ')
        .replace(/\f/g, ' ');
}

/**
 * Sets up scroll event handlers for the window to notify the editor about scroll events.
 * Adds listeners for both 'scroll' and 'scrollend' events, sending appropriate messages
 * to the editor when these events occur.
 */
export function scrollHandler() {
    // The scroll offset lets the editor keep its drop targets aligned while a drag scrolls the page.
    const scrollCallback = () => {
        sendMessageToUVE<DotCMSIframeScrollPayload>({
            action: DotCMSUVEAction.IFRAME_SCROLL,
            payload: { scrollY: window.scrollY }
        });
    };

    const scrollEndCallback = () => {
        sendMessageToUVE({
            action: DotCMSUVEAction.IFRAME_SCROLL_END
        });
    };

    // Native binder: scroll survives the iframe rewrite under Zone.js. It can't
    // move to a fresh `documentElement` like hover/click (viewport scroll only
    // fires on `window`). See getNativeEventBinder.
    const { addEventListener, removeEventListener } = getNativeEventBinder(window);

    addEventListener('scroll', scrollCallback);
    addEventListener('scrollend', scrollEndCallback);

    return {
        destroyScrollHandler: () => {
            removeEventListener('scroll', scrollCallback);
            removeEventListener('scrollend', scrollEndCallback);
        }
    };
}

/**
 * Adds 'empty-contentlet' class to contentlet elements that have no height.
 * This helps identify and style empty contentlets in the editor view.
 *
 * @remarks
 * The function queries all elements with data-dot-object="contentlet" attribute
 * and checks their clientHeight. If an element has no height (clientHeight = 0),
 * it adds the 'empty-contentlet' class to that element.
 */
export function addClassToEmptyContentlets(): void {
    const contentlets = document.querySelectorAll('[data-dot-object="contentlet"]');

    contentlets.forEach((contentlet) => {
        if (contentlet.clientHeight) {
            return;
        }

        contentlet.classList.add('empty-contentlet');
    });
}

/**
 * Registers event handlers for various UVE (Universal Visual Editor) events.
 *
 * This function sets up subscriptions for:
 * - Page reload events that refresh the window
 * - Bounds request events to update editor boundaries
 * - Iframe scroll events to handle smooth scrolling within bounds
 * - Scroll velocity events, which scroll the page every frame during a drag
 * - Contentlet hover events to notify the editor
 *
 * @remarks
 * For scroll events, the function includes logic to prevent scrolling beyond
 * the top or bottom boundaries of the iframe, which helps maintain proper
 * scroll event handling.
 */
export function registerUVEEvents() {
    const pageReloadSubscription = createUVESubscription(UVEEventType.PAGE_RELOAD, () => {
        window.location.reload();
    });

    // Both live per registration, so a later registration starts with a fresh flag and no loop.
    const dragScroller = createDragScroller();
    // Once the editor sends a velocity, it drives the scroll, and its one-step direction requests,
    // which it keeps sending for older pages, are ignored.
    let editorSendsVelocity = false;

    const iframeScrollVelocitySubscription = createUVESubscription(
        UVEEventType.IFRAME_SCROLL_VELOCITY,
        (velocity) => {
            editorSendsVelocity = true;
            dragScroller.setVelocity(velocity);
        }
    );

    const iframeScrollSubscription = createUVESubscription(
        UVEEventType.IFRAME_SCROLL,
        (direction) => {
            if (editorSendsVelocity) {
                return;
            }

            if (
                (window.scrollY === 0 && direction === 'up') ||
                (computeScrollIsInBottom() && direction === 'down')
            ) {
                // If the iframe scroll is at the top or bottom, do not send anything.
                // This avoids losing the scrollend event.
                return;
            }

            const scrollY = direction === 'up' ? -120 : 120;
            window.scrollBy({ left: 0, top: scrollY, behavior: 'smooth' });
        }
    );

    const contentletHoveredSubscription = createUVESubscription(
        UVEEventType.CONTENTLET_HOVERED,
        (contentletHovered) => {
            sendMessageToUVE({
                action: DotCMSUVEAction.SET_CONTENTLET,
                payload: contentletHovered
            });
        }
    );

    const contentletClickedSubscription = createUVESubscription(
        UVEEventType.CONTENTLET_CLICKED,
        (contentletClicked) => {
            sendMessageToUVE({
                action: DotCMSUVEAction.SET_SELECTED_CONTENTLET,
                payload: contentletClicked
            });
        }
    );

    const scrollToSectionSubscription = createUVESubscription(
        UVEEventType.SCROLL_TO_SECTION,
        (payload) => {
            sendMessageToUVE({
                action: DotCMSUVEAction.SECTION_OFFSET,
                payload
            });
        }
    );

    // The single bounds-sync channel. The SDK observes layout changes
    // inside the iframe (media-query reflows, image/font load shifts,
    // container mount/unmount, scroll, etc.) and emits SET_BOUNDS on the
    // trailing edge of a debounce window. The editor can also send a
    // UVE_FLUSH_BOUNDS message to request an immediate synchronous emit
    // (used during drag/drop, where the dropzone needs current bounds
    // before the user moves another pixel).
    const autoBoundsSubscription = createUVESubscription(UVEEventType.AUTO_BOUNDS, (bounds) => {
        setBounds(bounds);
    });

    return {
        subscriptions: [
            pageReloadSubscription,
            {
                ...iframeScrollVelocitySubscription,
                unsubscribe: () => {
                    iframeScrollVelocitySubscription.unsubscribe();
                    dragScroller.stop();
                    editorSendsVelocity = false;
                }
            },
            iframeScrollSubscription,
            contentletHoveredSubscription,
            contentletClickedSubscription,
            scrollToSectionSubscription,
            autoBoundsSubscription
        ]
    };
}

/**
 * Notifies the editor that the UVE client is ready to receive messages.
 *
 * This function sends a message to the editor indicating that the client-side
 * initialization is complete and it's ready to handle editor interactions.
 *
 * @remarks
 * This is typically called after all UVE event handlers and DOM listeners
 * have been set up successfully.
 */
export function setClientIsReady(config?: Partial<DotCMSPageResponse>): void {
    sendMessageToUVE({
        action: DotCMSUVEAction.CLIENT_READY,
        payload: config
    });
}

/**
 * Listen for block editor inline event.
 */
export function listenBlockEditorInlineEvent() {
    if (document.readyState === 'complete') {
        // The page is fully loaded or interactive
        listenBlockEditorClick();

        return {
            destroyListenBlockEditorInlineEvent: () => {
                // No need to remove listener if page was already loaded
            }
        };
    }

    // Native binder: `DOMContentLoaded` fires on `document` (not `<html>`), so
    // it survives the iframe rewrite under Zone.js. See getNativeEventBinder.
    const handleDOMContentLoaded = () => {
        listenBlockEditorClick();
    };

    const { addEventListener, removeEventListener } = getNativeEventBinder(document);
    addEventListener('DOMContentLoaded', handleDOMContentLoaded);

    return {
        destroyListenBlockEditorInlineEvent: () => {
            removeEventListener('DOMContentLoaded', handleDOMContentLoaded);
        }
    };
}

/**
 * Injects UVE editor styles for empty containers and contentlets into the page.
 * Provides visual placeholders so editors can identify and interact with empty areas.
 *
 * The empty-container label is read from the dotCMS i18n cache in localStorage
 * (`dotMessagesBundle`). Falls back to 'Empty container' if the cache is unavailable
 * (e.g. headless pages on a different origin).
 */
export function injectEmptyStateStyles(): void {
    let emptyContainerLabel = 'Empty container';

    try {
        const bundle = JSON.parse(localStorage.getItem('dotMessagesBundle') ?? 'null');
        const label = bundle?.messages?.['editpage.container.is.empty'];
        if (typeof label === 'string') emptyContainerLabel = label;
    } catch {
        // localStorage unavailable or JSON malformed — use default
    }

    const escapedEmptyContainerLabel = escapeCssContentValue(emptyContainerLabel);

    const style = document.createElement('style');
    style.dataset['dotStyles'] = 'uve-empty-state';
    style.textContent = `
        [data-dot-object="container"]:empty {
            width: 100%;
            background-color: #ECF0FD;
            display: flex;
            justify-content: center;
            align-items: center;
            color: #030E32;
            height: 10rem;
        }

        [data-dot-object="contentlet"].empty-contentlet {
            min-height: 4rem;
            width: 100%;
        }

        [data-dot-object="container"]:empty::after {
            content: '${escapedEmptyContainerLabel}';
        }
    `;
    document.head?.appendChild(style);
}

const listenBlockEditorClick = (): void => {
    const editBlockEditorNodes: NodeListOf<HTMLElement> = document.querySelectorAll(
        '[data-block-editor-content]'
    );

    if (!editBlockEditorNodes.length) {
        return;
    }

    editBlockEditorNodes.forEach((node: HTMLElement) => {
        const { inode, language = '1', contentType, fieldName, blockEditorContent } = node.dataset;
        const content = JSON.parse(blockEditorContent || '');

        if (!inode || !language || !contentType || !fieldName) {
            console.error('Missing data attributes for block editor inline editing.');
            console.warn('inode, language, contentType and fieldName are required.');

            return;
        }

        node.classList.add('dotcms__inline-edit-field');
        node.addEventListener('click', () => {
            initInlineEditing('BLOCK_EDITOR', {
                inode,
                content,
                language: parseInt(language),
                fieldName,
                contentType
            });
        });
    });
};
