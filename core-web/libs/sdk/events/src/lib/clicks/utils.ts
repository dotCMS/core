import { CLICKABLE_ELEMENTS_SELECTOR, CLICK_EVENT_TYPE } from './constants';

import { extractContentletData, findContentlets } from '../contentlets/utils';
import { getViewportMetrics } from '../contentlets/viewport';

import type { DotCMSContentClickPayload } from '../pipeline/models';
import type { createPluginLogger } from '../pipeline/utils';

/**
 * Handles click events on elements within a contentlet.
 * The tracker passes the contentlet the click landed in: the innermost one around the clicked
 * link or button.
 *
 * @param event - The mouse event
 * @param contentletElement - The contentlet container element
 * @param trackCallback - Callback to execute if the click is valid
 * @param logger - Logger instance for debug messages
 */
export const handleContentletClick = (
    event: MouseEvent,
    contentletElement: HTMLElement,
    trackCallback: (eventName: string, payload: DotCMSContentClickPayload) => void,
    logger: ReturnType<typeof createPluginLogger>
) => {
    const target = event.target as HTMLElement;

    logger.debug('Click detected on:', target);

    // 1. Find clickable element (a or button) within the event path
    const clickableElement = target.closest(CLICKABLE_ELEMENTS_SELECTOR) as HTMLElement;

    if (!clickableElement) {
        logger.debug('No <a> or <button> found in click path');
        return;
    }

    // 2. Verify clickable is actually inside this contentlet (safety check)
    if (!contentletElement.contains(clickableElement)) {
        logger.debug('Click was outside contentlet boundary');
        return;
    }

    logger.debug('Found clickable element:', clickableElement);

    // 3. Extract and validate contentlet data
    const contentletData = extractContentletData(contentletElement);

    if (!contentletData.identifier) {
        logger.debug('Contentlet has no identifier');
        return;
    }

    logger.debug('Contentlet data:', contentletData);

    // 4. Build payload with metadata
    const viewportMetrics = getViewportMetrics(contentletElement);

    // Extract attributes from the clickable element
    // Filter out: analytics attributes, and already-captured properties (class, id, href)
    // Format as 'key:value' strings (e.g., ['data-category:val', 'data-campaign:val2'])
    const attributes: string[] = [];
    for (const attr of clickableElement.attributes) {
        if (
            !attr.name.startsWith('data-dot-analytics') &&
            attr.name !== 'class' &&
            attr.name !== 'id' &&
            attr.name !== 'href'
        ) {
            attributes.push(`${attr.name}:${attr.value}`);
        }
    }

    // The contentlet's position among the page's contentlets, read when it is clicked: one
    // query per click, and the position stays right as contentlets are added or moved
    const domIndex = findContentlets().indexOf(contentletElement);

    const payload: DotCMSContentClickPayload = {
        content: {
            identifier: contentletData.identifier,
            inode: contentletData.inode,
            title: contentletData.title,
            content_type: contentletData.contentType
        },
        position: {
            viewport_offset_pct: viewportMetrics.offsetPercentage,
            dom_index: domIndex
        },
        element: {
            text: (clickableElement.innerText || clickableElement.textContent || '')
                .trim()
                .substring(0, 100), // Limit length
            type: clickableElement.tagName.toLowerCase(),
            id: clickableElement.id || '', // Required by backend, empty string if not present
            class: clickableElement.className || '', // Required by backend, empty string if not present
            href: clickableElement.getAttribute('href') || '', // Path as written in HTML (relative), empty string for buttons
            attributes: attributes // Additional attributes (data-*, aria-*, target, etc.)
        }
    };

    trackCallback(CLICK_EVENT_TYPE, payload);
};
