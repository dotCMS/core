import { CLICKABLE_ELEMENTS_SELECTOR, DEFAULT_CLICK_THROTTLE_MS } from './constants';
import { handleContentletClick } from './utils';

import { CONTENTLET_CLASS } from '../contentlets/constants';
import { createPluginLogger, isBrowser } from '../pipeline/utils';

import type { PipelineConfig, DotCMSContentClickPayload } from '../pipeline/models';

/** Callback function for click events */
export type ClickCallback = (eventName: string, payload: DotCMSContentClickPayload) => void;

/** Subscription object with unsubscribe method */
export interface ClickSubscription {
    unsubscribe: () => void;
}

/**
 * Tracks clicks on the links and buttons inside contentlets, through one click listener on the
 * document, in the capture phase. A click finds its contentlet with `closest`, so:
 * - nothing scans or observes the page, and a contentlet added at any time counts;
 * - an app's `stopPropagation()` cannot hide a click from the tracker;
 * - a click inside nested contentlets counts once, for the innermost;
 * - clicks on the same contentlet within 300 ms count once.
 *
 * @example
 * ```typescript
 * const tracker = new DotCMSClickTracker(config);
 * const subscription = tracker.onClick((eventName, payload) => {
 *   console.log('Click detected:', payload);
 * });
 * tracker.initialize();
 * // Later: subscription.unsubscribe();
 * ```
 */
export class DotCMSClickTracker {
    // When each contentlet was last clicked: a click on one never throttles another
    #lastClickAt = new WeakMap<HTMLElement, number>();
    #logger: ReturnType<typeof createPluginLogger>;
    #subscribers = new Set<ClickCallback>();
    #listening = false;

    constructor(config: PipelineConfig) {
        this.#logger = createPluginLogger('Click', config);
    }

    /**
     * Subscribe to click events
     * @param callback - Function called when click is detected
     * @returns Subscription object with unsubscribe method
     */
    public onClick(callback: ClickCallback): ClickSubscription {
        this.#subscribers.add(callback);

        return {
            unsubscribe: () => {
                this.#subscribers.delete(callback);
            }
        };
    }

    /** Starts listening for clicks; without a document, as on the server, it does nothing */
    public initialize(): void {
        if (!isBrowser()) {
            this.#logger.warn('No document, skipping');
            return;
        }

        if (this.#listening) {
            return;
        }

        document.addEventListener('click', this.#handleClick, true);
        this.#listening = true;

        this.#logger.info('Plugin initialized');
    }

    /** Reports a click on a link or button inside a contentlet, throttled per contentlet */
    readonly #handleClick = (event: MouseEvent): void => {
        const target = event.target;

        if (!(target instanceof Element)) {
            return;
        }

        const contentlet = target
            .closest(CLICKABLE_ELEMENTS_SELECTOR)
            ?.closest<HTMLElement>(`.${CONTENTLET_CLASS}`);

        if (!contentlet) {
            return;
        }

        handleContentletClick(
            event,
            contentlet,
            (eventName, payload) => {
                const now = Date.now();

                if (now - (this.#lastClickAt.get(contentlet) ?? 0) < DEFAULT_CLICK_THROTTLE_MS) {
                    return;
                }

                this.#lastClickAt.set(contentlet, now);
                this.#subscribers.forEach((callback) => callback(eventName, payload));
                this.#logger.info(`Fired click event for ${payload.content.identifier}`, payload);
            },
            this.#logger
        );
    };

    /**
     * Stops listening for clicks. Should be called when the plugin is disabled or the page is
     * discarded.
     */
    public cleanup(): void {
        if (this.#listening) {
            document.removeEventListener('click', this.#handleClick, true);
            this.#listening = false;
        }

        this.#logger.info('Click tracking cleaned up');
    }
}
