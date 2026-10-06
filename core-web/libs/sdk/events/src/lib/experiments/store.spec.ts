import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ONE_DAY_MS, STORAGE_KEYS } from './constants';
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

import type {
    DotCMSAssignedExperiment,
    DotCMSIsUserIncludedEntity,
    StoredAssignments,
    StoredExperiment
} from './models';

const NOW = new Date('2026-10-01T12:00:00Z').getTime();

const assigned = (id: string, expireMillis = 60_000): DotCMSAssignedExperiment => ({
    id,
    name: `Experiment ${id}`,
    runningId: `run-${id}`,
    pageUrl: '/index',
    lookBackWindow: { expireMillis, value: `window-${id}` },
    regexs: { isExperimentPage: '', isTargetPage: null },
    variant: { name: `${id}-variant-1`, url: `/index?variantName=${id}-variant-1` }
});

const stored = (id: string, expiresAt: number): StoredExperiment => ({
    ...assigned(id),
    expiresAt
});

const answer = (fields: Partial<DotCMSIsUserIncludedEntity>): DotCMSIsUserIncludedEntity => ({
    experiments: [],
    includedExperimentIds: [],
    excludedExperimentIds: [],
    excludedExperimentIdsEnded: [],
    ...fields
});

const storeAssignments = (assignments: StoredAssignments) =>
    localStorage.setItem(STORAGE_KEYS.assignments, JSON.stringify(assignments));

describe('experiments store', () => {
    beforeEach(() => {
        localStorage.clear();
        sessionStorage.clear();
        vi.useFakeTimers();
        vi.setSystemTime(NOW);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('saveAssignments', () => {
        it('stores each assigned experiment until its look-back window ends, as evaluated', () => {
            const saved = saveAssignments(
                answer({ experiments: [assigned('a', 60_000)], includedExperimentIds: ['a'] }),
                null
            );

            expect(saved).toEqual({
                fetchedAt: NOW,
                experiments: [{ ...assigned('a', 60_000), expiresAt: NOW + 60_000 }],
                evaluatedIds: ['a']
            });
            expect(JSON.parse(localStorage.getItem(STORAGE_KEYS.assignments)!)).toEqual(saved);
        });

        it('keeps an assignment a day when its experiment has no look-back window', () => {
            const saved = saveAssignments(answer({ experiments: [assigned('a', 0)] }), null);

            expect(saved.experiments[0]?.expiresAt).toBe(NOW + ONE_DAY_MS);
        });

        it('keeps earlier assignments the answer leaves out, and drops the ones that ended or expired', () => {
            const previous: StoredAssignments = {
                fetchedAt: NOW - 1000,
                experiments: [
                    stored('kept', NOW + 10_000),
                    stored('ended', NOW + 10_000),
                    stored('expired', NOW - 1)
                ],
                evaluatedIds: ['kept', 'ended', 'expired']
            };

            const saved = saveAssignments(
                answer({
                    experiments: [assigned('new')],
                    includedExperimentIds: ['new'],
                    excludedExperimentIds: ['left-out'],
                    excludedExperimentIdsEnded: ['ended']
                }),
                previous
            );

            expect(saved.experiments.map((experiment) => experiment.id)).toEqual(['kept', 'new']);
            // An ended experiment is no longer sent back as excluded
            expect(saved.evaluatedIds).toEqual(['kept', 'expired', 'new', 'left-out']);
        });

        it('remembers an experiment the visitor was left out of, so dotCMS does not draw it again', () => {
            const saved = saveAssignments(answer({ includedExperimentIds: ['drawn'] }), null);

            expect(saved.experiments).toEqual([]);
            expect(saved.evaluatedIds).toEqual(['drawn']);
        });
    });

    describe('loadAssignments', () => {
        it('returns null when nothing is stored, or what is stored has no experiments', () => {
            expect(loadAssignments()).toBeNull();

            localStorage.setItem(STORAGE_KEYS.assignments, JSON.stringify({ fetchedAt: NOW }));

            expect(loadAssignments()).toBeNull();
        });

        it('leaves out the assignments whose look-back window has passed', () => {
            storeAssignments({
                fetchedAt: NOW,
                experiments: [stored('current', NOW + 1000), stored('past', NOW - 1)],
                evaluatedIds: ['current', 'past']
            });

            const loaded = loadAssignments();

            expect(loaded?.experiments.map((experiment) => experiment.id)).toEqual(['current']);
            expect(loaded?.evaluatedIds).toEqual(['current', 'past']);
        });
    });

    describe('isCheckDue', () => {
        const fresh: StoredAssignments = { fetchedAt: NOW, experiments: [], evaluatedIds: [] };

        it('is due until this tab asks', () => {
            expect(isCheckDue(fresh)).toBe(true);

            markCheckedThisTab();

            expect(isCheckDue(fresh)).toBe(false);
        });

        it('is due again when nothing is stored, or the stored answer is more than a day old', () => {
            markCheckedThisTab();

            expect(isCheckDue(null)).toBe(true);
            expect(isCheckDue({ ...fresh, fetchedAt: NOW - ONE_DAY_MS - 1 })).toBe(true);
        });
    });

    describe('session experiments', () => {
        const entry = (id: string, variant = `${id}-variant-1`) => ({
            id,
            running_id: `run-${id}`,
            variant
        });

        it('adds an experiment to the session, replacing an older entry for the same experiment', () => {
            joinSessionExperiment('session-1', entry('a'));
            joinSessionExperiment('session-1', entry('b'));
            joinSessionExperiment('session-1', entry('a', 'DEFAULT'));

            expect(getSessionExperiments('session-1')).toEqual([entry('b'), entry('a', 'DEFAULT')]);
        });

        it('starts empty in another session', () => {
            joinSessionExperiment('session-1', entry('a'));

            expect(getSessionExperiments('session-2')).toEqual([]);
        });

        it('drops the experiments dotCMS reports as ended', () => {
            joinSessionExperiment('session-1', entry('a'));
            joinSessionExperiment('session-1', entry('b'));

            leaveEndedExperiments(['a']);

            expect(getSessionExperiments('session-1')).toEqual([entry('b')]);
        });
    });

    describe('disableForADay', () => {
        it('turns experiments off for a day, as after a 403', () => {
            expect(isDisabledForNow()).toBe(false);

            disableForADay();

            expect(isDisabledForNow()).toBe(true);

            vi.setSystemTime(NOW + ONE_DAY_MS + 1);

            expect(isDisabledForNow()).toBe(false);
        });
    });
});
