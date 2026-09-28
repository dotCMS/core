import { DotExperiment, DotExperimentStatus, GOAL_TYPES } from '@dotcms/dotcms-models';

import {
    comparatorFor,
    isScheduleRangeInverted,
    parseScheduleBound,
    parseViewState,
    QueryParamReader,
    scheduleBoundsOf,
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
 * The param is `pageId`, deliberately **not** `page`: `page` is already the pagination cursor in
 * this very function, so reusing it would silently collide with `?page=2`. `pageId` is also the
 * name Configure's own prefill answers on, so one address shape serves both screens.
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
        selectedCreators: [],
        scheduleFrom: null,
        scheduleTo: null,
        page: DEFAULT_EXPERIMENTS_LIST_PAGE,
        perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
        orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
        direction: DEFAULT_EXPERIMENTS_LIST_DIRECTION,
        selectedPageId: null,
        selectedPageUrl: null,
        languageId: null
    };

    describe('parseViewState', () => {
        it('should read the page filter from ?pageId=', () => {
            expect(parseViewState(reader({ pageId: 'page-1' })).selectedPageId).toBe('page-1');
        });

        it('should default to no page filter when the param is absent', () => {
            expect(parseViewState(reader({})).selectedPageId).toBeNull();
        });

        // The language the editor was on, carried so a return to the editor can land on that
        // version of the page instead of assuming the default one.
        it('should read the language from ?language_id=', () => {
            expect(parseViewState(reader({ language_id: '2' })).languageId).toBe(2);
        });

        it('should treat a missing or unusable ?language_id= as no language', () => {
            expect(parseViewState(reader({})).languageId).toBeNull();
            expect(parseViewState(reader({ language_id: 'abc' })).languageId).toBeNull();
        });

        /**
         * `?url=` is how the Universal Visual Editor names a page, so a UVE address pasted into
         * the list has to narrow rather than be ignored — the list used to answer it with every
         * experiment on the site.
         */
        it('should read the page filter from ?url=', () => {
            expect(parseViewState(reader({ url: '/destinations/index' })).selectedPageUrl).toBe(
                '/destinations/index'
            );
        });

        // The two ways the same path gets spelled, settled on the way in so the comparison against
        // the resolved Page column is not making them up on each side.
        it.each([
            ['destinations/index', '/destinations/index'],
            ['/destinations/index/', '/destinations/index'],
            ['  /destinations/index  ', '/destinations/index'],
            ['/', '/']
        ])('should normalise ?url=%s to %s', (raw, expected) => {
            expect(parseViewState(reader({ url: raw })).selectedPageUrl).toBe(expected);
        });

        it('should treat an empty ?url= as no filter', () => {
            expect(parseViewState(reader({ url: '' })).selectedPageUrl).toBeNull();
            expect(parseViewState(reader({})).selectedPageUrl).toBeNull();
        });

        // Case is left alone: dotCMS paths are not case-insensitive, and folding it here would
        // claim a match the backend would not make.
        it('should not fold the case of a path', () => {
            expect(parseViewState(reader({ url: '/Destinations/Index' })).selectedPageUrl).toBe(
                '/Destinations/Index'
            );
        });

        it('should treat an empty ?pageId= as no filter, not as a page named ""', () => {
            expect(parseViewState(reader({ pageId: '' })).selectedPageId).toBeNull();
        });

        // The collision this param name exists to avoid. `?page=2` is pagination; it must not be
        // read as a page filter, and `?pageId=` must not move the cursor.
        it('should keep ?page= and ?pageId= independent', () => {
            const view = parseViewState(reader({ page: '2', pageId: 'page-1' }));

            expect(view.page).toBe(2);
            expect(view.selectedPageId).toBe('page-1');
        });
    });

    describe('toQueryParams', () => {
        it('should write the page filter as pageId', () => {
            expect(toQueryParams({ ...DEFAULTS, selectedPageId: 'page-1' })).toMatchObject({
                pageId: 'page-1'
            });
        });

        // The util's existing rule: a value equal to its default is written as `null`, which
        // removes the param — so a pristine list carries no query string at all.
        it('should omit the param when there is no page filter', () => {
            expect(toQueryParams(DEFAULTS)['pageId']).toBeNull();
        });

        it('should not write the filter into the pagination key', () => {
            const params = toQueryParams({ ...DEFAULTS, selectedPageId: 'page-1' });

            expect(params['page']).toBeNull();
        });
    });

    it('should round-trip through both directions unchanged', () => {
        const written = toQueryParams({ ...DEFAULTS, selectedPageId: 'page-1' });

        expect(parseViewState(reader({ pageId: String(written['pageId']) })).selectedPageId).toBe(
            'page-1'
        );
    });
});

describe('creator filter view state (#37307)', () => {
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
        selectedCreators: [],
        scheduleFrom: null,
        scheduleTo: null,
        page: DEFAULT_EXPERIMENTS_LIST_PAGE,
        perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
        orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
        direction: DEFAULT_EXPERIMENTS_LIST_DIRECTION,
        selectedPageId: null,
        selectedPageUrl: null,
        languageId: null
    };

    describe('parseViewState', () => {
        it('should read a single creator from ?created_by=', () => {
            expect(parseViewState(reader({ created_by: 'dotcms.org.1' })).selectedCreators).toEqual(
                ['dotcms.org.1']
            );
        });

        it('should read every repeat of the param', () => {
            expect(
                parseViewState(reader({ created_by: ['dotcms.org.1', 'dotcms.org.2'] }))
                    .selectedCreators
            ).toEqual(['dotcms.org.1', 'dotcms.org.2']);
        });

        it('should default to no creator filter when the param is absent', () => {
            expect(parseViewState(reader({})).selectedCreators).toEqual([]);
        });

        it('should RETAIN an id it does not recognise', () => {
            // Deliberately unlike `status` and `goal`, whose value sets are closed and whose
            // unknown members are dropped. Any string can be a real user id, so the honest
            // behaviour is to keep it and let it match nothing (FR-048).
            expect(
                parseViewState(reader({ created_by: 'nobody-by-this-id' })).selectedCreators
            ).toEqual(['nobody-by-this-id']);
        });

        it('should drop an empty entry rather than filter by the empty string', () => {
            expect(parseViewState(reader({ created_by: '' })).selectedCreators).toEqual([]);
        });
    });

    describe('toQueryParams', () => {
        it('should write the selected creators to created_by', () => {
            expect(
                toQueryParams({ ...DEFAULTS, selectedCreators: ['dotcms.org.1', 'dotcms.org.2'] })[
                    'created_by'
                ]
            ).toEqual(['dotcms.org.1', 'dotcms.org.2']);
        });

        it('should omit the param entirely while nothing is selected', () => {
            expect(toQueryParams(DEFAULTS)['created_by']).toBeNull();
        });
    });

    it('should round-trip a selection through the address unchanged', () => {
        const written = toQueryParams({
            ...DEFAULTS,
            selectedCreators: ['dotcms.org.1', 'dotcms.org.2']
        });

        expect(
            parseViewState(reader(written as Record<string, string | string[]>)).selectedCreators
        ).toEqual(['dotcms.org.1', 'dotcms.org.2']);
    });
});

describe('schedule filter view state (#37307)', () => {
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
        selectedCreators: [],
        scheduleFrom: null,
        scheduleTo: null,
        page: DEFAULT_EXPERIMENTS_LIST_PAGE,
        perPage: DEFAULT_EXPERIMENTS_LIST_PER_PAGE,
        orderBy: DEFAULT_EXPERIMENTS_LIST_ORDER_BY,
        direction: DEFAULT_EXPERIMENTS_LIST_DIRECTION,
        selectedPageId: null,
        selectedPageUrl: null,
        languageId: null
    };

    const period = (from: string | null, to: string | null) => ({ from, to });

    describe('parseScheduleBound', () => {
        it('should read a calendar date as that day at local midnight', () => {
            const parsed = parseScheduleBound('2026-06-01');

            expect([parsed?.getFullYear(), parsed?.getMonth(), parsed?.getDate()]).toEqual([
                2026, 5, 1
            ]);
            expect([parsed?.getHours(), parsed?.getMinutes()]).toEqual([0, 0]);
        });

        it.each([null, undefined, '', 'not-a-date', 'June 2026', '2026-06'])(
            'should reject %s',
            (raw) => {
                expect(parseScheduleBound(raw)).toBeNull();
            }
        );

        /**
         * `new Date('2026-06-01')` would take all of these, each meaning a different day than the
         * address appears to name — and an ISO instant would shift the day in every zone but the
         * writer's. Strictness is what keeps the address and the calendar showing the same day.
         */
        it('should reject a shape that is a date but not this format', () => {
            expect(parseScheduleBound('2026-6-1')).toBeNull();
            expect(parseScheduleBound('2026-06-01T12:00:00.000Z')).toBeNull();
        });

        it('should reject a day that does not exist in its month', () => {
            // `parse` rolls 31 February into March rather than failing, so the round trip is what
            // catches it — otherwise the filter would silently use a day nobody named.
            expect(parseScheduleBound('2026-02-31')).toBeNull();
        });
    });

    describe('scheduleBoundsOf', () => {
        /** FR-021: whole local days, which is what makes a single-day period match anything. */
        it('should open the lower bound at the first instant of its day', () => {
            const bounds = scheduleBoundsOf(period('2026-06-01', null));

            expect(bounds?.min).toBe(new Date(2026, 5, 1, 0, 0, 0, 0).getTime());
        });

        it('should close the upper bound at the last instant of its day', () => {
            const bounds = scheduleBoundsOf(period(null, '2026-06-30'));

            expect(bounds?.max).toBe(new Date(2026, 5, 30, 23, 59, 59, 999).getTime());
        });

        it('should leave an absent side unbounded rather than absent', () => {
            // Infinity rather than a missing field, so the comparison downstream has no branches.
            expect(scheduleBoundsOf(period('2026-06-01', null))?.max).toBe(Infinity);
            expect(scheduleBoundsOf(period(null, '2026-06-30'))?.min).toBe(-Infinity);
        });

        it('should return no bounds when neither side is set', () => {
            expect(scheduleBoundsOf(period(null, null))).toBeNull();
        });

        it('should return no bounds when both sides are unusable', () => {
            expect(scheduleBoundsOf(period('nonsense', 'also-nonsense'))).toBeNull();
        });

        it('should return no bounds for an inverted range, so nothing is filtered', () => {
            expect(scheduleBoundsOf(period('2026-06-30', '2026-06-01'))).toBeNull();
        });
    });

    describe('isScheduleRangeInverted', () => {
        it('should recognise an end that precedes its start', () => {
            expect(isScheduleRangeInverted(period('2026-06-30', '2026-06-01'))).toBe(true);
        });

        it('should accept a period of a single day', () => {
            // Same date on both ends is a valid one-day period, not an inversion — the bounds
            // cover the whole day, so from-midnight is never after to-end-of-day.
            expect(isScheduleRangeInverted(period('2026-06-15', '2026-06-15'))).toBe(false);
        });

        it('should accept a period in order', () => {
            expect(isScheduleRangeInverted(period('2026-06-01', '2026-06-30'))).toBe(false);
        });

        it.each([
            ['only a start', '2026-06-30', null],
            ['only an end', null, '2026-06-01'],
            ['neither', null, null]
        ])('should not call %s inverted', (_label, from, to) => {
            expect(isScheduleRangeInverted(period(from, to))).toBe(false);
        });

        it('should not call an unusable bound inverted', () => {
            // It is dropped, not inverted: there is nothing to compare it against.
            expect(isScheduleRangeInverted(period('nonsense', '2026-06-01'))).toBe(false);
        });
    });

    describe('parseViewState', () => {
        it('should read both bounds from the address', () => {
            const view = parseViewState(
                reader({ schedule_from: '2026-06-01', schedule_to: '2026-06-30' })
            );

            expect([view.scheduleFrom, view.scheduleTo]).toEqual(['2026-06-01', '2026-06-30']);
        });

        it('should read one bound without the other', () => {
            const view = parseViewState(reader({ schedule_from: '2026-06-01' }));

            expect([view.scheduleFrom, view.scheduleTo]).toEqual(['2026-06-01', null]);
        });

        it('should default to no period when neither param is present', () => {
            const view = parseViewState(reader({}));

            expect([view.scheduleFrom, view.scheduleTo]).toEqual([null, null]);
        });

        it('should drop a bound that is not a date and keep the other (FR-048)', () => {
            const view = parseViewState(
                reader({ schedule_from: 'yesterday', schedule_to: '2026-06-30' })
            );

            expect([view.scheduleFrom, view.scheduleTo]).toEqual([null, '2026-06-30']);
        });

        it('should keep an inverted range rather than repairing it', () => {
            // Both dates are real, so parsing has nothing to object to. Whether to apply it is
            // FR-021a's question, answered by the store, and the user is told — silently swapping
            // the bounds would answer a question they did not ask.
            const view = parseViewState(
                reader({ schedule_from: '2026-06-30', schedule_to: '2026-06-01' })
            );

            expect([view.scheduleFrom, view.scheduleTo]).toEqual(['2026-06-30', '2026-06-01']);
        });
    });

    describe('toQueryParams', () => {
        it('should write both bounds', () => {
            const written = toQueryParams({
                ...DEFAULTS,
                scheduleFrom: '2026-06-01',
                scheduleTo: '2026-06-30'
            });

            expect([written['schedule_from'], written['schedule_to']]).toEqual([
                '2026-06-01',
                '2026-06-30'
            ]);
        });

        it('should omit the side the period leaves open', () => {
            const written = toQueryParams({ ...DEFAULTS, scheduleFrom: '2026-06-01' });

            expect(written['schedule_from']).toBe('2026-06-01');
            expect(written['schedule_to']).toBeNull();
        });

        it('should omit both while no period is in force', () => {
            expect(toQueryParams(DEFAULTS)['schedule_from']).toBeNull();
            expect(toQueryParams(DEFAULTS)['schedule_to']).toBeNull();
        });

        it('should not adopt the running_from and running_to names (FR-049a)', () => {
            // #36823 defines those, and they compare the running window rather than the scheduled
            // start. Sharing the names would assert an equivalence that does not hold.
            const written = toQueryParams({ ...DEFAULTS, scheduleFrom: '2026-06-01' });

            expect(written['running_from']).toBeUndefined();
            expect(written['running_to']).toBeUndefined();
        });
    });

    it('should round-trip a period through the address unchanged', () => {
        const written = toQueryParams({
            ...DEFAULTS,
            scheduleFrom: '2026-06-01',
            scheduleTo: '2026-06-30'
        });
        const view = parseViewState(reader(written as Record<string, string | string[]>));

        expect([view.scheduleFrom, view.scheduleTo]).toEqual(['2026-06-01', '2026-06-30']);
    });
});
