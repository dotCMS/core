import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { RelationshipFieldItem } from '@dotcms/edit-content/fields/dot-edit-content-relationship-field/models/relationship.models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotSelectExistingContentComponent } from './dot-select-existing-content.component';
import { ExistingContentStore } from './store/existing-content.store';

import { RelationshipFieldService } from '../../services/relationship-field.service';

describe('DotSelectExistingContentComponent', () => {
    let spectator: Spectator<DotSelectExistingContentComponent>;
    let store: InstanceType<typeof ExistingContentStore>;
    let dialogRef: DynamicDialogRef;

    const mockRelationshipItem = (id: string): RelationshipFieldItem => ({
        id,
        title: `Test Content ${id}`,
        language: '1',
        description: 'Test description',
        step: 'Step 1',
        modDate: new Date().toISOString()
    });

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.apply.one.entry': 'Apply 1 entry',
        'dot.file.relationship.dialog.apply.entries': 'Apply {0} entries'
    });

    const mockDialogConfig = {
        data: {
            contentTypeId: 'test-content-type-id',
            selectionMode: 'multiple'
        }
    };

    const createComponent = createComponentFactory({
        component: DotSelectExistingContentComponent,
        componentProviders: [ExistingContentStore],
        providers: [
            mockProvider(RelationshipFieldService, {
                getContent: jest.fn(() => of([]))
            }),
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
            { provide: DynamicDialogConfig, useValue: mockDialogConfig }
        ],
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
        it('should initialize with required configuration', () => {
            const spy = jest.spyOn(store, 'initLoad');
            spectator.component.ngOnInit();
            expect(spy).toHaveBeenCalledWith({
                contentTypeId: 'test-content-type-id',
                selectionMode: 'multiple'
            });
        });

        it('should throw error when selectionMode is missing', () => {
            const invalidConfig = createComponentFactory({
                component: DotSelectExistingContentComponent,
                componentProviders: [ExistingContentStore],
                providers: [
                    mockProvider(RelationshipFieldService, {
                        getContent: jest.fn(() => of([]))
                    }),
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: DynamicDialogRef, useValue: { close: jest.fn() } },
                    {
                        provide: DynamicDialogConfig,
                        useValue: { data: { contentTypeId: 'test-id' } }
                    }
                ]
            });

            expect(() => {
                const component = invalidConfig();
                component.detectChanges();
            }).toThrow('Selection mode is required');
        });
    });

    describe('Dialog Behavior', () => {
        it('should close dialog with selected items', () => {
            const mockItems = [mockRelationshipItem('1'), mockRelationshipItem('2')];
            spectator.component.$selectedItems.set(mockItems);

            spectator.component.closeDialog();

            expect(dialogRef.close).toHaveBeenCalledWith(mockItems);
        });

        it('should close dialog with empty array when no items selected', () => {
            spectator.component.$selectedItems.set([]);

            spectator.component.closeDialog();

            expect(dialogRef.close).toHaveBeenCalledWith([]);
        });
    });

    describe('Selected Items State', () => {
        it('should disable apply button when no items are selected', () => {
            spectator.component.$selectedItems.set([]);
            expect(spectator.component.$items().length).toBe(0);
        });

        it('should enable apply button when items are selected', () => {
            const mockContent = [mockRelationshipItem('1')];
            spectator.component.$selectedItems.set(mockContent);
            expect(spectator.component.$items().length).toBe(1);
        });

        it('should handle single item selection', () => {
            const singleItem = mockRelationshipItem('1');
            spectator.component.$selectedItems.set(singleItem);
            expect(spectator.component.$items().length).toBe(1);
            expect(spectator.component.$items()[0]).toEqual(singleItem);
        });

        it('should handle multiple items selection', () => {
            const multipleItems = [mockRelationshipItem('1'), mockRelationshipItem('2')];
            spectator.component.$selectedItems.set(multipleItems);
            expect(spectator.component.$items().length).toBe(2);
            expect(spectator.component.$items()).toEqual(multipleItems);
        });
    });

    describe('Apply Button Label', () => {
        it('should show singular label when one item is selected', () => {
            const mockContent = [mockRelationshipItem('1')];
            spectator.component.$selectedItems.set(mockContent);

            const label = spectator.component.$applyLabel();
            expect(label).toBe('Apply 1 entry');
        });

        it('should show plural label when multiple items are selected', () => {
            const mockContent = [mockRelationshipItem('1'), mockRelationshipItem('2')];
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
            const testContent = mockRelationshipItem('1');
            spectator.component.$selectedItems.set([testContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(true);
        });

        it('should return false when content is not in selectedContent array', () => {
            const testContent = mockRelationshipItem('123');
            const differentContent = mockRelationshipItem('456');
            spectator.component.$selectedItems.set([differentContent]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });

        it('should return false when selectedContent is empty', () => {
            const testContent = mockRelationshipItem('123');
            spectator.component.$selectedItems.set([]);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });

        it('should handle null selectedContent', () => {
            const testContent = mockRelationshipItem('123');
            spectator.component.$selectedItems.set(null);

            const result = spectator.component.checkIfSelected(testContent);

            expect(result).toBe(false);
        });
    });
});
