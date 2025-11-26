import { Experiment, ExperimentParsed, FetchExperiments } from '../models';

/**
 * This arrow function parses a given set of assigned experiments for analytics.
 *
 * This process involves iterating over the experiments, which are currently in the "Running" state as received from the DotCMS endpoint,
 * analyzing each experiment's relevant data such as running ID, variant name, and look back window value.
 * It also performs regular expression verification for both 'isExperimentPage' and 'isTargetPage' against the current URL.
 *
 * The parsed data is useful for tracking and understanding the user's interaction with the experiment-targeted components during their visit.
 *
 * Contains an object with experiments information.
 *
 * @param experiments
 * @param {Location} location - This parameter is the object representing the current location (URL) of the user.
 * Mostly employed for matching the regular expressions to detect whether the current page is an 'ExperimentPage' or a 'TargetPage'.
 *
 * @returns {ExperimentParsed} - The function returns an object with the original URL and an array of each experiment's comprehensive detail.
 * The return object is suitable for further analytical operations. Each experiment's detail includes the experiment ID, running ID, variant name,
 * look back window value, and booleans that represent whether current URL is 'isExperimentPage' or 'isTargetPage' for the respective experiment.
 */
export const parseDataForAnalytics = (
    experiments: Experiment[],
    location: Location
): ExperimentParsed => {
    const currentHref = location.href;

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
export const verifyRegex = (regexToCheck: string | null, href: string): boolean => {
    if (regexToCheck === null || href === null) {
        return false;
    }

    try {
        const regexExp = new RegExp(regexToCheck);

        const url = new URL(href);

        const sanitizedHref = `${url.origin}${url.pathname.toLowerCase()}${url.search}`;

        return regexExp.test(sanitizedHref);
    } catch (error) {
        console.warn(`The regex ${regexToCheck} it is not a valid regex to check. ${error}`);

        return false;
    }
};

/**
 * This function merges newly fetched data with the data stored from IndexedDB, preparing it for re-storage in IndexedDB.
 *
 * @param { AssignedExperiments | null } fetchExperiments - The experiment data fetched from the API.
 * @param { AssignedExperiments | null } storedExperiments - The experiment data currently stored in IndexedDB.
 *
 * @returns { AssignedExperiments } - The parsed experiment data ready for storing.
 *
 * Following cases are handled -
 * 1) When new Data is received without Old data. This is assumed to be the first time data is received.
 * 2) When only old data is received, implying that the timestamp hasn't expired and the data is available in IndexedDB.
 *    The data is verified and any expired experiment is removed before being assigned to dataToStorage.
 * 3) When both old data and new data is present. This implies that the record existed in IndexedDB but was fetched because the flag had expired.
 *    Additional operations are performed to add expiry time to experiments, merging all experiments, and storing them.
 *
 * There could be scenarios where none of these conditions are met, in that case, dataToStorage will be the default empty object.
 */
export const parseData = (
    fetchExperiments: FetchExperiments,
    storedExperiments: Experiment[] | undefined
): Experiment[] => {
    let dataToStorage: Experiment[] = {} as Experiment[];

    const { excludedExperimentIdsEnded } = fetchExperiments;

    if (fetchExperiments && !storedExperiments) {
        // TODO: Use fetchExperiment instead fetchExperimentsNoNoneExperimentID when the endpoint dont retrieve NONE experiment
        // https://github.com/dotCMS/core/issues/27905
        const fetchExperimentsNoNoneExperimentID: Experiment[] = fetchExperiments.experiments
            ? fetchExperiments.experiments.filter((experiment) => experiment.id !== 'NONE')
            : [];

        dataToStorage = addExpireTimeToExperiments(fetchExperimentsNoNoneExperimentID);
    }

    if (!fetchExperiments && storedExperiments) {
        dataToStorage = getUnexpiredExperiments(storedExperiments, excludedExperimentIdsEnded);
    }

    if (fetchExperiments && storedExperiments) {
        // TODO: Use fetchExperiment instead fetchExperimentsNoNoneExperimentID when the endpoint dont retrieve NONE experiment
        // https://github.com/dotCMS/core/issues/27905
        const fetchExperimentsNoNoneExperimentID: Experiment[] = fetchExperiments.experiments
            ? fetchExperiments.experiments.filter((experiment) => experiment.id !== 'NONE')
            : [];

        dataToStorage = [
            ...addExpireTimeToExperiments(fetchExperimentsNoNoneExperimentID),
            ...getUnexpiredExperiments(storedExperiments, excludedExperimentIdsEnded)
        ];
    }

    return dataToStorage;
};

/**
 * Retrieves the array of experiment IDs from the given AssignedExperiments..
 *
 * @returns {string[]} Returns an array of experiment IDs if available, otherwise an empty array.
 * @param experiments
 */
export const getExperimentsIds = (experiments: Experiment[]): string[] =>
    experiments.map((experiment: Experiment) => experiment.id) || [];

/**
 * Sets the expire time for new experiments based on the current time.
 * The expire time is calculated by adding the expireMillis value of each experiment's lookBackWindow to the current time (Date.now()).
 *
 * @param {Array<Experiment>} experiments - An array of experiments to set the expire time for.
 * @returns {Array<Experiment>} - An updated array of experiments with expire time set.
 */
const addExpireTimeToExperiments = (experiments: Experiment[]): Experiment[] => {
    const now = Date.now();

    return experiments.map((experiment) => ({
        ...experiment,
        lookBackWindow: {
            ...experiment.lookBackWindow,
            expireTime: now + experiment.lookBackWindow.expireMillis
        }
    }));
};

/**
 * Returns an array of experiments that have not expired yet.
 *
 * @param {Experiment[]} experiments - An array of experiments to filter.
 * @param excludedExperimentIdsEnded -  Array of Experiments ids that have been manually ended.
 * @returns {Experiment[]} An array of unexpired experiments.
 */
const getUnexpiredExperiments = (
    experiments: Experiment[],
    excludedExperimentIdsEnded: string[]
): Experiment[] => {
    const now = Date.now();

    return experiments.filter((experiment) => {
        const expireTime = experiment.lookBackWindow?.expireTime;

        return expireTime
            ? expireTime > now && !excludedExperimentIdsEnded.includes(experiment.id)
            : false;
    });
};
