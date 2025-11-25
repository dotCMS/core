/* eslint-disable @typescript-eslint/no-explicit-any */
import { DotCMSPageResponse, DotCMSUVEAction, UVEEventType } from '@dotcms/types';

import { createUVESubscription } from '../lib/core/core.utils';
import { computeScrollIsInBottom } from '../lib/dom/dom.utils';
import { setBounds } from '../lib/editor/internal';
import { initInlineEditing, sendMessageToUVE } from '../lib/editor/public';

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

    window.addEventListener('scroll', scrollCallback);
    window.addEventListener('scrollend', scrollEndCallback);

    return {
        destroyScrollHandler: () => {
            window.removeEventListener('scroll', scrollCallback);
            window.removeEventListener('scrollend', scrollEndCallback);
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

    const requestBoundsSubscription = createUVESubscription(
        UVEEventType.REQUEST_BOUNDS,
        (bounds) => {
            setBounds(bounds);
        }
    );

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

    return {
        subscriptions: [
            pageReloadSubscription,
            requestBoundsSubscription,
            iframeScrollSubscription,
            contentletHoveredSubscription
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

    // If the page is not fully loaded, listen for the DOMContentLoaded event
    const handleDOMContentLoaded = () => {
        listenBlockEditorClick();
    };

    document.addEventListener('DOMContentLoaded', handleDOMContentLoaded);

    return {
        destroyListenBlockEditorInlineEvent: () => {
            document.removeEventListener('DOMContentLoaded', handleDOMContentLoaded);
        }
    };
}

const listenBlockEditorClick = (): void => {
    console.log('listenBlockEditorClick');
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
