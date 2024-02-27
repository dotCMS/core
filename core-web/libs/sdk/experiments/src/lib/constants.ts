export const EXPERIMENT_SCRIPT_FILE_NAME = 'standalone.cjs.js';

export const EXPERIMENT_SCRIPT_DATA_PREFIX = 'data-experiment-';

export const EXPERIMENT_ALLOWED_DATA_ATTRIBUTES = [
    EXPERIMENT_SCRIPT_DATA_PREFIX + 'api-key',
    EXPERIMENT_SCRIPT_DATA_PREFIX + 'server',
    EXPERIMENT_SCRIPT_DATA_PREFIX + 'debug'
];

// Endpoint
export const API_EXPERIMENTS_URL = 'api/v1/experiments/isUserIncluded';

// Persistence Constants
export const EXPERIMENT_DB_NAME = 'DotExperimentDB';

export const EXPERIMENT_DB_STORE_NAME = 'dotExperimentStore';

export const EXPERIMENT_DB_KEY_PATH = 'running_experiment';

export const EXPERIMENT_DB_DEFAULT_VERSION = 1;
