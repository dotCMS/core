import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { of } from 'rxjs';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { createFakeContentlet, MockDotMessageService, mockLocales } from '@dotcms/utils-testing';

import { SearchComponent } from './components/search/search.compoment';
import { DotSelectExistingContentComponent } from './dot-select-existing-content.component';
import { ExistingContentStore } from './store/existing-content.store';

import { Column } from '../../models/column.model';
import { RelationshipFieldService } from '../../services/relationship-field.service';

const mockColumns: Column[] = [
    { field: 'title', header: 'Title' },
    { field: 'modDate', header: 'Mod Date' }
];

const mockData: DotCMSContentlet[] = [
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
];

describe('DotSelectExistingContentComponent', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;
    let store: InstanceType<typeof ExistingContentStore>;

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.apply.one.entry': 'Apply 1 entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} entries'
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
        componentProviders: [ExistingContentStore],
        providers: [
            mockProvider(RelationshipFieldService, {
                getColumnsAndContent: jest.fn(() => of([mockColumns, mockData]))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
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
                    currentItemsIds: []
                });
            });
        });
    });

    describe('Selected Items State', () => {
        it('should initialize selectedItems with store initialization', () => {
            const mockItems = [
                createFakeContentlet({ inode: '1' }),
                createFakeContentlet({ inode: '2' })
            ];
            jest.spyOn(store, 'initSelectedItems').mockImplementation(() => mockItems);

            // Trigger the effect
            spectator.component['store'].initSelectedItems();

            // Check if the signal was updated
            expect(spectator.component.$selectedItems()).toEqual(mockItems);
        });

        it('should update store selectedItems when $selectedItems changes', () => {
            const mockItems = [createFakeContentlet({ inode: '1' })];
            const spy = jest.spyOn(store, 'setSelectedItems');

            spectator.component.$selectedItems.set(mockItems);

            expect(spy).toHaveBeenCalledWith(mockItems);
        });
    });

    describe('Item Selection', () => {
        it('should return true when content is in selectedContent array', () => {
            const testContent = createFakeContentlet({ inode: '1' });
            jest.spyOn(store, 'items').mockImplementation(() => [testContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(true);
        });

        it('should return false when content is not in selectedContent array', () => {
            const testContent = createFakeContentlet({ inode: '123' });
            const differentContent = createFakeContentlet({ inode: '456' });
            jest.spyOn(store, 'items').mockImplementation(() => [differentContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });

        it('should return false when selectedContent is empty', () => {
            const testContent = createFakeContentlet({ inode: '123' });
            jest.spyOn(store, 'items').mockImplementation(() => []);

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
            mockProvider(RelationshipFieldService, {
                getColumnsAndContent: jest.fn(() => of([mockColumns, mockData]))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
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
            mockProvider(RelationshipFieldService, {
                getColumnsAndContent: jest.fn(() => of([mockColumns, mockData]))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
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
