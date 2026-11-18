import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';

import { signal, WritableSignal } from '@angular/core';

import { CONFIGURE_SECTION_VARIANTS } from '@dotcms/dotcms-models';

import { DotExperimentsPanelStore } from './dot-experiments-panel.store';

const PAGE_A = 'page-a';
const PAGE_B = 'page-b';
const EXPERIMENT_A = 'exp-a';
const EXPERIMENT_B = 'exp-b';

const LANGUAGE_EN = 1;
const LANGUAGE_ES = 2;

/**
 * The panel's view state and mode signal (#37478).
 *
 * Everything here is asserted against
 * `specs/37478-uve-experiments-panel/contracts/panel-store.contract.md`. Two of these tests
 * exist because the behaviour they pin is invisible when it breaks:
 *
 * - `close()` and `suspendForVariant()` are two different closes. Collapsing them into one with
 *   a boolean is the "simplification" that silently turns the variant round trip into a reset,
 *   and FR-023 is what it would break.
 * - A language change must transition nothing. Refetching or resetting on it would discard the
 *   editor's view state for a set of experiments that is provably identical, because
 *   `DotExperiment` carries `pageId` and no language at all.
 */
describe('DotExperimentsPanelStore', () => {
    let spectator: SpectatorService<DotExperimentsPanelStore>;
    let store: DotExperimentsPanelStore;

    let pageId: WritableSignal<string | null>;
    let languageId: WritableSignal<number | null>;

    const createService = createServiceFactory({
        service: DotExperimentsPanelStore
    });

    /**
     * Creates the store and hands it the editor's live context, as the shell does.
     *
     * Signals rather than values on purpose: the contract requires the page and language be read
     * live, so a captured value cannot go stale against the canvas (D10).
     */
    const initStore = (page: string | null = PAGE_A, language: number | null = LANGUAGE_EN) => {
        spectator = createService();
        store = spectator.service;

        pageId = signal(page);
        languageId = signal(language);

        store.setContext({ pageId, languageId });
        spectator.flushEffects();
    };

    /**
     * The trip that begins in the full-screen portlet: Preview from a Variants card navigates the
     * editor into the UVE, so the shell — and this store — are built from nothing, with the page
     * still unloaded. The panel then has to open on the experiment the editor came from, and the
     * page arriving underneath it must not be read as a page *change*.
     */
    describe('returning from a variant that started in the portlet', () => {
        it('should keep the experiment when the page resolves after the return', () => {
            // The shell calls setContext in ngOnInit, before the page has loaded.
            initStore(null);

            pageId.set(PAGE_A);
            spectator.flushEffects();

            store.openVariants('exp-1');

            // The way back reloads the page, which blanks the asset while it is in flight.
            pageId.set(null);
            spectator.flushEffects();
            pageId.set(PAGE_A);
            spectator.flushEffects();

            expect(store.isOpen()).toBe(true);
            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe('exp-1');
        });

        /**
         * The same trip, with the page arriving *after* the panel has been pointed somewhere.
         *
         * Re-scoping means "the editor moved to another page", and that needs a page to have moved
         * *from*. A store built on an unloaded shell has none, so the first identifier it ever sees
         * is the page it is already on — adopting it, not a change to react to. Reading it as a
         * change threw away the destination the return had just set and dropped the panel on the
         * list.
         */
        it('should adopt the first page it sees rather than re-scope to it', () => {
            initStore(null);

            store.openVariants('exp-1');

            pageId.set(PAGE_A);
            spectator.flushEffects();

            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe('exp-1');
        });
    });

    describe('initial state', () => {
        it('starts closed, on the list, with no experiment', () => {
            initStore();

            expect(store.isOpen()).toBe(false);
            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });

        it('exposes the page and language it was given', () => {
            initStore(PAGE_A, LANGUAGE_ES);

            expect(store.pageId()).toBe(PAGE_A);
            expect(store.languageId()).toBe(LANGUAGE_ES);
        });
    });

    describe('open()', () => {
        it('opens on the list', () => {
            initStore();

            store.open();

            expect(store.isOpen()).toBe(true);
            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });

        it('opens on the list even when the panel was last showing a configuration', () => {
            initStore();

            store.open();
            store.showConfigure(EXPERIMENT_A);
            store.close();
            store.open();

            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });
    });

    describe('navigating between views', () => {
        it('shows the creation screen', () => {
            initStore();
            store.open();

            store.showCreate();

            expect(store.view()).toBe('create');
            expect(store.experimentId()).toBeNull();
        });

        it('shows a configuration, holding the experiment it is for', () => {
            initStore();
            store.open();

            store.showConfigure(EXPERIMENT_A);

            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe(EXPERIMENT_A);
        });

        it('shows results, holding the experiment they are for', () => {
            initStore();
            store.open();

            store.showResults(EXPERIMENT_A);

            expect(store.view()).toBe('results');
            expect(store.experimentId()).toBe(EXPERIMENT_A);
        });

        it('returns to the list and forgets the experiment', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);

            store.backToList();

            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });
    });

    describe('openResults() — the toolbar badge', () => {
        it('opens the panel straight onto that experiment’s results', () => {
            initStore();

            store.openResults(EXPERIMENT_A);

            expect(store.isOpen()).toBe(true);
            expect(store.view()).toBe('results');
            expect(store.experimentId()).toBe(EXPERIMENT_A);
        });

        it('replaces what the panel was showing instead of stacking a second surface', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);

            store.openResults(EXPERIMENT_B);

            expect(store.isOpen()).toBe(true);
            expect(store.view()).toBe('results');
            expect(store.experimentId()).toBe(EXPERIMENT_B);
        });
    });

    describe('close()', () => {
        it('closes and discards the view state', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);

            store.close();

            expect(store.isOpen()).toBe(false);
            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });

        it('leaves nothing for a later open to show as current', () => {
            initStore();
            store.open();
            store.showResults(EXPERIMENT_A);
            store.close();

            store.open();

            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });
    });

    /**
     * #37478, FR-023, FR-046. The return leg, reconstructed rather than remembered.
     *
     * The experiment is named by the editor's own address while they are on the variant, so the
     * way back depends on nothing surviving the trip. These tests are written from the worst
     * starting points on purpose — a panel that was never suspended, and one whose state was wiped
     * while the editor was away — because those are the states a reload or a re-scope leaves
     * behind, and the first implementation could not return from either.
     */
    /**
     * The re-scope, and the one input it must not react to (#37478, FR-034).
     *
     * `pageId` is read off the editor's page asset, which is re-fetched whenever the editor's
     * address changes — including when the variant round trip clears the variant off it. While
     * that fetch is out the asset has no identifier, and a `null` read there means "not loaded
     * yet", not "the editor moved to another page". Treating the two alike let a reload wipe the
     * view the return had just restored, and the panel came back on the list instead of on the
     * configuration it left.
     */
    /**
     * FR-023. The trip leaves from the Variants card and has to come back to it: Configure is four
     * stacked cards tall, and returning to the top loses the reader's place.
     */
    it('should remember the Variants card across the round trip', () => {
        initStore();
        store.showConfigure('exp-9');

        store.suspendForVariant();
        store.resumeFromVariant();

        expect(store.view()).toBe('configure');
        expect(store.experimentId()).toBe('exp-9');
        expect(store.section()).toBe(CONFIGURE_SECTION_VARIANTS);
    });

    describe('re-scoping on the editor page', () => {
        it('should not re-scope while the page asset is between loads', () => {
            initStore();
            store.openVariants('exp-9');

            pageId.set(null);
            spectator.flushEffects();

            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe('exp-9');
        });

        it('should re-scope once the editor is really on another page', () => {
            initStore();
            store.openVariants('exp-9');

            pageId.set(PAGE_B);
            spectator.flushEffects();

            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });

        /** The same page coming back from a reload is not a move either. */
        it('should not re-scope when the same page returns after a gap', () => {
            initStore();
            store.openVariants('exp-9');

            pageId.set(null);
            spectator.flushEffects();
            pageId.set(PAGE_A);
            spectator.flushEffects();

            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe('exp-9');
        });
    });

    describe('openVariants()', () => {
        it('should land on that experiment configuration, at the Variants card', () => {
            initStore();

            store.openVariants('exp-9');

            expect(store.isOpen()).toBe(true);
            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe('exp-9');
            expect(store.section()).toBe(CONFIGURE_SECTION_VARIANTS);
        });

        /**
         * A panel that never suspended, built rather than inherited. The trip that began in the
         * full-screen portlet arrives at a store this fresh, so the precondition is the test's to
         * establish — asserting it off whatever a sibling left behind proves nothing about this
         * path.
         */
        it('should return from a panel that was never suspended', () => {
            initStore();
            expect(store.suspendedForVariant()).toBe(false);

            store.openVariants('exp-9');

            expect(store.isOpen()).toBe(true);
            expect(store.experimentId()).toBe('exp-9');
        });

        it('should clear the suspension it may or may not have been holding', () => {
            initStore();
            store.showConfigure('exp-9');
            store.suspendForVariant();

            store.openVariants('exp-9');

            expect(store.suspendedForVariant()).toBe(false);
        });

        /** Only the return asks for a card; everything else starts at the top of the form. */
        it('should leave the Variants card behind on the next move', () => {
            initStore();
            store.openVariants('exp-9');

            store.backToList();

            expect(store.section()).toBeNull();
        });
    });

    describe('the variant round trip', () => {
        it('suspendForVariant() closes the panel but keeps where the editor was', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);

            store.suspendForVariant();

            expect(store.isOpen()).toBe(false);
            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe(EXPERIMENT_A);
        });

        it('resumeFromVariant() reopens on the same experiment’s configuration', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);
            store.suspendForVariant();

            store.resumeFromVariant();

            expect(store.isOpen()).toBe(true);
            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe(EXPERIMENT_A);
        });

        it('does nothing when the panel was never suspended', () => {
            initStore();

            store.resumeFromVariant();

            expect(store.isOpen()).toBe(false);
            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });

        it('does nothing when the panel was closed rather than suspended', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);
            store.close();

            store.resumeFromVariant();

            expect(store.isOpen()).toBe(false);
            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });

        it('a close after a suspend discards the suspended state', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);
            store.suspendForVariant();

            store.close();
            store.resumeFromVariant();

            expect(store.isOpen()).toBe(false);
            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });
    });

    describe('the page on the canvas changes', () => {
        it('re-scopes to the new page, back on the list', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);

            pageId.set(PAGE_B);
            spectator.flushEffects();

            expect(store.pageId()).toBe(PAGE_B);
            expect(store.view()).toBe('list');
            expect(store.experimentId()).toBeNull();
        });

        it('leaves the panel open while it re-scopes', () => {
            initStore();
            store.open();

            pageId.set(PAGE_B);
            spectator.flushEffects();

            expect(store.isOpen()).toBe(true);
        });

        it('does not reopen a panel the editor had closed', () => {
            initStore();

            pageId.set(PAGE_B);
            spectator.flushEffects();

            expect(store.isOpen()).toBe(false);
        });

        it('discards a suspended round trip that belonged to the old page', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);
            store.suspendForVariant();

            pageId.set(PAGE_B);
            spectator.flushEffects();
            store.resumeFromVariant();

            expect(store.isOpen()).toBe(false);
            expect(store.experimentId()).toBeNull();
        });
    });

    describe('the language on the canvas changes', () => {
        it('changes nothing about what the panel is showing', () => {
            initStore();
            store.open();
            store.showConfigure(EXPERIMENT_A);

            languageId.set(LANGUAGE_ES);
            spectator.flushEffects();

            expect(store.isOpen()).toBe(true);
            expect(store.view()).toBe('configure');
            expect(store.experimentId()).toBe(EXPERIMENT_A);
        });

        it('updates the return context, read live rather than captured at open', () => {
            initStore(PAGE_A, LANGUAGE_EN);
            store.open();

            languageId.set(LANGUAGE_ES);
            spectator.flushEffects();

            expect(store.languageId()).toBe(LANGUAGE_ES);
        });
    });
});
