import { afterEach, beforeEach, describe, expect, it } from '@jest/globals';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotLanguagesService
} from '@dotcms/data-access';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';

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
            mockProvider(DotHttpErrorManagerService),
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
        it('should open the content type selector for "All Content Types"', () => {
            const items = spectator.component.$items();

            const allContentTypesItem = items.find(
                (item) => item.label === 'content-drive.add-new.all-content-types'
            );

            expect(allContentTypesItem).toBeTruthy();

            allContentTypesItem?.command?.({});

            expect(store.setDialog).toHaveBeenCalledWith({
                type: DIALOG_TYPE.CONTENT_TYPE_SELECTOR,
                header: 'content-drive.dialog.content-type-selector.header',
                payload: { listType: DotUVEPaletteListTypes.ALL_CONTENT_TYPES }
            });
        });

        it('should NOT render the removed "Asset" menu item', () => {
            const items = spectator.component.$items();

            const assetItem = items.find(
                (item) => item.label === 'content-drive.add-new.context-menu.asset'
            );

            expect(assetItem).toBeUndefined();
        });

        it.each([
            {
                labelKey: 'content-drive.base-type.content',
                listType: DotUVEPaletteListTypes.ALL_CONTENT
            },
            {
                labelKey: 'content-drive.base-type.widget',
                listType: DotUVEPaletteListTypes.ALL_WIDGET
            },
            {
                labelKey: 'content-drive.base-type.fileasset',
                listType: DotUVEPaletteListTypes.ALL_FILEASSET
            },
            {
                labelKey: 'content-drive.base-type.dotasset',
                listType: DotUVEPaletteListTypes.ALL_DOTASSET
            },
            {
                labelKey: 'content-drive.base-type.persona',
                listType: DotUVEPaletteListTypes.ALL_PERSONA
            },
            {
                labelKey: 'content-drive.base-type.vanity_url',
                listType: DotUVEPaletteListTypes.ALL_VANITY_URL
            },
            {
                labelKey: 'content-drive.base-type.key_value',
                listType: DotUVEPaletteListTypes.ALL_KEY_VALUE
            },
            {
                labelKey: 'content-drive.base-type.htmlpage',
                listType: DotUVEPaletteListTypes.ALL_HTMLPAGE
            }
        ])(
            'should open the content type selector for base type "$labelKey" with listType "$listType"',
            ({ labelKey, listType }) => {
                const items = spectator.component.$items();

                const baseTypeItem = items.find((item) => item.label === labelKey);

                expect(baseTypeItem).toBeTruthy();

                baseTypeItem?.command?.({});

                expect(store.setDialog).toHaveBeenCalledWith({
                    type: DIALOG_TYPE.CONTENT_TYPE_SELECTOR,
                    header: 'content-drive.dialog.content-type-selector.header',
                    payload: { listType }
                });
            }
        );

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

    describe('Upload button', () => {
        it('should render the upload button', () => {
            expect(spectator.query(byTestId('upload-asset-button'))).toBeTruthy();
        });

        it('should emit addNewDotAsset when the upload button is clicked', () => {
            const emitSpy = jest.fn();
            spectator.component.$addNewDotAsset.subscribe(emitSpy);

            const uploadButton = spectator
                .query(byTestId('upload-asset-button'))
                ?.querySelector('button');
            spectator.click(uploadButton as HTMLElement);

            expect(emitSpy).toHaveBeenCalledTimes(1);
        });
    });
});
