import { AnalyticsInstance } from 'analytics';

import {
    ANALYTICS_CONTENTLET_CLASS,
    CLICK_EVENT_TYPE,
    DEFAULT_CLICK_THROTTLE_MS
} from '../../shared/constants/dot-analytics.constants';
import { DotCMSAnalyticsConfig, DotCMSContentClickPayload } from '../../shared/models';
import { extractContentletData } from '../../shared/utils/dot-analytics.utils';
import { getViewportMetrics } from '../impression/dot-analytics.impression.utils';

/**
 * Tracks a click event with throttling to prevent duplicates.
 *
 * @param eventName - The name of the event to track
 * @param payload - The event payload
 * @param instance - The analytics instance
 * @param config - The analytics configuration (for debug logging)
 * @param lastClickTime - Mutable object to store the timestamp of the last click
 */
export const trackClick = (
    eventName: string,
    payload: DotCMSContentClickPayload,
    instance: AnalyticsInstance,
    config: DotCMSAnalyticsConfig,
    lastClickTime: { value: number }
) => {
    const now = Date.now();
    if (now - lastClickTime.value < DEFAULT_CLICK_THROTTLE_MS) {
        return;
    }
    lastClickTime.value = now;
    instance.track(eventName, payload);

    if (config.debug) {
        console.warn(
            `DotCMS Analytics [Click]: Fired click event for ${payload.content.identifier}`,
            payload
        );
    }
};

/**
 * Handles document click events to detect clicks on contentlets.
 *
 * @param event - The mouse event
 * @param trackCallback - Callback to execute if the click is valid
 */
export const handleDocumentClick = (
    event: MouseEvent,
    trackCallback: (eventName: string, payload: DotCMSContentClickPayload) => void,
    debug = false
) => {
    const target = event.target as HTMLElement;

    if (debug) {
        console.warn('DotCMS Analytics [Click]: Click detected on:', target);
    }

    // 1. Check if clicked element is relevant (a or button)
    // We traverse up from target to find closest a or button
    const clickableElement = target.closest('a, button') as HTMLElement;

    if (!clickableElement) {
        if (debug) {
            console.warn('DotCMS Analytics [Click]: No <a> or <button> found in click path');
        }
        return;
    }

    if (debug) {
        console.warn('DotCMS Analytics [Click]: Found clickable element:', clickableElement);
    }

    // 2. Check if it's inside a tracked contentlet
    const contentletElement = clickableElement.closest(
        `.${ANALYTICS_CONTENTLET_CLASS}`
    ) as HTMLElement;

    if (!contentletElement) {
        if (debug) {
            console.warn('DotCMS Analytics [Click]: Clickable element is not inside a contentlet');
        }
        return;
    }

    if (debug) {
        console.warn('DotCMS Analytics [Click]: Found contentlet element:', contentletElement);
    }

    // 3. Check for required attributes (validation)
    // The contentlet element must have the analytics attributes.
    const contentletData = extractContentletData(contentletElement);

    if (!contentletData.identifier) {
        if (debug) {
            console.warn('DotCMS Analytics [Click]: Contentlet has no identifier');
        }
        return;
    }

    if (debug) {
        console.warn('DotCMS Analytics [Click]: Contentlet data:', contentletData);
    }

    // 4. Extract Metadata
    const viewportMetrics = getViewportMetrics(contentletElement);

    // Extract attributes from the clickable element
    const attributes: Record<string, string> = {};
    if (clickableElement.hasAttributes()) {
        for (let i = 0; i < clickableElement.attributes.length; i++) {
            const attr = clickableElement.attributes[i];
            // Filter out dot-analytics attributes to avoid noise
            if (!attr.name.startsWith('data-dot-analytics')) {
                attributes[attr.name] = attr.value;
            }
        }
    }

    const payload: DotCMSContentClickPayload = {
        content: {
            identifier: contentletData.identifier,
            inode: contentletData.inode,
            title: contentletData.title,
            content_type: contentletData.contentType
        },
        position: {
            viewport_offset_pct: viewportMetrics.offsetPercentage,
            dom_index: Array.from(
                document.querySelectorAll(`.${ANALYTICS_CONTENTLET_CLASS}`)
            ).indexOf(contentletElement)
        },
        element: {
            text: (clickableElement.innerText || clickableElement.textContent || '')
                .trim()
                .substring(0, 100), // Limit length
            type: clickableElement.tagName.toLowerCase(),
            id: clickableElement.id,
            class: clickableElement.className,
            attributes: attributes
        }
    };

    trackCallback(CLICK_EVENT_TYPE, payload);
};
