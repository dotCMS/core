import { API_EXPERIMENTS_URL } from './contants';
import { IsUserIncludedApiResponse, SdkExperimentConfig } from './models';
import { persistData } from './persistence/persistence';
import { Logger } from './utils/utils';

/**
 * This class handles all operations related to fetching, storing, parsing, and navigating data for SdkExperiments.
 * Utilizes the Jitsu SDK to send events.
 *
 * The operations are
 * 1. Fetch and store data ✅
 * 2. Parse data for backend use
 * 3. Detect and redirect to the appropriate variant
 * 4. Send event using Jitsu SDK
 */
export class SdkExperiments {
    private static instance: SdkExperiments;

    constructor(private config: SdkExperimentConfig) {
        // api-key could be an empty string but server should not be empty or undefined.
        if (!this.config.server) {
            throw new Error('server must be provided and should not be empty!');
        }

        Logger(
            `SDK instanced with ${JSON.stringify(config)} configuration`,
            this.getIsDebugActive()
        );
    }

    /**
     * Retrieves or initializes the instance of SdkExperiments class.
     * If the instance does not exist, it creates a new instance with the provided configuration and calls the 'getExperimentData' method.
     *
     * @param {SdkExperimentConfig} config - The configuration object for initializing the SdkExperiments instance.
     * @return {SdkExperiments} - The instance of the SdkExperiments class.
     */
    public static getInstance(config: SdkExperimentConfig): SdkExperiments {
        if (!SdkExperiments.instance) {
            SdkExperiments.instance = new SdkExperiments(config);
            this.instance.getExperimentData();
        }

        return SdkExperiments.instance;
    }

    /**
     * Fetches experiments from the server.
     *
     * @private
     * @returns {Promise<Object>} - A promise that resolves to the entity object if the request is successful.
     * @throws {Error} - If there is an HTTP error.
     */
    private async fetchExperiments(): Promise<IsUserIncludedApiResponse['entity']> {
        const response: Response = await fetch(`${this.config.server}${API_EXPERIMENTS_URL}`, {
            method: 'POST',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json'
            }
        });

        const responseJson = (await response.json()) as IsUserIncludedApiResponse;
        const { status } = response;

        switch (status) {
            case 200:
                Logger(`Experiment data get successfully `, this.getIsDebugActive());

                return responseJson.entity;

            default:
                // TODO: Handle error array
                throw new Error(`HTTP error! status: ${status}`);
        }
    }

    /**
     * Retrieves the current debug status.
     *
     * @private
     * @returns {boolean} - The debug status.
     */
    private getIsDebugActive(): boolean {
        return this.config.debug;
    }

    /**
     * Retrieves experiment data from the server.
     *
     * @private
     * @async
     * @throws {Error} If an error occurs while loading the experiments.
     */
    private async getExperimentData() {
        try {
            this.persistExperiments(await this.fetchExperiments());
        } catch (e) {
            // TODO: Improve error handling
            throw Error(`Error loading experiments from ${this.config.server}, ${e}`);
        }
    }

    /**
     * Persists experiments to the IndexDB.
     *
     * @param {IsUserIncludedApiResponse['entity']} entity - The entity containing experiments to persist.
     * @private
     */
    private persistExperiments(entity: IsUserIncludedApiResponse['entity']) {
        if (entity.experiments.length > 0) {
            // TODO: Final parsed data will be stored
            persistData(entity);
            Logger(`Experiment data stored successfully `, this.getIsDebugActive());
        }
    }
}
