import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService, mockLocales } from '@dotcms/utils-testing';

import { DotLanguageFilterChipComponent } from './dot-language-filter-chip.component';

import { DotLanguageFilterComponent } from '../../../dot-language-filter/dot-language-filter.component';
import { DOT_FILTER_FACADE, DotFilterFacade } from '../../filter-facade.token';

describe('DotLanguageFilterChipComponent', () => {
    let spectator: Spectator<DotLanguageFilterChipComponent>;

    const stored = signal<Record<string, string | string[]>>({});
    const patchFilters = jest.fn();
    const removeFilter = jest.fn();

    const facade: DotFilterFacade = {
        getFilterValue: jest.fn((key: string) => stored()[key]),
        patchFilters,
        removeFilter,
        clearFilters: jest.fn(),
        $hasNonDefaultFilters: signal(false)
    };

    const createComponent = createComponentFactory({
        component: DotLanguageFilterChipComponent,
        providers: [
            { provide: DOT_FILTER_FACADE, useValue: facade },
            mockProvider(DotLanguagesService, { get: jest.fn().mockReturnValue(of(mockLocales)) }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    const inner = () => spectator.query(DotLanguageFilterComponent);

    beforeEach(() => {
        stored.set({});
        spectator = createComponent();
        spectator.detectChanges();
    });

    afterEach(() => jest.clearAllMocks());

    it('should identify itself for the canonical order check', () => {
        expect(spectator.element.getAttribute('data-filter-chip')).toBe('language');
    });

    it('should bind the stored ids into the presentational filter as numbers', () => {
        // The bag holds strings — it has to survive a URL on one surface — while the filter takes
        // numeric language ids.
        stored.set({ languageId: ['1', '2'] });
        spectator.detectChanges();

        expect(inner()?.$selectedLanguageIds()).toEqual([1, 2]);
    });

    it('should survive a single stored string rather than an array', () => {
        // Worse here than elsewhere: `.map(Number)` on a bare string is a TypeError, so the cast
        // does not just mislead — it crashes the chip.
        stored.set({ languageId: '2' as unknown as string[] });
        spectator.detectChanges();

        expect(inner()?.$selectedLanguageIds()).toEqual([2]);
    });

    it('should write ids back as strings', () => {
        spectator.triggerEventHandler(DotLanguageFilterComponent, 'selectionChange', [1, 2]);

        expect(patchFilters).toHaveBeenCalledWith({ languageId: ['1', '2'] });
    });

    it('should remove the key rather than write an empty selection', () => {
        spectator.triggerEventHandler(DotLanguageFilterComponent, 'selectionChange', []);

        expect(removeFilter).toHaveBeenCalledWith('languageId');
        expect(patchFilters).not.toHaveBeenCalled();
    });

    // Restored from the deleted `dot-content-drive-language-field` adapter, where this behaviour
    // (bug 5) lived and was lost when the adapter was replaced by this chip. The rule moved here so
    // a third surface inherits it; the wiring half — that Content Drive actually passes its seeded
    // locale — is pinned in the toolbar's own spec.
    describe('removable', () => {
        it('should not offer removal while the only selection is the locale the surface re-seeds', () => {
            // Clearing it would re-seed the very same value, so the X would do nothing visible.
            stored.set({ languageId: ['1'] });
            spectator.setInput('defaultLanguageId', 1);
            spectator.detectChanges();

            expect(inner()?.$removable()).toBe(false);
        });

        it('should offer removal once a non-default language is selected', () => {
            stored.set({ languageId: ['2'] });
            spectator.setInput('defaultLanguageId', 1);
            spectator.detectChanges();

            expect(inner()?.$removable()).toBe(true);
        });

        it('should offer removal when the default is selected alongside another', () => {
            // Two selections means clearing genuinely changes what is filtered.
            stored.set({ languageId: ['1', '2'] });
            spectator.setInput('defaultLanguageId', 1);
            spectator.detectChanges();

            expect(inner()?.$removable()).toBe(true);
        });

        it('should offer removal when nothing is selected yet', () => {
            spectator.setInput('defaultLanguageId', 1);
            spectator.detectChanges();

            expect(inner()?.$removable()).toBe(true);
        });

        it('should track the seeded locale of the surface rather than hardcoding an id', () => {
            // A different default must move the behaviour with it.
            stored.set({ languageId: ['2'] });
            spectator.setInput('defaultLanguageId', 2);
            spectator.detectChanges();

            expect(inner()?.$removable()).toBe(false);
        });

        it('should keep the X on a surface that seeds no locale', () => {
            // The AssetPicker's case: it re-seeds only on open and on "Clear all", so removing the
            // filter really does widen the results.
            stored.set({ languageId: ['1'] });
            spectator.detectChanges();

            expect(inner()?.$removable()).toBe(true);
        });
    });
});
