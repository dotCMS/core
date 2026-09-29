import onRouteChange from '@analytics/router-utils';

import { getUVEState } from '@dotcms/uve';

import { clicksPlugin } from './clicks/plugin';
import { getEnhancedTrackingPlugins } from './contentlets/utils';
import { DEFAULT_ASSIGNMENT_TIMEOUT_MS } from './experiments/constants';
import { revealRows } from './experiments/dom';
import { createExperimentsEngine } from './experiments/engine';
import { experimentsPlugin } from './experiments/plugin';
import { impressionsPlugin } from './impressions/plugin';
import {
    ANALYTICS_READY_EVENT,
    ANALYTICS_WINDOWS_ACTIVE_KEY,
    DotCMSPredefinedEventType,
    EVENTS_WINDOW_KEY
} from './pipeline/constants';
import { enricherPlugin } from './pipeline/enricher/plugin';
import { identityPlugin } from './pipeline/identity/plugin';
import { senderPlugin } from './pipeline/sender/plugin';

import type { ExperimentPages, ExperimentsEngine } from './experiments/engine';
import type {
    DotCMSEvents,
    DotCMSEventsConfig,
    DotCMSEventsError,
    DotCMSEventsJsonObject
} from './models';
import type { PipelineConfig } from './pipeline/models';
import type { AnalyticsInstance, AnalyticsPlugin } from 'analytics';

/** Calls made before `init`, replayed once it runs. Capped so a missing `init` can't grow it. */
const MAX_PENDING_CALLS = 50;

/** Where Analytics.js keeps its anonymous id and user traits: `createMemoryStorage`. */
interface AnalyticsJsStorage {
    getItem(key: string): unknown;
    setItem(key: string, value: unknown): void;
    removeItem(key: string): void;
}

/**
 * A storage that lives as long as the page. Analytics.js persists an anonymous id that nothing
 * here reads, and its default storage falls back to a cookie when localStorage is
 * unavailable: with this one, nothing it keeps reaches localStorage, sessionStorage or a
 * cookie.
 *
 * @returns The storage
 */
const createMemoryStorage = (): AnalyticsJsStorage => {
    const values = new Map<string, unknown>();

    return {
        getItem: (key) => values.get(key),
        setItem: (key, value) => void values.set(key, value),
        removeItem: (key) => void values.delete(key)
    };
};

let instance: AnalyticsInstance | null = null;
let engine: ExperimentsEngine | null = null;
let activeConfigKey: string | null = null;
let lastPageKey: string | null = null;
let debug = false;
const pendingCalls: Array<() => void> = [];

const isBrowser = (): boolean => typeof window !== 'undefined' && typeof document !== 'undefined';

const log = (...args: unknown[]): void => {
    if (debug) {
        // eslint-disable-next-line no-console
        console.info('[dotCMS events]', ...args);
    }
};

let onErrorHandler: DotCMSEventsConfig['onError'];

/**
 * Hands a failure to the app's `onError`. A handler that throws must not break the page.
 *
 * @param error - The failure
 */
const reportError = (error: DotCMSEventsError): void => {
    try {
        onErrorHandler?.(error);
    } catch {
        // The app's handler failed; events carry on
    }
};

/** Reports a setup mistake in the page, only in debug like `log`. */
const warn = (message: string): void => {
    if (debug) {
        console.warn(`[dotCMS events] ${message}`);
    }
};

const defer = (call: () => void): void => {
    if (pendingCalls.length < MAX_PENDING_CALLS) {
        pendingCalls.push(call);
    }
};

// Optional settings pass through only when set, so the core applies its own defaults.
const toAnalyticsConfig = (config: DotCMSEventsConfig): PipelineConfig => ({
    server: config.dotcmsUrl.replace(/\/+$/, ''),
    siteAuth: config.siteAuth,
    debug: Boolean(config.debug),
    autoPageView: config.autoPageView !== false,
    ...(config.logLevel !== undefined && { logLevel: config.logLevel }),
    ...(config.queue !== undefined && { queue: config.queue }),
    ...(config.impressions !== undefined && { impressions: config.impressions }),
    ...(config.clicks !== undefined && { clicks: config.clicks }),
    onError: reportError
});

const assignmentTimeout = (config: DotCMSEventsConfig): number =>
    typeof config.experiments === 'object' && typeof config.experiments.timeout === 'number'
        ? config.experiments.timeout
        : DEFAULT_ASSIGNMENT_TIMEOUT_MS;

const nextFrame = (): Promise<void> =>
    new Promise((resolve) => requestAnimationFrame(() => resolve()));

/**
 * Sends a pageview, after the experiments engine decides the page. When the engine
 * redirects, the pageview is dropped: the variant's page sends its own.
 */
const sendPageView = async (data: DotCMSEventsJsonObject): Promise<void> => {
    if (!instance) {
        return;
    }

    if (engine) {
        const { redirected } = await engine.decide();

        if (redirected) {
            log('pageview dropped: redirecting to the assigned variant');

            return;
        }
    }

    instance.page(data);
};

const trackAutomaticPageView = async (): Promise<void> => {
    const pageKey = `${window.location.pathname}${window.location.search}`;

    if (pageKey === lastPageKey) {
        return;
    }

    lastPageKey = pageKey;
    await sendPageView({});
};

const startAutomaticPageViews = (): void => {
    const first = () => void trackAutomaticPageView();

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', first, { once: true });
    } else {
        first();
    }

    // Wait two frames after a History change, so the new route's rows are in the DOM.
    onRouteChange(() => {
        void nextFrame()
            .then(nextFrame)
            .then(() => trackAutomaticPageView());
    });
};

/**
 * Starts the SDK: what `events.init` does, with how the pages mark their experiment. Apps call
 * `events.init`, for pages that print the experiment markup; the script dotCMS injects into
 * traditional pages calls this with `contentlets`, since those pages carry dotCMS's contentlet
 * wrappers instead. Not exported by the package.
 *
 * @param config - The app's config
 * @param pages - How the pages mark what their experiment varies
 */
export const initEvents = (config: DotCMSEventsConfig, pages: ExperimentPages): void => {
    if (!isBrowser()) {
        return;
    }

    const configKey = `${config?.dotcmsUrl}|${config?.siteAuth}`;

    // Claimed as soon as init starts: Analytics.js loads after this call returns
    if (activeConfigKey) {
        if (configKey !== activeConfigKey) {
            console.warn('[dotCMS events] init was called again with another config; ignored');
        }

        return;
    }

    debug = Boolean(config?.debug);
    onErrorHandler = config?.onError;

    if (!config?.dotcmsUrl || !config?.siteAuth) {
        console.error('[dotCMS events] init needs dotcmsUrl and siteAuth');

        return;
    }

    if (getUVEState()) {
        // Inside the editor nothing is tracked, and nothing may stay hidden.
        revealRows();
        window[ANALYTICS_WINDOWS_ACTIVE_KEY] = false;
        log('inside UVE: events off');

        return;
    }

    activeConfigKey = configKey;

    const analyticsConfig = toAnalyticsConfig(config);
    const experimentsOn = config.experiments !== false;

    engine = experimentsOn
        ? createExperimentsEngine({
              dotcmsUrl: analyticsConfig.server,
              timeoutMs: assignmentTimeout(config),
              log,
              warn,
              onError: reportError,
              pages
          })
        : null;

    if (engine) {
        // On pages dotCMS renders, a returning visitor is decided now, while the head is parsed.
        // Otherwise a new visitor's isUserIncluded leaves now, while Analytics.js loads.
        if (!engine.prepare()) {
            engine.start();
        }
    } else {
        revealRows();
    }

    // The page renderers print contentlet attributes while analytics is active, so they
    // learn it now, before they hydrate, and never render twice.
    window[ANALYTICS_WINDOWS_ACTIVE_KEY] = true;
    window.dispatchEvent(new CustomEvent(ANALYTICS_READY_EVENT));

    // Traditional pages and plain scripts reach the same object as window.dotEvents.
    const globalScope = window as unknown as Record<string, unknown>;

    if (!globalScope[EVENTS_WINDOW_KEY]) {
        globalScope[EVENTS_WINDOW_KEY] = events;
    }

    // Analytics.js reads the time zone when its module is evaluated: loading it here keeps
    // that work, and the module's, out of the task that starts the app. Calls wait in
    // pendingCalls meanwhile.
    void import('analytics')
        .then(({ Analytics }) => {
            const plugins = [
                identityPlugin(analyticsConfig),
                ...(engine ? [experimentsPlugin(engine)] : []),
                ...getEnhancedTrackingPlugins(analyticsConfig, impressionsPlugin, clicksPlugin),
                enricherPlugin(),
                senderPlugin(analyticsConfig)
            ] as AnalyticsPlugin[];

            // Analytics.js reads `storage` from its config, though its types leave it out
            const analyticsJsConfig: Parameters<typeof Analytics>[0] & {
                storage: AnalyticsJsStorage;
            } = {
                app: 'dotEvents',
                debug: analyticsConfig.debug,
                plugins,
                storage: createMemoryStorage()
            };

            instance = Analytics(analyticsJsConfig);
            log('ready', {
                experiments: experimentsOn,
                autoPageView: analyticsConfig.autoPageView
            });

            pendingCalls.splice(0).forEach((call) => call());

            if (analyticsConfig.autoPageView) {
                startAutomaticPageViews();
            }
        })
        .catch((error: unknown) => {
            console.error('[dotCMS events] Analytics.js failed to load; no events are sent', error);
        });
};

/**
 * The events SDK. There is one object per page: every import gets the same instance,
 * configured by the one `init` call.
 */
export const events: DotCMSEvents = {
    init(config) {
        initEvents(config, 'markup');
    },

    conversion(name) {
        if (!isBrowser()) {
            return;
        }

        if (!name || name.trim() === '') {
            console.warn('[dotCMS events] a conversion needs a name');

            return;
        }

        if (!instance) {
            defer(() => events.conversion(name));

            return;
        }

        instance.track(DotCMSPredefinedEventType.CONVERSION, { name });
    },

    pageView(data = {}) {
        if (!isBrowser()) {
            return;
        }

        if (!instance) {
            defer(() => events.pageView(data));

            return;
        }

        void sendPageView(data);
    }
};
