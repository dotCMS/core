import {CONSTANT} from './constant';

/**
 * Encapsulates the global constants.
 * @type {{ENV: any; PROD_MODE: string; DEV_MODE: string}}
 */
export const CONSTANTS = {
    ENV:        CONSTANT.env,
    PROD_MODE: 'PROD',
    DEV_MODE:  'DEV',
    DEFAULT_LOCALE: 'en-US'
};
