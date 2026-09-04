import { DotExperiment, DotExperimentStatus, GOAL_TYPES } from '@dotcms/dotcms-models';

import {
    comparatorFor,
    parseViewState,
    QueryParamReader,
    toQueryParams
} from './dot-experiments-list-store.util';

import {
    DEFAULT_EXPERIMENTS_LIST_DIRECTION,
    DEFAULT_EXPERIMENTS_LIST_GOALS,
    DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
    DEFAULT_EXPERIMENTS_LIST_PAGE,
    DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
    DEFAULT_EXPERIMENTS_LIST_STATUSES
} from '../shared/constants';
import { DotExperimentPageInfo, DotExperimentsListViewState } from '../shared/models';

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

/**
 * The page filter's URL round-trip (#37005, US3, FR-021a).
 *
 * The switch-on entry point lands on the site-wide list narrowed to the page the editor came from,
 * and that narrowing has to survive a reload and a shared link like every other filter.
 *
 * The param is `pageAsset`, deliberately **not** `page`: `page` is already the pagination cursor
 * in this very function, so reusing it would silently collide with `?page=2`. `pageAsset` also
 * matches the content type the list's own lookup queries (`+contentType:htmlpageasset`).
 */
describe('page filter view state', () => {
    const reader = (params: Record<string, string | string[]>): QueryParamReader => ({
        get: (key) => {
            const value = params[key];

            return (Array.isArray(value) ? value[0] : value) ?? null;
        },
        getAll: (key) => {
            const value = params[key];

            return value == null ? [] : Array.isArray(value) ? value : [value];
        }
    });

    const DEFAULTS: DotExperimentsListViewState = {
        filter: '',
        selectedStatuses: DEFAULT_EXPERIMENTS_LIST_STATUSES,
        selectedGoals: DEFAULT_EXPERIMENTS_LIST_GOALS,
        page: DEFAULT_EXPERIMENTS_LIST_PAGE,
        perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
        orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
        direction: DEFAULT_EXPERIMENTS_LIST_DIRECTION,
        selectedPageId: null
    };

    describe('parseViewState', () => {
        it('should read the page filter from ?pageAsset=', () => {
            expect(parseViewState(reader({ pageAsset: 'page-1' })).selectedPageId).toBe('page-1');
        });

        it('should default to no page filter when the param is absent', () => {
            expect(parseViewState(reader({})).selectedPageId).toBeNull();
        });

        it('should treat an empty ?pageAsset= as no filter, not as a page named ""', () => {
            expect(parseViewState(reader({ pageAsset: '' })).selectedPageId).toBeNull();
        });

        // The collision this param name exists to avoid. `?page=2` is pagination; it must not be
        // read as a page-asset filter, and `?pageAsset=` must not move the cursor.
        it('should keep ?page= and ?pageAsset= independent', () => {
            const view = parseViewState(reader({ page: '2', pageAsset: 'page-1' }));

            expect(view.page).toBe(2);
            expect(view.selectedPageId).toBe('page-1');
        });
    });

    describe('toQueryParams', () => {
        it('should write the page filter as pageAsset', () => {
            expect(toQueryParams({ ...DEFAULTS, selectedPageId: 'page-1' })).toMatchObject({
                pageAsset: 'page-1'
            });
        });

        // The util's existing rule: a value equal to its default is written as `null`, which
        // removes the param — so a pristine list carries no query string at all.
        it('should omit the param when there is no page filter', () => {
            expect(toQueryParams(DEFAULTS)['pageAsset']).toBeNull();
        });

        it('should not write the filter into the pagination key', () => {
            const params = toQueryParams({ ...DEFAULTS, selectedPageId: 'page-1' });

            expect(params['page']).toBeNull();
        });
    });

    it('should round-trip through both directions unchanged', () => {
        const written = toQueryParams({ ...DEFAULTS, selectedPageId: 'page-1' });

        expect(
            parseViewState(reader({ pageAsset: String(written['pageAsset']) })).selectedPageId
        ).toBe('page-1');
    });
});
