// TODO: Move this to the sdk/experiment lib
// WIP: missing logic and implemetation from 'dotCMS/src/main/resources/experiment/js/init_script.js'

const LOCAL_STORAGE_KEY = 'experiment_data';
const LOCAL_STORAGE_TIME_KEY = 'experiment_time';
const LOCAL_STORAGE_TIME_DURATION_MILLISECONDS = 24 * 60 * 60 * 1000; // a day
// const LOCAL_STORAGE_TIME_DURATION_MILLISECONDS = 5 * 1000; // a minute for test

export const QUERY_PARAM_VARIANT_KEY = 'variantName';
const QUERY_PARAM_REDIRECT_KEY = 'redirect';

//Todo: usar TS para esto en un lib
class ExperimentHelper {
    #pageLocationUrl;
    #pageExperiment;
    #regexsChecks = [];

    getPageExperiment() {
        return this.#pageExperiment;
    }

    async check(currentUrl) {
        this.#pageLocationUrl = currentUrl;

        console.info('shouldCheckAPI', this.#shouldCheckAPI());
        if (this.#shouldCheckAPI()) {
            const experiments = await this.#getExperimentConfig();
            this.#setLocalStorageData(this.#mergeParseAndCleanData(experiments));
        } else {
            console.info('data exist and valid not implemented yet');
        }
    }

    /**
     * Retrieves the experiment configuration by making a request to the server.
     *
     * @returns {Promise<Object>} A Promise that resolves with the experiment configuration.
     * @throws {Error} If there is an HTTP error during the request.
     */
    async #getExperimentConfig() {
        const { includedExperimentIds } = this.getLocalStorageData();

        // TODO: fix this url
        const response = await fetch('http://localhost:8080/api/v1/experiments/isUserIncluded', {
            method: 'POST',
            body: JSON.stringify({ exclude: includedExperimentIds || [] }),
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const { entity } = await response.json();

        return entity;
    }

    /**
     * Gets the data stored in the local storage.
     *
     * @returns {Object} - The parsed local storage data or an empty object if parsing fails.
     */
    getLocalStorageData() {
        let experimentData = localStorage.getItem(LOCAL_STORAGE_KEY);
        try {
            return experimentData ? JSON.parse(experimentData) : {};
        } catch (e) {
            console.error('Error parsing localStorage data: ', e);
            return false;
        }
    }

    /**
     * Sets the Jitsu experiment data by retrieving the experiments from local storage,
     * formatting and sending the data to the Jitsu server.
     *
     * @private
     * @returns {void}
     */
    #getParsedDataForJitsu() {
        const { experiments } = this.getLocalStorageData();
        console.log(this.getLocalStorageData());

        return experiments
            ? {
                  experiments: experiments.map((experiment) => ({
                      experiment: experiment.id,
                      runningId: experiment.runningId,
                      variant: experiment.variant.name,
                      lookBackWindow: experiment.lookBackWindow.value,
                      ...this.#parseExperimentRegexChecksForJitsu().filter(
                          (regexCheked) => regexCheked.id === experiment.id
                      )[0].checks
                  }))
              }
            : { experiments: [] };
    }

    getExperimentJitsuData() {
        return this.#getParsedDataForJitsu() || { experiments: false };
    }

    /**
     * Sets experiment data to the local storage.
     *
     * @private
     * @returns {void}
     */
    #setLocalStorageData(cleanDataToSave) {
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(cleanDataToSave));
        localStorage.setItem(LOCAL_STORAGE_TIME_KEY, Date.now().toString());
    }

    /**
     * Generates regular expression checks for each experiment in the experiment data.
     * @return {void}
     */
    #parseExperimentRegexChecksForJitsu() {
        const regexsChecks = [];

        const { experiments } = this.getLocalStorageData();
        experiments.forEach((experiment) => {
            const experimentId = experiment.id;
            const experimentRegex = { id: experimentId, checks: {} };

            Object.keys(experiment.regexs).forEach((key) => {
                const pattern = new RegExp(experiment.regexs[key]);
                let indexOf = location.href.indexOf('?');
                let urlWithoutParameters =
                    indexOf > -1 ? location.href.substring(0, indexOf) : location.href;
                let parameters = indexOf > -1 ? location.href.substring(indexOf) : '';
                let url = urlWithoutParameters.toLowerCase() + parameters;
                experimentRegex.checks[key] = pattern.test(url);
            });

            regexsChecks.push(experimentRegex);
        });

        return regexsChecks;
    }

    /**
     * Parses and cleans the given experiment data.
     *
     * @param {Object} experiments - The new experiment data to be parsed and cleaned.
     * @returns {Object} - The parsed and cleaned experiment data with updated time.
     */
    #mergeParseAndCleanData(experiments) {
        let now = Date.now();
        // Retrieve existing data from localStorage or default to an object with an empty 'experiments' field.
        let oldExperimentData = this.getLocalStorageData() || { experiments: [] };
        delete oldExperimentData['excludedExperimentIds'];

        return {
            // Retain all existing properties of oldExperimentData.
            ...oldExperimentData,

            // Merge existing and new Experiment Ids.
            includedExperimentIds: [
                ...(oldExperimentData.includedExperimentIds || []),
                ...experiments.excludedExperimentIds
            ],

            // Merge existing and new Experiments, and set new expiration time.
            experiments: [...(oldExperimentData.experiments || []), ...experiments.experiments].map(
                (experiment) => ({
                    ...experiment,
                    lookBackWindow: {
                        ...experiment.lookBackWindow,
                        expireTime: now + experiment.lookBackWindow.expireMillis
                    }
                })
            )
        };
    }

    /**
     * Determines whether to make a request to the API or not based on the token status.
     *
     * @returns {boolean} Returns true if a request should be made to the API, otherwise false.
     */
    #shouldCheckAPI() {
        const token = localStorage.getItem(LOCAL_STORAGE_KEY);
        const tokenTimeStamp = localStorage.getItem(LOCAL_STORAGE_TIME_KEY);

        if (token === null) {
            return true;
        }

        if (Date.now() - tokenTimeStamp > LOCAL_STORAGE_TIME_DURATION_MILLISECONDS) {
            localStorage.removeItem(LOCAL_STORAGE_KEY);
            localStorage.removeItem(LOCAL_STORAGE_TIME_KEY);
            return true;
        }

        this.identifyExperimentPage();

        // the token exists and it is not outdated, hence no need to make a request to the API
        return false;
    }

    #redirectRules() {
        if (!this.#pageExperiment) {
            return false;
        }

        const { checks } = this.#parseExperimentRegexChecksForJitsu().find(
            (rule) => rule.id === this.#pageExperiment.id
        );

        if (checks.isExperimentPage) {
            // yes this page has an experiment
            console.info(this.#pageLocationUrl.searchParams.get(QUERY_PARAM_VARIANT_KEY));
            if (this.#pageLocationUrl.searchParams.get(QUERY_PARAM_VARIANT_KEY) === null) {
                const variant = this.#pageExperiment.variant.name;

                this.#pageLocationUrl.searchParams.set(QUERY_PARAM_VARIANT_KEY, variant);
            } else if (
                this.#pageLocationUrl.searchParams.get(QUERY_PARAM_VARIANT_KEY) ===
                this.#pageExperiment.variant.name
            ) {
            }
        }
        return false;
    }

    removeVariantQueryParam() {
        this.#pageLocationUrl.searchParams.delete(QUERY_PARAM_VARIANT_KEY);
        window.history.replaceState(null, '', this.#pageLocationUrl.toString());
    }

    /**
     * Identifies the experiment page.
     *
     * @returns {void}
     */
    identifyExperimentPage() {
        const { pathname } = this.#pageLocationUrl;
        const { experiments } = this.getLocalStorageData();

        this.#pageExperiment =
            experiments.find((experiment) => experiment.pageUrl === pathname) || false;
    }
}

export default ExperimentHelper;
