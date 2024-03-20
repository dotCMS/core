import { jitsuClient, JitsuClient } from '@jitsu/sdk-js';

import {
    API_EXPERIMENTS_URL,
    DEBUG_LEVELS,
    EXPERIMENT_DB_KEY_PATH,
    EXPERIMENT_DB_STORE_NAME
} from './constants';
import {
    AssignedExperiments,
    DotExperimentConfig,
    IndexDbStoredData,
    IsUserIncludedApiResponse
} from './models';
import { parseData, parseDataForAnalytics } from './parser/parser';
import { IndexDBDatabaseHandler } from './persistence/index-db-database-handler';
import {
    checkFlagExperimentAlreadyChecked,
    checkInvalidateDataChecked,
    dotLogger
} from './utils/utils';

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
    private static instance: DotExperiments;
    /**
     * Represents the analytics client for Analytics.
     */
    private analytics!: JitsuClient;
    private persistenceHandler!: IndexDBDatabaseHandler;
    private indexDBData!: IndexDbStoredData;

    private constructor(private config: DotExperimentConfig) {
        if (!this.config['server']) {
            throw new Error('`server` must be provided and should not be empty!');
        }

        if (!this.config['api-key']) {
            throw new Error('`api-key` must be provided and should not be empty!');
        }

        dotLogger(
            `DotExperiments instanced with ${JSON.stringify(config)} configuration`,
            this.getIsDebugActive()
        );
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
        }

        return DotExperiments.instance;
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
    public async initialize(): Promise<void> {
        this.initializeDatabaseHandler();
        await this.getPersistedData();
        await this.setExperimentData();
    }

    /**
     * Retrieves the current debug status.
     *
     * @private
     * @returns {boolean} - The debug status.
     */
    getIsDebugActive(): boolean {
        return this.config.debug;
    }

    /**
     * Fetches experiments from the server.
     *
     * @private
     * @returns {Promise<AssignedExperiments>} - The entity object returned from the server.
     * @throws {Error} - If an HTTP error occurs or an error occurs during the fetch request.
     */
    private async getExperimentsFromServer(): Promise<AssignedExperiments> {
        try {
            const body = {
                exclude: this.indexDBData?.experiments.includedExperimentIds || []
            };
            const response: Response = await fetch(`${this.config.server}${API_EXPERIMENTS_URL}`, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(body)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const responseJson = (await response.json()) as IsUserIncludedApiResponse;

            dotLogger(`Experiment data get successfully `, this.getIsDebugActive());

            this.persistenceHandler.setFlagExperimentAlreadyChecked();

            return responseJson.entity;
        } catch (error) {
            throw new Error(`An error occurred while trying to fetch the experiments: ${error}`);
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
     * @method setExperimentData
     * @async
     * @throws {Error} Throws an error with details if there is any failure in loading the experiments or during their persistence in indexDB.
     *
     * @returns {Promise<void>} An empty promise that fulfills once the experiment data has been successfully loaded and persisted.
     */
    private async setExperimentData() {
        try {
            let fetchExperiments: AssignedExperiments | null = null;
            let storedExperiments: AssignedExperiments | null = null;

            // Checks whether fetching experiment data from the server is necessary.
            if (this.shouldCheckAnalytics()) {
                fetchExperiments = await this.getExperimentsFromServer();
            }

            // Parses the fetched data for storage.
            storedExperiments = this.indexDBData ? this.indexDBData.experiments : null;

            const dataToPersist: AssignedExperiments = parseData(
                fetchExperiments,
                storedExperiments
            );

            // Persists the data in the indexDB.
            this.persistExperiments(dataToPersist);

            //
            this.initAnalyticsClient();
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
     *
     * Any errors encountered during storage are logged with the `dotLogger` utility.
     *
     * @note This method utilizes Promises for the asynchronous handling of data persistence. Errors during data persistence are caught and logged, but not re-thrown.
     *
     * @param {AssignedExperiments} entity - The object containing the experiment data to persist.
     * @private
     * @method persistExperiments
     * @throws Nothing – Errors are caught and logged, but not re-thrown.
     *
     */
    private persistExperiments(entity: AssignedExperiments) {
        const dataToStore = {
            created: Date.now(),
            experiments: entity
        };

        if (!entity.experiments.length) {
            return;
        }

        this.indexDBData = dataToStore;

        this.persistenceHandler.clearData().then(() => {
            this.persistenceHandler
                .persistData(dataToStore)
                .then(() => {
                    dotLogger('Experiment data stored successfully', this.getIsDebugActive());
                })
                .catch((onerror) => {
                    dotLogger(`Error storing data. ${onerror}`, this.getIsDebugActive());
                });
        });
    }

    /**
     * Initializes the database handler.
     *
     * This private method instantiates the class handling the IndexDB database
     * and assigns this instance to 'persistenceHandler'.
     *
     * @private
     */
    private initializeDatabaseHandler() {
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
    private initAnalyticsClient() {
        try {
            this.analytics = jitsuClient({
                key: this.config['api-key'],
                tracking_host: this.config['server'],
                log_level: this.config['debug'] ? DEBUG_LEVELS.DEBUG : DEBUG_LEVELS.WARN
            });

            const { experiments } = parseDataForAnalytics(this.indexDBData.experiments, location);
            this.analytics.set({ experiments });

            dotLogger(`Analytics client created successfully.`, this.getIsDebugActive());
        } catch (error) {
            throw Error(`Error creating analytics client, ${error}`);
        }
    }

    /**
     * Determines whether analytics should be checked.
     *
     * @private
     * @returns {Promise<boolean>} A boolean value indicating whether analytics should be checked.
     */
    private shouldCheckAnalytics(): boolean {
        // If the user close the tab, reload the data
        if (!checkFlagExperimentAlreadyChecked()) {
            this.persistenceHandler.clearData();

            return true;
        }

        if (!this.indexDBData) {
            return true;
        }

        if (checkInvalidateDataChecked(this.indexDBData)) {
            return true;
        }

        dotLogger(`Not should Check Analytics by now...`, this.getIsDebugActive());

        return false;
    }

    /**
     * Retrieves persisted data from the database.
     *
     * @private
     * @returns {Promise<void>} A promise that resolves with no value.
     */
    private async getPersistedData(): Promise<void> {
        const storedData = await this.persistenceHandler.getData<IndexDbStoredData>();
        if (storedData) {
            this.indexDBData = storedData;
        }
    }
}
