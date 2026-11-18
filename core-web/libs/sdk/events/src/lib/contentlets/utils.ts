import {
    CONTENTLET_CLASS,
    CONTENTLET_IDENTIFIER_ATTRIBUTE,
    CONTENTLET_OBSERVER_DEBOUNCE_MS
} from './constants';

import { isBrowser, onPageDiscard } from '../pipeline/utils';

import type { ContentletData, PipelineConfig } from '../pipeline/models';
import type { AnalyticsPlugin } from 'analytics';

/*
 * Helpers of the content trackers: they find contentlets in the DOM, read their data, watch
 * for new ones and decide which trackers run.
 */

/**
 * Creates a throttled version of a callback function
 * Ensures the callback is executed at most once every `limitMs` milliseconds
 * @param callback - The function to throttle
 * @param limitMs - The time limit in milliseconds
 * @param options - `trailing`: instead of dropping the calls made inside the window, run the
 * last one when the window ends
 * @returns A throttled function
 */
export function createThrottle<T extends (...args: unknown[]) => void>(
    callback: T,
    limitMs: number,
    options: { trailing?: boolean } = {}
): (...args: Parameters<T>) => void {
    let lastRun = 0;
    let trailingTimer: ReturnType<typeof setTimeout> | null = null;

    return (...args: Parameters<T>) => {
        const now = Date.now();
        const wait = limitMs - (now - lastRun);

        if (wait <= 0) {
            callback(...args);
            lastRun = now;

            return;
        }

        if (options.trailing && trailingTimer === null) {
            trailingTimer = setTimeout(() => {
                trailingTimer = null;
                lastRun = Date.now();
                callback(...args);
            }, wait);
        }
    };
}

/**
 * Extracts the contentlet identifier from a DOM element
 * @param element - The HTML element containing data attributes
 * @returns The contentlet identifier or null if not found
 */
export function extractContentletIdentifier(element: HTMLElement): string | null {
    return element.dataset['dotIdentifier'] || null;
}

/**
 * Extracts all contentlet data from a DOM element's data attributes
 * @param element - The HTML element containing data attributes
 * @returns Complete contentlet data object
 */
export function extractContentletData(element: HTMLElement): ContentletData {
    return {
        identifier: element.dataset['dotIdentifier'] || '',
        inode: element.dataset['dotInode'] || '',
        contentType: element.dataset['dotType'] || '',
        title: element.dataset['dotTitle'] || '',
        baseType: element.dataset['dotBasetype'] || ''
    };
}

/**
 * Initial scan delay for DOM readiness
 * Allows React/Next.js to finish rendering before scanning for contentlets
 */
export const INITIAL_SCAN_DELAY_MS = 100;

/**
 * Finds all contentlet elements in the DOM
 * @returns Array of contentlet HTMLElements
 */
export const findContentlets = (): HTMLElement[] => {
    return Array.from(document.querySelectorAll<HTMLElement>(`.${CONTENTLET_CLASS}`));
};

/**
 * Creates a MutationObserver that watches for contentlet changes in the DOM
 * @param callback - Function to call when mutations are detected
 * @param debounceMs - Debounce time in milliseconds (default: 250ms)
 * @param options - `identifiers`: also call back when a contentlet receives its
 * `data-dot-identifier`. The renderers print it after hydration, usually within
 * milliseconds of the contentlet itself, so calls inside the window are not dropped: the
 * last one runs when it ends.
 * @returns Configured and active MutationObserver
 */
export const createContentletObserver = (
    callback: () => void,
    debounceMs: number = CONTENTLET_OBSERVER_DEBOUNCE_MS,
    options: { identifiers?: boolean } = {}
): MutationObserver => {
    const watchIdentifiers = options.identifiers === true;
    const throttledCallback = createThrottle(callback, debounceMs, { trailing: watchIdentifiers });

    const isContentlet = (node: Node): boolean =>
        node.nodeType === Node.ELEMENT_NODE &&
        (node as HTMLElement).classList?.contains(CONTENTLET_CLASS) === true;

    const observer = new MutationObserver((mutations) => {
        // This reduces observer callback executions by ~90% on dynamic sites
        const hasRelevantChanges = mutations.some((mutation) => {
            // Only the identifier attribute is observed, so any attribute change counts
            // when it lands on a contentlet
            if (mutation.type === 'attributes') {
                return isContentlet(mutation.target);
            }

            // Skip if no nodes were added or removed
            if (mutation.addedNodes.length === 0 && mutation.removedNodes.length === 0) {
                return false;
            }

            // Check if any added/removed nodes are or contain contentlets
            const nodes = [
                ...Array.from(mutation.addedNodes),
                ...Array.from(mutation.removedNodes)
            ];

            return nodes.some((node) => {
                // Only check element nodes
                if (node.nodeType !== Node.ELEMENT_NODE) {
                    return false;
                }

                const element = node as HTMLElement;

                // Check if node itself is a contentlet
                if (element.classList?.contains(CONTENTLET_CLASS)) {
                    return true;
                }

                // Check if node contains contentlets
                return element.querySelector?.(`.${CONTENTLET_CLASS}`) !== null;
            });
        });

        // Only invoke callback if relevant changes detected
        if (hasRelevantChanges) {
            throttledCallback();
        }
    });

    const observeOptions: MutationObserverInit = {
        childList: true,
        subtree: true,
        characterData: false,
        ...(watchIdentifiers
            ? { attributes: true, attributeFilter: [CONTENTLET_IDENTIFIER_ATTRIBUTE] }
            : { attributes: false })
    };

    // Handle case where document.body might not be available yet if script is in <head>
    const target = document.body || document.documentElement;
    if (target) {
        observer.observe(target, observeOptions);
    } else {
        // Fallback: wait for DOMContentLoaded if everything is null (rare)
        window.addEventListener('DOMContentLoaded', () => {
            if (document.body) {
                observer.observe(document.body, observeOptions);
            }
        });
    }

    return observer;
};

/**
 * Cleans up a content tracker when the browser discards the page. A page kept in the
 * back/forward cache keeps its tracker, which works again when the visitor goes back to it.
 * @param cleanup - Function to call when the page is discarded
 */
export const setupPluginCleanup = (cleanup: () => void): void => {
    if (!isBrowser()) return;

    onPageDiscard(cleanup);
};

/**
 * Gets enhanced tracking plugins based on configuration
 * Returns content impression and click tracking plugins if enabled
 * @param config - Analytics configuration
 * @param impressionPlugin - Impression tracking plugin factory
 * @param clickPlugin - Click tracking plugin factory
 * @returns Array of enabled tracking plugins
 */
export const getEnhancedTrackingPlugins = (
    config: PipelineConfig,
    impressionPlugin: (config: PipelineConfig) => AnalyticsPlugin,
    clickPlugin: (config: PipelineConfig) => AnalyticsPlugin
): AnalyticsPlugin[] => {
    return [
        config.impressions && impressionPlugin(config),
        config.clicks && clickPlugin(config)
    ].filter(Boolean) as AnalyticsPlugin[];
};
