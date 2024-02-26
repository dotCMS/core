import { API_EXPERIMENTS_URL } from './constants';
import { AssignedExperiments, IsUserIncludedApiResponse, SdkExperimentConfig } from './models';
import { persistData } from './persistence/persistence';
import { dotLogger } from './utils/utils';

/**
 * `SdkExperiments` is a Typescript class to handles all operations related to fetching, storing, parsing, and navigating
 * data for Experiments (A/B Testing).
 *
 * It requires a configuration object for instantiation, please instance it using the method `getInstance` sending
 * an object with `api-key`, `server` and `debug`.
 *
 * Here's an example of how you can instantiate SdkExperiments class:
 * @example
 * ```typescript
 * const instance = SdkExperiments.getInstance({
 *   server: "yourServerUrl",
 *   "api-key": "yourApiKey"
 * });
 * ```
 *
 * @export
 * @class SdkExperiments
 *
 */
export class SdkExperiments {
    private static instance: SdkExperiments;

    constructor(private config: SdkExperimentConfig) {
        if (!this.config['server']) {
            throw new Error('`server` must be provided and should not be empty!');
        }

        if (!this.config['api-key']) {
            throw new Error('`api-key` must be provided and should not be empty!');
        }

        dotLogger(
            `SDK instanced with ${JSON.stringify(config)} configuration`,
            this.getIsDebugActive()
        );

        // TODO: Steps
        // - 1. Fetching and storing data ✅
        // - 2. Parsing data for backend use
        // - 3. Detecting and navigate to the appropriate variant
        // - 4. Sending events to Analytics Cloud
    }

    /**
     * Retrieves instance of SdkExperiments class if doesn't exist create a new one.
     * If the instance does not exist, it creates a new instance with the provided configuration and calls the `getExperimentData` method.
     *
     * @param {SdkExperimentConfig} config - The configuration object for initializing the SdkExperiments instance.
     * @return {SdkExperiments} - The instance of the SdkExperiments class.
     */
    public static getInstance(config: SdkExperimentConfig): SdkExperiments {
        if (!SdkExperiments.instance) {
            SdkExperiments.instance = new SdkExperiments(config);

            this.instance.setExperimentData();
        }

        return SdkExperiments.instance;
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
            const response: Response = await fetch(`${this.config.server}${API_EXPERIMENTS_URL}`, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const responseJson = (await response.json()) as IsUserIncludedApiResponse;
            dotLogger(`Experiment data get successfully `, this.getIsDebugActive());

            return responseJson.entity;
        } catch (error) {
            throw new Error(`An error occurred while trying to fetch the experiments: ${error}`);
        }
    }

    /**
     * Retrieves experiment data from the server.
     *
     * @private
     * @async
     * @throws {Error} If an error occurs while loading the experiments.
     */
    private async setExperimentData() {
        try {
            this.persistExperiments(await this.getExperimentsFromServer());
        } catch (e) {
            throw Error(`Error persisting experiments to indexDB, ${e}`);
        }
    }

    /**
     * Persists experiments to the IndexDB.
     *
     * @param {AssignedExperiments} entity - The entity containing experiments to persist.
     * @private
     */
    private persistExperiments(entity: AssignedExperiments) {
        if (entity.experiments.length > 0) {
            // TODO: Final parsed data will be stored
            persistData(entity)
                .then(() => {
                    dotLogger(`Experiment data stored successfully `, this.getIsDebugActive());
                })
                .catch((onerror) => {
                    dotLogger(`Error storing data. ${onerror}`, this.getIsDebugActive());
                });
        }
    }
}
