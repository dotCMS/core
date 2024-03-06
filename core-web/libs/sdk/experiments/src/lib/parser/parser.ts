import { AssignedExperiments, ExperimentParsed } from '../models';

/**
 * Parses data for analytics.
 *
 * @param {AssignedExperiments} data - The assigned experiments data.
 * @param {Location} location - The current location object.
 * @returns {ExperimentParsed} - The parsed experiment data for analytics.
 */
export const parseDataForAnalytics = (
    data: AssignedExperiments,
    location: Location
): ExperimentParsed => {
    const currentHref = location.href;
    const { experiments } = data;

    return {
        href: currentHref,
        experiments: experiments.map((experiment) => ({
            experiment: experiment.id,
            runningId: experiment.runningId,
            variant: experiment.variant.name,
            lookBackWindow: experiment.lookBackWindow.value,
            isExperimentPage: verifyRegex(experiment.regexs.isExperimentPage, currentHref),
            isTargetPage: verifyRegex(experiment.regexs.isTargetPage, currentHref)
        }))
    };
};

/**
 * Verifies if a given regular expression matches a URL.
 *
 * @param {string | null} regexToCheck - The regular expression to match against the URL.
 * @param {string} href - The URL to be matched against the regular expression.
 * @returns {boolean} - True if the regular expression matches the URL, false otherwise.
 */
const verifyRegex = (regexToCheck: string | null, href: string): boolean => {
    if (regexToCheck === null) {
        return false;
    }

    try {
        const regexExp = new RegExp(regexToCheck);
        const url = new URL(href);
        const sanitizedHref = `${url.origin}${url.pathname.toLowerCase()}${url.search}`;

        return regexExp.test(sanitizedHref);
    } catch (error) {
        console.warn(`The regex ${regexToCheck} it is not a valid regex to check`);

        return false;
    }
};
