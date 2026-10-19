/**
 * Attribute on the element that wraps what an experiment varies, with the experiment's id.
 * `DotCMSExperiment` and `experimentMarkup` put it there; the engine reads it.
 */
export const EXPERIMENT_ATTRIBUTE = 'data-dot-experiment';

/** Attribute with the variant the server rendered in that element. */
export const VARIANT_ATTRIBUTE = 'data-dot-variant';

/** Name dotCMS gives the original variant. */
export const DEFAULT_VARIANT = 'DEFAULT';

/** Query parameter the page API reads to render a variant. */
export const VARIANT_QUERY_PARAM = 'variantName';

/** Endpoint that assigns the visitor to the running experiments. */
export const IS_USER_INCLUDED_PATH = '/api/v1/experiments/isUserIncluded';

/**
 * How long an experiment's content may stay hidden while its variant is decided. Past it the
 * hiding rule shows the content, the boot script no longer redirects, and the engine's default
 * wait ends.
 */
export const DECISION_TIMEOUT_MS = 3000;

/** Default wait for the assignment on the pages the engine decides itself. */
export const DEFAULT_ASSIGNMENT_TIMEOUT_MS = DECISION_TIMEOUT_MS;

/** The request itself may run longer than the wait, so the next page can use its answer. */
export const IS_USER_INCLUDED_REQUEST_TIMEOUT_MS = 10000;

export const ONE_DAY_MS = 24 * 60 * 60 * 1000;

/** How long the page being replaced keeps its new requests waiting. */
export const QUIET_MS = 5000;

/**
 * Rule printed before an experiment's content: the marked element stays hidden until the
 * variant is decided, and shows on its own after `DECISION_TIMEOUT_MS` if nothing decides it
 * (JavaScript off, SDK missing).
 */
export const HIDING_RULE =
    `[${EXPERIMENT_ATTRIBUTE}]{visibility:hidden;animation:dotcms-experiment-timeout 0s linear ${DECISION_TIMEOUT_MS / 1000}s forwards}` +
    `@keyframes dotcms-experiment-timeout{to{visibility:visible}}`;

export const STORAGE_KEYS = {
    /** localStorage: assignments and the experiment ids already evaluated. */
    assignments: 'dot_events_experiments',
    /** localStorage: until when the site answered 403 (Experiments off). */
    disabledUntil: 'dot_events_experiments_off_until',
    /** sessionStorage: this tab already asked `isUserIncluded`. */
    checkedThisTab: 'dot_events_experiments_checked',
    /** sessionStorage: the cumulative `context.experiments` of the analytics session. */
    sessionExperiments: 'dot_events_session_experiments'
} as const;

/** Window property where the boot script leaves its decision on the page it ran on. */
export const BOOT_STATE_KEY = '__dotEventsExperimentBoot';

/** Id of the style element that reveals decided content. */
export const REVEAL_STYLE_ID = 'dotcms-experiment-reveal';
