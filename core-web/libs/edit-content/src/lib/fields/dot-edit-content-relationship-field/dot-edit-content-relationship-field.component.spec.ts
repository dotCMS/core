import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentType,
    FeaturedFlags,
    DotCMSContentlet,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import { PaginationComponent } from './components/pagination/pagination.component';
import { DotEditContentRelationshipFieldComponent } from './dot-edit-content-relationship-field.component';
import { RelationshipFieldStore } from './store/relationship-field.store';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';

const RELATIONSHIP_FIELD_MOCK = createFakeRelationshipField({
    relationships: {
        cardinality: 0, // ONE_TO_MANY
        isParentField: true,
        velocityVar: 'AllTypes'
    },
    variable: 'relationshipField'
});

const mockRelationships = [
    createFakeContentlet({
        title: 'Relationship 1',
        inode: '1',
        identifier: 'id-1'
    }),
    createFakeContentlet({
        title: 'Relationship 2',
        inode: '2',
        identifier: 'id-2'
    })
];

const mockContentlet = createFakeContentlet({
    [RELATIONSHIP_FIELD_MOCK.variable]: mockRelationships
});

const mockContentType: DotCMSContentType = {
    id: 'test-content-type',
    name: 'Test Content Type',
    variable: 'testContentType',
    baseType: 'CONTENT',
    clazz: DotCMSClazzes.SIMPLE_CONTENT_TYPE,
    defaultType: false,
    fields: [],
    fixed: false,
    folder: '',
    host: '',
    iDate: 0,
    layout: [],
    modDate: 0,
    multilingualable: false,
    nEntries: 0,
    system: false,
    versionable: false,
    workflows: [],
    metadata: {
        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true
    }
};

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: DotCMSContentTypeField;
    contentlet: DotCMSContentlet;
}

describe('DotEditContentRelationshipFieldComponent', () => {
    let spectator: SpectatorHost<DotEditContentRelationshipFieldComponent, MockFormComponent>;
    let store: InstanceType<typeof RelationshipFieldStore>;
    let dialogService: DialogService;

    const createHost = createHostFactory({
        component: DotEditContentRelationshipFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [DotCardFieldComponent, DotCardFieldContentComponent, PaginationComponent],
        providers: [
            RelationshipFieldStore,
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('Mock Message')
            }),
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn().mockReturnValue(of(mockContentType))
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: jest.fn()
            }),
            mockProvider(DotCurrentUserService),
            DialogService
        ]
    });

    describe('Behavior', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
            </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RELATIONSHIP_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: RELATIONSHIP_FIELD_MOCK,
                        contentlet: mockContentlet
                    }
                }
            );

            store = spectator.inject(RelationshipFieldStore, true);
            dialogService = spectator.inject(DialogService);
        });

        describe('Component Initialization', () => {
            it('should create the component', () => {
                spectator.detectChanges();
                expect(spectator.component).toBeTruthy();
            });

            it('should initialize with correct data', () => {
                spectator.detectChanges();
                spectator.flushEffects();

                const tableElement = spectator.query(byTestId('relationship-field-table'));
                expect(tableElement).toBeTruthy();
                expect(store.data()).toEqual(mockRelationships);
            });
        });

        describe('Disabled State Management', () => {
            beforeEach(() => {
                spectator.detectChanges();
            });

            it('should handle disabled state', () => {
                spectator.component.setDisabledState(true);
                spectator.detectChanges();

                expect(spectator.component.$isDisabled()).toBe(true);
            });

            it('should not delete item when disabled', () => {
                const deleteSpy = jest.spyOn(store, 'deleteItem');
                spectator.component.setDisabledState(true);
                spectator.detectChanges();

                spectator.component.deleteItem('1');
                expect(deleteSpy).not.toHaveBeenCalled();
            });

            it('should not reorder items when disabled', () => {
                const setDataSpy = jest.spyOn(store, 'setData');
                spectator.component.setDisabledState(true);
                spectator.detectChanges();

                spectator.component.onRowReorder({ dragIndex: 0, dropIndex: 1 });
                expect(setDataSpy).not.toHaveBeenCalled();
            });

            it('should not show existing content dialog when disabled', () => {
                const openSpy = jest.spyOn(dialogService, 'open');
                spectator.component.setDisabledState(true);
                spectator.detectChanges();

                spectator.component.showExistingContentDialog();
                expect(openSpy).not.toHaveBeenCalled();
            });

            it('should not show create content dialog when disabled', () => {
                const openSpy = jest.spyOn(dialogService, 'open');
                spectator.component.setDisabledState(true);
                spectator.detectChanges();

                spectator.component.showCreateNewContentDialog();
                expect(openSpy).not.toHaveBeenCalled();
            });
        });

        describe('Item Management', () => {
            beforeEach(() => {
                spectator.detectChanges();
            });

            it('should delete item when not disabled', () => {
                const deleteSpy = jest.spyOn(store, 'deleteItem');

                spectator.component.deleteItem('1');
                expect(deleteSpy).toHaveBeenCalledWith('1');
            });

            it('should reorder items when not disabled', () => {
                const setDataSpy = jest.spyOn(store, 'setData');

                spectator.component.onRowReorder({ dragIndex: 0, dropIndex: 1 });
                expect(setDataSpy).toHaveBeenCalledWith(store.data());
            });

            it('should not reorder items with invalid indices', () => {
                const setDataSpy = jest.spyOn(store, 'setData');

                spectator.component.onRowReorder({ dragIndex: null, dropIndex: 1 });
                expect(setDataSpy).not.toHaveBeenCalled();

                spectator.component.onRowReorder({ dragIndex: 0, dropIndex: null });
                expect(setDataSpy).not.toHaveBeenCalled();
            });
        });

        describe('Existing Content Dialog', () => {
            beforeEach(() => {
                spectator.detectChanges();
                // Initialize store with many-to-many cardinality (1) to allow multiple items
                const fieldWithCardinality = createFakeRelationshipField({
                    relationships: {
                        cardinality: 1, // MANY_TO_MANY
                        isParentField: true,
                        velocityVar: 'AllTypes'
                    },
                    variable: 'test'
                });
                store.initialize({
                    field: fieldWithCardinality,
                    contentlet: createFakeContentlet({})
                });
                store.setData([]);
            });

            it('should call showExistingContentDialog without errors', () => {
                const openSpy = jest.spyOn(dialogService, 'open');
                const mockDialogRef = {
                    onClose: of([]),
                    close: jest.fn()
                };
                openSpy.mockReturnValue(mockDialogRef as unknown as DynamicDialogRef);

                expect(() => {
                    spectator.component.showExistingContentDialog();
                }).not.toThrow();
            });

            it('should handle dialog close with selection', () => {
                const newContentlet = createFakeContentlet({ title: 'New Content', inode: '3' });
                const mockDialogRef = {
                    onClose: of([newContentlet]),
                    close: jest.fn()
                };

                jest.spyOn(dialogService, 'open').mockReturnValue(
                    mockDialogRef as unknown as DynamicDialogRef
                );

                expect(() => {
                    spectator.component.showExistingContentDialog();
                    spectator.flushEffects();
                }).not.toThrow();
            });

            it('should handle dialog close with no selection', () => {
                const mockDialogRef = {
                    onClose: of(null),
                    close: jest.fn()
                };

                jest.spyOn(dialogService, 'open').mockReturnValue(
                    mockDialogRef as unknown as DynamicDialogRef
                );
                const setDataSpy = jest.spyOn(store, 'setData');

                spectator.component.showExistingContentDialog();
                spectator.flushEffects();

                expect(setDataSpy).not.toHaveBeenCalled();
            });
        });

        describe('Create New Content Dialog', () => {
            let openSpy: jest.SpyInstance;
            let mockDialogRef: DynamicDialogRef;

            beforeEach(async () => {
                spectator.detectChanges();
                // Initialize store with many-to-many cardinality (1) to allow multiple items

                const mockField = createFakeRelationshipField({
                    relationships: {
                        cardinality: 1, // MANY_TO_MANY
                        isParentField: true,
                        velocityVar: 'test-content-type'
                    },
                    variable: 'test'
                });

                store.initialize({
                    field: mockField,
                    contentlet: createFakeContentlet({})
                });
                store.setData([]);
                // Flush effects to ensure async operations complete
                spectator.flushEffects();

                // Wait for the content type to load
                await new Promise((resolve) => setTimeout(resolve, 0));
                spectator.flushEffects();

                // Set up spy and mock dialog ref
                mockDialogRef = {
                    onClose: of(null),
                    close: jest.fn()
                } as unknown as DynamicDialogRef;

                // Set up spy on the service instance
                openSpy = jest
                    .spyOn(dialogService, 'open')
                    .mockReturnValue(mockDialogRef as unknown as DynamicDialogRef);
            });

            it('should open the new content dialog when the feature flag is enabled', () => {
                // Check initial state
                expect(spectator.component.$isDisabled()).toBe(false);
                expect(store.contentType()).toEqual(mockContentType);

                spectator.component.showCreateNewContentDialog();
                spectator.flushEffects();

                expect(openSpy).toHaveBeenCalledTimes(1);

                const callArgs = openSpy.mock.calls[0];
                expect(callArgs[0]).toBeDefined(); // Dialog component
                expect(callArgs[1]).toBeDefined(); // Dialog config
                expect(callArgs[1].modal).toBe(true);
                expect(callArgs[1].width).toBe('95%');
                expect(callArgs[1].height).toBe('95%');
                expect(callArgs[1].data.contentTypeId).toBe('test-content-type');
                expect(callArgs[1].data.relationshipInfo).toBeDefined();
                expect(callArgs[1].data.relationshipInfo.relationshipName).toBe(
                    'relationshipField'
                );
                expect(callArgs[1].data.relationshipInfo.isParent).toBe(true);
                expect(callArgs[1].header).toBe('Create Test Content Type');
            });

            it('should not open dialog when disabled', () => {
                const openSpy = jest.spyOn(dialogService, 'open');
                spectator.component.setDisabledState(true);
                spectator.detectChanges();

                spectator.component.showCreateNewContentDialog();
                spectator.flushEffects();

                expect(openSpy).not.toHaveBeenCalled();
            });

            it('should not open dialog when content type is not available', () => {
                // Mock the store's contentType method to return null
                jest.spyOn(store, 'contentType').mockReturnValue(null);

                spectator.component.showCreateNewContentDialog();
                spectator.flushEffects();

                expect(openSpy).not.toHaveBeenCalled();
            });

            it('should handle content creation callback', () => {
                const newContentlet = createFakeContentlet({ title: 'New Content', inode: '3' });
                const setDataSpy = jest.spyOn(store, 'setData');

                spectator.component.showCreateNewContentDialog();
                spectator.flushEffects();

                // Verify that the dialog was opened
                expect(openSpy).toHaveBeenCalled();

                // Get the dialog data and call the onContentSaved callback
                const dialogData = openSpy.mock.calls[0][1].data;
                dialogData.onContentSaved(newContentlet);

                expect(setDataSpy).toHaveBeenCalledTimes(1);
                const callArgs = setDataSpy.mock.calls[0][0];
                expect(callArgs).toContain(newContentlet);
            });
        });

        describe('Form Control Integration', () => {
            it('should implement ControlValueAccessor methods', () => {
                expect(spectator.component.writeValue).toBeDefined();
                expect(spectator.component.registerOnChange).toBeDefined();
                expect(spectator.component.registerOnTouched).toBeDefined();
                expect(spectator.component.setDisabledState).toBeDefined();
            });

            it('should handle writeValue with empty value', () => {
                expect(() => {
                    spectator.component.writeValue('');
                }).not.toThrow();
            });

            it('should register onChange callback', () => {
                const mockCallback = jest.fn();
                spectator.component.registerOnChange(mockCallback);

                // The callback should be stored internally
                expect(spectator.component['onChange']).toBe(mockCallback);
            });

            it('should register onTouched callback', () => {
                const mockCallback = jest.fn();
                spectator.component.registerOnTouched(mockCallback);

                // The callback should be stored internally
                expect(spectator.component['onTouched']).toBe(mockCallback);
            });
        });

        describe('Computed Properties', () => {
            beforeEach(() => {
                spectator.detectChanges();
            });

            it('should compute menu items correctly', () => {
                const menuItems = spectator.component.$menuItems();

                expect(menuItems).toHaveLength(2);
                expect(menuItems[0]).toHaveProperty('label');
                expect(menuItems[1]).toHaveProperty('label');
                expect(menuItems[0]).toHaveProperty('command');
                expect(menuItems[1]).toHaveProperty('command');
            });

            it('should disable menu items when component is disabled', () => {
                spectator.component.setDisabledState(true);
                spectator.detectChanges();

                const menuItems = spectator.component.$menuItems();

                expect(menuItems[0].disabled).toBe(true);
                expect(menuItems[1].disabled).toBe(true);
            });
        });
    });

    describe('Edge Cases', () => {
        it('should handle empty field data', () => {
            const emptyField = createFakeRelationshipField({
                relationships: {
                    cardinality: 0,
                    isParentField: true,
                    velocityVar: 'AllTypes'
                },
                variable: 'emptyField'
            });

            const emptyContentlet = createFakeContentlet({
                [emptyField.variable]: []
            });

            const emptySpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [emptyField.variable]: new FormControl()
                        }),
                        field: emptyField,
                        contentlet: emptyContentlet
                    }
                }
            );

            emptySpectator.detectChanges();
            emptySpectator.flushEffects();

            const emptyStore = emptySpectator.inject(RelationshipFieldStore, true);
            expect(emptyStore.data()).toEqual([]);
        });

        it('should handle invalid field data gracefully', () => {
            const invalidField = createFakeRelationshipField({
                relationships: null,
                variable: 'invalidField'
            });

            const invalidContentlet = createFakeContentlet({
                [invalidField.variable]: null
            });

            const invalidSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [invalidField.variable]: new FormControl()
                        }),
                        field: invalidField,
                        contentlet: invalidContentlet
                    }
                }
            );

            invalidSpectator.detectChanges();
            invalidSpectator.flushEffects();

            const invalidStore = invalidSpectator.inject(RelationshipFieldStore, true);
            expect(invalidStore.data()).toBeDefined();
        });

        it('should handle null contentlet gracefully', () => {
            const nullContentletSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" [formControlName]="field.variable" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [RELATIONSHIP_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: RELATIONSHIP_FIELD_MOCK,
                        contentlet: null
                    }
                }
            );

            nullContentletSpectator.detectChanges();
            nullContentletSpectator.flushEffects();

            const nullStore = nullContentletSpectator.inject(RelationshipFieldStore, true);
            expect(nullStore.data()).toBeDefined();
        });
    });
});
