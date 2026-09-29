/** A variant, as `isUserIncluded` returns it. */
export interface DotExperimentVariant {
    name: string;
    url: string;
}

/** An experiment the visitor was assigned to, as `isUserIncluded` returns it. */
export interface DotAssignedExperiment {
    id: string;
    name: string;
    runningId: string;
    pageUrl: string;
    lookBackWindow: { expireMillis: number; value: string };
    regexs: { isExperimentPage: string; isTargetPage: string | null };
    variant: DotExperimentVariant;
}

/** The `entity` of the `isUserIncluded` response. */
export interface DotIsUserIncludedEntity {
    experiments: DotAssignedExperiment[];
    includedExperimentIds: string[];
    excludedExperimentIds: string[];
    excludedExperimentIdsEnded: string[];
}

/** An assignment kept in localStorage, with the moment it stops being valid. */
export type StoredExperiment = DotAssignedExperiment & { expiresAt: number };

/** What the engine keeps in localStorage between visits. */
export interface StoredAssignments {
    fetchedAt: number;
    experiments: StoredExperiment[];
    /** Every experiment already evaluated, assigned or not, sent back as `exclude`. */
    evaluatedIds: string[];
}

/** The result of asking `isUserIncluded`. */
export type IsUserIncludedResult =
    | { status: 'ok'; entity: DotIsUserIncludedEntity }
    | { status: 'disabled' }
    | { status: 'failed'; httpStatus?: number };

/** The experiment a page carries in its marks. */
export interface PageExperimentMark {
    experimentId: string;
    variant: string;
}

/**
 * What the boot script leaves on `window[BOOT_STATE_KEY]` when it replaces the page with the
 * assigned variant, so the engine neither redirects again nor sends that page's pageview.
 */
export interface ExperimentBootState {
    /** The variant's URL. */
    redirectedTo: string;
}
