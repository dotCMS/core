import { it, describe, expect, beforeEach, afterEach } from '@jest/globals';
import { Spectator, SpyObject, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';

import { DotContentTypeService, DotLanguagesService } from '@dotcms/data-access';

import { DotContentDriveToolbarComponent } from './dot-content-drive-toolbar.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { MOCK_BASE_TYPES, MOCK_CONTENT_TYPES } from '../../shared/mocks';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotContentDriveToolbarComponent', () => {
    let spectator: Spectator<DotContentDriveToolbarComponent>;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveToolbarComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                // Tree collapsed at start to render the toggle button on toolbar
                isTreeExpanded: jest.fn().mockReturnValue(false),
                setIsTreeExpanded: jest.fn(),
                getFilterValue: jest.fn().mockReturnValue(undefined),
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                filters: jest.fn().mockReturnValue({}),
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
    });

    it('should render toolbar container', () => {
        const toolbar = spectator.query('.dot-content-drive-toolbar');
        expect(toolbar).toBeTruthy();
    });

    it('should render the tree toggler', () => {
        const toggler = spectator.query('[data-testid="tree-toggler"]');
        expect(toggler).toBeTruthy();
    });

    it('should render the Add New button', () => {
        const button = spectator.query('[data-testid="add-new-button"]');
        expect(button).toBeTruthy();
    });

    it('should render start and end groups', () => {
        expect(spectator.query('.p-toolbar-group-top')).toBeTruthy();
        expect(spectator.query('.p-toolbar-group-bottom')).toBeTruthy();
    });

    it('should render the content type field', () => {
        const field = spectator.query('[data-testid="content-type-field"]');
        expect(field).toBeTruthy();
    });

    it('should render the search input', () => {
        const input = spectator.query('[data-testid="search-input"]');
        expect(input).toBeTruthy();
    });

    it('should render the base type selector', () => {
        const selector = spectator.query('[data-testid="base-type-selector"]');
        expect(selector).toBeTruthy();
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

        it('should add the hidden class to the tree toggler when tree is expanded', () => {
            store.isTreeExpanded.mockReturnValue(true);
            spectator.detectChanges();
            const toggler = spectator.debugElement.query(By.css('[data-testid="tree-toggler"]'));
            expect(toggler).toBeDefined();
            expect(toggler?.classes['sidebar-expanded']).toBe(true);
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
