import { DotExperiments } from './dot-experiments';
import { EXPERIMENT_WINDOWS_KEY } from './shared/constants';
import { getScriptDataAttributes } from './shared/utils/utils';

declare global {
    interface Window {
        [EXPERIMENT_WINDOWS_KEY]: DotExperiments;
    }
}

/**
 * This file sets up everything necessary to run an Experiment in Standalone Mode(Immediately Invoked Function Expressions).
 * It checks which experiments are currently running, and generates the essential data
 * needed for storing in DotCMS for subsequent A/B Testing validation.
 *
 */
if (window) {
    // TODO: make this file buildable by task and publish to dotCMS/src/main/webapp/html
    try {
        const dataAttributes = getScriptDataAttributes(window.location);

        if (dataAttributes) {
            window[EXPERIMENT_WINDOWS_KEY] = DotExperiments.getInstance({ ...dataAttributes });
        }
    } catch (error) {
        throw new Error(`Error instancing DotExperiments: ${error}`);
    }
}
