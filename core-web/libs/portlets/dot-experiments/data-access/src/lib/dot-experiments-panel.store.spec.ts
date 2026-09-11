import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';

import { signal, WritableSignal } from '@angular/core';

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
