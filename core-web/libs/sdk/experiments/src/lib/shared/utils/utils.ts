import {
    EXPERIMENT_ALLOWED_DATA_ATTRIBUTES,
    EXPERIMENT_ALREADY_CHECKED_KEY,
    EXPERIMENT_FETCH_EXPIRE_TIME_KEY,
    EXPERIMENT_QUERY_PARAM_KEY,
    EXPERIMENT_SCRIPT_FILE_NAME
} from '../constants';
import { DotExperimentConfig, Experiment, Variant } from '../models';

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
export const getDataExperimentAttributes = (location: Location): DotExperimentConfig | null => {
    const script = getExperimentScriptTag();

    const defaultExperimentAttributes: DotExperimentConfig = {
        apiKey: '',
        server: location.href,
        debug: false
    };

    let experimentAttribute: Partial<DotExperimentConfig> = {};

    if (!script.hasAttribute('data-experiment-api-key')) {
        throw new Error('You need specify the `data-experiment-api-key`');
    }

    Array.from(script.attributes).forEach((attr) => {
        if (EXPERIMENT_ALLOWED_DATA_ATTRIBUTES.includes(attr.name)) {
            // Server of dotCMS
            if (attr.name === 'data-experiment-server') {
                experimentAttribute = { ...experimentAttribute, server: attr.value };
            }

            // Api Key for Analytics App
            if (attr.name === 'data-experiment-api-key') {
                experimentAttribute = { ...experimentAttribute, apiKey: attr.value };
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
export const getScriptDataAttributes = (location: Location): DotExperimentConfig | null => {
    const dataExperimentAttributes = getDataExperimentAttributes(location);

    if (dataExperimentAttributes) {
        return dataExperimentAttributes;
    }

    return null;
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
 * @returns {boolean} - True if the data needs to be invalidated, false otherwise.
 */
export const isDataCreateValid = (): boolean => {
    try {
        const timeValidUntil = Number(localStorage.getItem(EXPERIMENT_FETCH_EXPIRE_TIME_KEY));

        if (isNaN(timeValidUntil)) {
            return false;
        }

        const now = Date.now();

        return timeValidUntil > now;
    } catch {
        return false;
    }
};

/**
 * Ad to an absolute path the baseUrl depending on the location.
 *
 * @param {string | null} absolutePath - The absolute path of the URL.
 * @param {Location} location - The location object representing the current URL.
 * @returns {string | null} - The full URL or null if absolutePath is null.
 */
export const getFullUrl = (location: Location, absolutePath: string | null): string | null => {
    if (absolutePath === null) {
        return null;
    }

    if (!isFullUrl(absolutePath)) {
        const baseUrl = location.origin;

        return `${baseUrl}${absolutePath}`;
    }

    return absolutePath;
};

const isFullUrl = (url: string): boolean => {
    const pattern = /^https?:\/\//i;

    return pattern.test(url);
};

/**
 * Updates the URL with the queryParam with the experiment variant name.
 *
 * @param {Location|string} location - The current location object or the URL string.
 * @param {Variant} variant - The experiment variant to update the URL with.
 * @returns {string} The updated URL string.
 */
export const updateUrlWithExperimentVariant = (
    location: Location | string,
    variant: Variant | null
): string => {
    const href = typeof location === 'string' ? location : location.href;

    const url = new URL(href);

    if (variant !== null) {
        const params = url.searchParams;

        params.set(EXPERIMENT_QUERY_PARAM_KEY, variant.name);
        url.search = params.toString();
    }

    return url.toString();
};

/**
 * Check if two arrays of Experiment objects are equal.
 *
 * @param {Experiment[]} obj1 - The first array of Experiment objects.
 * @param {Experiment[]} obj2 - The second array of Experiment objects.
 * @return {boolean} - True if the arrays are equal, false otherwise.
 */
export const objectsAreEqual = (obj1: Experiment[], obj2: Experiment[]): boolean => {
    if (obj1.length === 0 && obj2.length === 0) {
        return false;
    }

    return JSON.stringify(obj1) === JSON.stringify(obj2);
};

/**
 * A function to redirect the user to a new URL.
 *
 * @param {string} href - The URL to redirect to.
 * @returns {void}
 */
export const defaultRedirectFn = (href: string) => (window.location.href = href);
