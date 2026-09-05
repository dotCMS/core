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
});
