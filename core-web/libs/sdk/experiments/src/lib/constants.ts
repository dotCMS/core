/**
 * The key for identifying the Window key for SdkExperiment.
 *
 * @constant {string}
 */
export const EXPERIMENT_WINDOWS_KEY = 'dotExperiment';

/**
 * The name of the experiment script file.
 *
 * @constant {string}
 */
export const EXPERIMENT_SCRIPT_FILE_NAME = 'standalone.cjs.js';

/**
 * The prefix used for the experiment script data attributes.
 *
 * @constant {string}
 */
export const EXPERIMENT_SCRIPT_DATA_PREFIX = 'data-experiment-';

/**
 * Array containing the allowed data attributes for an experiment.
 *
 * @type {Array.<string>}
 * @constant
 */
export const EXPERIMENT_ALLOWED_DATA_ATTRIBUTES = [
    EXPERIMENT_SCRIPT_DATA_PREFIX + 'api-key',
    EXPERIMENT_SCRIPT_DATA_PREFIX + 'server',
    EXPERIMENT_SCRIPT_DATA_PREFIX + 'debug'
];

/**
 * API_EXPERIMENTS_URL
 *
 * The URL of the endpoint to check if a user is included in experiments.
 *
 * @type {string}
 * @constant
 */
export const API_EXPERIMENTS_URL = 'api/v1/experiments/isUserIncluded';

/**
 * The name of the experiment database in the IndexDB.
 *
 * @type {string}
 * @constant
 */
export const EXPERIMENT_DB_NAME = 'DotExperimentDB';

/**
 * The name of the experiment database store in indexDB.
 *
 * @type {string}
 * @constant
 */
export const EXPERIMENT_DB_STORE_NAME = 'dotExperimentStore';

/**
 * The path to the key in the database IndexDB representing the running experiment data.
 * @type {string}
 */
export const EXPERIMENT_DB_KEY_PATH = 'running_experiment';

/**
 * EXPERIMENT_DB_DEFAULT_VERSION is a constant variable that represents
 * the default version of the experiment database in IndexDB.
 *
 * @type {number}
 * @const
 */
export const EXPERIMENT_DB_DEFAULT_VERSION = 1;
