import { describe, it, expect, beforeEach, afterEach, beforeAll, afterAll } from '@jest/globals';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';

import { Listbox } from 'primeng/listbox';
import { Popover } from 'primeng/popover';

import { DotContentTypeService, DotMessageService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentType,
    StructureTypeView
} from '@dotcms/dotcms-models';
import { DotChipFilterComponent } from '@dotcms/portlets/content-drive/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveContentTypeFilterComponent } from './dot-content-drive-content-type-filter.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

const BASE_TYPES: StructureTypeView[] = [
    { name: 'CONTENT', label: 'Content', types: [] },
    { name: 'FILEASSET', label: 'File', types: [] },
    { name: 'HTMLPAGE', label: 'Page', types: [] },
    { name: 'WIDGET', label: 'Widget', types: [] },
    { name: 'FORM', label: 'Form', types: [] }
];

const CONTENT_TYPES: DotCMSContentType[] = [
    {
        id: '1',
        name: 'Blog',
        variable: 'blog',
        baseType: 'CONTENT',
        system: false
    } as DotCMSContentType,
    {
        id: '2',
        name: 'Banner',
        variable: 'banner',
        baseType: 'CONTENT',
        system: false
    } as DotCMSContentType,
    {
        id: '3',
        name: 'Video File',
        variable: 'videoFile',
        baseType: 'FILEASSET',
        system: false
    } as DotCMSContentType,
    {
        id: '4',
        name: 'Code',
        variable: 'code',
        baseType: 'FILEASSET',
        system: false
    } as DotCMSContentType,
    {
        id: '5',
        name: 'Landing',
        variable: 'landing',
        baseType: 'HTMLPAGE',
        system: false
    } as DotCMSContentType,
    {
        id: '6',
        name: 'Form A',
        variable: 'formA',
        baseType: 'FORM',
        system: false
    } as DotCMSContentType,
    {
        id: '7',
        name: 'Sys',
        variable: 'sys',
        baseType: 'CONTENT',
        system: true
    } as DotCMSContentType
];

describe('DotContentDriveContentTypeFilterComponent', () => {
    let spectator: Spectator<DotContentDriveContentTypeFilterComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let contentTypeService: SpyObject<DotContentTypeService>;

    const filtersSnapshot = jest.fn().mockReturnValue({});

    const createComponent = createComponentFactory({
        component: DotContentDriveContentTypeFilterComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                filters: filtersSnapshot,
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn(),
                removeFilter: jest.fn()
            }),
            mockProvider(DotContentTypeService, {
                getAllContentTypes: jest.fn().mockReturnValue(of(BASE_TYPES)),
                getContentTypesWithPagination: jest.fn().mockReturnValue(
                    of({
                        contentTypes: CONTENT_TYPES,
                        pagination: {
                            currentPage: 1,
                            perPage: 10,
                            totalEntries: CONTENT_TYPES.length,
                            totalPages: 1
                        }
                    })
                )
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.type-filter.title': 'Content Types',
                    'content-drive.type-filter.all-content': 'All Content',
                    'content-drive.type-filter.all': 'All',
                    'content-drive.type-filter.column.base-type': 'Base Type',
                    'content-drive.type-filter.column.content-type': 'Content Type',
                    'content-drive.content-type-field.empty-state': 'No content types found',
                    'content-drive.chip-filter.overflow-label': '{0} and {1} more',
                    search: 'Search',
                    'dot.common.remove': 'Remove'
                })
            },
            provideHttpClient()
        ],
        detectChanges: false
    });

    /** Open the popover so the listboxes are rendered. */
    const openPopover = () => {
        const chip = spectator.fixture.debugElement.query(By.directive(DotChipFilterComponent));
        spectator.triggerEventHandler(chip, 'clicked', new MouseEvent('click'));
        spectator.detectChanges();
    };

    const findListbox = (predicate: (l: Listbox) => boolean): DebugElement =>
        spectator.fixture.debugElement
            .queryAll(By.directive(Listbox))
            .find((de) => predicate(de.componentInstance as Listbox)) as DebugElement;

    const leftListbox = () => findListbox((l) => !l.multiple);
    const rightListbox = () => findListbox((l) => l.multiple);

    const triggerFocusChange = (name: string | null) => {
        spectator.triggerEventHandler(leftListbox(), 'ngModelChange', name);
        spectator.detectChanges();
    };

    const triggerBaseTypeToggle = (name: string, checked: boolean) => {
        spectator.triggerEventHandler(`[data-testid="base-type-checkbox-${name}"]`, 'onChange', {
            checked
        });
        spectator.detectChanges();
    };

    const triggerContentTypeChange = (items: DotCMSContentType[] | null) => {
        spectator.triggerEventHandler(rightListbox(), 'ngModelChange', items);
        spectator.detectChanges();
    };

    const triggerContentTypeFilter = (filter: string) => {
        spectator.triggerEventHandler(rightListbox(), 'onFilter', { filter });
        spectator.detectChanges();
    };

    const triggerLazyLoad = (event: { first: number; last: number }) => {
        spectator.triggerEventHandler(rightListbox(), 'onLazyLoad', event);
        spectator.detectChanges();
    };

    const triggerPanelHide = () => {
        const popover = spectator.fixture.debugElement.query(By.directive(Popover));
        spectator.triggerEventHandler(popover, 'onHide', undefined);
        spectator.detectChanges();
    };

    const triggerChipRemoved = () => {
        const chip = spectator.fixture.debugElement.query(By.directive(DotChipFilterComponent));
        spectator.triggerEventHandler(chip, 'removed', undefined);
        spectator.detectChanges();
    };

    beforeAll(() => jest.useFakeTimers());
    afterAll(() => jest.useRealTimers());

    beforeEach(() => {
        filtersSnapshot.mockReset();
        filtersSnapshot.mockReturnValue({});
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        contentTypeService = spectator.inject(DotContentTypeService, true);
        // Reset implementations so per-test mockImplementation calls don't leak.
        store.getFilterValue.mockReset().mockReturnValue(undefined);
        contentTypeService.getAllContentTypes.mockReset().mockReturnValue(of(BASE_TYPES));
        contentTypeService.getContentTypesWithPagination.mockReset().mockReturnValue(
            of({
                contentTypes: CONTENT_TYPES,
                pagination: {
                    currentPage: 1,
                    perPage: 10,
                    totalEntries: CONTENT_TYPES.length,
                    totalPages: 1
                }
            })
        );
    });

    afterEach(() => {
        jest.clearAllTimers();
        jest.clearAllMocks();
    });

    describe('Initialization', () => {
        it('should load base types excluding FORM', () => {
            spectator.detectChanges();
            expect(contentTypeService.getAllContentTypes).toHaveBeenCalled();
            expect(spectator.component.$state.baseTypes().map((b) => b.name)).toEqual([
                'CONTENT',
                'FILEASSET',
                'HTMLPAGE',
                'WIDGET'
            ]);
        });

        it('should load initial content types excluding FORM and system', () => {
            spectator.detectChanges();
            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ per_page: 10 })
            );
            const visible = spectator.component.$state.contentTypes();
            expect(visible.every((ct) => ct.baseType !== DotCMSBaseTypesContentTypes.FORM)).toBe(
                true
            );
            expect(visible.every((ct) => !ct.system)).toBe(true);
        });

        it('should default focus to ALL_CONTENT', () => {
            spectator.detectChanges();
            expect(spectator.component.$focusedBaseType()).toBe('__ALL_CONTENT__');
        });

        it('should hydrate selected base types from the store', () => {
            store.getFilterValue.mockImplementation(((key: string) =>
                key === 'baseType' ? ['1', '4'] : undefined) as never);
            spectator.detectChanges();
            expect(spectator.component.$selectedBaseTypes()).toEqual(['CONTENT', 'FILEASSET']);
        });

        it('should hydrate selected content types from the store via the cache', () => {
            store.getFilterValue.mockImplementation(((key: string) =>
                key === 'contentType' ? ['blog', 'videoFile'] : undefined) as never);
            spectator.detectChanges();
            expect(
                spectator.component
                    .$selectedContentTypes()
                    .map((ct) => ct.variable)
                    .sort()
            ).toEqual(['blog', 'videoFile']);
        });
    });

    describe('Chip label rules', () => {
        beforeEach(() => spectator.detectChanges());

        it('shows "Name (All)" when a base type is selected with no narrowed content types', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            expect(spectator.component.$chipSelections()).toEqual(['Content (All)']);
        });

        it('shows multiple base types each with "(All)" suffix', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT', 'HTMLPAGE']);
            expect(spectator.component.$chipSelections()).toEqual(['Content (All)', 'Page (All)']);
        });

        it('shows specific content type names instead of "(All)" when narrowed', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0]]);
            expect(spectator.component.$chipSelections()).toEqual(['Blog']);
        });

        it('mixes "(All)" and specific names across base types', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT', 'HTMLPAGE']);
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0]]);
            expect(spectator.component.$chipSelections()).toEqual(['Blog', 'Page (All)']);
        });
    });

    describe('Cascade selection', () => {
        beforeEach(() => {
            spectator.detectChanges();
            openPopover();
        });

        it('adds the parent base type when a content type is selected (cascade up)', () => {
            triggerContentTypeChange([CONTENT_TYPES[2]]); // Video File / FILEASSET

            expect(spectator.component.$selectedBaseTypes()).toContain('FILEASSET');
            expect(store.patchFilters).toHaveBeenCalledWith(
                expect.objectContaining({ baseType: ['4'] })
            );
            expect(store.patchFilters).toHaveBeenCalledWith(
                expect.objectContaining({ contentType: ['videoFile'] })
            );
        });

        it('drops the parent base type when its last content type is unselected (cascade down)', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0]]);
            spectator.detectChanges();

            triggerContentTypeChange([]);

            expect(spectator.component.$selectedBaseTypes()).toEqual([]);
            expect(store.removeFilter).toHaveBeenCalledWith('baseType');
            expect(store.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('keeps the base type when other content types of it remain selected', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0], CONTENT_TYPES[1]]);
            spectator.detectChanges();

            triggerContentTypeChange([CONTENT_TYPES[1]]); // unselect Blog, keep Banner

            expect(spectator.component.$selectedBaseTypes()).toEqual(['CONTENT']);
        });

        it('drops content types of a base type when that base type is unselected', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0], CONTENT_TYPES[2]]);
            spectator.detectChanges();

            triggerBaseTypeToggle('CONTENT', false);

            expect(spectator.component.$selectedBaseTypes()).toEqual([]);
            expect(spectator.component.$selectedContentTypes().map((ct) => ct.variable)).toEqual([
                'videoFile'
            ]);
        });

        it('does not auto-select content types when a base type is selected alone', () => {
            triggerBaseTypeToggle('CONTENT', true);

            expect(spectator.component.$selectedBaseTypes()).toEqual(['CONTENT']);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
            expect(store.patchFilters).toHaveBeenCalledWith(
                expect.objectContaining({ baseType: ['1'] })
            );
            expect(store.removeFilter).toHaveBeenCalledWith('contentType');
        });

        it('removes the base type filter when no base types remain selected', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            spectator.detectChanges();

            triggerBaseTypeToggle('CONTENT', false);

            expect(store.removeFilter).toHaveBeenCalledWith('baseType');
        });
    });

    describe('Focus vs selection', () => {
        beforeEach(() => {
            spectator.detectChanges();
            openPopover();
        });

        it('changes focus without altering selection', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            spectator.detectChanges();

            triggerFocusChange('FILEASSET');

            expect(spectator.component.$focusedBaseType()).toBe('FILEASSET');
            expect(spectator.component.$selectedBaseTypes()).toEqual(['CONTENT']);
        });

        it('falls back to ALL_CONTENT when focus is cleared', () => {
            triggerFocusChange('FILEASSET');
            triggerFocusChange(null);
            expect(spectator.component.$focusedBaseType()).toBe('__ALL_CONTENT__');
        });

        it('refetches immediately with the focused base type as the type param', () => {
            jest.clearAllMocks();
            triggerFocusChange('FILEASSET');

            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ type: 'FILEASSET', page: 1 })
            );
        });

        it('refetches without a type param when focus is ALL_CONTENT', () => {
            triggerFocusChange('FILEASSET');
            jest.clearAllMocks();

            triggerFocusChange('__ALL_CONTENT__');

            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ type: undefined, page: 1 })
            );
        });

        it('eagerly clears the right list when focus changes', () => {
            patchState(spectator.component.$state, {
                contentTypes: CONTENT_TYPES.slice(0, 3)
            });
            // Make the service hang so we can observe the cleared state.
            contentTypeService.getContentTypesWithPagination.mockReturnValue(of() as never);

            triggerFocusChange('FILEASSET');

            expect(spectator.component.$state.contentTypes()).toEqual([]);
            expect(spectator.component.$state.loading()).toBe(true);
        });
    });

    describe('Pinned selections in the right list', () => {
        beforeEach(() => {
            spectator.detectChanges();
            openPopover();
        });

        const rightListboxOptions = () =>
            (rightListbox().componentInstance as Listbox).options as DotCMSContentType[];

        it('pins selected content types not in the fetched page to the top of the listbox', () => {
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0]]);
            patchState(spectator.component.$state, {
                contentTypes: [CONTENT_TYPES[2], CONTENT_TYPES[3]] // Video File, Code (FILEASSET)
            });
            spectator.detectChanges();

            expect(rightListboxOptions().map((ct) => ct.variable)).toEqual([
                'blog',
                'videoFile',
                'code'
            ]);
        });

        it('does not duplicate items already in the fetched page', () => {
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0]]);
            patchState(spectator.component.$state, {
                contentTypes: [CONTENT_TYPES[0], CONTENT_TYPES[1]] // Blog already in fetch
            });
            spectator.detectChanges();

            expect(rightListboxOptions().filter((ct) => ct.variable === 'blog')).toHaveLength(1);
        });

        it('removes a pinned item from the list when it is unselected', () => {
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0]]);
            patchState(spectator.component.$state, {
                contentTypes: [CONTENT_TYPES[2]] // FILEASSET only
            });
            spectator.detectChanges();
            expect(rightListboxOptions().map((ct) => ct.variable)).toEqual(['blog', 'videoFile']);

            triggerContentTypeChange([]);

            expect(rightListboxOptions().map((ct) => ct.variable)).toEqual(['videoFile']);
        });
    });

    describe('Filter input', () => {
        beforeEach(() => {
            spectator.detectChanges();
            openPopover();
        });

        it('debounces filter changes and calls the service with the latest value', () => {
            jest.clearAllMocks();
            triggerContentTypeFilter('b');
            triggerContentTypeFilter('bl');
            triggerContentTypeFilter('blog');
            jest.advanceTimersByTime(600);

            const calls = contentTypeService.getContentTypesWithPagination.mock.calls;
            const last = calls[calls.length - 1]?.[0] as { filter?: string };
            expect(last?.filter).toBe('blog');
        });

        it('resets the filter when the popover hides', () => {
            patchState(spectator.component.$state, { contentTypeFilter: 'blog' });
            triggerPanelHide();
            expect(spectator.component.$state.contentTypeFilter()).toBe('');
        });

        it('handles fetch errors gracefully', () => {
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                throwError(() => new Error('boom'))
            );
            jest.clearAllMocks();
            triggerContentTypeFilter('blog');
            jest.advanceTimersByTime(600);

            expect(spectator.component.$state.contentTypes()).toEqual([]);
            expect(spectator.component.$state.loading()).toBe(false);
        });
    });

    describe('Lazy load', () => {
        beforeEach(() => {
            spectator.detectChanges();
            openPopover();
        });

        it('loads the next page based on the last visible index', () => {
            patchState(spectator.component.$state, {
                contentTypes: CONTENT_TYPES.slice(0, 5),
                currentPage: 1,
                canLoadMore: true,
                loading: false
            });
            const next = [
                {
                    id: '8',
                    name: 'Extra',
                    variable: 'extra',
                    baseType: 'CONTENT',
                    system: false
                } as DotCMSContentType
            ];
            contentTypeService.getContentTypesWithPagination.mockReturnValue(
                of({
                    contentTypes: next,
                    pagination: { currentPage: 2, totalEntries: 6, totalPages: 2 } as never
                })
            );

            triggerLazyLoad({ first: 0, last: 10 });

            expect(contentTypeService.getContentTypesWithPagination).toHaveBeenCalledWith(
                expect.objectContaining({ page: 2, per_page: 10 })
            );
            expect(spectator.component.$state.contentTypes()).toHaveLength(6);
            expect(spectator.component.$state.currentPage()).toBe(2);
        });

        it('does not load when canLoadMore is false', () => {
            patchState(spectator.component.$state, { canLoadMore: false });
            jest.clearAllMocks();
            triggerLazyLoad({ first: 0, last: 40 });
            expect(contentTypeService.getContentTypesWithPagination).not.toHaveBeenCalled();
        });
    });

    describe('Chip integration', () => {
        beforeEach(() => spectator.detectChanges());

        it('renders the chip with the configured title', () => {
            const chip = spectator.query(byTestId('content-type-filter-chip'));
            expect(chip?.querySelector('[data-testid="chip-title"]')?.textContent?.trim()).toBe(
                'Content Types'
            );
        });

        it('toggles the popover when the chip is clicked', () => {
            const popoverDe = spectator.fixture.debugElement.query(By.directive(Popover));
            const popover = popoverDe.componentInstance as Popover;
            const toggleSpy = jest.spyOn(popover, 'toggle');

            const chipDe = spectator.fixture.debugElement.query(
                By.directive(DotChipFilterComponent)
            );
            spectator.triggerEventHandler(chipDe, 'clicked', new MouseEvent('click'));

            expect(toggleSpy).toHaveBeenCalled();
        });

        it('clears all selections and store when the chip emits removed', () => {
            spectator.component.$selectedBaseTypes.set(['CONTENT']);
            spectator.component.$selectedContentTypes.set([CONTENT_TYPES[0]]);
            spectator.detectChanges();

            triggerChipRemoved();

            expect(spectator.component.$selectedBaseTypes()).toEqual([]);
            expect(spectator.component.$selectedContentTypes()).toEqual([]);
            expect(store.removeFilter).toHaveBeenCalledWith('baseType');
            expect(store.removeFilter).toHaveBeenCalledWith('contentType');
        });
    });

    describe('Listbox configuration', () => {
        it('configures the content-type listbox for multi-select with checkbox + lazy load', () => {
            spectator.detectChanges();
            openPopover();

            const right = rightListbox().componentInstance as Listbox;

            expect(right.multiple).toBe(true);
            expect(right.checkbox).toBe(true);
            expect(right.lazy).toBe(true);
            expect(right.virtualScroll).toBe(true);
        });
    });
});
