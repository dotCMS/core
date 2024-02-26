import { SdkExperiments } from './sdk-experiments';
import { getScriptDataAttributes } from './utils/utils';

declare global {
    interface Window {
        dotcmsExperiment: SdkExperiments;
    }
}

/**
 * This file sets up everything necessary to run an Experiment in Standalone Mode(Immediately Invoked Function Expressions).
 * It checks which experiments are currently running, and generates the essential data
 * needed for storing in DotCMS for subsequent A/B Testing validation.
 *
 */
if (window) {
    try {
        const dataAttributes = getScriptDataAttributes();
        if (dataAttributes) {
            window.dotcmsExperiment = SdkExperiments.getInstance({ ...dataAttributes });
        }
    } catch (error) {
        throw new Error(`Error instancing SdkExperiments: ${error}`);
    }
}
