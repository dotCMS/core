import { AssignedExperiments, ExperimentParsed } from '../models';

/**
 * This arrow function parses a given set of assigned experiments for analytics.
 *
 * This process involves iterating over the experiments, which are currently in the "Running" state as received from the DotCMS endpoint,
 * analyzing each experiment's relevant data such as running ID, variant name, and look back window value.
 * It also performs regular expression verification for both 'isExperimentPage' and 'isTargetPage' against the current URL.
 *
 * The parsed data is useful for tracking and understanding the user's interaction with the experiment-targeted components during their visit.
 *
 * @param {AssignedExperiments} data - This parameter represents the assigned experiments data received from DotCMS endpoint.
 * Contains an object with experiments information.
 *
 * @param {Location} location - This parameter is the object representing the current location (URL) of the user.
 * Mostly employed for matching the regular expressions to detect whether the current page is an 'ExperimentPage' or a 'TargetPage'.
 *
 * @returns {ExperimentParsed} - The function returns an object with the original URL and an array of each experiment's comprehensive detail.
 * The return object is suitable for further analytical operations. Each experiment's detail includes the experiment ID, running ID, variant name,
 * look back window value, and booleans that represent whether current URL is 'isExperimentPage' or 'isTargetPage' for the respective experiment.
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
 * This utility function performs regular expression (regex) matching on a supplied URL.
 *
 * @param {string | null} regexToCheck - The regular expression to match against the URL.
 * @param {string} href - This is the target URL, which is aimed to be matched against the provided regular expression.
 * @returns {boolean} -The function returns a Boolean value.
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
