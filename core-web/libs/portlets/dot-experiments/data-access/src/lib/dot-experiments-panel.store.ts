import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withProps,
    withState
} from '@ngrx/signals';

import {
    computed,
    effect,
    EffectRef,
    inject,
    Injector,
    Signal,
    signal,
    untracked
} from '@angular/core';

/** The single surface the panel shows at any moment. Never two of these at once. */
export type DotExperimentsPanelView = 'list' | 'create' | 'configure' | 'results';

/**
 * The editor's live context, handed to the panel by the UVE shell.
 *
 * Signals rather than values, deliberately: the page and the language must be read at the moment
 * they are used. A value captured when the panel opened goes stale the first time the editor
 * changes page or language, and a stale language sends the variant round trip back to the wrong
 * version of the page — which is invisible until the wrong content loads (#37478 D10, FR-022a).
 */
export interface DotExperimentsPanelContext {
    pageId: Signal<string | null>;
    languageId: Signal<number | null>;
}

export interface DotExperimentsPanelState {
    isOpen: boolean;
    view: DotExperimentsPanelView;
    /** The experiment `configure` and `results` are showing; `null` on `list` and `create`. */
    experimentId: string | null;
    /**
     * The panel is closed **because the editor left to see a variant**, not because they
     * dismissed it.
     *
     * This is the one state in which a closed panel keeps its view state, and it is why closing
     * and suspending are two named methods rather than one with a flag: collapsing them turns
     * the variant round trip into a reset, and the editor loses their place on every trip
     * (FR-023, FR-025 vs FR-039).
     */
    suspendedForVariant: boolean;
}

const initialState: DotExperimentsPanelState = {
    isOpen: false,
    view: 'list',
    experimentId: null,
    suspendedForVariant: false
};

/**
 * The Experiments panel's view state, and the signal that tells the portlet's screens they are
 * rendering inside it (#37478).
 *
 * **Provided by the UVE shell, not by the panel.** The panel component is mounted and destroyed
 * by the shell's `@if` on {@link isOpen}, so a store owned by the panel would die with it — and
 * the variant round trip needs the state to survive a period during which the panel is closed.
 * The shell is the smallest scope that outlives that trip: leaving for a variant only changes
 * the editor's query params, so the shell is never re-created. A browser reload does destroy it,
 * which is exactly the case the spec declines to define.
 *
 * **Methods rather than dispatched events**, unlike the three screen stores beside it. Those
 * hold server state and every transition is an async flow worth naming as an event. This holds
 * only what the editor is looking at, no request ever reaches it, and its callers are a
 * component and a toolbar. `withReducer` here would be ceremony around four fields.
 *
 * Nothing in this store touches the router, `Location` or `window.history` — not the fact that
 * it is open, not the view, not the experiment (FR-002, FR-031, FR-035).
 *
 * @see specs/37478-uve-experiments-panel/contracts/panel-store.contract.md
 */
export const DotExperimentsPanelStore = signalStore(
    withState<DotExperimentsPanelState>(initialState),
    withProps(() => ({
        /** Underscore-prefixed: internal to the store, not part of its contract. */
        _context: signal<DotExperimentsPanelContext | null>(null),
        _injector: inject(Injector),
        /** Holder so {@link withHooks} can tear down an effect {@link withMethods} created. */
        _rescope: { effect: null as EffectRef | null }
    })),
    withComputed((store) => ({
        /** The page in hand. Read live from the editor, never stored — see the context doc. */
        pageId: computed(() => store._context()?.pageId() ?? null),
        /**
         * The language version the editor is standing in — **return context only**.
         *
         * It never narrows the panel and is never part of what is created or saved: an
         * experiment belongs to a page, not to a language version of one (FR-015, FR-034a).
         */
        languageId: computed(() => store._context()?.languageId() ?? null)
    })),
    withMethods((store) => ({
        /**
         * Hands the panel the editor's page and language, and starts watching the page.
         *
         * The watch reads **only** `pageId`. A language change therefore cannot reach it by
         * construction rather than by a guard someone could later remove — which is the point,
         * because refetching on a language change would re-request a provably identical set and
         * resetting would discard the editor's view state for no change in the answer (D10).
         */
        setContext(context: DotExperimentsPanelContext): void {
            store._rescope.effect?.destroy();
            store._context.set(context);

            let knownPageId = untracked(() => context.pageId());

            store._rescope.effect = effect(
                () => {
                    const currentPageId = context.pageId();

                    if (currentPageId === knownPageId) {
                        return;
                    }

                    knownPageId = currentPageId;

                    // `isOpen` is deliberately untouched: a page change re-scopes the panel, it
                    // neither closes one the editor is using nor reopens one they dismissed
                    // (FR-034). Everything that described the old page goes, including a
                    // suspended round trip, whose experiment is not on this page (FR-034b).
                    untracked(() =>
                        patchState(store, {
                            view: 'list',
                            experimentId: null,
                            suspendedForVariant: false
                        })
                    );
                },
                { injector: store._injector }
            );
        },

        /**
         * Opens the panel on the page's list — always, whether or not the page has experiments.
         *
         * Never on creation: the gesture is "show me this page's experiments", and the honest
         * answer on an untested page is "none yet", not a form the editor has to back out of
         * (D11). The empty state carries the create action instead.
         */
        open(): void {
            patchState(store, { ...initialState, isOpen: true });
        },

        /**
         * Opens the panel straight onto an experiment's results — the toolbar's running-experiment
         * badge.
         *
         * Replaces whatever the panel was showing rather than opening a second surface: the badge
         * always means "show me the running experiment" (FR-025c).
         */
        openResults(experimentId: string): void {
            patchState(store, {
                ...initialState,
                isOpen: true,
                view: 'results',
                experimentId
            });
        },

        showCreate(): void {
            patchState(store, { view: 'create', experimentId: null });
        },

        showConfigure(experimentId: string): void {
            patchState(store, { view: 'configure', experimentId });
        },

        showResults(experimentId: string): void {
            patchState(store, { view: 'results', experimentId });
        },

        backToList(): void {
            patchState(store, { view: 'list', experimentId: null });
        },

        /**
         * The editor dismissed the panel. Everything it held goes, so a later open shows current
         * data rather than what was on screen at this close (FR-039, SC-016).
         */
        close(): void {
            patchState(store, initialState);
        },

        /**
         * The editor left for a variant. The panel closes but remembers where they were, so the
         * return lands on the same experiment's configuration rather than on the list (FR-023).
         */
        suspendForVariant(): void {
            patchState(store, { isOpen: false, suspendedForVariant: true });
        },

        /**
         * The return control. A no-op unless the panel was suspended — a reload during the trip
         * leaves the editor on the page, not on a phantom panel, and a dismissed panel stays
         * dismissed.
         */
        resumeFromVariant(): void {
            if (!store.suspendedForVariant()) {
                return;
            }

            patchState(store, { isOpen: true, suspendedForVariant: false });
        }
    })),
    withHooks((store) => ({
        onDestroy() {
            store._rescope.effect?.destroy();
        }
    }))
);

/** Injectable type of {@link DotExperimentsPanelStore}, for typing component/service fields. */
export type DotExperimentsPanelStore = InstanceType<typeof DotExperimentsPanelStore>;
