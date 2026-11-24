import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { MockInstance, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotContentTypeService,
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeRelationshipField } from '@dotcms/utils-testing';

import { DotRelationshipFieldComponent } from './components/dot-relationship-field/dot-relationship-field.component';
import { DotEditContentRelationshipFieldComponent } from './dot-edit-content-relationship-field.component';
import { RelationshipFieldStore } from './store/relationship-field.store';

import { DotEditContentService } from '../../services/dot-edit-content.service';
import { EDIT_CONTENT_HOST } from '../../services/host/edit-content-host.model';
import { DotEditContentStore } from '../../store/edit-content.store';
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

/** Flipped by tests that need the required error to surface (#37464 gates it on save). */
const submitAttempted = signal(false);

/** The host's `openContent`: the field asks it to create related content (#38303). */
const openContent = vi.fn();

describe('DotEditContentRelationshipFieldComponent', () => {
    beforeEach(() => {
        submitAttempted.set(false);
        openContent.mockClear();
    });

    let spectator: SpectatorHost<DotEditContentRelationshipFieldComponent, MockFormComponent>;
    let store: InstanceType<typeof RelationshipFieldStore>;
    let dialogService: DialogService;

    const createHost = createHostFactory({
        component: DotEditContentRelationshipFieldComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false,
        componentMocks: [DotCardFieldComponent, DotCardFieldContentComponent],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, {
                // The create-new title is now i18n: `get('contenttypes.content.create.contenttype', name)`.
                // Resolve that key to keep the header assertion meaningful; other keys stay generic.
                get: vi.fn((key: string, ...args: string[]) =>
                    key === 'contenttypes.content.create.contenttype'
                        ? `Create ${args[0] ?? ''}`
                        : 'Mock Message'
                )
            }),
            mockProvider(DotContentTypeService, {
                getContentType: vi.fn().mockReturnValue(of(mockContentType))
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: vi.fn()
            }),
            mockProvider(DotCurrentUserService),
            mockProvider(DotEditContentStore, {
                // BaseWrapperField gates required errors on this.
                hasAttemptedSubmit: submitAttempted,
                contentType: vi.fn().mockReturnValue(null),
                currentLocale: vi.fn().mockReturnValue(null),
                isCopyingLocale: vi.fn().mockReturnValue(false),
                contentlet: vi.fn().mockReturnValue(null)
            }),
            mockProvider(DotEditContentService, {
                getContentById: vi.fn().mockReturnValue(of({}))
            }),
            DialogService,
            {
                provide: EDIT_CONTENT_HOST,
                useValue: {
                    inPlaceNavigation: false,
                    setContentTitle: vi.fn(),
                    addBreadcrumb: vi.fn(),
                    goToSavedContent: vi.fn(),
                    leaveDeletedContent: vi.fn(),
                    goToRestoredVersion: vi.fn(),
                    goToRelatedContent: vi.fn(),
                    goToCrumb: vi.fn(),
                    openContent
                }
            }
        ]
    });

    describe('Behavior', () => {
        beforeEach(() => {
            spectator = createHost(
                `<form [formGroup]="formGroup">
                <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" />
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

            const fieldComponent = spectator.query(DotRelationshipFieldComponent);
            store = fieldComponent.store;
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
                const control = spectator.hostComponent.formGroup.get(
                    RELATIONSHIP_FIELD_MOCK.variable
                );
                control.disable();
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                expect(fieldComponent.$isDisabled()).toBe(true);
            });

            it('should not delete item when disabled', () => {
                const deleteSpy = vi.spyOn(store, 'deleteItem');
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.deleteItem('1');
                expect(deleteSpy).not.toHaveBeenCalled();
            });

            it('should not reorder items when disabled', () => {
                const reorderDataSpy = vi.spyOn(store, 'reorderData');
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.onRowReorder({ dragIndex: 0, dropIndex: 1 });
                expect(reorderDataSpy).not.toHaveBeenCalled();
            });

            it('should not show existing content dialog when disabled', () => {
                const openSpy = vi.spyOn(dialogService, 'open');
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.showExistingContentDialog();
                expect(openSpy).not.toHaveBeenCalled();
            });

            it('should not ask the host to create content when disabled', () => {
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.showCreateNewContentDialog();
                expect(openContent).not.toHaveBeenCalled();
            });
        });

        describe('Item Management', () => {
            beforeEach(() => {
                spectator.detectChanges();
            });

            it('should delete item when not disabled', () => {
                const deleteSpy = vi.spyOn(store, 'deleteItem');
                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.deleteItem('1');
                expect(deleteSpy).toHaveBeenCalledWith('1');
            });

            it('should reorder items when not disabled', () => {
                const reorderDataSpy = vi.spyOn(store, 'reorderData');
                const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                fieldComponent.onRowReorder({ dragIndex: 0, dropIndex: 1 });
                expect(reorderDataSpy).toHaveBeenCalledWith(store.data());
            });

            it('should not reorder items with invalid indices', () => {
                const reorderDataSpy = vi.spyOn(store, 'reorderData');

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                fieldComponent.onRowReorder({ dragIndex: null, dropIndex: 1 });
                expect(reorderDataSpy).not.toHaveBeenCalled();

                fieldComponent.onRowReorder({ dragIndex: 0, dropIndex: null });
                expect(reorderDataSpy).not.toHaveBeenCalled();
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
                const openSpy = vi.spyOn(dialogService, 'open');
                const mockDialogRef = {
                    onClose: of([]),
                    close: vi.fn()
                };
                openSpy.mockReturnValue(mockDialogRef as unknown as DynamicDialogRef);
                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                expect(() => {
                    fieldComponent.showExistingContentDialog();
                }).not.toThrow();
            });

            it('should handle dialog close with selection', () => {
                const newContentlet = createFakeContentlet({ title: 'New Content', inode: '3' });
                const mockDialogRef = {
                    onClose: of([newContentlet]),
                    close: vi.fn()
                };

                vi.spyOn(dialogService, 'open').mockReturnValue(
                    mockDialogRef as unknown as DynamicDialogRef
                );
                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                expect(() => {
                    fieldComponent.showExistingContentDialog();
                    spectator.flushEffects();
                }).not.toThrow();
            });

            it('should handle dialog close with no selection', () => {
                const mockDialogRef = {
                    onClose: of(null),
                    close: vi.fn()
                };

                vi.spyOn(dialogService, 'open').mockReturnValue(
                    mockDialogRef as unknown as DynamicDialogRef
                );
                const setDataSpy = vi.spyOn(store, 'setData');

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.showExistingContentDialog();
                spectator.flushEffects();

                expect(setDataSpy).not.toHaveBeenCalled();
            });
        });

        // The field asks its host to create related content; the host opens the panel (#38303).
        describe('Create New Content', () => {
            let openSpy: MockInstance;

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

                openSpy = vi.spyOn(dialogService, 'open');
            });

            it('should ask the host to create the related type, joined to this relationship', () => {
                const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                expect(fieldComponent.$isDisabled()).toBe(false);
                expect(store.contentType()).toEqual(mockContentType);

                fieldComponent?.showCreateNewContentDialog();
                spectator.flushEffects();

                expect(openContent).toHaveBeenCalledTimes(1);
                expect(openContent).toHaveBeenCalledWith(
                    expect.objectContaining({
                        request: expect.objectContaining({
                            mode: 'new',
                            contentType: 'testContentType',
                            title: 'Create Test Content Type',
                            relationship: expect.objectContaining({
                                relationshipName: 'relationshipField',
                                isParent: true
                            })
                        }),
                        source: 'relationship'
                    })
                );
                expect(openSpy).not.toHaveBeenCalled();
            });

            it('should not ask the host when disabled', () => {
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.showCreateNewContentDialog();
                spectator.flushEffects();

                expect(openContent).not.toHaveBeenCalled();
            });

            it('should not ask the host when content type is not available', () => {
                // Mock the store's contentType method to return null
                vi.spyOn(store, 'contentType').mockReturnValue(null);

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);

                fieldComponent.showCreateNewContentDialog();
                spectator.flushEffects();

                expect(openContent).not.toHaveBeenCalled();
            });

            it('should add the created content to the relationship on its first save', () => {
                const newContentlet = createFakeContentlet({
                    title: 'New Content',
                    inode: '3',
                    identifier: 'id-3'
                });
                const setDataSpy = vi.spyOn(store, 'setData');

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                fieldComponent?.showCreateNewContentDialog();
                spectator.flushEffects();

                const [{ onSaved }] = openContent.mock.calls[0];
                onSaved(newContentlet);

                expect(setDataSpy).toHaveBeenCalledTimes(1);
                const callArgs = setDataSpy.mock.calls[0][0];
                expect(callArgs).toContainEqual(expect.objectContaining({ identifier: 'id-3' }));
            });
        });

        describe('Form Control Integration', () => {
            it('should implement ControlValueAccessor methods', () => {
                const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                expect(fieldComponent.writeValue).toBeDefined();
                expect(fieldComponent.registerOnChange).toBeDefined();
                expect(fieldComponent.registerOnTouched).toBeDefined();
                expect(fieldComponent.setDisabledState).toBeDefined();
            });

            it('should handle writeValue with empty value', () => {
                expect(() => {
                    const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                    fieldComponent.writeValue('');
                }).not.toThrow();
            });
        });

        describe('Computed Properties', () => {
            beforeEach(() => {
                spectator.detectChanges();
            });

            it('should compute menu items correctly', () => {
                const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                const menuItems = fieldComponent.$menuItems();

                expect(menuItems).toHaveLength(2);
                expect(menuItems[0]).toHaveProperty('label');
                expect(menuItems[1]).toHaveProperty('label');
                expect(menuItems[0]).toHaveProperty('command');
                expect(menuItems[1]).toHaveProperty('command');
            });

            it('should disable menu items when component is disabled', () => {
                spectator.hostComponent.formGroup.disable();
                spectator.detectChanges();

                const fieldComponent = spectator.query(DotRelationshipFieldComponent);
                const menuItems = fieldComponent.$menuItems();

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
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" />
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

            const fieldComponent = emptySpectator.query(DotRelationshipFieldComponent);
            expect(fieldComponent.store.data()).toEqual([]);
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
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" />
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

            const fieldComponent = invalidSpectator.query(DotRelationshipFieldComponent);
            expect(fieldComponent.store.data()).toBeDefined();
        });

        it('should handle null contentlet gracefully', () => {
            const nullContentletSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" />
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

            const fieldComponent = nullContentletSpectator.query(DotRelationshipFieldComponent);
            expect(fieldComponent.store.data()).toBeDefined();
        });
    });

    // Shared by the two footer host factories below (hint + error), which are
    // otherwise identical — keeps provider changes a single-point edit.
    const FOOTER_PROVIDERS = [
        provideHttpClient(),
        provideHttpClientTesting(),
        mockProvider(DotMessageService, {
            get: vi.fn().mockReturnValue('Mock Message')
        }),
        mockProvider(DotContentTypeService, {
            getContentType: vi.fn().mockReturnValue(of(mockContentType))
        }),
        mockProvider(DotHttpErrorManagerService, {
            handle: vi.fn()
        }),
        mockProvider(DotCurrentUserService),
        mockProvider(DotEditContentStore, {
            // BaseWrapperField gates required errors on this.
            hasAttemptedSubmit: submitAttempted,
            contentType: vi.fn().mockReturnValue(null),
            currentLocale: vi.fn().mockReturnValue(null),
            isCopyingLocale: vi.fn().mockReturnValue(false),
            contentlet: vi.fn().mockReturnValue(null)
        }),
        mockProvider(DotEditContentService, {
            getContentById: vi.fn().mockReturnValue(of({}))
        }),
        DialogService
    ];

    describe('Footer hint rendering', () => {
        // Uses a dedicated host that does NOT mock the card-field components so
        // their <ng-content> projects, allowing the footer hint to render for real.
        const HINTED_FIELD_MOCK = createFakeRelationshipField({
            relationships: {
                cardinality: 0,
                isParentField: true,
                velocityVar: 'AllTypes'
            },
            variable: 'relationshipField',
            hint: 'This is the relationship hint'
        });

        const createFooterHost = createHostFactory({
            component: DotEditContentRelationshipFieldComponent,
            host: MockFormComponent,
            imports: [ReactiveFormsModule],
            detectChanges: false,
            providers: FOOTER_PROVIDERS
        });

        let footerSpectator: SpectatorHost<
            DotEditContentRelationshipFieldComponent,
            MockFormComponent
        >;

        beforeEach(() => {
            footerSpectator = createFooterHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [HINTED_FIELD_MOCK.variable]: new FormControl()
                        }),
                        field: HINTED_FIELD_MOCK,
                        contentlet: mockContentlet
                    }
                }
            );
            footerSpectator.detectChanges();
            footerSpectator.flushEffects();
        });

        it('should render the hint inside the card field footer, outside the table', () => {
            const hintElement = footerSpectator.query(
                byTestId(`hint-${HINTED_FIELD_MOCK.variable}`)
            );
            expect(hintElement).toBeTruthy();
            expect(hintElement.textContent.trim()).toBe(HINTED_FIELD_MOCK.hint);
        });

        it('should not render the hint inside the relationship table', () => {
            const table = footerSpectator.query(byTestId('relationship-field-table'));
            const hintInsideTable = table?.querySelector(
                `[data-testid="hint-${HINTED_FIELD_MOCK.variable}"]`
            );
            expect(hintInsideTable).toBeFalsy();
        });
    });

    describe('Footer error rendering', () => {
        // Dedicated host (own factory) so the required-error branch of the footer can be
        // exercised: with a required field whose control is invalid AND touched, the
        // @if(fieldHasError) branch wins and the mutually-exclusive hint branch is skipped.
        const REQUIRED_HINTED_FIELD_MOCK = createFakeRelationshipField({
            relationships: {
                cardinality: 0,
                isParentField: true,
                velocityVar: 'AllTypes'
            },
            variable: 'requiredHintedField',
            hint: 'This is the relationship hint',
            required: true
        });

        const createErrorHost = createHostFactory({
            component: DotEditContentRelationshipFieldComponent,
            host: MockFormComponent,
            imports: [ReactiveFormsModule],
            detectChanges: false,
            providers: FOOTER_PROVIDERS
        });

        it('should render the required error AND keep the hint once a save is attempted (#37464)', () => {
            const control = new FormControl(null, Validators.required);
            const errorSpectator = createErrorHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [REQUIRED_HINTED_FIELD_MOCK.variable]: control
                        }),
                        field: REQUIRED_HINTED_FIELD_MOCK,
                        contentlet: mockContentlet
                    }
                }
            );

            control.markAsTouched();
            control.updateValueAndValidity();
            submitAttempted.set(true);
            errorSpectator.detectChanges();
            errorSpectator.flushEffects();
            errorSpectator.detectChanges();

            // Error and hint are no longer mutually exclusive: the hint explains how to satisfy
            // the requirement, which is exactly what the author needs while the field is in error.
            expect(errorSpectator.query('.p-field-error')).toBeTruthy();
            expect(
                errorSpectator.query(byTestId(`hint-${REQUIRED_HINTED_FIELD_MOCK.variable}`))
            ).toBeTruthy();
        });
    });

    describe('Required-empty field touched state', () => {
        // Regression: a required, empty relationship field must not mark its control
        // touched on first render, otherwise the "mandatory" error shows before the
        // user interacts with the field.
        it('should not mark the control as touched on init', () => {
            const requiredField = createFakeRelationshipField({
                relationships: {
                    cardinality: 0,
                    isParentField: true,
                    velocityVar: 'AllTypes'
                },
                variable: 'requiredRelationshipField',
                required: true
            });

            const control = new FormControl(null, Validators.required);
            const requiredSpectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-edit-content-relationship-field [field]="field" [contentlet]="contentlet" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [requiredField.variable]: control
                        }),
                        field: requiredField,
                        contentlet: createFakeContentlet({ [requiredField.variable]: [] })
                    }
                }
            );

            requiredSpectator.detectChanges();
            requiredSpectator.flushEffects();

            // The control is invalid (required + empty) but must remain untouched,
            // so BaseWrapperField.$hasError (invalid && touched) stays false on init.
            expect(control.invalid).toBe(true);
            expect(control.touched).toBe(false);
        });
    });
});
