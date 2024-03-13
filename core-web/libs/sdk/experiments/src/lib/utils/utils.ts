import {
    EXPERIMENT_ALLOWED_DATA_ATTRIBUTES,
    EXPERIMENT_ALREADY_CHECKED_KEY,
    EXPERIMENT_SCRIPT_FILE_NAME,
    LOCAL_STORAGE_TIME_DURATION_MILLISECONDS
} from '../constants';
import { DotExperimentConfig } from '../models';

/**
 * Returns the first script element that includes the experiment script identifier.
 *
 * @return {HTMLScriptElement|undefined} - The found script element or undefined if none is found.
 */
export const getExperimentScriptTag = (): HTMLScriptElement => {
    const experimentScript = Array.from(document.getElementsByTagName('script')).find((script) =>
        script.src.includes(EXPERIMENT_SCRIPT_FILE_NAME)
    );

    if (!experimentScript) {
        throw new Error('Experiment script not found');
    }

    return experimentScript;
};

/**
 * Retrieves experiment attributes from a given script element.
 *
 *
 * @return {DotExperimentConfig | null} - The experiment attributes or null if there are no valid attributes present.
 */
export const getDataExperimentAttributes = (): DotExperimentConfig | null => {
    const script = getExperimentScriptTag();
    const defaultExperimentAttributes: DotExperimentConfig = {
        'api-key': '',
        server: window.location.href,
        debug: false
    };

    let experimentAttribute: Partial<DotExperimentConfig> = {};

    if (!script.hasAttribute('data-experiment-api-key')) {
        dotLogger('You need specify the `data-experiment-api-key`');

        return null;
    }

    Array.from(script.attributes).forEach((attr) => {
        if (EXPERIMENT_ALLOWED_DATA_ATTRIBUTES.includes(attr.name)) {
            // Server of dotCMS
            if (attr.name === 'data-experiment-server') {
                experimentAttribute = { ...experimentAttribute, server: attr.value };
            }

            // Api Key for Analytics App
            if (attr.name === 'data-experiment-api-key') {
                experimentAttribute = { ...experimentAttribute, 'api-key': attr.value };
            }

            // Show debug
            if (attr.name === 'data-experiment-debug') {
                experimentAttribute = {
                    ...experimentAttribute,
                    debug: true
                };
            }
        }
    });

    return { ...defaultExperimentAttributes, ...experimentAttribute };
};

/**
 * Retrieves the data attributes from the experiment script tag.
 *
 * @example
 * Given the custom script tag in your HTML:
 * <script src="/dot-experiments.iife.js"
 *        defer=""
 *        data-experiment-api-key="api-token"
 *        data-experiment-server="http://localhost:8080/"
 *        data-experiment-debug>
 * </script>
 *
 * @returns {DotExperimentConfig | null} The data attributes of the experiment script tag, or null if no experiment script is found.
 */
export const getScriptDataAttributes = (): DotExperimentConfig | null => {
    const dataExperimentAttributes = getDataExperimentAttributes();

    if (dataExperimentAttributes) {
        return dataExperimentAttributes;
    }

    return null;
};

/**
 * Logger is a function that logs a message to the console with a prefix.
 *
 * @param {string} msg - The message to be logged.
 * @param isDebug
 * @returns {void}
 */
export const dotLogger = (msg: string, isDebug?: boolean): void => {
    if (isDebug !== false) {
        console.warn(`[dotCMS Experiments] ${msg}`);
    }
};

/**
 * Sets the flag indicating that the experiment has already been checked.
 *
 * @function setFlagExperimentAlreadyChecked
 * @returns {void}
 */
export const setFlagExperimentAlreadyChecked = (): void => {
    sessionStorage.setItem(EXPERIMENT_ALREADY_CHECKED_KEY, 'true');
};

/**
 * Checks the flag indicating whether the experiment has already been checked.
 *
 * @function checkFlagExperimentAlreadyChecked
 * @returns {boolean} - returns true if experiment has already been checked, otherwise false.
 */
export const checkFlagExperimentAlreadyChecked = (): boolean => {
    const flag = sessionStorage.getItem(EXPERIMENT_ALREADY_CHECKED_KEY);

    return flag === 'true';
};

/**
 * Checks if the data needs to be invalidated based on the creation date.
 *
 * @param {number} created - The creation date of the data in milliseconds.
 * @returns {boolean} - True if the data needs to be invalidated, false otherwise.
 */
export const checkInvalidateDataChecked = (created: number): boolean => {
    const now = Date.now();

    return now - created > LOCAL_STORAGE_TIME_DURATION_MILLISECONDS;
};
