import { ONE_DAY_MS, STORAGE_KEYS } from './constants';

import type { DotIsUserIncludedEntity, StoredAssignments } from './models';
import type { DotCMSEventContextExperiment } from '../pipeline/models';

type StorageKind = 'local' | 'session';

const storageOf = (kind: StorageKind): Storage | null => {
    try {
        return kind === 'local' ? window.localStorage : window.sessionStorage;
    } catch {
        return null;
    }
};

const read = <T>(kind: StorageKind, key: string): T | null => {
    try {
        const raw = storageOf(kind)?.getItem(key);

        return raw ? (JSON.parse(raw) as T) : null;
    } catch {
        return null;
    }
};

const write = (kind: StorageKind, key: string, value: unknown): void => {
    try {
        storageOf(kind)?.setItem(key, JSON.stringify(value));
    } catch {
        // Storage can be full or blocked; the engine keeps working in memory.
    }
};

/**
 * Loads the stored assignments, without the ones whose look-back window has passed.
 *
 * @returns The assignments, or null when there are none
 */
export const loadAssignments = (): StoredAssignments | null => {
    const stored = read<StoredAssignments>('local', STORAGE_KEYS.assignments);

    if (!stored || !Array.isArray(stored.experiments)) {
        return null;
    }

    const now = Date.now();

    return {
        fetchedAt: stored.fetchedAt ?? 0,
        experiments: stored.experiments.filter((experiment) => experiment.expiresAt > now),
        evaluatedIds: Array.isArray(stored.evaluatedIds) ? stored.evaluatedIds : []
    };
};

/**
 * Merges an `isUserIncluded` answer into the stored assignments and saves them.
 * Ended experiments drop out; every evaluated experiment is remembered, so a visitor
 * left out by traffic allocation is not evaluated again.
 *
 * @param entity - The response entity
 * @param previous - What was stored before the request
 * @returns The assignments now stored
 */
export const saveAssignments = (
    entity: DotIsUserIncludedEntity,
    previous: StoredAssignments | null
): StoredAssignments => {
    const now = Date.now();
    const ended = new Set(entity.excludedExperimentIdsEnded);
    const byId = new Map(
        (previous?.experiments ?? [])
            .filter((experiment) => !ended.has(experiment.id) && experiment.expiresAt > now)
            .map((experiment) => [experiment.id, experiment])
    );

    entity.experiments.forEach((experiment) => {
        byId.set(experiment.id, {
            ...experiment,
            expiresAt: now + (experiment.lookBackWindow?.expireMillis || ONE_DAY_MS)
        });
    });

    const evaluated = new Set([
        ...(previous?.evaluatedIds ?? []),
        ...entity.includedExperimentIds,
        ...entity.excludedExperimentIds,
        ...entity.experiments.map((experiment) => experiment.id)
    ]);
    ended.forEach((id) => evaluated.delete(id));

    const next: StoredAssignments = {
        fetchedAt: now,
        experiments: [...byId.values()],
        evaluatedIds: [...evaluated]
    };

    write('local', STORAGE_KEYS.assignments, next);

    return next;
};

/**
 * Tells whether this tab should ask `isUserIncluded`: once per tab session, when nothing
 * is stored, or when the stored answer is older than a day.
 *
 * @param stored - The current assignments
 * @returns True when a request is due
 */
export const isCheckDue = (stored: StoredAssignments | null): boolean => {
    const checkedThisTab = read<boolean>('session', STORAGE_KEYS.checkedThisTab) === true;

    return !checkedThisTab || !stored || Date.now() - stored.fetchedAt > ONE_DAY_MS;
};

/** Records that this tab asked `isUserIncluded`. */
export const markCheckedThisTab = (): void => write('session', STORAGE_KEYS.checkedThisTab, true);

/** Tells whether the site answered 403 in the last day. */
export const isDisabledForNow = (): boolean =>
    (read<number>('local', STORAGE_KEYS.disabledUntil) ?? 0) > Date.now();

/** Remembers a 403 for a day, so sites without Experiments stop paying for the call. */
export const disableForADay = (): void =>
    write('local', STORAGE_KEYS.disabledUntil, Date.now() + ONE_DAY_MS);

interface SessionExperiments {
    sessionId: string;
    experiments: DotCMSEventContextExperiment[];
}

/**
 * Returns the experiments the visitor reached in this analytics session. A new session
 * starts empty.
 *
 * @param sessionId - The analytics session id
 * @returns The cumulative `context.experiments`
 */
export const getSessionExperiments = (sessionId: string): DotCMSEventContextExperiment[] => {
    const stored = read<SessionExperiments>('session', STORAGE_KEYS.sessionExperiments);

    return stored && stored.sessionId === sessionId ? stored.experiments : [];
};

/**
 * Adds an experiment to the session's `context.experiments`, replacing an older entry
 * for the same experiment.
 *
 * @param sessionId - The analytics session id
 * @param entry - The experiment the visitor reached
 */
export const joinSessionExperiment = (
    sessionId: string,
    entry: DotCMSEventContextExperiment
): void => {
    const others = getSessionExperiments(sessionId).filter(
        (experiment) => experiment.id !== entry.id
    );

    write('session', STORAGE_KEYS.sessionExperiments, {
        sessionId,
        experiments: [...others, entry]
    } satisfies SessionExperiments);
};

/**
 * Removes ended experiments from the session's `context.experiments`.
 *
 * @param endedIds - Ids `isUserIncluded` reported as ended
 */
export const leaveEndedExperiments = (endedIds: string[]): void => {
    if (endedIds.length === 0) {
        return;
    }

    const stored = read<SessionExperiments>('session', STORAGE_KEYS.sessionExperiments);

    if (!stored) {
        return;
    }

    const ended = new Set(endedIds);

    write('session', STORAGE_KEYS.sessionExperiments, {
        sessionId: stored.sessionId,
        experiments: stored.experiments.filter((experiment) => !ended.has(experiment.id))
    } satisfies SessionExperiments);
};
