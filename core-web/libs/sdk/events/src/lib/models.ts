/*
 * The public types live here, apart from the core's internal ones, so the package's contract
 * is only what index.ts exports. They keep the core's shapes, so the config passes straight
 * through to it.
 */

/** A value in a pageview's custom data. */
export type DotCMSEventsJsonValue =
    | string
    | number
    | boolean
    | null
    | undefined
    | DotCMSEventsJsonObject
    | DotCMSEventsJsonValue[];

/** The custom data a pageview carries. */
export type DotCMSEventsJsonObject = { [key: string]: DotCMSEventsJsonValue };

/** Minimum level for the analytics core's console logs. */
export type DotCMSEventsLogLevel = 'debug' | 'info' | 'warn' | 'error';

/**
 * Options for content impressions.
 */
export interface DotCMSEventsImpressionsConfig {
    /** Share of the contentlet that must be in the viewport, from 0 to 1. Defaults to 0.5. */
    visibilityThreshold?: number;
    /** How long, in milliseconds, the contentlet must stay in view. Defaults to 750. */
    dwellMs?: number;
    /** Most contentlets tracked on a page. Defaults to 100. */
    maxNodes?: number;
    /** Throttle for intersection callbacks, in milliseconds. */
    throttleMs?: number;
}

/**
 * Options for batching.
 */
export interface DotCMSEventsQueueConfig {
    /** Most events in one batch; a full batch is sent at once. Defaults to 15. */
    eventBatchSize?: number;
    /** Milliseconds between sends. Defaults to 5000. */
    flushInterval?: number;
}

/**
 * Options for the experiments engine.
 */
export interface DotCMSEventsExperimentsConfig {
    /**
     * How long, in milliseconds, a pageview and the experiment's hidden rows wait for the
     * assignment before the page is shown without it. Defaults to 3000.
     */
    timeout?: number;
}

/**
 * Configuration for `dotEvents.init()`.
 */
export interface DotCMSEventsConfig {
    /** The dotCMS origin, the same value `createDotCMSClient` takes. */
    dotcmsUrl: string;
    /** The site's Site Auth, from the Content Analytics app. */
    siteAuth: string;
    /** A pageview on load and on every History change. Defaults to `true`. */
    autoPageView?: boolean;
    /** Content impressions. Off by default. */
    impressions?: DotCMSEventsImpressionsConfig | boolean;
    /** Content clicks. Off by default. */
    clicks?: boolean;
    /**
     * Experiments: `isUserIncluded`, the pageview hold, the redirect and
     * `context.experiments`. On by default; `false` turns every part off.
     */
    experiments?: boolean | DotCMSEventsExperimentsConfig;
    /** Batching, as in today's analytics config. */
    queue?: DotCMSEventsQueueConfig | boolean;
    /** Logs what the SDK does to the console. */
    debug?: boolean;
    /** Minimum log level for the analytics core. */
    logLevel?: DotCMSEventsLogLevel;
    /**
     * Called when something fails where the page cannot see it: dotCMS rejects events, an events
     * request gets no answer, or the experiments check fails. Nothing is retried, and an error
     * this handler throws is ignored.
     */
    onError?: (error: DotCMSEventsError) => void;
}

/**
 * What failed, for `onError`:
 * - `REJECTED`: dotCMS answered an events request with an error, or accepted it but failed
 *   some of its events; `status` and `detail` (its error list) say why.
 * - `NETWORK`: an events request got no answer.
 * - `EXPERIMENTS`: `isUserIncluded` failed; a 403 means experiments are off for the site.
 */
export type DotCMSEventsErrorCode = 'REJECTED' | 'NETWORK' | 'EXPERIMENTS';

/** A failure reported to `onError`. */
export interface DotCMSEventsError {
    code: DotCMSEventsErrorCode;
    message: string;
    /** The HTTP status, when dotCMS answered. */
    status?: number;
    /** What dotCMS said about the failure, for `REJECTED`. */
    detail?: unknown;
    /** How many events the failed request carried, for `REJECTED` and `NETWORK`. */
    events?: number;
}

/**
 * The events object. There is one per page: every import gets the same instance.
 */
export interface DotCMSEvents {
    /** Starts the SDK. Calling it again with the same config does nothing. */
    init(config: DotCMSEventsConfig): void;
    /** Sends a conversion. dotCMS records its name and the page it happened on, nothing else. */
    conversion(name: string): void;
    /** Sends a pageview by hand, for apps that set `autoPageView: false`. */
    pageView(data?: DotCMSEventsJsonObject): void;
}

/**
 * The page fields `experimentMarkup` and `DotCMSExperiment` read. A dotCMS page asset fits,
 * requested for the variant in the URL's `variantName`.
 */
export interface DotCMSEventsExperimentPage {
    /** The experiment running on the page, if any. */
    runningExperimentId?: string | null;
    /** `variantId` is the variant the server rendered; `DEFAULT` when absent. */
    viewAs?: { variantId?: string | null } | null;
}

/**
 * What a page prints so its experiment runs, from `experimentMarkup`. The style and the
 * script go before the element that wraps what the experiment varies, in the HTML the server
 * sends: the script decides the visitor's variant while that HTML is parsed.
 */
export interface DotCMSEventsExperimentMarkup {
    /** Attributes for the element that wraps what the experiment varies. */
    attributes: { 'data-dot-experiment': string; 'data-dot-variant': string };
    /** The rule that keeps that element hidden until the variant is decided. */
    style: string;
    /** The boot script's source, for an inline script element. */
    script: string;
}
