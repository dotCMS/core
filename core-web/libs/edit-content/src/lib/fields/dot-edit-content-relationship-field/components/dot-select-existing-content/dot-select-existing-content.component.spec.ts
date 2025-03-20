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
    let dialogRef: DynamicDialogRef;

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
        store = spectator.inject(ExistingContentStore, true);
        dialogRef = spectator.inject(DynamicDialogRef);
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

    describe('Dialog Behavior', () => {
        it('should close dialog with selected items', () => {
            const mockItems = [
                createFakeContentlet({ inode: '1' }),
                createFakeContentlet({ inode: '2' })
            ];
            spectator.component.$selectedItems.set(mockItems);

            spectator.component.applyChanges();

            expect(dialogRef.close).toHaveBeenCalledWith(mockItems);
        });

        it('should close dialog with empty array when no items selected', () => {
            spectator.component.$selectedItems.set([]);

            spectator.component.applyChanges();

            expect(dialogRef.close).toHaveBeenCalledWith([]);
        });

        it('should close dialog when cancel button is clicked', () => {
            spectator.component.closeDialog();

            expect(dialogRef.close).toHaveBeenCalledWith();
        });
    });

    describe('Selected Items State', () => {
        it('should disable apply button when no items are selected', () => {
            spectator.component.$selectedItems.set([]);
            expect(spectator.component.$items().length).toBe(0);
        });

        it('should enable apply button when items are selected', () => {
            const mockContent = [createFakeContentlet({ inode: '1' })];
            spectator.component.$selectedItems.set(mockContent);
            expect(spectator.component.$items().length).toBe(1);
        });

        it('should handle single item selection', () => {
            const singleItem = createFakeContentlet({ inode: '1' });
            spectator.component.$selectedItems.set(singleItem);
            expect(spectator.component.$items().length).toBe(1);
            expect(spectator.component.$items()[0]).toEqual(singleItem);
        });

        it('should handle multiple items selection', () => {
            const multipleItems = [
                createFakeContentlet({ inode: '1' }),
                createFakeContentlet({ inode: '2' })
            ];
            spectator.component.$selectedItems.set(multipleItems);
            expect(spectator.component.$items().length).toBe(2);
            expect(spectator.component.$items()).toEqual(multipleItems);
        });
    });

    describe('Apply Button Label', () => {
        it('should show singular label when one item is selected', () => {
            const mockContent = [createFakeContentlet({ inode: '1' })];
            spectator.component.$selectedItems.set(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 1 entry');
        });

        it('should show plural label when multiple items are selected', () => {
            const mockContent = [
                createFakeContentlet({ inode: '1' }),
                createFakeContentlet({ inode: '2' })
            ];
            spectator.component.$selectedItems.set(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 2 entries');
        });

        it('should handle empty selection', () => {
            spectator.component.$selectedItems.set([]);
            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 0 entries');
        });
    });

    describe('Item Selection', () => {
        it('should return true when content is in selectedContent array', () => {
            const testContent = createFakeContentlet({ inode: '1' });
            spectator.component.$selectedItems.set([testContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(true);
        });

        it('should return false when content is not in selectedContent array', () => {
            const testContent = createFakeContentlet({ inode: '123' });
            const differentContent = createFakeContentlet({ inode: '456' });
            spectator.component.$selectedItems.set([differentContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });

        it('should return false when selectedContent is empty', () => {
            const testContent = createFakeContentlet({ inode: '123' });
            spectator.component.$selectedItems.set([]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });

        it('should handle null selectedContent', () => {
            const testContent = createFakeContentlet({ inode: '123' });
            spectator.component.$selectedItems.set(null);

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
