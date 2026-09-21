import { Dispatcher } from '@ngrx/signals/events';

import { computed, inject, Provider } from '@angular/core';

import { DOT_FILTER_FACADE, DotFilterFacade, DotFilterValue, toFilterValues } from '@dotcms/ui';

import { dotExperimentsListPageEvents } from './dot-experiments-list-page.events';
import { DotExperimentsListStore } from './dot-experiments-list.store';

import { parseGoals, parseStatuses } from '../util/dot-experiments-list-store.util';

type ExperimentsListStore = InstanceType<typeof DotExperimentsListStore>;

/**
 * The listing's filter keys as the shared chips see them.
 *
 * Not the query-parameter names, and not the state field names either. `created_by` is the
 * parameter (#36823's name, which #37007 consumes) and `selectedCreators` is the state; this is
 * the third vocabulary, the one the chips speak. Keeping them distinct is what lets the address
 * keep the names the API contract fixed while the chips stay surface-agnostic.
 */
export const EXPERIMENTS_FILTER_KEYS = {
    STATUS: 'status',
    GOAL: 'goal',
    CREATED_BY: 'createdBy',
    SCHEDULE_FROM: 'scheduleFrom',
    SCHEDULE_TO: 'scheduleTo'
} as const;

/** A single bound, which the facade carries as a one-or-zero-element value. */
const firstOrNull = (value: DotFilterValue | undefined): string | null =>
    toFilterValues(value)[0] ?? null;

/**
 * The experiments listing's {@link DotFilterFacade}.
 *
 * What it absorbs is a shape difference rather than an encoding one. Content Drive's store keeps a
 * string-keyed filter bag, so its facade mostly delegates; this store keeps **typed state** and
 * changes it only through dispatched events, which is deliberate — a reducer that took
 * `Record<string, string | string[]>` would lose the exhaustiveness that makes an unknown status
 * impossible to store. So this maps five string keys onto four typed events, and back.
 *
 * Two consequences worth naming.
 *
 * `undefined` versus `[]` (contract O2) is answered from the state's own emptiness: an empty
 * selection *is* "not filtered" on this screen, because no filter here has a "selected nothing"
 * state distinct from being off. So `getFilterValue` reports `undefined` for an empty selection
 * rather than `[]`, which is the honest reading and is what keeps `removeFilter` and "patch to
 * empty" indistinguishable — as they are in the store.
 *
 * Nothing is unmappable on the way out, so the conformance suite runs with `normalizes: false`.
 * An unknown status cannot be stored: the address drops it on parse (FR-048) and the reducer only
 * accepts typed payloads. That is a property of the store, not an omission here.
 */
export function createExperimentsFilterFacade(
    store: ExperimentsListStore,
    dispatcher: Dispatcher
): DotFilterFacade {
    const { STATUS, GOAL, CREATED_BY, SCHEDULE_FROM, SCHEDULE_TO } = EXPERIMENTS_FILTER_KEYS;

    /** Absent rather than empty — see the note on O2 above. */
    const orAbsent = (values: readonly string[]): DotFilterValue | undefined =>
        values.length ? [...values] : undefined;

    const readers: Record<string, () => DotFilterValue | undefined> = {
        [STATUS]: () => orAbsent(store.selectedStatuses()),
        [GOAL]: () => orAbsent(store.selectedGoals()),
        [CREATED_BY]: () => orAbsent(store.selectedCreators()),
        [SCHEDULE_FROM]: () => store.scheduleFrom() ?? undefined,
        [SCHEDULE_TO]: () => store.scheduleTo() ?? undefined
    };

    /**
     * Both schedule bounds reach the store in one event, so a patch naming only one has to carry
     * the other's current value across. Two events would put a half-applied period on screen, and
     * each would reset paging.
     */
    const dispatchSchedule = (patch: Record<string, DotFilterValue>): void => {
        dispatcher.dispatch(
            dotExperimentsListPageEvents.scheduleChanged({
                from:
                    SCHEDULE_FROM in patch
                        ? firstOrNull(patch[SCHEDULE_FROM])
                        : store.scheduleFrom(),
                to: SCHEDULE_TO in patch ? firstOrNull(patch[SCHEDULE_TO]) : store.scheduleTo()
            })
        );
    };

    const patchFilters = (patch: Record<string, DotFilterValue>): void => {
        // Parsed, not cast. These values arrive through the shared facade contract, so any chip
        // on any surface can hand over arbitrary strings — the same untrusted shape the address
        // has, which is why these parsers already exist and why the URL path already uses them.
        // A cast would let an unknown status into typed state through the one door that was left
        // open.
        if (STATUS in patch) {
            dispatcher.dispatch(
                dotExperimentsListPageEvents.statusesChanged(
                    parseStatuses(toFilterValues(patch[STATUS]))
                )
            );
        }

        if (GOAL in patch) {
            dispatcher.dispatch(
                dotExperimentsListPageEvents.goalsChanged(parseGoals(toFilterValues(patch[GOAL])))
            );
        }

        if (CREATED_BY in patch) {
            dispatcher.dispatch(
                dotExperimentsListPageEvents.creatorsChanged(toFilterValues(patch[CREATED_BY]))
            );
        }

        if (SCHEDULE_FROM in patch || SCHEDULE_TO in patch) {
            dispatchSchedule(patch);
        }
    };

    return {
        getFilterValue: (key: string) => readers[key]?.(),

        patchFilters,

        // Emptying is removing on this screen: no filter here distinguishes "selected nothing"
        // from "off", so there is no state left behind for a key to hold. An unknown key is
        // ignored rather than dispatched blindly.
        removeFilter: (key: string): void => {
            if (readers[key]) {
                patchFilters({ [key]: [] });
            }
        },

        clearFilters: (): void => {
            dispatcher.dispatch(dotExperimentsListPageEvents.filtersCleared());
        },

        /**
         * Every default here is "nothing selected", so this is simply "is anything narrowing".
         *
         * Deliberately **not** the same question as the screen's `$hasActiveFilters`, which also
         * counts a page narrowing that arrived in the address and matched nothing. That one decides
         * which empty state to show; this one decides whether clearing is worth offering, and the
         * page narrowing is not the user's to clear from here.
         */
        $hasNonDefaultFilters: computed(
            () =>
                store.selectedStatuses().length > 0 ||
                store.selectedGoals().length > 0 ||
                store.selectedCreators().length > 0 ||
                store.scheduleFrom() !== null ||
                store.scheduleTo() !== null
        )
    };
}

/**
 * Provides {@link DOT_FILTER_FACADE} over the experiments list store.
 *
 * Goes on the component that provides `DotExperimentsListStore` — the list component — so every
 * chip in its toolbar resolves the same instance. Never in `root`: the panel renders a second
 * listing with its own store.
 */
export function provideExperimentsFilterFacade(): Provider {
    return {
        provide: DOT_FILTER_FACADE,
        useFactory: () =>
            createExperimentsFilterFacade(inject(DotExperimentsListStore), inject(Dispatcher))
    };
}
