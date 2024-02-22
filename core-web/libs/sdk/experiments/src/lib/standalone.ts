import { SdkExperiments } from './sdk-experiments';
import { getScriptDataAttributes } from './utils/utils';

declare global {
    interface Window {
        experiment: SdkExperiments;
    }
}

/**
 * This file sets up everything necessary to run an Experiment in Standalone Mode.
 * It checks which experiments are currently running, and generates the essential data
 * needed for storing in DotCMS for subsequent A/B Testing validation.
 *
 */
if (window) {
    try {
        const dataAttributes = getScriptDataAttributes();
        if (dataAttributes != null) {
            window.experiment = SdkExperiments.getInstance({ mode: 'js', ...dataAttributes });
        }
    } catch (error) {
        throw new Error(`HTTP error! status: ${error}`);
    }
}
