import { fetchIsUserIncluded } from './api';
import { BOOT_STATE_KEY, DEFAULT_VARIANT, VARIANT_QUERY_PARAM } from './constants';
import { decideVariant } from './decision';
import {
    hideContentlets,
    leavePageFor,
    observeMarks,
    readPageExperiments,
    readRenderedVariant,
    revealContentlets,
    revealRows
} from './dom';
import {
    disableForADay,
    getSessionExperiments,
    isCheckDue,
    isDisabledForNow,
    joinSessionExperiment,
    leaveEndedExperiments,
    loadAssignments,
    markCheckedThisTab,
    saveAssignments
} from './store';

import { onNavigation } from '../pipeline/navigation';
import { getSessionId } from '../pipeline/utils';

import type {
    ExperimentBootState,
    PageExperimentMark,
    StoredAssignments,
    StoredExperiment
} from './models';
import type { DotCMSEventsError } from '../models';
import type { DotCMSEventContextExperiment } from '../pipeline/models';

/** What the engine decided for the current page. */
export interface PageDecision {
    /** True when the engine navigated to the assigned variant; the pageview is dropped. */
    redirected: boolean;
    /**
     * True when the visitor went to another page while this one was decided; its pageview is
     * dropped, and the other page is decided and counted on its own.
     */
    left?: true;
}

/** What deciding one experiment the page renders ended in. */
type MarkOutcome = 'shown' | 'redirected' | 'left';

/**
 * How the pages mark what their experiment varies. `markup`: the element `experimentMarkup`
 * and `DotCMSExperiment` print, on headless apps. `contentlets`: the contentlet wrappers dotCMS
 * prints on the pages it renders, which carry the rendered variant in `data-dot-variant`; the
 * experiment comes from the `isExperimentPage` rule `isUserIncluded` returns.
 */
export type ExperimentPages = 'markup' | 'contentlets';

/** The experiments engine behind `dotEvents`. */
export interface ExperimentsEngine {
    /**
     * On `contentlets` pages, decides from storage while the page's head is parsed: it replaces
     * the page with the assigned variant before it paints, or, on a first visit, hides the
     * contentlets until `isUserIncluded` answers. Does nothing on `markup` pages.
     *
     * @returns True when it replaced the page
     */
    prepare(): boolean;
    /** Starts the assignment check, when one is due. */
    start(): void;
    /**
     * Decides the current page: reveal its rows, or redirect to the assigned variant. From then
     * on it also decides the marks that arrive later, as content streams in.
     */
    decide(): Promise<PageDecision>;
    /**
     * The session's cumulative `context.experiments`, while the page on screen runs an
     * experiment the visitor is in; none on any other page.
     */
    contextExperiments(sessionId: string): DotCMSEventContextExperiment[];
    /** Stops watching the page: for marks that arrive later, and for its navigations. */
    stop(): void;
}

interface EngineOptions {
    dotcmsUrl: string;
    timeoutMs: number;
    log: (...args: unknown[]) => void;
    /** Reports a setup mistake; prints only in debug, like `log`. */
    warn: (message: string) => void;
    /** Reports a failed experiments check to the app's `onError`. */
    onError?: (error: DotCMSEventsError) => void;
    /** Replaces the page with a variant's URL (`leavePageFor`); a parameter so specs can watch it. */
    navigate?: (url: string) => void;
    /** How the pages mark their experiment; `markup` by default. */
    pages?: ExperimentPages;
}

type WaitOutcome = 'answered' | 'timeout';

/**
 * Tells whether a URL rule from `isUserIncluded` matches a URL.
 *
 * @param pattern - The rule, a regular expression in a string
 * @param href - The URL
 * @returns False when the rule is missing or is not a valid expression
 */
const matchesRule = (pattern: string | null | undefined, href: string): boolean => {
    if (!pattern) {
        return false;
    }

    // dotCMS writes the rules for a lowercase address and its own experiments script tests
    // them that way: the part before the query in lowercase, the query as it is
    const query = href.indexOf('?');
    const url =
        query < 0 ? href.toLowerCase() : href.slice(0, query).toLowerCase() + href.slice(query);

    try {
        return new RegExp(pattern).test(url);
    } catch {
        return false;
    }
};

/**
 * The warning for a route that renders another variant than the one its URL asks for.
 *
 * @param mark - The experiment and the variant the server rendered
 * @param variant - The variant the URL asks for
 * @returns The message
 */
const routeIgnoresVariantMessage = (mark: PageExperimentMark, variant: string): string =>
    `this URL asks for ${variant} of experiment ${mark.experimentId}, but the server rendered ${mark.variant}. ` +
    'The route does not pass variantName to its page request, so the page is shown as it is and the visit is not counted in the experiment.';

/**
 * The page a URL shows: its path and query, which the route renders. The hash changes nothing.
 *
 * @param href - The URL
 * @returns The path and the query
 */
const pageOf = (href: string): string => {
    const url = new URL(href);

    return url.pathname + url.search;
};

/**
 * The URL the page's boot script is replacing the page with, if it is.
 *
 * @returns The variant's URL, or undefined when the script did not redirect
 */
const bootRedirect = (): string | undefined => {
    const state = (window as unknown as Record<string, unknown>)[BOOT_STATE_KEY];

    return state && typeof (state as ExperimentBootState).redirectedTo === 'string'
        ? (state as ExperimentBootState).redirectedTo
        : undefined;
};

/**
 * Creates the engine that assigns the visitor, holds and releases pageviews, redirects to
 * the assigned variant and fills `context.experiments`.
 *
 * @param options - dotCMS origin, the assignment timeout and a logger
 * @returns The engine
 */
export const createExperimentsEngine = ({
    dotcmsUrl,
    timeoutMs,
    log,
    warn,
    onError,
    navigate = leavePageFor,
    pages = 'markup'
}: EngineOptions): ExperimentsEngine => {
    let assignments: StoredAssignments | null = loadAssignments();
    let disabled = isDisabledForNow();
    let inflight: Promise<void> | null = null;
    // URLs already warned about, so a page warns once however many pageviews it sends
    const warnedUrls = new Set<string>();
    // The variant's URL, when prepare replaced the page
    let preparedRedirect: string | undefined;
    // One decision per page visit and rendered experiment, shared by decide and the mark observer
    const decisions = new Map<string, Promise<MarkOutcome>>();
    let stopObserving: (() => void) | undefined;
    // Navigations inside the page so far. A visit is the page plus this count: a visitor who
    // leaves and comes back during a wait starts a new visit, decided and counted on its own
    let navigations = 0;
    // The visit whose page runs an experiment the visitor is in: only its events carry them
    let visitInExperiment: number | undefined;
    const stopCountingNavigations =
        pages === 'markup'
            ? onNavigation(() => {
                  navigations += 1;
              })
            : undefined;

    const findAssignment = (experimentId: string): StoredExperiment | undefined =>
        assignments?.experiments.find((experiment) => experiment.id === experimentId);

    /**
     * Warns when an experiment the visitor is in runs on the current URL, by its URL rule,
     * while nothing on the page carries experiment marks: the experiment would never show.
     */
    const warnIfUnmarked = (): void => {
        const href = window.location.href;
        const experiment = assignments?.experiments.find((candidate) =>
            matchesRule(candidate.regexs?.isExperimentPage, href)
        );

        if (!experiment || warnedUrls.has(href)) {
            return;
        }

        warnedUrls.add(href);
        warn(
            `experiment "${experiment.name}" (${experiment.id}) runs on this page, but nothing on it carries the experiment's marks. ` +
                'Wrap the content the experiment varies in DotCMSExperiment (@dotcms/events/react), or print experimentMarkup (@dotcms/events/markup): ' +
                'until then the visitor sees the original and the experiment records nothing.'
        );
    };

    const check = (force = false): Promise<void> => {
        if (disabled) {
            return Promise.resolve();
        }

        if (inflight) {
            return inflight;
        }

        if (!force && !isCheckDue(assignments)) {
            return Promise.resolve();
        }

        log('asking isUserIncluded');

        inflight = fetchIsUserIncluded(dotcmsUrl, assignments?.evaluatedIds ?? [])
            .then((result) => {
                if (result.status === 'disabled') {
                    disabled = true;
                    disableForADay();
                    log('isUserIncluded answered 403: experiments off for a day');
                    onError?.({
                        code: 'EXPERIMENTS',
                        status: 403,
                        message:
                            'isUserIncluded answered 403: experiments are off for the site, for a day'
                    });

                    return;
                }

                if (result.status === 'failed') {
                    log('isUserIncluded failed; keeping the stored assignments');
                    onError?.({
                        code: 'EXPERIMENTS',
                        ...(result.httpStatus !== undefined && { status: result.httpStatus }),
                        message: 'isUserIncluded failed: the stored assignments stay'
                    });

                    return;
                }

                assignments = saveAssignments(result.entity, assignments);
                leaveEndedExperiments(result.entity.excludedExperimentIdsEnded);
                markCheckedThisTab();
                log(
                    'assignments',
                    assignments.experiments.map(
                        (experiment) => `${experiment.id}: ${experiment.variant.name}`
                    )
                );
            })
            .finally(() => {
                inflight = null;
            });

        return inflight;
    };

    /** The stored experiment whose `isExperimentPage` rule matches the current URL. */
    const experimentOnThisPage = (): StoredExperiment | undefined =>
        assignments?.experiments.find(
            (experiment) =>
                experiment.expiresAt > Date.now() &&
                matchesRule(experiment.regexs?.isExperimentPage, window.location.href)
        );

    const decisionFor = (mark: PageExperimentMark, href = window.location.href) =>
        decideVariant({
            experimentId: mark.experimentId,
            rendered: mark.variant,
            href,
            stored: assignments,
            disabled,
            now: Date.now(),
            param: VARIANT_QUERY_PARAM,
            defaultVariant: DEFAULT_VARIANT
        });

    const waitForAnswer = (): Promise<WaitOutcome> =>
        Promise.race([
            check(true).then((): WaitOutcome => 'answered'),
            new Promise<WaitOutcome>((resolve) => setTimeout(() => resolve('timeout'), timeoutMs))
        ]);

    /**
     * The mark of a page dotCMS rendered: its experiment from the URL rule, and the variant its
     * contentlet wrappers carry. On a first visit nothing stored has the rule yet, so it waits
     * for `isUserIncluded`, up to the timeout, with the contentlets hidden.
     *
     * @returns The mark, or null when no experiment runs on the page for this visitor
     */
    const contentletsMark = async (): Promise<PageExperimentMark | null> => {
        if (disabled) {
            return null;
        }

        if (!assignments && (await waitForAnswer()) === 'timeout') {
            log('the assignment timed out; showing the page without the experiment');

            return null;
        }

        const experiment = experimentOnThisPage();

        return experiment
            ? { experimentId: experiment.id, variant: readRenderedVariant(window.location.href) }
            : null;
    };

    /**
     * Decides one experiment the page renders: shows it, or leaves for the assigned variant.
     *
     * @param mark - The experiment and the variant the server rendered
     * @param href - The page's URL when its decision started
     * @param reveal - Shows what the page hid while it waited
     * @returns How it ended; `left` when the visitor went to another page during the wait
     */
    const decideMark = async (
        mark: PageExperimentMark,
        href: string,
        reveal: () => void
    ): Promise<MarkOutcome> => {
        // The same decision the boot script makes while the HTML is parsed
        let decision = decisionFor(mark, href);
        const visit = navigations;

        if (decision.kind === 'unknown') {
            log(`holding the pageview for ${mark.experimentId}`);

            const outcome = await waitForAnswer();

            // A click during the wait, even one that comes back: the next page is decided on
            // its own, and this visit neither redirects to a variant of another URL nor counts
            if (navigations !== visit || pageOf(window.location.href) !== pageOf(href)) {
                log(`left ${pageOf(href)} while ${mark.experimentId} was decided`);

                return 'left';
            }

            if (outcome === 'timeout') {
                log('the assignment timed out; showing the page without the experiment');
                reveal();

                return 'shown';
            }

            decision = decisionFor(mark, href);
        }

        switch (decision.kind) {
            case 'redirect':
                log(`rendered ${mark.variant}, assigned ${decision.variant}: ${decision.url}`);
                navigate(decision.url);

                return 'redirected';

            case 'assigned': {
                const assigned = findAssignment(mark.experimentId);

                if (assigned) {
                    joinSessionExperiment(getSessionId(), {
                        id: assigned.id,
                        running_id: assigned.runningId,
                        variant: assigned.variant.name
                    });
                    visitInExperiment = visit;
                }

                log(`showing ${decision.variant} of ${mark.experimentId}`);
                break;
            }

            case 'ignored':
                warn(routeIgnoresVariantMessage(mark, decision.variant));
                break;

            default:
                log(`not in ${mark.experimentId}; showing the page`);
        }

        reveal();

        return 'shown';
    };

    /**
     * Decides a marked experiment once per page, whoever asks first: decide, for the pageview,
     * or the observer, for marks that arrive later.
     *
     * @param mark - The experiment and the variant the server rendered
     * @param href - The page's URL
     * @returns How the decision ended
     */
    const decideOnce = (mark: PageExperimentMark, href: string): Promise<MarkOutcome> => {
        const key = `${navigations}\n${pageOf(href)}\n${mark.experimentId}\n${mark.variant}`;
        let outcome = decisions.get(key);

        if (!outcome) {
            outcome = decideMark(mark, href, () => revealRows(mark.experimentId, mark.variant));
            decisions.set(key, outcome);
            // A visit left mid-decision is never asked for again: its key has an older count
            void outcome.then((result) => {
                if (result === 'left') {
                    decisions.delete(key);
                }
            });
        }

        return outcome;
    };

    /**
     * Decides every experiment the page renders, in document order; the first redirect wins.
     *
     * @param marks - The page's marks
     * @param href - The page's URL
     * @returns The page's decision
     */
    const decideMarks = async (
        marks: PageExperimentMark[],
        href: string
    ): Promise<PageDecision> => {
        for (const mark of marks) {
            const outcome = await decideOnce(mark, href);

            if (outcome === 'redirected') {
                return { redirected: true };
            }

            if (outcome === 'left') {
                return { redirected: false, left: true };
            }
        }

        return { redirected: false };
    };

    /**
     * Waits for the marks of a page whose experiment runs on its URL, by the stored rule, while
     * the page has none yet: content behind Suspense streams in after the page loads, and after
     * a route change.
     *
     * @returns The marks, or none after the timeout
     */
    const waitForMarks = (): Promise<PageExperimentMark[]> =>
        new Promise((resolve) => {
            let stop = (): void => undefined;
            const timer = setTimeout(() => {
                stop();
                resolve([]);
            }, timeoutMs);

            stop = observeMarks(() => {
                const marks = readPageExperiments();

                if (marks.length) {
                    clearTimeout(timer);
                    stop();
                    resolve(marks);
                }
            });
        });

    /**
     * From the first decision on, decides the marks that arrive later, as soon as they are in
     * the page: streamed content, or a route rendered after its pageview.
     */
    const watchLateMarks = (): void => {
        stopObserving ??= observeMarks(() => {
            const href = window.location.href;

            readPageExperiments().forEach((mark) => void decideOnce(mark, href));
        });
    };

    return {
        prepare: () => {
            if (pages !== 'contentlets' || disabled) {
                return false;
            }

            const experiment = experimentOnThisPage();

            if (!experiment) {
                // A first visit: nothing stored says whether this page runs an experiment
                if (!assignments) {
                    hideContentlets();
                }

                return false;
            }

            // The body is not parsed yet: the URL says which variant dotCMS renders
            const decision = decisionFor({
                experimentId: experiment.id,
                variant: readRenderedVariant(window.location.href)
            });

            if (decision.kind !== 'redirect') {
                return false;
            }

            log(`replacing the page with ${decision.variant} of ${experiment.id}: ${decision.url}`);
            preparedRedirect = decision.url;
            navigate(decision.url);

            return true;
        },

        start: () => {
            void check();
        },

        decide: async () => {
            // The boot script decided from storage while the HTML was parsed
            const redirectedTo = preparedRedirect ?? bootRedirect();

            if (redirectedTo) {
                log(`the page script is replacing the page: ${redirectedTo}`);

                return { redirected: true };
            }

            const href = window.location.href;
            const visit = navigations;
            let marks = readPageExperiments();

            if (pages === 'markup') {
                watchLateMarks();

                // The page runs an experiment by its URL rule, but its marks are not in yet
                if (!marks.length && !disabled && experimentOnThisPage()) {
                    marks = await waitForMarks();

                    if (navigations !== visit || pageOf(window.location.href) !== pageOf(href)) {
                        return { redirected: false, left: true };
                    }

                    if (!marks.length) {
                        warnIfUnmarked();
                    }
                }

                return decideMarks(marks, href);
            }

            // A traditional page that prints the experiment markup is decided by it
            if (marks.length) {
                return decideMarks(marks, href);
            }

            const mark = await contentletsMark();

            if (!mark) {
                revealContentlets();

                return { redirected: false };
            }

            const outcome = await decideMark(mark, href, revealContentlets);

            if (outcome === 'left') {
                return { redirected: false, left: true };
            }

            return { redirected: outcome === 'redirected' };
        },

        contextExperiments: (sessionId) =>
            visitInExperiment === navigations ? getSessionExperiments(sessionId) : [],

        stop: () => {
            stopObserving?.();
            stopObserving = undefined;
            stopCountingNavigations?.();
        }
    };
};
