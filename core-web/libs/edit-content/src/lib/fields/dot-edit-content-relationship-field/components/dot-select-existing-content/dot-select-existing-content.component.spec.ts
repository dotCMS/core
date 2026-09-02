import { patchState } from '@ngrx/signals';
import { Spectator, createComponentFactory, mockProvider } from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { createFakeContentlet, MockDotMessageService, mockLocales } from '@dotcms/utils-testing';

import { SearchComponent } from './components/search/search.component';
import { DotSelectExistingContentComponent } from './dot-select-existing-content.component';
import {
    RelationshipFieldSearchResponse,
    ExistingContentService
} from './store/existing-content.service';
import { ExistingContentStore } from './store/existing-content.store';

import { Column } from '../../models/column.model';

const mockColumns: Column[] = [
    { field: 'title', header: 'Title' },
    { field: 'modDate', header: 'Mod Date' }
];

const mockData: RelationshipFieldSearchResponse = {
    contentlets: [
        createFakeContentlet({
            title: 'Content 1',
            inode: '1',
            identifier: 'id-1',
            languageId: mockLocales[0].id
        }),
        createFakeContentlet({
            title: 'Content 2',
            inode: '2',
            identifier: 'id-2',
            languageId: mockLocales[1].id
        }),
        createFakeContentlet({
            title: 'Content 3',
            inode: '3',
            identifier: 'id-3',
            languageId: mockLocales[0].id
        })
    ],
    totalResults: 3
};

describe('DotSelectExistingContentComponent', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;
    let store: InstanceType<typeof ExistingContentStore>;

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.apply.one.entry': 'Apply 1 entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} entries',
        'dot.file.relationship.dialog.show.selected.items': 'Show Selected Items'
    });

    const mockDialogConfig = {
        data: {
            contentTypeId: 'test-content-type-id',
            selectionMode: 'multiple',
            currentItemsIds: []
        }
    };

    const createComponent = createComponentFactory({
        component: DotSelectExistingContentComponent,
        providers: [
            ExistingContentStore,
            mockProvider(ExistingContentService, {
                getColumnsAndContent: jest.fn().mockReturnValue(of([mockColumns, mockData]))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogConfig, useValue: mockDialogConfig }
        ],
        declarations: [MockComponent(SearchComponent)],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(ExistingContentStore);
        spectator.detectChanges();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
        expect(store).toBeTruthy();
    });

    describe('Initialization', () => {
        describe('with valid configuration', () => {
            it('should initialize with required configuration', () => {
                const spy = jest.spyOn(store, 'initLoad');
                spectator.component.ngOnInit();
                expect(spy).toHaveBeenCalledWith({
                    contentTypeId: 'test-content-type-id',
                    selectionMode: 'multiple',
                    selectedItemsIds: []
                });
            });
        });
    });

    describe('Selected Items State', () => {
        it('should initialize selectedItems with store initialization', () => {
            spectator.flushEffects();
            const initSelectedItems = store.selectionItems();
            expect(spectator.component.$selectionItems()).toEqual(initSelectedItems);
        });

        it('should update store selectedItems when $selectedItems changes', () => {
            const mockItems = [createFakeContentlet({ inode: '1' })];

            spectator.component.$selectionItems.set(mockItems);
            spectator.detectChanges();

            expect(store.selectionItems()).toEqual(mockItems);
        });
    });

    describe('Select All excludes constrained items', () => {
        const item1 = createFakeContentlet({ inode: '1', identifier: 'id-1' });
        const item2 = createFakeContentlet({ inode: '2', identifier: 'id-2' });
        const item3 = createFakeContentlet({ inode: '3', identifier: 'id-3' });

        const markConstrained = (ids: string[]) => {
            patchState(store, { constrainedIdentifiers: new Set(ids) });
        };

        beforeEach(() => {
            patchState(store, { searchData: [item1, item2, item3] });
            markConstrained([]);
            spectator.detectChanges();
        });

        it('selects only non-constrained items when checked', () => {
            markConstrained(['id-2']);

            spectator.component.onSelectAllChange({
                originalEvent: new Event('click'),
                checked: true
            });

            const selection = spectator.component.$selectionItems() as ReturnType<
                typeof createFakeContentlet
            >[];
            expect(selection.map((i) => i.identifier)).toEqual(['id-1', 'id-3']);
        });

        it('leaves constrained items unselected when Select All is checked', () => {
            markConstrained(['id-2']);

            spectator.component.onSelectAllChange({
                originalEvent: new Event('click'),
                checked: true
            });

            const selection = spectator.component.$selectionItems() as ReturnType<
                typeof createFakeContentlet
            >[];
            expect(selection.some((i) => i.identifier === 'id-2')).toBe(false);
        });

        it('selects zero items when all rows are constrained', () => {
            markConstrained(['id-1', 'id-2', 'id-3']);

            spectator.component.onSelectAllChange({
                originalEvent: new Event('click'),
                checked: true
            });

            expect(spectator.component.$selectionItems()).toEqual([]);
        });

        it('selects every item when no row is constrained', () => {
            markConstrained([]);

            spectator.component.onSelectAllChange({
                originalEvent: new Event('click'),
                checked: true
            });

            const selection = spectator.component.$selectionItems() as ReturnType<
                typeof createFakeContentlet
            >[];
            expect(selection.map((i) => i.identifier).sort()).toEqual(['id-1', 'id-2', 'id-3']);
        });

        it('clears the visible selection when Select All is unchecked', () => {
            spectator.component.$selectionItems.set([item1, item2]);

            spectator.component.onSelectAllChange({
                originalEvent: new Event('click'),
                checked: false
            });

            expect(spectator.component.$selectionItems()).toEqual([]);
        });

        it('preserves selections from other pages when Select All is checked', () => {
            const offPageItem = createFakeContentlet({ inode: '99', identifier: 'id-99' });
            spectator.component.$selectionItems.set([offPageItem]);
            spectator.detectChanges();

            spectator.component.onSelectAllChange({
                originalEvent: new Event('click'),
                checked: true
            });

            const selection = spectator.component.$selectionItems() as ReturnType<
                typeof createFakeContentlet
            >[];
            expect(selection.map((i) => i.identifier).sort()).toEqual([
                'id-1',
                'id-2',
                'id-3',
                'id-99'
            ]);
        });

        it('preserves selections from other pages when Select All is unchecked', () => {
            const offPageItem = createFakeContentlet({ inode: '99', identifier: 'id-99' });
            spectator.component.$selectionItems.set([item1, item2, offPageItem]);
            spectator.detectChanges();

            spectator.component.onSelectAllChange({
                originalEvent: new Event('click'),
                checked: false
            });

            const selection = spectator.component.$selectionItems() as ReturnType<
                typeof createFakeContentlet
            >[];
            expect(selection.map((i) => i.identifier)).toEqual(['id-99']);
        });

        it('reports $selectAll as false when no selectable items exist', () => {
            markConstrained(['id-1', 'id-2', 'id-3']);

            expect(spectator.component.$selectAll()).toBe(false);
        });

        it('reports $isPartiallySelected when some (not all) selectable items are selected', () => {
            spectator.component.$selectionItems.set([item1]);
            spectator.detectChanges();

            expect(spectator.component.$isPartiallySelected()).toBe(true);
            expect(spectator.component.$selectAll()).toBe(false);
        });

        it('reports $isPartiallySelected false when every selectable item is selected', () => {
            spectator.component.$selectionItems.set([item1, item2, item3]);
            spectator.detectChanges();

            expect(spectator.component.$isPartiallySelected()).toBe(false);
        });

        it('reports $selectAll as true when every selectable item is selected', () => {
            markConstrained(['id-2']);
            spectator.component.$selectionItems.set([item1, item3]);
            spectator.detectChanges();

            expect(spectator.component.$selectAll()).toBe(true);
        });

        it('forces $selectAll to false in selected-view mode to avoid wiping the selection', () => {
            spectator.component.$selectionItems.set([item1, item2, item3]);
            patchState(store, { viewMode: 'selected' });
            spectator.detectChanges();

            expect(store.isSelectedView()).toBe(true);
            expect(spectator.component.$selectAll()).toBe(false);
        });
    });

    describe('Item Selection', () => {
        it('should return true when content is in selectedContent array', () => {
            const testContent = createFakeContentlet({ inode: '1' });
            store.setSelectionItems([testContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(true);
        });

        it('should return false when content is not in selectedContent array', () => {
            const testContent = createFakeContentlet({ inode: '123' });
            const differentContent = createFakeContentlet({ inode: '456' });
            store.setSelectionItems([differentContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });

        it('should return false when selectedContent is empty', () => {
            const testContent = createFakeContentlet({ inode: '123' });
            store.setSelectionItems([]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });
    });
});

describe('DotSelectExistingContentComponent when selectionMode is missing', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.apply.one.entry': 'Apply 1 entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} entries'
    });

    const createComponentWithInvalidConfig = createComponentFactory({
        component: DotSelectExistingContentComponent,
        componentProviders: [ExistingContentStore],
        providers: [
            mockProvider(ExistingContentService, {
                getColumnsAndContent: jest.fn().mockReturnValue(of([mockColumns, mockData]))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: DynamicDialogConfig,
                useValue: { data: { contentTypeId: 'test-id' } }
            }
        ]
    });

    it('should throw error when selectionMode is missing', () => {
        expect(() => {
            spectator = createComponentWithInvalidConfig();
            spectator.detectChanges();
        }).toThrow('Selection mode is required');
    });
});

describe('DotSelectExistingContentComponent when contentTypeId is missing', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.apply.one.entry': 'Apply 1 entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} entries'
    });

    const createComponentWithInvalidConfig = createComponentFactory({
        component: DotSelectExistingContentComponent,
        componentProviders: [ExistingContentStore],
        providers: [
            mockProvider(ExistingContentService, {
                getColumnsAndContent: jest.fn().mockReturnValue(of([mockColumns, mockData]))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: DynamicDialogConfig,
                useValue: { data: { selectionMode: 'multiple' } }
            }
        ]
    });

    it('should throw error when contentTypeId is missing', () => {
        expect(() => {
            spectator = createComponentWithInvalidConfig();
            spectator.detectChanges();
        }).toThrow('Content type id is required');
    });
});
