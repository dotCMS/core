import { afterEach, beforeEach, describe, expect, it } from '@jest/globals';
import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';

import { DotContentTypeService, DotLanguagesService } from '@dotcms/data-access';

import { DotContentDriveToolbarComponent } from './dot-content-drive-toolbar.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { MOCK_BASE_TYPES, MOCK_CONTENT_TYPES } from '../../shared/mocks';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotContentDriveToolbarComponent', () => {
    let spectator: Spectator<DotContentDriveToolbarComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    // Real signals so the component's computeds re-run when they change
    const isTreeExpandedSignal = signal(false);
    const filtersSignal = signal<Record<string, unknown>>({});

    const createComponent = createComponentFactory({
        component: DotContentDriveToolbarComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                isTreeExpanded: isTreeExpandedSignal,
                setIsTreeExpanded: jest.fn(),
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                clearFilters: jest.fn(),
                filters: filtersSignal,
                setDialog: jest.fn(),
                selectedItems: jest.fn().mockReturnValue([])
            }),
            mockProvider(DotContentTypeService, {
                getContentTypes: jest.fn().mockReturnValue(of(MOCK_CONTENT_TYPES)),
                getContentTypesWithPagination: jest.fn().mockReturnValue(
                    of({
                        contentTypes: MOCK_CONTENT_TYPES,
                        pagination: {
                            currentPage: MOCK_CONTENT_TYPES.length,
                            totalEntries: MOCK_CONTENT_TYPES.length * 2,
                            totalPages: 1
                        }
                    })
                ),
                getAllContentTypes: jest.fn().mockReturnValue(of(MOCK_BASE_TYPES))
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of())
            }),
            provideHttpClient()
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.clearAllMocks();
        isTreeExpandedSignal.set(false);
        filtersSignal.set({});
    });

    it('should render toolbar container', () => {
        const toolbar = spectator.query('p-toolbar');
        expect(toolbar).toBeTruthy();
    });

    it('should render the tree toggler', () => {
        const toggler = spectator.query('[data-testid="tree-toggler"]');
        expect(toggler).toBeTruthy();
    });

    it('should render the Add New button', async () => {
        // Wait for animation state to settle
        await new Promise((resolve) => setTimeout(resolve, 200));
        spectator.detectChanges();

        const button = spectator.query('[data-testid="add-new-button"]');
        expect(button).toBeTruthy();
    });

    it('should render grid layout with rows', () => {
        expect(spectator.query('.row-start-1.col-start-2')).toBeTruthy();
        expect(spectator.query('.row-start-2.col-start-2')).toBeTruthy();
    });

    it('should render the content type filter', () => {
        const field = spectator.query('[data-testid="content-type-filter"]');
        expect(field).toBeTruthy();
    });

    it('should render the search input', () => {
        const input = spectator.query('[data-testid="search-input"]');
        expect(input).toBeTruthy();
    });

    it('should render the language selector', () => {
        spectator.detectChanges();
        const selector = spectator.query('[data-testid="language-field"]');
        expect(selector).toBeTruthy();
    });

    describe('Tree toggler', () => {
        it('should render the tree toggler', () => {
            const toggler = spectator.query('[data-testid="tree-toggler"]');
            expect(toggler).toBeDefined();
        });

        it('should hide the tree toggler with styles when tree is expanded', () => {
            isTreeExpandedSignal.set(true);
            spectator.detectChanges();

            const toggler = spectator.query('[data-testid="tree-toggler"]') as HTMLElement;
            expect(toggler).toBeTruthy();
            expect(toggler.style.opacity).toBe('0');
            expect(toggler.style.visibility).toBe('hidden');
            expect(toggler.style.width).toBe('0px');
        });
    });

    describe('Clear all button', () => {
        it('should not render when no filters are applied', () => {
            expect(spectator.query('[data-testid="clear-all-filters"]')).toBeNull();
        });

        it('should render when at least one filter is applied', () => {
            filtersSignal.set({ contentType: ['Blog'] });
            spectator.detectChanges();

            expect(spectator.query('[data-testid="clear-all-filters"]')).toBeTruthy();
        });

        it('should call store.clearFilters when clicked', () => {
            filtersSignal.set({ contentType: ['Blog'] });
            spectator.detectChanges();

            const clearButton = spectator
                .query('[data-testid="clear-all-filters"]')
                ?.querySelector('button');
            spectator.click(clearButton as HTMLElement);

            expect(store.clearFilters).toHaveBeenCalled();
        });
    });

    describe('$items', () => {
        it('should call setDialog for folders', () => {
            const items = spectator.component.$items();

            const foldersItem = items.find(
                (item) => item.label == 'content-drive.add-new.context-menu.folder'
            );

            foldersItem?.command({});

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.FOLDER,
                header: 'content-drive.dialog.folder.header'
            });
        });
    });
});
