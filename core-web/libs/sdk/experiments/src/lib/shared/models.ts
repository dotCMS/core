/**
 * Represents the configuration for the SDK experiment.
 *
 * @interface DotExperimentConfig
 */
export interface DotExperimentConfig {
    /**
     * This is `Analytics Key` provided by Analytics Integration App at DotCMS.
     */
    apiKey: string;
    /**
     * The URL of the Public DotCMS Host server for the SDK to connect to.
     */
    server: string;
    /**
     * A flag that determines whether the experiment is in debug mode or not. When in debug mode, the SDK could provide more logging information.
     */
    debug: boolean;
    /**
     * An optional property. True by default to automatically track page views. It's useful for understanding user behavior in the experiment.
     */
    trackPageView?: boolean;
    /**
     * An optional property. It is a function that handles URL redirections. When supplied, the SDK will call this function instead of using the default browser redirect when it's necessary to redirect the page.
     * @param url
     */
    redirectFn?: (url: string) => void;
}

/**
 * Represents the configuration for the LookBackWindow.
 *
 * @interface LookBackWindow
 */
export interface LookBackWindow {
    /**
     * Defines the time period in milliseconds when the LookBackWindow should expire.
     */
    expireMillis: number;

    /**
     * A value indicating the expiration timestamp of the experiment.
     */
    expireTime?: number;

    /**
     * Represents the associated value with the LookBackWindow.
     */
    value: string;
}

/**
 * Represents the configurations for checking whether the current page is an Experiment page, or a target.
 *
 * @interface Regexs
 */
interface Regexs {
    /**
     * A regular expression for validating if the page is an Experiment page.
     */
    isExperimentPage: string;

    /**
     * A regular expression for validating if the page is the target page. This can be null.
     */
    isTargetPage: string | null;
}

/**
 * Represents a variant that is applied when a request is made.
 *
 * @interface Variant
 */
export interface Variant {
    /**
     * The name of the variant.
     */
    name: string;

    /**
     * The fully qualified URL where the variant is being applied, with query parameters already set.
     */
    url: string;
}

/**
 * Represents an experiment with all its configurations.
 *
 * @interface Experiment
 */
export interface Experiment {
    /**
     * The unique identifier for the experiment.
     */
    id: string;

    /**
     * The lookback window object for the experiment.
     */
    lookBackWindow: LookBackWindow;

    /**
     * The name of the experiment.
     */
    name: string;

    /**
     * The URL of the page where the experiment is applied.
     */
    pageUrl: string;

    /**
     * The object containing regular expressions for validating pages.
     */
    regexs: Regexs;

    /**
     * The unique running identifier for the experiment.
     */
    runningId: string;

    /**
     * The variant applied to the user making the request.
     */
    variant: Variant;
}

/**
 * Represents the experiments assigned and their details.
 *
 * @interface AssignedExperiments
 */
export interface AssignedExperiments {
    /**
     * The ids of the experiments that are excluded in the assignment.
     */
    excludedExperimentIds: string[];

    /**
     * An array representing the assigned experiments.
     */
    experiments: Experiment[];

    /**
     * The ids of the experiments included in the assignment.
     */
    includedExperimentIds: string[];

    /**
     * The ids of the experiments that are excluded in the request and have ended.
     */
    excludedExperimentIdsEnded: string[];
}

/**
 * Represents the response from the backend when fetching an experiment and the excludedExperimentIdsEnded.
 */
export type FetchExperiments = Pick<
    AssignedExperiments,
    'excludedExperimentIdsEnded' | 'experiments'
>;

/**
 * Represents the response from backend holding information about running experiments and variant assignment.
 *
 * @interface IsUserIncludedApiResponse
 */
export interface IsUserIncludedApiResponse {
    /**
     * The object holding all experiment-related information.
     */
    entity: AssignedExperiments;

    /**
     * An array holding possible error messages.
     */
    errors: string[];

    /**
     * A map that holds internationalization (i18n) messages.
     */
    i18nMessagesMap: Record<string, string>;

    /**
     * An array of generic messages.
     */
    messages: string[];
}

/**
 * Represents a single experiment event.
 *
 * @interface ExperimentEvent
 */
export interface ExperimentEvent {
    /**
     * The name or identifier of the experiment.
     */
    experiment: string;

    /**
     * The unique running identifier of the experiment.
     */
    runningId: string;

    /**
     * A flag that determines if the current page is the one where the experiment is conducted.
     */
    isExperimentPage: boolean;

    /**
     * A flag that determines if the current page is the target page of the experiment.
     */
    isTargetPage: boolean;

    /**
     * Represents the time period for which the experiment is valid or should be considered.
     */
    lookBackWindow: string;

    /**
     * Represents the variant of the experiment to specify different versions of an experiment.
     */
    variant: string;
}

/**
 * Represents parsed data of an experiment that is ready to be sent to Analytics.
 *
 * @interface ExperimentParsed
 */
export interface ExperimentParsed {
    /**
     * The URL of the experiment. It helps track the location or origin of the experiment.
     */
    href: string;

    /**
     * An array holding details of individual experiments being conducted.
     */
    experiments: ExperimentEvent[];
}
