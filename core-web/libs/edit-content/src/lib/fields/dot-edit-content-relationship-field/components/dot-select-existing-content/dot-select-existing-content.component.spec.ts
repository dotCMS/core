import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
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
