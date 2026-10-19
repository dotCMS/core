import { Dispatcher } from '@ngrx/signals/events';
import { vi } from 'vitest';

import { signal } from '@angular/core';

import { DotExperimentStatus, GOAL_TYPES } from '@dotcms/dotcms-models';
import { DotFilterFacade } from '@dotcms/ui';

import {
    createExperimentsFilterFacade,
    EXPERIMENTS_FILTER_KEYS
} from './dot-experiments-filter-facade';
import { dotExperimentsListPageEvents } from './dot-experiments-list-page.events';
import { DotExperimentsListStore } from './dot-experiments-list.store';

/**
 * The listing's `DotFilterFacade`.
 *
 * **The shared conformance suite is deliberately not run here, and that is a finding rather than
 * an exemption.** `testFilterFacadeConformance` assumes the surface keeps an open, string-keyed
 * filter bag: its obligations are asserted with literal content-browsing keys (`title`,
 * `contentType`, `languageId`), O1 requires an arbitrary key to round-trip, and O2 requires
 * `patchFilters({ contentType: [] })` to read back as `[]` rather than `undefined`.
 *
 * This store keeps **typed state** and changes it only through dispatched events, which is what
 * makes an unknown status unstorable — the address drops it on parse and the reducer accepts only
 * typed payloads. There is no bag to write `title` into, and no filter here has a "selected
 * nothing" state distinct from being off, so O2's distinction does not exist to observe.
 *
 * Satisfying the suite would mean giving the store a second, open filter bag beside its typed
 * state — two sources of truth — or replacing the typed state outright, which rewrites the parsing
 * rules and every reducer. Neither is worth passing a suite whose premise does not hold here.
 *
 * What the suite *would* have covered and is covered below instead: round-tripping each of this
 * surface's own keys, removal, paging reset on every write, clearing, and
 * `$hasNonDefaultFilters`. The two obligations `dot-filter-bar` actually depends on — clearing and
 * that signal — are the ones asserted hardest.
 */
describe('DotExperimentsFilterFacade', () => {
    const { STATUS, GOAL, CREATED_BY, SCHEDULE_FROM, SCHEDULE_TO } = EXPERIMENTS_FILTER_KEYS;

    let facade: DotFilterFacade;
    let dispatch: ReturnType<typeof vi.fn>;
    let state: {
        filter: ReturnType<typeof signal<string>>;
        selectedStatuses: ReturnType<typeof signal<DotExperimentStatus[]>>;
        selectedGoals: ReturnType<typeof signal<GOAL_TYPES[]>>;
        selectedCreators: ReturnType<typeof signal<string[]>>;
        scheduleFrom: ReturnType<typeof signal<string | null>>;
        scheduleTo: ReturnType<typeof signal<string | null>>;
    };

    const dispatched = () => dispatch.mock.calls.map(([event]) => event);

    beforeEach(() => {
        state = {
            filter: signal<string>(''),
            selectedStatuses: signal<DotExperimentStatus[]>([]),
            selectedGoals: signal<GOAL_TYPES[]>([]),
            selectedCreators: signal<string[]>([]),
            scheduleFrom: signal<string | null>(null),
            scheduleTo: signal<string | null>(null)
        };
        dispatch = vi.fn();

        facade = createExperimentsFilterFacade(
            state as unknown as InstanceType<typeof DotExperimentsListStore>,
            { dispatch } as unknown as Dispatcher
        );
    });

    describe('reading', () => {
        it('should report a key nobody set as absent', () => {
            expect(facade.getFilterValue(STATUS)).toBeUndefined();
        });

        it('should report a key this surface does not have as absent', () => {
            // Content Drive's keys reach every chip through the same token, so an unknown one has
            // to read as "not filtered" rather than throw.
            expect(facade.getFilterValue('contentType')).toBeUndefined();
        });

        it('should report an empty selection as absent, not as an empty list', () => {
            // The contract calls the `undefined`/`[]` distinction load-bearing, and on this screen
            // it does not exist: no filter here has a "selected nothing" state separate from off.
            state.selectedStatuses.set([]);

            expect(facade.getFilterValue(STATUS)).toBeUndefined();
        });

        it.each([
            [STATUS, () => state.selectedStatuses.set([DotExperimentStatus.RUNNING]), ['RUNNING']],
            [GOAL, () => state.selectedGoals.set([GOAL_TYPES.BOUNCE_RATE]), ['BOUNCE_RATE']],
            [CREATED_BY, () => state.selectedCreators.set(['dotcms.org.1']), ['dotcms.org.1']]
        ])('should read %s off the typed state', (key, setUp, expected) => {
            setUp();

            expect(facade.getFilterValue(key)).toEqual(expected);
        });

        it.each([
            [SCHEDULE_FROM, 'scheduleFrom' as const],
            [SCHEDULE_TO, 'scheduleTo' as const]
        ])('should read %s as a single value', (key, field) => {
            state[field].set('2026-06-01');

            expect(facade.getFilterValue(key)).toBe('2026-06-01');
        });
    });

    describe('writing', () => {
        it('should dispatch the typed status event', () => {
            facade.patchFilters({ [STATUS]: ['RUNNING'] });

            expect(dispatched()).toContainEqual(
                dotExperimentsListPageEvents.statusesChanged([DotExperimentStatus.RUNNING])
            );
        });

        it('should dispatch the typed goal event', () => {
            facade.patchFilters({ [GOAL]: ['BOUNCE_RATE'] });

            expect(dispatched()).toContainEqual(
                dotExperimentsListPageEvents.goalsChanged([GOAL_TYPES.BOUNCE_RATE])
            );
        });

        it('should accept a bare string where the chip speaks one', () => {
            // A URL decoder that loses the array shape hands a chip a bare string, which is why
            // the shared helper exists. Mapping it as a string would filter by each character.
            facade.patchFilters({ [CREATED_BY]: 'dotcms.org.1' });

            expect(dispatched()).toContainEqual(
                dotExperimentsListPageEvents.creatorsChanged(['dotcms.org.1'])
            );
        });

        it('should ignore a key this surface does not have', () => {
            facade.patchFilters({ contentType: ['Blog'] });

            expect(dispatched()).toEqual([]);
        });

        it.each([
            [
                'status',
                () => facade.patchFilters({ [STATUS]: ['RUNNING', 'NOT-A-STATUS'] }),
                dotExperimentsListPageEvents.statusesChanged([DotExperimentStatus.RUNNING])
            ],
            [
                'goal',
                () => facade.patchFilters({ [GOAL]: ['BOUNCE_RATE', 'NOT-A-GOAL'] }),
                dotExperimentsListPageEvents.goalsChanged([GOAL_TYPES.BOUNCE_RATE])
            ]
        ])(
            'should drop an unknown %s rather than casting it into typed state',
            (_l, act, expected) => {
                // Values reach this through the shared facade contract, so they are as untrusted as
                // the address is — and the address path already drops what it cannot recognise.
                act();

                expect(dispatched()).toContainEqual(expected);
            }
        );

        /**
         * The two bounds are one event, so a patch naming one has to carry the other across.
         * Dispatching twice would put a half-applied period on screen and reset paging twice.
         */
        it('should carry the untouched bound across when only one is patched', () => {
            state.scheduleFrom.set('2026-06-01');
            state.scheduleTo.set('2026-06-30');

            facade.patchFilters({ [SCHEDULE_TO]: '2026-07-15' });

            expect(dispatched()).toEqual([
                dotExperimentsListPageEvents.scheduleChanged({
                    from: '2026-06-01',
                    to: '2026-07-15'
                })
            ]);
        });

        it('should send one event carrying both bounds when they are patched together', () => {
            facade.patchFilters({ [SCHEDULE_FROM]: '2026-06-01', [SCHEDULE_TO]: '2026-06-30' });

            // The payload, not just the count: two events with the bounds swapped would also be
            // "one event each" and would read as a period nobody picked.
            expect(dispatched()).toEqual([
                dotExperimentsListPageEvents.scheduleChanged({
                    from: '2026-06-01',
                    to: '2026-06-30'
                })
            ]);
        });
    });

    describe('removing', () => {
        it.each([
            [STATUS, dotExperimentsListPageEvents.statusesChanged([])],
            [GOAL, dotExperimentsListPageEvents.goalsChanged([])],
            [CREATED_BY, dotExperimentsListPageEvents.creatorsChanged([])]
        ])('should empty %s, which is what removal means here', (key, expected) => {
            facade.removeFilter(key);

            expect(dispatched()).toContainEqual(expected);
        });

        it('should open the removed schedule bound and keep the other', () => {
            state.scheduleFrom.set('2026-06-01');
            state.scheduleTo.set('2026-06-30');

            facade.removeFilter(SCHEDULE_FROM);

            expect(dispatched()).toEqual([
                dotExperimentsListPageEvents.scheduleChanged({ from: null, to: '2026-06-30' })
            ]);
        });

        it('should dispatch nothing for a key it does not have', () => {
            facade.removeFilter('never-set');

            expect(dispatched()).toEqual([]);
        });
    });

    describe('clearing', () => {
        /**
         * The chip-scoped event, not `filtersCleared`. The bar's button is on screen while the
         * list is working, and a page narrowing that arrived from the editor is not the user's
         * to widen from there — `filtersCleared` stays the empty state's way out of a narrowing
         * that matched nothing.
         */
        it('should clear what the chips own without touching the page narrowing', () => {
            facade.clearFilters();

            expect(dispatched()).toEqual([dotExperimentsListPageEvents.chipFiltersCleared()]);
        });
    });

    describe('$hasNonDefaultFilters', () => {
        it('should be false on a listing nobody has filtered', () => {
            expect(facade.$hasNonDefaultFilters()).toBe(false);
        });

        it.each([
            ['a status', () => state.selectedStatuses.set([DotExperimentStatus.RUNNING])],
            ['a goal', () => state.selectedGoals.set([GOAL_TYPES.EXIT_RATE])],
            ['a creator', () => state.selectedCreators.set(['dotcms.org.1'])],
            ['a lower schedule bound', () => state.scheduleFrom.set('2026-06-01')],
            ['an upper schedule bound', () => state.scheduleTo.set('2026-06-30')]
        ])('should be true for %s alone', (_label, setUp) => {
            setUp();

            expect(facade.$hasNonDefaultFilters()).toBe(true);
        });

        it('should not count the search term', () => {
            // The term is not a chip, and "Clear all" sits in the chip row: the search box has its
            // own clear affordance beside it, and the no-results state clears both together. Set
            // for real — asserting `false` without setting one tests nothing and reads identically
            // to the fresh-listing case above.
            state.filter.set('quarterly report');

            expect(facade.$hasNonDefaultFilters()).toBe(false);
        });
    });
});
