/**
 * Represents the configuration for the SDK experiment.
 *
 * @interface DotExperimentConfig
 * @property {string} 'api-key' - The API key for the experiment.
 * @property {string} server - The server URL of the backend.
 * @property {boolean} debug - Specifies whether to enable debug mode for the experiment.
 */
export interface DotExperimentConfig {
    'api-key': string;
    server: string;
    debug: boolean;
    trackPageView?: boolean;
}

/**
 * Interface representing a LookBackWindow.
 *
 * @interface LookBackWindow
 * @property {number} expireMillis - The number of days in milliseconds when the LookBackWindow should expire.
 * @property {string} value - The value associated with the LookBackWindow.
 * @property {number} expireTime - Value generated to define the expire timestamp of the experiment
 */
interface LookBackWindow {
    expireMillis: number;
    expireTime?: number;
    value: string;
}

/**
 * This interface holds regular expressions which validate whether the current page
 * is either displayed in the Experiment page, or is the target in case that goal is used.
 * The result of these regex tests yield two boolean attributes.
 * These attributes are stored as event attributes and are used later on for results.
 *
 * @interface
 * @property {string} isExperimentPage - A regular expression for validating if the page is an Experiment page.
 * @property {string | null} isTargetPage - A regular expression for validating if the page is the target page. May be null.
 */
interface Regexs {
    isExperimentPage: string;
    isTargetPage: string | null;
}

/**
 * This interface represents a Variant that is assigned at the moment of making a request.
 * It consists of a name and a fully qualified URL which includes any necessary query parameters.
 *
 * @interface
 * @property {string} name - The name of the variant.
 * @property {string} url - The fully qualified URL where the variant is being applied, with query parameters already set.
 */
export interface Variant {
    name: string;
    url: string;
}

/**
 * This interface represents an Experiment that is currently running, including all its configurations.
 * It includes identifiers like id and runningId, a lookBackWindow object, name of the experiment,
 * the URL of the page where the experiment should be applied, regular expressions stored as Regexs object
 * and a variant assigned to the user making the request.
 *
 * @interface
 * @property {string} id - The unique identifier for the experiment.
 * @property {LookBackWindow} lookBackWindow - The lookback window object for the experiment.
 * @property {string} name - The name of the experiment.
 * @property {string} pageUrl - The URL of the page where the experiment should be applied.
 * @property {Regexs} regexs - The Regexs object containing regular expressions for validating pages.
 * @property {string} runningId - The running identifier of the experiment.
 * @property {Variant} variant - The Variant assigned to the user making the request.
 */
export interface Experiment {
    id: string;
    lookBackWindow: LookBackWindow;
    name: string;
    pageUrl: string;
    regexs: Regexs;
    runningId: string;
    variant: Variant;
}

/**
 * Represents assigned experiments and their details.
 *
 * @interface AssignedExperiments
 * @property {string[]} excludedExperimentIds - The ids of experiments that are excluded in the assignment.
 * @property {Experiment[]} experiments - An array of Experiment objects representing the assigned experiments.
 * @property {string[]} includedExperimentIds - The ids of experiments that are included in the assignment.
 */
export interface AssignedExperiments {
    excludedExperimentIds: string[];
    experiments: Experiment[];
    includedExperimentIds: string[];
}

/**
 * This interface represents the response from the backend which contains all the information
 * about Running Experiments and the assignment of a variant to a user.
 * It includes an entity containing Experiment details, any possible errors, internationalization messages,
 * generic messages, pagination details if any, and permissions.
 *
 * @interface
 * @property {AssignedExperiments} entity - The entity object from which all experiment-related information is extracted.
 * @property {never[]} errors - An array that contains any possible error messages.
 * @property {Record<string, unknown>} i18nMessagesMap - A map that contains internationalization (i18n) messages.
 * @property {unknown[]} messages - An array of generic messages.
 */
export interface IsUserIncludedApiResponse {
    entity: AssignedExperiments;
    i18nMessagesMap: Record<string, string>;
    errors: string[];
    messages: string[];
}

/**
 * `ExperimentParsed` is an interface representing the parsed data of an experiment.
 *
 * This data is slated to be sent to Analytics in the PagaView event.
 * `ExperimentParsed` consists of two main properties that capture the essence of the experiment.
 *
 * @property {string} href - This is the URL of the experiment, aiding in tracking the location or origin of the experiment.
 *
 * @property {ExperimentEvent[]} experiments - This property is an array of `ExperimentEvent` objects.
 * Each `ExperimentEvent` object represents the detail of an individual experiment being carried out.
 */
export interface ExperimentParsed {
    href: string;
    experiments: ExperimentEvent[];
}

/**
 * `ExperimentEvent` is an interface representing a single experiment event.
 *
 * This event contains several properties detailing the nature, status, and categorization of the experiment.
 *
 * @property {string} experiment - This is the name or identifier of the experiment. It helps track the specific experiment being conducted.
 *
 * @property {string} runningId - This is the running identifier of the experiment. It should be unique for each instance of an experiment.
 *
 * @property {boolean} isExperimentPage - This Boolean value indicates if the current page is the one where the experiment is being conducted.
 *
 * @property {boolean} isTargetPage - This Boolean value indicates if the current page is the target page of the experiment.
 * The target is the page that you want users to land on as a result of the experiment.
 *
 * @property {string} lookBackWindow - This is the lookback window, which defines the time period for which the experiment is valid or should be considered.
 *
 * @property {string} variant - This is the variant of the experiment. Use this to specify different versions of an experiment.
 */
export interface ExperimentEvent {
    experiment: string;
    runningId: string;
    isExperimentPage: boolean;
    isTargetPage: boolean;
    lookBackWindow: string;
    variant: string;
}
