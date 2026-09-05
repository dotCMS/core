import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentTypeFilterChipComponent } from './dot-content-type-filter-chip.component';

import { DotContentTypeFilterComponent } from '../../../dot-content-type-filter/dot-content-type-filter.component';
import { DOT_FILTER_FACADE, DotFilterFacade } from '../../filter-facade.token';

describe('DotContentTypeFilterChipComponent', () => {
    let spectator: Spectator<DotContentTypeFilterChipComponent>;

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
        component: DotContentTypeFilterChipComponent,
        providers: [
            { provide: DOT_FILTER_FACADE, useValue: facade },
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of([])),
                getContentTypesWithPagination: jest
                    .fn()
                    .mockReturnValue(of({ contentTypes: [], pagination: {} }))
            }),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    const inner = () => spectator.query(DotContentTypeFilterComponent);

    beforeEach(() => {
        stored.set({});
        spectator = createComponent();
        spectator.detectChanges();
    });

    afterEach(() => jest.clearAllMocks());

    it('should identify itself for the canonical order check', () => {
        expect(spectator.element.getAttribute('data-filter-chip')).toBe('contentType');
    });

    it('should bind the stored selection into the presentational filter', () => {
        stored.set({ baseType: [DotCMSBaseTypesContentTypes.DOTASSET], contentType: ['Blog'] });
        spectator.detectChanges();

        expect(inner()?.$baseTypes()).toEqual([DotCMSBaseTypesContentTypes.DOTASSET]);
        expect(inner()?.$contentTypes()).toEqual(['Blog']);
    });

    it('should pass the caller restriction through as a bound, not as a selection', () => {
        // `allowedBaseTypes` is what the surface may ever OFFER; the selection is what is picked.
        // Conflating them is what once let a File field list Pages.
        spectator.setInput('allowedBaseTypes', [DotCMSBaseTypesContentTypes.FILEASSET]);
        spectator.detectChanges();

        expect(inner()?.$allowedBaseTypes()).toEqual([DotCMSBaseTypesContentTypes.FILEASSET]);
    });

    it('should survive a single stored string rather than an array', () => {
        // `DotFilterValue` is `string | string[]`, and Content Drive's own status filter documents
        // that a bare string is what a URL decoder produces when it loses the array shape. A cast
        // would let that reach the presentational filter as a non-array.
        stored.set({
            baseType: 'DOTASSET' as unknown as string[],
            contentType: 'Blog' as unknown as string[]
        });
        spectator.detectChanges();

        expect(inner()?.$baseTypes()).toEqual(['DOTASSET']);
        expect(inner()?.$contentTypes()).toEqual(['Blog']);
    });

    it('should write a selection through the facade', () => {
        spectator.triggerEventHandler(DotContentTypeFilterComponent, 'selectionChange', {
            baseTypes: [DotCMSBaseTypesContentTypes.DOTASSET],
            contentTypes: ['Blog']
        });

        expect(patchFilters).toHaveBeenCalledWith({
            baseType: [DotCMSBaseTypesContentTypes.DOTASSET]
        });
        expect(patchFilters).toHaveBeenCalledWith({ contentType: ['Blog'] });
    });

    it('should remove a key rather than write an empty selection', () => {
        // An empty array and an absent key mean different things downstream, and only the absent
        // one leaves the request byte-identical to never having filtered.
        spectator.triggerEventHandler(DotContentTypeFilterComponent, 'selectionChange', {
            baseTypes: [],
            contentTypes: []
        });

        expect(removeFilter).toHaveBeenCalledWith('baseType');
        expect(removeFilter).toHaveBeenCalledWith('contentType');
        expect(patchFilters).not.toHaveBeenCalled();
    });
});
