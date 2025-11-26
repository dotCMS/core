import { EventPayload, jitsuClient, JitsuClient } from '@jitsu/sdk-js';

import {
    API_EXPERIMENTS_URL,
    DEBUG_LEVELS,
    EXPERIMENT_ALREADY_CHECKED_KEY,
    EXPERIMENT_DB_KEY_PATH,
    EXPERIMENT_DB_STORE_NAME,
    EXPERIMENT_DEFAULT_VARIANT_NAME,
    EXPERIMENT_QUERY_PARAM_KEY,
    PAGE_VIEW_EVENT_NAME
} from './shared/constants';
import {
    AssignedExperiments,
    DotExperimentConfig,
    Experiment,
    FetchExperiments,
    Variant
} from './shared/models';
import {
    getExperimentsIds,
    parseData,
    parseDataForAnalytics,
    verifyRegex
} from './shared/parser/parser';
import { IndexDBDatabaseHandler } from './shared/persistence/index-db-database-handler';
import { DotLogger } from './shared/utils/DotLogger';
import {
    checkFlagExperimentAlreadyChecked,
    defaultRedirectFn,
    getFullUrl,
    isDataCreateValid,
    objectsAreEqual,
    updateUrlWithExperimentVariant
} from './shared/utils/utils';

/**
 * `DotExperiments` is a Typescript class to handles all operations related to fetching, storing, parsing, and navigating
 * data for Experiments (A/B Testing).
 *
 * It requires a configuration object for instantiation, please instance it using the method `getInstance` sending
 * an object with `api-key`, `server` and `debug`.
 *
 * Here's an example of how you can instantiate DotExperiments class:
 * @example
 * ```typescript
 * const instance = DotExperiments.getInstance({
 *   server: "yourServerUrl",
 *   "api-key": "yourApiKey"
 * });
 * ```
 *
 * @export
 * @class DotExperiments
 *
 */
export class DotExperiments {
    /**
     * The instance of the DotExperiments class.
     * @private
     */
    private static instance: DotExperiments;
    /**
     * Represents the default configuration for the DotExperiment library.
     * @property {boolean} trackPageView - Specifies whether to track page view or not. Default value is true.
     */
    private static readonly defaultConfig: Partial<DotExperimentConfig> = {
        // By default, we track the page view
        trackPageView: true,
        // By default, debug is off
        debug: false
    };
    /**
     * Represents the promise for the initialization process.
     * @private
     */
    private initializationPromise: Promise<void> | null = null;
    /**
     * Represents the analytics client for Analytics.
     * @private
     */
    private analytics!: JitsuClient;
    /**
     * Class representing a database handler for IndexDB.
     * @class
     */
    private persistenceHandler!: IndexDBDatabaseHandler;
    /**
     * Represents the stored data in the IndexedDB.
     * @private
     */
    private experimentsAssigned: Experiment[] = [];
    /**
     * A logger utility for logging messages.
     *
     * @class
     */
    private logger!: DotLogger;
    /**
     * Represents the current location.
     * @private
     */
    // eslint-disable-next-line no-restricted-globals
    private currentLocation: Location = location;

    /**
     * Represents the previous location.
     *
     * @type {string}
     */
    private prevLocation = '';

    private constructor(private readonly config: DotExperimentConfig) {
        // merge default config and the config to the instance
        this.config = { ...DotExperiments.defaultConfig, ...config };

        if (!this.config['server']) {
            throw new Error('`server` must be provided and should not be empty!');
        }

        if (!this.config['apiKey']) {
            throw new Error('`apiKey` must be provided and should not be empty!');
        }

        this.logger = new DotLogger(this.config.debug, 'DotExperiment');
    }

    /**
     * Retrieves the array of experiments assigned to an instance of the class.
     *
     * @return {Experiment[]} An array containing the experiments assigned to the instance.
     */
    public get experiments(): Experiment[] {
        return this.experimentsAssigned;
    }

    /**
     * Returns a custom redirect function. If a custom redirect function is not configured,
     * the default redirect function will be used.
     *
     * @return {function} A function that accepts a URL string parameter and performs a redirect.
     *                    If no parameter is provided, the function will not perform any action.
     */
    public get customRedirectFn(): (url: string) => void {
        return this.config.redirectFn ?? defaultRedirectFn;
    }

    /**
     * Retrieves the current location.
     *
     * @returns {Location} The current location.
     */
    public get location(): Location {
        return this.currentLocation;
    }

    /**
     * Retrieves instance of DotExperiments class if it doesn't exist create a new one.
     * If the instance does not exist, it creates a new instance with the provided configuration and calls the `getExperimentData` method.
     *
     * @param {DotExperimentConfig} config - The configuration object for initializing the DotExperiments instance.
     * @return {DotExperiments} - The instance of the DotExperiments class.
     */
    public static getInstance(config?: DotExperimentConfig): DotExperiments {
        if (!DotExperiments.instance) {
            if (!config) {
                throw new Error('Configuration is required to create a new instance.');
            }

            DotExperiments.instance = new DotExperiments(config);
            DotExperiments.instance.initialize();

            DotExperiments.instance.logger.log(
                'Instance created with configuration: ' + JSON.stringify(config)
            );
        } else {
            DotExperiments.instance.logger.log('Instance of DotExperiments already exist');
        }

        return DotExperiments.instance;
    }

    /**
     * Waits for the initialization process to be completed.
     *
     * @return {Promise<void>} A Promise that resolves when the initialization is ready.
     */
    public ready(): Promise<void> {
        return this.initializationPromise ?? Promise.resolve();
    }

    /**
     * This method appends variant parameters to navigation links based on the provided navClass.
     *
     * Note: In order for this method's functionality to apply, you need to define a class for the navigation elements (anchors)
     * to which you would like this functionality applied and pass it as an argument when calling this method.
     *
     * @param {string} navClass - The class of the navigation elements to which variant parameters should be appended. Such elements should be anchors (`<a>`).
     *
     * @example
     * <ul class="navbar-nav me-auto mb-2 mb-md-0">
     *     <li class="nav-item">
     *         <a class="nav-link " aria-current="page" href="/">Home</a>
     *      </li>
     *      <li class="nav-item ">
     *         <a class="nav-link active" href="/blog">Travel Blog</a>
     *      </li>
     *      <li class="nav-item">
     *         <a class="nav-link" href="/destinations">Destinations</a>
     *      </li>
     * </ul>
     *
     * dotExperiment.ready().then(() => {
     *    dotExperiment.appendVariantParams('.navbar-nav .nav-link');
     * });
     * appendVariantParams('nav-item-class');
     *
     * @returns {void}
     */
    public appendVariantParams(navClass: string): void {
        // Todo: Add a config for the standalone pages
        // This is only for standalone pages
        const navItems: NodeListOf<HTMLAnchorElement> = document.querySelectorAll(navClass);

        if (navItems.length > 0) {
            navItems.forEach((link) => {
                const href =
                    getFullUrl(this.currentLocation, link.getAttribute('href') || '') ?? '';

                const variant = this.getVariantFromHref(href);

                if (variant !== null && href !== null) {
                    link.href = updateUrlWithExperimentVariant(href, variant);
                }
            });
        }
    }

    /**
     * Retrieves the current debug status.
     *
     * @private
     * @returns {boolean} - The debug status.
     */
    public getIsDebugActive(): boolean {
        return this.config.debug;
    }

    /**
     * Updates the current location and checks if a variant should be applied.
     * Redirects to the variant URL if necessary.
     *
     * @param {Location} location - The new location.
     * @param redirectFunction
     */
    public async locationChanged(
        location: Location,
        redirectFunction?: (url: string) => void
    ): Promise<void> {
        this.logger.group('Location Changed Process');
        this.logger.time('Total location changed');

        this.currentLocation = location;
        await this.verifyExperimentData();

        const variantAssigned = this.getVariantFromHref(location.href);

        if (variantAssigned && variantAssigned.name !== EXPERIMENT_DEFAULT_VARIANT_NAME) {
            const searchParams = new URLSearchParams(location.search);

            const currentVariant = searchParams.get(EXPERIMENT_QUERY_PARAM_KEY);

            if (currentVariant !== variantAssigned.name) {
                const variantUrl = updateUrlWithExperimentVariant(location, variantAssigned);

                if (redirectFunction) {
                    this.logger.log(
                        `Page redirected to ${variantUrl} using the provided redirect function.`
                    );
                    this.logger.timeEnd('Total location changed');
                    this.logger.groupEnd();
                    redirectFunction(variantUrl);
                } else {
                    this.logger.log(
                        `Page redirected to ${variantUrl} using the default redirect function.`
                    );

                    defaultRedirectFn(variantUrl);
                }
            } else {
                this.logger.log(`No redirection needed.`);
            }
        } else {
            this.logger.log(`No experiment variant matched for the current location.`);
        }

        this.logger.timeEnd('Total location changed');
        this.logger.groupEnd();
    }

    /**
     * Tracks a page view event in the analytics system.
     *
     * @return {void}
     */
    public trackPageView(): void {
        this.track(PAGE_VIEW_EVENT_NAME);
        this.prevLocation = this.currentLocation.href;
    }

    /**
     * This method is used to retrieve the variant associated with a given URL.
     *
     * It checks if the URL is part of an experiment by verifying it against an experiment's regex. If the URL matches the regex of an experiment,
     * it returns the variant attached to that experiment; otherwise, it returns null.
     *
     * @param {string | null} path - The URL to check for a variant. This should be the path of the URL.
     *
     * @returns {Variant | null} The variant associated with the URL if it exists, null otherwise.
     */
    public getVariantFromHref(path: string | null): Variant | null {
        const experiment = this.experimentsAssigned.find((experiment) => {
            const url = getFullUrl(this.currentLocation, path) ?? '';

            return verifyRegex(experiment.regexs.isExperimentPage, url);
        });

        return experiment?.variant || null;
    }

    /**
     * Returns the experiment variant name as a URL search parameter.
     *
     * @param {string|null} path - The path to the current page.
     * @returns {URLSearchParams} - The URL search parameters containing the experiment variant name.
     */
    public getVariantAsQueryParam(path: string | null): URLSearchParams {
        let params: Record<string, string> = {};

        if (this.experimentsAssigned && path) {
            const experiment = this.experimentsAssigned.find((experiment) => {
                const url = getFullUrl(this.currentLocation, path) ?? '';

                return verifyRegex(experiment.regexs.isExperimentPage, url);
            });

            if (experiment && experiment.variant.name !== EXPERIMENT_DEFAULT_VARIANT_NAME) {
                params = { [EXPERIMENT_QUERY_PARAM_KEY]: experiment.variant.name };
            }
        }

        return new URLSearchParams(params);
    }

    /**
     * Determines whether a page view should be tracked.
     *
     * @private
     * @returns {boolean} True if a page view should be tracked, otherwise false.
     */
    private shouldTrackPageView(): boolean {
        if (!this.config.trackPageView) {
            this.logger.log(
                `No send pageView. Tracking disabled. Config: ${this.config.trackPageView}.`
            );

            return false;
        }

        if (this.experimentsAssigned.length === 0) {
            this.logger.log(`No send pageView. No experiments to track.`);

            return false;
        }

        // If the previous location is the same as the current location, we don't need to track the page view
        if (this.prevLocation === this.currentLocation.href) {
            this.logger.log(`No send pageView. Same location.`);

            return false;
        }

        return true;
    }

    /**
     * Tracks an event using the analytics service.
     *
     * @param {string} typeName - The type of event to track.
     * @param {EventPayload} [payload] - Optional payload associated with the event.
     * @return {void}
     */
    private track(typeName: string, payload?: EventPayload): void {
        this.analytics.track(typeName, payload).then(() => {
            this.logger.log(`${typeName} event sent`);
        });
    }

    /**
     * Initializes the application using lazy initialization. This method performs
     * necessary setup steps and should be invoked to ensure proper execution of the application.
     *
     * Note: This method uses lazy initialization. Make sure to call this method to ensure
     * the application works correctly.
     *
     * @return {Promise<void>} A promise that resolves when the initialization is complete.
     */
    private async initialize(): Promise<void> {
        if (this.initializationPromise != null) {
            // The initialization promise already exists, so we don't need to initialize again.
            // Wait for the current initialization to complete.
            await this.initializationPromise;
        } else {
            // First time initialization or after the promise has been explicitly set to null elsewhere.
            this.initializationPromise = (async () => {
                this.logger.group('Initialization Process');
                this.logger.time('Total Initialization');
                // Load the database handler
                this.initializeDatabaseHandler();
                // Initialize the analytics client
                this.initAnalyticsClient();
                // Retrieve persisted data
                this.experimentsAssigned = await this.getPersistedData();

                await this.verifyExperimentData();

                this.logger.timeEnd('Total Initialization');
                this.logger.groupEnd();
            })();
        }
    }

    /**
     * Fetches experiments from the server.
     *
     * @private
     * @returns {Promise<AssignedExperiments>} - The entity object returned from the server.
     * @throws {Error} - If an HTTP error occurs or an error occurs during the fetch request.
     */
    private async getExperimentsFromServer(): Promise<
        Pick<AssignedExperiments, 'excludedExperimentIdsEnded' | 'experiments'>
    > {
        this.logger.group('Fetch Experiments');
        this.logger.time('Fetch Time');

        try {
            const body = {
                exclude: this.experimentsAssigned ? getExperimentsIds(this.experimentsAssigned) : []
            };

            const response: Response = await fetch(`${this.config.server}/${API_EXPERIMENTS_URL}`, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });

            if (!response.ok) {
                const responseText = await response.text();

                throw new Error(`HTTP error! status: ${response.status}, body: ${responseText}`);
            }

            const responseJson = await response.json();

            const experiments = responseJson?.entity?.experiments ?? [];

            const excludedExperimentIdsEnded =
                responseJson?.entity?.excludedExperimentIdsEnded ?? [];

            this.logger.log(`Experiment data get successfully `);

            this.persistenceHandler.setFlagExperimentAlreadyChecked();
            this.persistenceHandler.setFetchExpiredTime();

            return { experiments, excludedExperimentIdsEnded };
        } catch (error) {
            this.logger.error(
                `An error occurred while trying to fetch the experiments: ${
                    (error as Error).message
                }`
            );
            throw error;
        } finally {
            this.logger.timeEnd('Fetch Time');
            this.logger.groupEnd();
        }
    }

    /**
     * This method is responsible for retrieving and persisting experiment data from the server to the local indexDB database.
     *
     * - Checks whether making a request to the server to fetch experiment data is required.
     * - Sends the request to the server for data if required.
     * - Parses the fetched data to the form required for storage in the database.
     * - Persists the data in the indexDB database.
     *
     * @private
     * @method verifyExperimentData
     * @async
     * @throws {Error} Throws an error with details if there is any failure in loading the experiments or during their persistence in indexDB.
     *
     * @returns {Promise<void>} An empty promise that fulfills once the experiment data has been successfully loaded and persisted.
     */
    private async verifyExperimentData(): Promise<void> {
        try {
            let fetchedExperiments: FetchExperiments = {
                excludedExperimentIdsEnded: [],
                experiments: []
            };

            const storedExperiments: Experiment[] = this.experimentsAssigned
                ? this.experimentsAssigned
                : [];

            // Checks whether fetching experiment data from the server is necessary.
            if (this.shouldFetchNewData()) {
                fetchedExperiments = await this.getExperimentsFromServer();
            }

            const dataToPersist: Experiment[] = parseData(fetchedExperiments, storedExperiments);

            // If my stored data is equal to my parsed data, I don't need to persist again
            if (!objectsAreEqual(dataToPersist, storedExperiments)) {
                this.experimentsAssigned = await this.persistExperiments(dataToPersist);
            }

            this.refreshAnalyticsForCurrentLocation();
        } catch (e) {
            throw Error(`Error persisting experiments to indexDB, ${e}`);
        }
    }

    /**
     * Persists the parsed experiment data into the indexDB database.
     *
     * The method does the following:
     * - Receives the parsed data.
     * - Updates the creation date.
     * - Clears existing data from the indexDB database.
     * - Stores the new data in the indexDB database.
     *
     * If there are no experiments in the received data, the method will not attempt to clear or persist anything and will return immediately.
     *.
     *
     * @note This method utilizes Promises for the asynchronous handling of data persistence. Errors during data persistence are caught and logged, but not re-thrown.
     *
     * @private
     * @method persistExperiments
     * @throws Nothing – Errors are caught and logged, but not re-thrown.
     *
     * @param experiments
     */
    private async persistExperiments(experiments: Experiment[]): Promise<Experiment[]> {
        if (!experiments.length) {
            return [];
        }

        this.logger.group('Persisting Experiments');
        this.logger.time('Persistence Time');

        try {
            await this.persistenceHandler.persistData(experiments);
            this.logger.log('Experiment data stored successfully');

            return experiments;
        } catch (onerror) {
            this.logger.log(`Error storing data: ${onerror}`);

            return Promise.reject(`Error storing data: ${onerror}`);
        } finally {
            this.logger.timeEnd('Persistence Time');
            this.logger.groupEnd();
        }
    }

    /**
     * Initializes the database handler.
     *
     * This private method instantiates the class handling the IndexDB database
     * and assigns this instance to 'persistenceHandler'.
     *
     * @private
     */
    private initializeDatabaseHandler(): void {
        this.persistenceHandler = new IndexDBDatabaseHandler({
            db_store: EXPERIMENT_DB_STORE_NAME,
            db_name: EXPERIMENT_DB_STORE_NAME,
            db_key_path: EXPERIMENT_DB_KEY_PATH
        });
    }

    /**
     * Initializes the Jitsu analytics client.
     *
     * This private method sets up the Jitsu client responsible for sending events
     * to the server with the provided configuration. It also uses the parsed data
     * and registers it as global within Jitsu.
     *
     * @private
     */
    private initAnalyticsClient(): void {
        try {
            if (!this.analytics) {
                this.analytics = jitsuClient({
                    key: this.config['apiKey'],
                    tracking_host: this.config['server'],
                    log_level: this.config['debug'] ? DEBUG_LEVELS.WARN : DEBUG_LEVELS.NONE
                });
                this.logger.log('Analytics client initialized successfully.');
            }
        } catch (error) {
            this.logger.log(`Error creating/updating analytics client: ${error}`);
        }
    }

    /**
     * Updates the analytics client's data using the experiments data
     * currently available in the IndexDB database, based on the current location.
     *
     * Retrieves and processes the experiments information according to the
     * current location into a suitable format for `analytics.set()`.
     *
     * @private
     * @method refreshAnalyticsForCurrentLocation
     * @returns {void}
     */
    private refreshAnalyticsForCurrentLocation(): void {
        const experimentsData = this.experimentsAssigned ?? [];

        if (experimentsData.length > 0) {
            const { experiments } = parseDataForAnalytics(experimentsData, this.currentLocation);

            this.analytics.set({ experiments });

            this.logger.log('Analytics client updated with experiments data.');
        } else {
            this.analytics.set({ experiments: [] });
            this.logger.log('No experiments data available to update analytics client.');
        }

        // trigger the page view event
        if (this.shouldTrackPageView()) {
            this.trackPageView();

            return;
        }
    }

    /**
     * Determines whether analytics should be checked.
     *
     * @private
     * @returns {Promise<boolean>} A boolean value indicating whether analytics should be checked.
     */
    private shouldFetchNewData(): boolean {
        // If the user close the tab, reload the data
        if (!checkFlagExperimentAlreadyChecked()) {
            this.logger.log(
                `${EXPERIMENT_ALREADY_CHECKED_KEY} not found, fetch data from Analytics`
            );

            return true;
        }

        if (!this.experimentsAssigned || this.experimentsAssigned.length === 0) {
            this.logger.log(`No experiments assigned to the client, fetch data from Analytics`);

            return true;
        }

        if (!isDataCreateValid()) {
            this.logger.log(
                `The validity period of the persistence has passed, fetch data from Analytics`
            );

            return true;
        }

        this.logger.log(`Not should Check Analytics by now...`);

        return false;
    }

    /**
     * Retrieves persisted data from the database.
     *
     * @private
     * @returns {Promise<void>} A promise that resolves with no value.
     */
    private async getPersistedData(): Promise<Experiment[]> {
        this.logger.group('Loading Persisted Data');
        this.logger.time('Loading Time');

        let storedData: Experiment[] = [];

        try {
            storedData = await this.persistenceHandler.getData<Experiment[]>();

            if (storedData) {
                this.logger.log(`Data persisted loaded, ${storedData.length} experiments loaded`);
            } else {
                this.logger.log(`No persisted data found, let's fetch from the server.`);
            }
        } catch (error) {
            this.logger.warn(`Error loading persisted data: ${error}`);
        } finally {
            this.logger.timeEnd('Loading Time');
            this.logger.groupEnd();
        }

        return storedData;
    }
}
