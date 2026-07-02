/* eslint-disable @typescript-eslint/no-explicit-any */
import { DotCMSPageResponse, DotCMSUVEAction, UVEEventType } from '@dotcms/types';

import { createUVESubscription } from '../lib/core/core.utils';
import { computeScrollIsInBottom, getNativeEventBinder } from '../lib/dom/dom.utils';
import { setBounds } from '../lib/editor/internal';
import { initInlineEditing, sendMessageToUVE } from '../lib/editor/public';

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
    const scrollCallback = () => {
        sendMessageToUVE({
            action: DotCMSUVEAction.IFRAME_SCROLL
        });
    };

    const scrollEndCallback = () => {
        sendMessageToUVE({
            action: DotCMSUVEAction.IFRAME_SCROLL_END
        });
    };

    // Bind through Zone.js's native (untracked) listeners so scroll survives the
    // iframe's document.open()/write()/close() rewrites. Zone kills listeners
    // rebound on the persistent `window` node after the first navigation, and
    // viewport scroll can't be moved to a fresh `documentElement` the way
    // hover/click were. See getNativeEventBinder for the full rationale.
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

    const iframeScrollSubscription = createUVESubscription(
        UVEEventType.IFRAME_SCROLL,
        (direction) => {
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
export function setClientIsReady(config?: DotCMSPageResponse): void {
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

    // If the page is not fully loaded, listen for the DOMContentLoaded event.
    // Bind through Zone's native listener so it survives the iframe's
    // document.open()/write()/close() rewrites — without it, inline block-editor
    // editing stops wiring up after the first in-editor navigation on a Zone.js
    // page. `DOMContentLoaded` fires on `document`, not `<html>`, so the
    // documentElement trick used for hover/click doesn't apply here. See
    // getNativeEventBinder.
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
 * (`dotMessagesKeys`). Falls back to 'Empty container' if the cache is unavailable
 * (e.g. headless pages on a different origin).
 */
export function injectEmptyStateStyles(): void {
    let emptyContainerLabel = 'Empty container';

    try {
        const messages = JSON.parse(localStorage.getItem('dotMessagesKeys') ?? '{}');
        emptyContainerLabel = messages['editpage.container.is.empty'] ?? emptyContainerLabel;
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
