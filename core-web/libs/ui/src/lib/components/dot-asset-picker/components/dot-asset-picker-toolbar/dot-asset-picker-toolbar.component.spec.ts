import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import {
    DotCategoriesService,
    DotContentletService,
    DotContentTypeService,
    DotLanguagesService,
    DotMessageService,
    DotTagsService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentTypeField,
    DotSite
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAssetPickerToolbarComponent } from './dot-asset-picker-toolbar.component';

import { DotContentTypeFilterComponent } from '../../../dot-content-type-filter/dot-content-type-filter.component';
import {
    DOT_FIELD_FILTER_HOST,
    DotFieldFilterHost
} from '../../../dot-filter-bar/chips/dot-field-filter/field-filter-host.token';
import { DotStatusFilterComponent } from '../../../dot-filter-bar/chips/dot-status-filter/dot-status-filter.component';
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
    userSearchableFields: signal<DotCMSContentTypeField[]>([]),
    userSearchableActive: signal<string[]>([]),
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
            // Reached by the field-filter chips, one service per field type that fetches options.
            mockProvider(DotContentletService),
            mockProvider(DotTagsService),
            mockProvider(DotCategoriesService),
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
            },
            // The "More" overflow and the chips it mints reach the surface through this one.
            {
                provide: DOT_FIELD_FILTER_HOST,
                useValue: {
                    $activeFields: signal<string[]>([]),
                    $fields: signal<DotCMSContentTypeField[]>([]),
                    addField: jest.fn(),
                    setFields: jest.fn(),
                    clearFields: jest.fn()
                } satisfies DotFieldFilterHost
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

    /** The rendered chip ids, in display order. */
    const renderedChips = () =>
        Array.from(spectator.element.querySelectorAll('[data-filter-chip]')).map(
            (element) => element.getAttribute('data-filter-chip') as string
        );

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
            expect(renderedChips()[0]).toBe('sharedAssets');
        });

        it('should present its chips in the canonical order', () => {
            expect(isCanonicalChipOrder(renderedChips())).toBe(true);
        });
    });

    // ── US3 (T062–T066): the picker's bar IS the Content Drive bar, minus Workflow and New.

    describe('the chip set (FR-014)', () => {
        beforeEach(() => setup(buildAssetPickerConfig({ mode: 'image', site: SITE })));

        it('should offer exactly the opted-in chips, in canonical order', () => {
            expect(renderedChips()).toEqual([
                'sharedAssets',
                'contentType',
                'status',
                'language',
                'fieldFilters'
            ]);
        });

        it('should not offer the Workflow chip', () => {
            // A recorded exclusion, not an omission (FR-014a): choosing an asset to reference is
            // not managing where it sits in a review process.
            expect(renderedChips()).not.toContain('workflow');
        });
    });

    describe('create-content affordances (FR-008, FR-017)', () => {
        beforeEach(() => setup(buildAssetPickerConfig({ mode: 'image', site: SITE })));

        it('should offer Upload', () => {
            expect(spectator.query(byTestId('asset-picker-upload'))).toBeTruthy();
        });

        it('should offer no create-content affordance of any kind', () => {
            expect(spectator.query(byTestId('add-new-button'))).toBeNull();
            expect(spectator.query(byTestId('add-new-tooltip'))).toBeNull();
        });

        it('should never enter the selection-driven action mode', () => {
            // The picker confirms one selection through its own dialog footer; Content Drive's
            // workflow/bulk entry points have no counterpart here (FR-017).
            expect(spectator.query(byTestId('workflow-actions'))).toBeNull();
            expect(spectator.query(byTestId('action-center-button'))).toBeNull();
        });
    });

    describe('the Status control bound (FR-014d)', () => {
        /** The bound the Status chip actually received. */
        const offeredStatuses = () => spectator.query(DotStatusFilterComponent)?.$allowedOptions();

        it('should offer only Locked when the caller pinned the picker to published content', () => {
            setup(
                buildAssetPickerConfig({
                    mode: 'browse',
                    site: SITE,
                    browse: { showWorking: false }
                })
            );

            // Neither Archived nor Unpublished has a published version, so offering them would
            // force the whole query onto the working version (SC-009).
            expect(offeredStatuses()).toEqual(['LOCKED']);
        });

        it('should offer every condition when working versions are included', () => {
            setup(
                buildAssetPickerConfig({
                    mode: 'browse',
                    site: SITE,
                    browse: { showWorking: true }
                })
            );

            expect(offeredStatuses()).toBeNull();
        });

        it('should offer every condition when the caller pinned nothing', () => {
            setup(buildAssetPickerConfig({ mode: 'image', site: SITE }));

            expect(offeredStatuses()).toBeNull();
        });
    });

    describe('the "More" overflow', () => {
        beforeEach(() => setup(buildAssetPickerConfig({ mode: 'image', site: SITE })));

        it('should be present and reachable', () => {
            expect(spectator.query(byTestId('field-filter-more-button'))).toBeTruthy();
        });

        it('should be disabled until exactly one content type is selected', () => {
            // With no eligible fields to offer, it says so by being unusable rather than opening an
            // empty panel (spec Edge Cases).
            const button = spectator
                .query(byTestId('field-filter-more-button'))
                ?.querySelector('button');

            expect(button?.disabled).toBe(true);
        });

        it('should re-emit a failed field fetch instead of handling it', () => {
            const reported = jest.fn();
            spectator.output('fieldFilterError').subscribe(reported);

            spectator.triggerEventHandler('dot-field-filter-menu', 'error', {
                messageKey: 'content-drive.field-filter.more.error'
            });

            // The dialog's own toast is this host's only error channel (FR-015).
            expect(reported).toHaveBeenCalledWith({
                messageKey: 'content-drive.field-filter.more.error'
            });
        });
    });

    describe('the field-filter chips', () => {
        it('should render one chip per active field, resolved against the published metadata', () => {
            setup(buildAssetPickerConfig({ mode: 'image', site: SITE }));

            // From the COMPONENT injector: the picker provides its store per dialog instance.
            const store = spectator.inject(DotAssetPickerStore, true) as unknown as ReturnType<
                typeof createMockStore
            >;
            store.userSearchableFields.set([
                { variable: 'body', name: 'Body', fieldType: 'Text' } as DotCMSContentTypeField
            ]);
            store.userSearchableActive.set(['body', 'unknownField']);
            spectator.detectChanges();

            // `unknownField` has no metadata yet, so it renders nothing — the same rule Content
            // Drive applies, which is what keeps a restored value from minting a blank chip.
            expect(spectator.query(byTestId('field-filter-chip-body'))).toBeTruthy();
            expect(spectator.query(byTestId('field-filter-chip-unknownField'))).toBeNull();
        });
    });

    // ── T067 (FR-018, SC-007): keyboard parity with Content Drive's toolbar.

    describe('keyboard operation', () => {
        beforeEach(() => setup(buildAssetPickerConfig({ mode: 'image', site: SITE })));

        it('should put every filter control in the tab order, in the order displayed', () => {
            const chips = Array.from(
                spectator.element.querySelectorAll<HTMLElement>('[data-filter-chip]')
            );

            // Tab follows DOM order, and the chips are rendered in canonical order, so reaching
            // them in visual order needs nothing but each one being focusable. A chip that opted
            // out of the tab order (`tabindex="-1"`) would be unreachable without a pointer.
            expect(chips.length).toBeGreaterThan(0);
            chips.forEach((chip) => {
                const focusable = chip.matches('[tabindex]')
                    ? chip
                    : chip.querySelector<HTMLElement>('[tabindex], button');

                expect(focusable?.getAttribute('tabindex')).not.toBe('-1');
                expect(focusable).toBeTruthy();
            });
        });

        it('should open a filter panel from the keyboard alone', () => {
            const chip = spectator.query(byTestId('content-type-filter-chip')) as HTMLElement;

            chip.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
            spectator.detectChanges();

            // The panel is body-appended, so it is queried from the root.
            expect(
                spectator.query('.p-popover, [data-pc-name="popover"]', { root: true })
            ).toBeTruthy();
        });
    });
});
