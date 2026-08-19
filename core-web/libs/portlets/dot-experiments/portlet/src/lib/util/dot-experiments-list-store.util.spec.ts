import { DotExperiment, DotExperimentStatus, GOAL_TYPES } from '@dotcms/dotcms-models';

import { comparatorFor } from './dot-experiments-list-store.util';

import { DotExperimentPageInfo } from '../shared/models';

const experiment = (partial: Partial<DotExperiment>): DotExperiment =>
    ({ id: 'id', pageId: 'page-1', name: 'Experiment', ...partial }) as DotExperiment;

const PAGE_INFO: Record<string, DotExperimentPageInfo> = {
    'page-a': { url: '/about', host: 'host-1' },
    'page-z': { url: '/zoo', host: 'host-1' }
};

/** Sorts with the comparator under test, returning the field that identifies each row. */
const sortedBy = (
    field: string,
    experiments: DotExperiment[],
    pick: (experiment: DotExperiment) => unknown = ({ id }) => id
) => {
    const compare = comparatorFor(field, PAGE_INFO);

    return [...experiments].sort(compare ?? undefined).map(pick);
};

describe('comparatorFor', () => {
    it('should return null for an unrecognised field', () => {
        expect(comparatorFor('not-a-column', PAGE_INFO)).toBeNull();
    });

    describe('name', () => {
        it('should sort alphabetically', () => {
            const experiments = [
                experiment({ id: 'b', name: 'Beta' }),
                experiment({ id: 'a', name: 'Alpha' })
            ];

            expect(sortedBy('name', experiments)).toEqual(['a', 'b']);
        });

        it('should ignore case, so mixed casing does not split the alphabet', () => {
            const experiments = [
                experiment({ id: 'upper', name: 'Zebra' }),
                experiment({ id: 'lower', name: 'apple' })
            ];

            // A codepoint sort would put 'Zebra' first, since uppercase sorts before lowercase.
            expect(sortedBy('name', experiments)).toEqual(['lower', 'upper']);
        });
    });

    describe('page', () => {
        it('should sort by the resolved path, not the pageId', () => {
            const experiments = [
                experiment({ id: 'zoo', pageId: 'page-z' }),
                experiment({ id: 'about', pageId: 'page-a' })
            ];

            expect(sortedBy('page', experiments)).toEqual(['about', 'zoo']);
        });

        it('should treat an unresolved page as empty rather than dropping it', () => {
            const experiments = [
                experiment({ id: 'about', pageId: 'page-a' }),
                experiment({ id: 'orphan', pageId: 'page-missing' })
            ];

            expect(sortedBy('page', experiments)).toEqual(['orphan', 'about']);
        });
    });

    describe('goal', () => {
        const withGoal = (id: string, type?: GOAL_TYPES) =>
            experiment({
                id,
                goals: type
                    ? ({ primary: { type, conditions: [] } } as unknown as DotExperiment['goals'])
                    : null
            });

        it('should sort by goal type', () => {
            const experiments = [
                withGoal('exit', GOAL_TYPES.EXIT_RATE),
                withGoal('bounce', GOAL_TYPES.BOUNCE_RATE)
            ];

            expect(sortedBy('goal', experiments)).toEqual(['bounce', 'exit']);
        });

        it('should push experiments with no goal to the end', () => {
            const experiments = [withGoal('none'), withGoal('bounce', GOAL_TYPES.BOUNCE_RATE)];

            expect(sortedBy('goal', experiments)).toEqual(['bounce', 'none']);
        });
    });

    describe('schedule', () => {
        const withStart = (id: string, startDate: number | null) =>
            experiment({
                id,
                scheduling: { startDate, endDate: null } as DotExperiment['scheduling']
            });

        it('should sort by start date', () => {
            const experiments = [withStart('later', 2000), withStart('earlier', 1000)];

            expect(sortedBy('schedule', experiments)).toEqual(['earlier', 'later']);
        });

        it('should push unscheduled experiments after every scheduled one', () => {
            const experiments = [
                experiment({ id: 'unscheduled', scheduling: null }),
                withStart('scheduled', 1000)
            ];

            expect(sortedBy('schedule', experiments)).toEqual(['scheduled', 'unscheduled']);
        });
    });

    describe('status', () => {
        it('should sort by lifecycle order, not alphabetically', () => {
            const experiments = [
                experiment({ id: 'running', status: DotExperimentStatus.RUNNING }),
                experiment({ id: 'draft', status: DotExperimentStatus.DRAFT }),
                experiment({ id: 'archived', status: DotExperimentStatus.ARCHIVED })
            ];

            // Alphabetically this would be archived, draft, running.
            expect(sortedBy('status', experiments)).toEqual(['draft', 'running', 'archived']);
        });
    });

    describe('modDate', () => {
        it('should sort numerically', () => {
            const experiments = [
                experiment({ id: 'new', modDate: 300 }),
                experiment({ id: 'old', modDate: 100 })
            ];

            expect(sortedBy('modDate', experiments)).toEqual(['old', 'new']);
        });
    });
});
