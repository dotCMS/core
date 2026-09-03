import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotContentTypeService, DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerToolbarComponent } from './dot-asset-picker-toolbar.component';

import { DotContentTypeFilterComponent } from '../../../dot-content-type-filter/dot-content-type-filter.component';
import { isCanonicalChipOrder } from '../../../dot-filter-bar/constants';
import { DOT_FILTER_FACADE, DotFilterFacade } from '../../../dot-filter-bar/filter-facade.token';
import { buildAssetPickerConfig } from '../../asset-picker-config';
import { DotAssetPickerStore } from '../../store/dot-asset-picker.store';
import { DotAssetPickerConfig } from '../../store/models';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

/** Only the slice of the store the toolbar reads. Signals, not `jest.fn()`s, so `computed` reacts. */
const createMockStore = (config: DotAssetPickerConfig) => ({
    config: signal(config),
    filters: signal({}),
    selectedNode: signal(undefined),
    setSearch: jest.fn(),
    patchFilters: jest.fn(),
    removeFilter: jest.fn(),
    clearFilters: jest.fn(),
    getFilterValue: jest.fn(() => undefined),
    $hasNonDefaultFilters: signal(false)
});

describe('DotAssetPickerToolbarComponent', () => {
    let spectator: Spectator<DotAssetPickerToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotAssetPickerToolbarComponent,
        providers: [
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of([])),
                getContentTypesWithPagination: jest
                    .fn()
                    .mockReturnValue(of({ contentTypes: [], pagination: {} }))
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of([]))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            // The shared filter chips reach the store through this seam rather than injecting it.
            {
                provide: DOT_FILTER_FACADE,
                useValue: {
                    getFilterValue: jest.fn(() => undefined),
                    patchFilters: jest.fn(),
                    removeFilter: jest.fn(),
                    clearFilters: jest.fn(),
                    $hasNonDefaultFilters: signal(false)
                } satisfies DotFilterFacade
            }
        ],
        detectChanges: false
    });

    const setup = (config: DotAssetPickerConfig) => {
        TestBed.overrideComponent(DotAssetPickerToolbarComponent, {
            add: {
                providers: [{ provide: DotAssetPickerStore, useValue: createMockStore(config) }]
            }
        });

        spectator = createComponent();
        spectator.detectChanges();
    };

    /** What the selector is allowed to OFFER, as the child filter sees it. */
    const offeredBaseTypes = () =>
        spectator.query(DotContentTypeFilterComponent)?.$allowedBaseTypes();

    describe('content-type selector restriction', () => {
        // AC (#36836): the selector offers dotAsset + File Asset in BOTH modes. What differs is
        // only the pre-selection.
        it('should offer only the asset-bearing base types for an Image field', () => {
            setup(buildAssetPickerConfig({ mode: 'image', site: SITE }));

            expect(offeredBaseTypes()).toEqual([
                DotCMSBaseTypesContentTypes.DOTASSET,
                DotCMSBaseTypesContentTypes.FILEASSET
            ]);
        });

        it('should offer only the asset-bearing base types for a File field', () => {
            // Regression: this used to be derived from `config.baseTypes` — empty for File — so the
            // selector fell back to "no restriction" and listed Widget and Content.
            setup(buildAssetPickerConfig({ mode: 'file', site: SITE }));

            expect(offeredBaseTypes()).toEqual([
                DotCMSBaseTypesContentTypes.DOTASSET,
                DotCMSBaseTypesContentTypes.FILEASSET
            ]);
        });

        it('should not pre-select anything for a File field while still restricting the options', () => {
            setup(buildAssetPickerConfig({ mode: 'file', site: SITE }));

            expect(spectator.query(DotContentTypeFilterComponent)?.$baseTypes()).toEqual([]);
            expect(offeredBaseTypes()).toHaveLength(2);
        });

        it('should leave the selector unrestricted when the config allows everything', () => {
            setup({ site: SITE });

            expect(offeredBaseTypes()).toBeNull();
        });
    });

    // ── US1 (T026): the Shared Assets chip reaches the picker's toolbar.

    describe('shared assets chip', () => {
        beforeEach(() => setup(buildAssetPickerConfig({ mode: 'image', site: SITE })));

        it('should render the shared assets chip', () => {
            expect(spectator.query(byTestId('shared-assets-filter-chip'))).toBeTruthy();
        });

        it('should render it as the first chip in the row', () => {
            // FR-007: the picker presents its filters in the same order Content Drive does, and
            // Shared Assets leads because it scopes which assets are in play at all.
            const chips = Array.from(spectator.element.querySelectorAll('[data-filter-chip]')).map(
                (element) => element.getAttribute('data-filter-chip')
            );

            expect(chips[0]).toBe('sharedAssets');
        });

        it('should present its chips in the canonical order', () => {
            const chips = Array.from(spectator.element.querySelectorAll('[data-filter-chip]')).map(
                (element) => element.getAttribute('data-filter-chip') as string
            );

            expect(isCanonicalChipOrder(chips)).toBe(true);
        });
    });
});
