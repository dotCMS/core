import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { Component, input, signal, WritableSignal } from '@angular/core';
import { ComponentFixture, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { MessageService } from 'primeng/api';

import {
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSContentlet
} from '@dotcms/dotcms-models';
import {
    DotEditContentBinaryFieldComponent,
    DotEditContentService,
    DotFileFieldComponent,
    DotTagFieldComponent
} from '@dotcms/edit-content';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    ContentletEditData,
    CopyMode,
    DotUveContentletQuickEditComponent
} from './dot-uve-contentlet-quick-edit.component';

import { UveOptimisticSaveService } from '../../../services/uve-optimistic-save/uve-optimistic-save.service';
import { EDIT_ACTION_PAYLOAD_MOCK } from '../../../shared/mocks';
import { UVEStore } from '../../../store/dot-uve.store';
import { PageType } from '../../../store/models';

@Component({
    selector: 'dot-file-field',
    standalone: true,
    template: '',
    providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: MockDotFileFieldComponent, multi: true }]
})
class MockDotFileFieldComponent implements ControlValueAccessor {
    field = input<unknown>();
    contentlet = input<unknown>();
    hasError = input<boolean>(false);
    vertical = input<boolean>(false);
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    writeValue(_value: unknown) {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnChange(_fn: unknown) {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnTouched(_fn: unknown) {}
}

@Component({
    selector: 'dot-edit-content-binary-field',
    standalone: true,
    template: '',
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: MockDotEditContentBinaryFieldComponent,
            multi: true
        }
    ]
})
class MockDotEditContentBinaryFieldComponent implements ControlValueAccessor {
    field = input<unknown>();
    contentlet = input<unknown>();
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    writeValue(_value: unknown) {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnChange(_fn: unknown) {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnTouched(_fn: unknown) {}
}

@Component({
    selector: 'dot-tag-field',
    standalone: true,
    template: '',
    providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: MockDotTagFieldComponent, multi: true }]
})
class MockDotTagFieldComponent implements ControlValueAccessor {
    variableName = input<string>('');
    hasError = input<boolean>(false);
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    writeValue(_value: unknown) {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnChange(_fn: unknown) {}
    // eslint-disable-next-line @typescript-eslint/no-empty-function
    registerOnTouched(_fn: unknown) {}
}

const mockContentlet = {
    identifier: 'contentlet-123',
    inode: 'inode-123',
    title: 'Test Contentlet',
    contentType: 'TestType',
    baseType: 'CONTENT',
    archived: false,
    folder: 'folder-123',
    hasTitleImage: false,
    host: 'host-123',
    locked: false,
    modDate: '2024-01-01',
    sortOrder: 0,
    stInode: 'stInode-123',
    titleField: 'Test Title',
    hostName: 'demo.dotcms.com',
    languageId: 1,
    live: true,
    modUser: 'admin',
    working: true,
    owner: 'admin',
    modUserName: 'Admin User',
    titleImage: 'test',
    url: '/test-contentlet'
} as DotCMSContentlet;

/** Builds a minimal content type layout from a flat list of fields. */
function makeLayout(fields: Partial<DotCMSContentTypeField>[]): DotCMSContentTypeLayoutRow[] {
    return [
        {
            divider: {} as DotCMSContentTypeField,
            columns: [
                {
                    columnDivider: {} as DotCMSContentTypeField,
                    fields: fields as DotCMSContentTypeField[]
                }
            ]
        }
    ];
}

/** Builds a contentTypeCache record for a given variable and field list. */
function makeCache(
    variable: string,
    fields: Partial<DotCMSContentTypeField>[]
): Record<string, DotCMSContentType> {
    return {
        [variable]: { layout: makeLayout(fields) } as unknown as DotCMSContentType
    };
}

const mockDefaultField: Partial<DotCMSContentTypeField> = {
    name: 'Test Field',
    variable: 'testField',
    clazz: DotCMSClazzes.TEXT,
    required: true,
    readOnly: false,
    dataType: 'TEXT',
    fieldVariables: [],
    fieldType: 'TEXT'
};

const mockContentletEditData: ContentletEditData = {
    container: {
        identifier: 'container-123',
        uuid: 'uuid-123',
        acceptTypes: 'test',
        maxContentlets: 1
    },
    contentlet: mockContentlet
};

describe('DotUveContentletQuickEditComponent', () => {
    let spectator: Spectator<DotUveContentletQuickEditComponent>;
    let fixture: ComponentFixture<DotUveContentletQuickEditComponent>;

    /**
     * WritableSignal so tests can mutate the cache and Angular's reactivity
     * re-evaluates $fields / $isLoadingContentType without needing jest.spyOn.
     * Reset to the default in beforeEach to prevent test-to-test contamination.
     */
    const contentTypeCacheSignal: WritableSignal<Record<string, DotCMSContentType>> = signal(
        makeCache('TestType', [mockDefaultField])
    );

    const createComponent = createComponentFactory({
        component: DotUveContentletQuickEditComponent,
        imports: [NoopAnimationsModule],
        overrideComponents: [
            [
                DotUveContentletQuickEditComponent,
                {
                    remove: {
                        imports: [
                            DotFileFieldComponent,
                            DotEditContentBinaryFieldComponent,
                            DotTagFieldComponent
                        ]
                    },
                    add: {
                        imports: [
                            MockDotFileFieldComponent,
                            MockDotEditContentBinaryFieldComponent,
                            MockDotTagFieldComponent
                        ]
                    }
                }
            ]
        ],
        componentProviders: [
            mockProvider(UveOptimisticSaveService, {
                updateIframeOptimistically: jest.fn(),
                extractFromRollback: jest.fn().mockReturnValue({})
            }),
            mockProvider(DotEditContentService),
            mockProvider(DotCopyContentService),
            mockProvider(DotHttpErrorManagerService)
        ],
        providers: [
            mockProvider(UVEStore, {
                editorActiveContentlet: jest.fn().mockReturnValue(null),
                pageType: jest.fn().mockReturnValue(PageType.HEADLESS),
                addCurrentPageToHistory: jest.fn(),
                setUveStatus: jest.fn(),
                pageReload: jest.fn(),
                saveQuickEditFields: jest.fn().mockReturnValue(of({})),
                getCurrentTreeNode: jest.fn().mockReturnValue(null),
                getPageSavePayload: jest.fn().mockReturnValue(null),
                setActiveContentlet: jest.fn(),
                contentTypeCache: contentTypeCacheSignal,
                loadContentType: jest.fn()
            }),
            mockProvider(MessageService),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'message.content.saved': 'Saved',
                    'message.content.note.already.published': 'Already published',
                    'editpage.content.update.contentlet.error': 'Error updating contentlet',
                    'uve.quick-edit.empty.no-selection.title': 'Select a contentlet',
                    'uve.quick-edit.empty.no-fields.title': 'No editable fields',
                    'uve.quick-edit.empty.empty-container.title': 'Empty container',
                    'uve.quick-edit.copy-decision.confirm': 'Confirm',
                    'uve.quick-edit.copy-decision.confirm.all-pages': 'Edit All Pages',
                    'uve.quick-edit.copy-decision.confirm.this-page': 'Copy & Edit'
                })
            }
        ]
    });

    afterEach(() => jest.restoreAllMocks());

    beforeEach(fakeAsync(() => {
        // Reset cache signal to default before each test to prevent contamination
        contentTypeCacheSignal.set(makeCache('TestType', [mockDefaultField]));
        spectator = createComponent({
            props: {
                data: mockContentletEditData,
                loading: false
            }
        });
        fixture = spectator.fixture;
        flushMicrotasks();
        spectator.detectChanges();
    }));

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('form building', () => {
        it('should render the form when the content type is cached with supported fields', () => {
            expect(spectator.query('form')).toBeTruthy();
        });

        it('should include a hidden inode field when the contentlet has an inode', () => {
            const inodeInput = spectator.query(
                'input[formcontrolname="inode"]'
            ) as HTMLInputElement;
            expect(inodeInput).toBeTruthy();
            expect(inodeInput.value).toBe('inode-123');
        });

        it('should not rebuild the form when the same contentlet identifier is provided again', () => {
            const inputBefore = spectator.query('#testField');
            fixture.componentRef.setInput('data', { ...mockContentletEditData });
            spectator.detectChanges();

            // Same identifier → no rebuild, same DOM node is still present
            expect(spectator.query('#testField')).toBe(inputBefore);
        });

        it('should rebuild the form when the contentlet identifier changes', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [{ ...mockDefaultField, variable: 'rebuiltField' }])
            );

            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'different-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('#rebuiltField')).toBeTruthy();
            expect(spectator.query('#testField')).toBeFalsy();
        }));

        it('should reset currentIdentifier when data no longer has a contentType', fakeAsync(() => {
            // Fields are present initially — form is built for 'contentlet-123'
            expect(spectator.component.$contentletForm()).not.toBeNull();

            // Simulate active contentlet going null (no contentType → $fields returns [])
            fixture.componentRef.setInput('data', {
                container: undefined,
                contentlet: {} as DotCMSContentlet
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            // Form must be cleared AND currentIdentifier must be null so the form can rebuild
            expect(spectator.component.$contentletForm()).toBeNull();
            expect(spectator.component['currentIdentifier']()).toBeNull();
        }));

        it('should rebuild the form when the same contentlet is re-selected after a null period', fakeAsync(() => {
            // Phase 1: form is built for 'contentlet-123'
            expect(spectator.component.$contentletForm()).not.toBeNull();

            // Phase 2: simulate active contentlet going null (e.g. during add+save flow)
            // No contentType in data → $fields returns [] → form cleared → currentIdentifier reset
            fixture.componentRef.setInput('data', {
                container: undefined,
                contentlet: {} as DotCMSContentlet
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.component.$contentletForm()).toBeNull();

            // Phase 3: user clicks the same contentlet again — same identifier, cache still populated
            fixture.componentRef.setInput('data', mockContentletEditData);
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            // Form must be rebuilt even though the identifier did not change
            expect(spectator.component.$contentletForm()).not.toBeNull();
            expect(spectator.query('#testField')).toBeTruthy();
        }));
    });

    describe('loading state', () => {
        it('should show a spinner when the content type is not yet in cache', () => {
            contentTypeCacheSignal.set({});
            spectator.detectChanges();

            expect(spectator.query('[data-testid="loading-content-type"]')).toBeTruthy();
            expect(spectator.query('form')).toBeFalsy();
        });

        it('should block interaction on the form when loading is true', () => {
            fixture.componentRef.setInput('loading', true);
            spectator.detectChanges();

            expect(spectator.query('form')?.hasAttribute('inert')).toBe(true);
        });

        it('should restore interaction on the form when loading returns to false', () => {
            fixture.componentRef.setInput('loading', true);
            spectator.detectChanges();

            fixture.componentRef.setInput('loading', false);
            spectator.detectChanges();

            expect(spectator.query('form')?.hasAttribute('inert')).toBe(false);
        });
    });

    describe('empty states', () => {
        it('should show "Select a contentlet" when no contentlet is selected', () => {
            fixture.componentRef.setInput('data', {
                container: undefined,
                contentlet: {} as DotCMSContentlet
            });
            spectator.detectChanges();

            expect(spectator.query('form')).toBeFalsy();
            expect(spectator.query('[data-testid="empty-no-selection"]')).toBeTruthy();
            expect(spectator.query('[data-testid="empty-no-selection"] .font-bold')).toHaveText(
                'Select a contentlet'
            );
        });

        it('should show "Empty container" when a container is selected but has no contentlet', () => {
            fixture.componentRef.setInput('data', {
                container: mockContentletEditData.container,
                contentlet: {
                    contentType: 'TEMP_EMPTY_CONTENTLET_TYPE'
                } as unknown as DotCMSContentlet
            });
            spectator.detectChanges();

            expect(spectator.query('form')).toBeFalsy();
            expect(spectator.query('[data-testid="empty-container"]')).toBeTruthy();
            expect(spectator.query('[data-testid="empty-container"] .font-bold')).toHaveText(
                'Empty container'
            );
        });

        it('should not show a loading spinner for a TEMP_EMPTY_CONTENTLET_TYPE contentlet', () => {
            contentTypeCacheSignal.set({});
            fixture.componentRef.setInput('data', {
                container: mockContentletEditData.container,
                contentlet: {
                    contentType: 'TEMP_EMPTY_CONTENTLET_TYPE'
                } as unknown as DotCMSContentlet
            });
            spectator.detectChanges();

            expect(spectator.query('[data-testid="loading-content-type"]')).toBeFalsy();
            expect(spectator.query('[data-testid="empty-container"]')).toBeTruthy();
        });

        it('should not call loadContentType for a TEMP_EMPTY_CONTENTLET_TYPE contentlet', () => {
            const uveStore = spectator.inject(UVEStore);
            jest.clearAllMocks();
            fixture.componentRef.setInput('data', {
                container: mockContentletEditData.container,
                contentlet: {
                    contentType: 'TEMP_EMPTY_CONTENTLET_TYPE'
                } as unknown as DotCMSContentlet
            });
            spectator.detectChanges();

            expect(uveStore.loadContentType).not.toHaveBeenCalled();
        });

        it('should show "No editable fields" when a contentlet is selected but has no supported fields', () => {
            contentTypeCacheSignal.set(makeCache('TestType', []));
            fixture.componentRef.setInput('data', { ...mockContentletEditData });
            spectator.detectChanges();

            expect(spectator.query('form')).toBeFalsy();
            expect(spectator.query('[data-testid="empty-no-fields"]')).toBeTruthy();
            expect(spectator.query('[data-testid="empty-no-fields"] .font-bold')).toHaveText(
                'No editable fields'
            );
        });

        it('should render the "Click here to open the full editor" button in the no-fields state', () => {
            contentTypeCacheSignal.set(makeCache('TestType', []));
            fixture.componentRef.setInput('data', { ...mockContentletEditData });
            spectator.detectChanges();

            expect(spectator.query(byTestId('empty-open-full-editor-button'))).toBeTruthy();
        });

        it('should emit openFullEditor when clicking "Click here to open the full editor"', () => {
            contentTypeCacheSignal.set(makeCache('TestType', []));
            fixture.componentRef.setInput('data', { ...mockContentletEditData });
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('openFullEditor').subscribe(handler);

            const btn = spectator
                .query(byTestId('empty-open-full-editor-button'))
                ?.querySelector('button') as HTMLButtonElement;
            spectator.click(btn);

            expect(handler).toHaveBeenCalled();
        });
    });

    describe('openFullEditor output', () => {
        it('should render the "Go to full editor" button when fields are present', () => {
            expect(spectator.query(byTestId('open-full-editor-button'))).toBeTruthy();
        });

        it('should emit openFullEditor when clicking "Go to full editor"', () => {
            const handler = jest.fn();
            spectator.output('openFullEditor').subscribe(handler);

            const btn = spectator
                .query(byTestId('open-full-editor-button'))
                ?.querySelector('button') as HTMLButtonElement;
            spectator.click(btn);

            expect(handler).toHaveBeenCalled();
        });

        it('should disable the "Go to full editor" button when loading', () => {
            fixture.componentRef.setInput('loading', true);
            spectator.detectChanges();

            const btn = spectator
                .query(byTestId('open-full-editor-button'))
                ?.querySelector('button') as HTMLButtonElement;

            expect(btn.disabled).toBe(true);
        });
    });

    describe('field labels', () => {
        it('should display the field name as a label', () => {
            expect(spectator.query('label')).toHaveText('Test Field');
        });

        it('should mark required fields with the required CSS class', () => {
            expect(spectator.query('label')).toHaveClass('p-label-input-required');
        });

        it('should not mark optional fields with the required CSS class', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    { ...mockDefaultField, variable: 'optionalField', required: false }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'optional-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('label')).not.toHaveClass('p-label-input-required');
        }));
    });

    describe('TEXT field', () => {
        it('should render a text input', () => {
            expect(spectator.query('#testField')).toBeTruthy();
        });

        it('should render a readOnly TEXT field as disabled', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Read Only',
                        variable: 'readOnlyField',
                        clazz: DotCMSClazzes.TEXT,
                        required: false,
                        readOnly: true,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'TEXT'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'readonly-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect((spectator.query('#readOnlyField') as HTMLInputElement)?.disabled).toBe(true);
        }));
    });

    describe('TEXTAREA field', () => {
        it('should render a textarea', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Description',
                        variable: 'description',
                        clazz: DotCMSClazzes.TEXTAREA,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Textarea'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'textarea-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('#description')).toBeTruthy();
        }));
    });

    describe('CHECKBOX field', () => {
        it('should render a binary checkbox when there are no options', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Active',
                        variable: 'active',
                        clazz: DotCMSClazzes.CHECKBOX,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Checkbox'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'checkbox-binary-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('p-checkbox')).toBeTruthy();
        }));

        it('should render one checkbox per option when options are provided', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Colors',
                        variable: 'colors',
                        clazz: DotCMSClazzes.CHECKBOX,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Checkbox',
                        values: 'Red|red\nBlue|blue'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'checkbox-options-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.queryAll('p-checkbox').length).toBe(2);
        }));
    });

    describe('SELECT field', () => {
        it('should render a p-select', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Status',
                        variable: 'status',
                        clazz: DotCMSClazzes.SELECT,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Select',
                        values: 'Active|active\nInactive|inactive'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'select-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('p-select')).toBeTruthy();
        }));
    });

    describe('RADIO field', () => {
        it('should render one radio button per option', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Priority',
                        variable: 'priority',
                        clazz: DotCMSClazzes.RADIO,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Radio',
                        values: 'High|high\nLow|low'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'radio-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.queryAll('p-radiobutton').length).toBe(2);
        }));
    });

    describe('MULTI_SELECT field', () => {
        it('should render a p-multiSelect', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Tags',
                        variable: 'categories',
                        clazz: DotCMSClazzes.MULTI_SELECT,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Multi-Select',
                        values: 'News|news\nSports|sports'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'multiselect-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('p-multiselect')).toBeTruthy();
        }));
    });

    describe('IMAGE and FILE fields', () => {
        it('should render dot-file-field with vertical=true for IMAGE fields', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Image Field',
                        variable: 'imageField',
                        clazz: DotCMSClazzes.IMAGE,
                        fieldType: 'Image',
                        fieldVariables: [],
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'image-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            const fileFieldInstance = spectator.query(MockDotFileFieldComponent);
            expect(fileFieldInstance).toBeTruthy();
            expect(fileFieldInstance?.vertical()).toBe(true);
        }));

        it('should render dot-file-field with vertical=true for FILE fields', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'File Field',
                        variable: 'fileField',
                        clazz: DotCMSClazzes.FILE,
                        fieldType: 'File',
                        fieldVariables: [],
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'file-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            const fileFieldInstance = spectator.query(MockDotFileFieldComponent);
            expect(fileFieldInstance).toBeTruthy();
            expect(fileFieldInstance?.vertical()).toBe(true);
        }));

        it('should render dot-edit-content-binary-field for BINARY fields', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Binary Field',
                        variable: 'binaryField',
                        clazz: DotCMSClazzes.BINARY,
                        fieldType: 'Binary',
                        fieldVariables: [],
                        required: false,
                        readOnly: false,
                        dataType: 'SYSTEM'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'binary-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query(MockDotEditContentBinaryFieldComponent)).toBeTruthy();
        }));
    });

    describe('TAG field', () => {
        it('should render dot-tag-field for TAG fields', fakeAsync(() => {
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Tags',
                        variable: 'tags',
                        clazz: DotCMSClazzes.TAG,
                        fieldType: 'Tag',
                        fieldVariables: [],
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'tag-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            const tagFieldInstance = spectator.query(MockDotTagFieldComponent);
            expect(tagFieldInstance).toBeTruthy();
            expect(tagFieldInstance?.variableName()).toBe('tags');
        }));

        it('should pass hasError=true to dot-tag-field when the control is invalid', fakeAsync(() => {
            // required + no existing value → control is invalid immediately on render
            contentTypeCacheSignal.set(
                makeCache('TestType', [
                    {
                        name: 'Tags',
                        variable: 'tags',
                        clazz: DotCMSClazzes.TAG,
                        fieldType: 'Tag',
                        fieldVariables: [],
                        required: true,
                        readOnly: false,
                        dataType: 'TEXT'
                    }
                ])
            );
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'tag-error-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query(MockDotTagFieldComponent)?.hasError()).toBe(true);
        }));
    });

    describe('Save button', () => {
        const getSaveButton = () =>
            spectator.query(byTestId('save-button'))?.querySelector('button') as HTMLButtonElement;

        it('should render the save button when fields are present', () => {
            expect(spectator.query(byTestId('save-button'))).toBeTruthy();
        });

        it('should be disabled initially because the form is not dirty', () => {
            expect(getSaveButton()?.disabled).toBe(true);
        });

        it('should become enabled after changing a form value', fakeAsync(() => {
            spectator.component.$contentletForm()?.patchValue({ testField: 'new value' });
            spectator.detectChanges();

            expect(getSaveButton()?.disabled).toBe(false);
        }));

        it('should be disabled when the form is invalid', fakeAsync(() => {
            // testField is required — setting it to empty makes the form invalid
            spectator.component.$contentletForm()?.patchValue({ testField: '' });
            spectator.detectChanges();

            expect(getSaveButton()?.disabled).toBe(true);
        }));

        it('should be disabled when loading is true even if the form is dirty', fakeAsync(() => {
            spectator.component.$contentletForm()?.patchValue({ testField: 'changed' });
            fixture.componentRef.setInput('loading', true);
            spectator.detectChanges();

            expect(getSaveButton()?.disabled).toBe(true);
        }));

        it('should be disabled again after a successful save', fakeAsync(() => {
            const uveStore = spectator.inject(UVEStore, true);
            jest.spyOn(uveStore, 'editorActiveContentlet').mockReturnValue(
                EDIT_ACTION_PAYLOAD_MOCK
            );

            spectator.component.$contentletForm()?.patchValue({ testField: 'changed' });
            spectator.detectChanges();
            expect(getSaveButton()?.disabled).toBe(false);

            spectator.click(getSaveButton());
            spectator.detectChanges();

            expect(getSaveButton()?.disabled).toBe(true);
        }));
    });

    describe('save()', () => {
        let uveStore: InstanceType<typeof UVEStore>;
        let optimisticSave: UveOptimisticSaveService;

        beforeEach(() => {
            jest.clearAllMocks();
            uveStore = spectator.inject(UVEStore, true);
            optimisticSave = spectator.inject(UveOptimisticSaveService, true);
            jest.spyOn(uveStore, 'saveQuickEditFields').mockReturnValue(of({}));
            jest.spyOn(uveStore, 'editorActiveContentlet').mockReturnValue(
                EDIT_ACTION_PAYLOAD_MOCK
            );
        });

        it('should not call saveQuickEditFields when the form has not changed', () => {
            spectator.component['save']();

            expect(uveStore.saveQuickEditFields).not.toHaveBeenCalled();
        });

        it('should not auto-save when form values change', fakeAsync(() => {
            spectator.component.$contentletForm()?.patchValue({ testField: 'auto-save attempt' });
            spectator.detectChanges();

            expect(uveStore.saveQuickEditFields).not.toHaveBeenCalled();
        }));

        it('should call saveQuickEditFields with filtered form values on save', fakeAsync(() => {
            spectator.component.$contentletForm()?.patchValue({ testField: 'hello' });
            spectator.detectChanges();

            spectator.component['save']();

            expect(uveStore.saveQuickEditFields).toHaveBeenCalledWith(
                expect.objectContaining({ testField: 'hello' })
            );
        }));

        it('should call addCurrentPageToHistory before saving', fakeAsync(() => {
            spectator.component.$contentletForm()?.patchValue({ testField: 'hello' });
            spectator.detectChanges();

            spectator.component['save']();

            expect(uveStore.addCurrentPageToHistory).toHaveBeenCalled();
        }));

        it('should show a success toast after saving', fakeAsync(() => {
            const messageService = spectator.inject(MessageService, true);
            spectator.component.$contentletForm()?.patchValue({ testField: 'hello' });
            spectator.detectChanges();

            spectator.component['save']();

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({ severity: 'success' })
            );
        }));

        it('should show an error toast and restore the form on save failure', fakeAsync(() => {
            const messageService = spectator.inject(MessageService, true);
            jest.spyOn(uveStore, 'saveQuickEditFields').mockReturnValue(
                throwError(() => new Error('API error'))
            );

            spectator.component.$contentletForm()?.patchValue({ testField: 'hello' });
            spectator.detectChanges();

            spectator.component['save']();

            expect(messageService.add).toHaveBeenCalledWith(
                expect.objectContaining({ severity: 'error' })
            );
            expect(optimisticSave.extractFromRollback).toHaveBeenCalled();
        }));

        describe('traditional page', () => {
            beforeEach(() => {
                jest.spyOn(uveStore, 'pageType').mockReturnValue(PageType.TRADITIONAL);
                jest.spyOn(uveStore, 'saveQuickEditFields').mockReturnValue(of({}));
            });

            it('should call pageReload after a successful save', fakeAsync(() => {
                spectator.component.$contentletForm()?.patchValue({ testField: 'hello' });
                spectator.detectChanges();

                spectator.component['save']();

                expect(uveStore.pageReload).toHaveBeenCalled();
            }));

            it('should set UVE status to LOADING before saving', fakeAsync(() => {
                spectator.component.$contentletForm()?.patchValue({ testField: 'hello' });
                spectator.detectChanges();

                spectator.component['save']();

                expect(uveStore.setUveStatus).toHaveBeenCalledWith('loading');
            }));
        });
    });

    describe('copy decision state', () => {
        const mockMultiPageContentlet = {
            ...mockContentlet,
            onNumberOfPages: 2
        } as DotCMSContentlet;

        const mockMultiPageEditData: ContentletEditData = {
            ...mockContentletEditData,
            contentlet: mockMultiPageContentlet
        };

        const getCopyConfirmButton = () =>
            spectator
                .query(byTestId('copy-confirm-button'))
                ?.querySelector('button') as HTMLButtonElement;

        beforeEach(() => {
            fixture.componentRef.setInput('data', mockMultiPageEditData);
            spectator.detectChanges();
        });

        it('should show the copy decision screen when contentlet is on multiple pages', () => {
            expect(spectator.query(byTestId('copy-mode-all-pages'))).toBeTruthy();
            expect(spectator.query(byTestId('copy-mode-this-page'))).toBeTruthy();
            expect(spectator.query('form')).toBeFalsy();
        });

        it('should keep the confirm button disabled when no mode is selected', () => {
            expect(getCopyConfirmButton()?.disabled).toBe(true);
        });

        it('should enable the confirm button after selecting a mode', () => {
            spectator.click(spectator.query(byTestId('copy-mode-all-pages')) as HTMLElement);
            spectator.detectChanges();

            expect(getCopyConfirmButton()?.disabled).toBe(false);
        });

        it('should show the form after confirming "All Pages"', () => {
            spectator.click(spectator.query(byTestId('copy-mode-all-pages')) as HTMLElement);
            spectator.detectChanges();
            spectator.click(getCopyConfirmButton());
            spectator.detectChanges();

            expect(spectator.query('form')).toBeTruthy();
            expect(spectator.query(byTestId('copy-mode-all-pages'))).toBeFalsy();
        });

        it('should call copyInPage and setActiveContentlet after confirming "This Page Only"', fakeAsync(() => {
            const uveStore = spectator.inject(UVEStore, true);
            const copyContentService = spectator.inject(DotCopyContentService, true);
            const copiedContentlet = {
                ...mockContentlet,
                identifier: 'copied-123',
                inode: 'copied-inode'
            } as DotCMSContentlet;

            jest.spyOn(uveStore, 'getCurrentTreeNode').mockReturnValue({} as never);
            jest.spyOn(uveStore, 'getPageSavePayload').mockReturnValue({} as never);
            jest.spyOn(copyContentService, 'copyInPage').mockReturnValue(of(copiedContentlet));

            spectator.click(spectator.query(byTestId('copy-mode-this-page')) as HTMLElement);
            spectator.detectChanges();
            spectator.click(getCopyConfirmButton());
            flushMicrotasks();
            spectator.detectChanges();

            expect(copyContentService.copyInPage).toHaveBeenCalled();
            expect(uveStore.setActiveContentlet).toHaveBeenCalled();
            expect(uveStore.pageReload).toHaveBeenCalled();
        }));

        it('should call dotHttpErrorManagerService.handle on copyInPage error', fakeAsync(() => {
            const uveStore = spectator.inject(UVEStore, true);
            const copyContentService = spectator.inject(DotCopyContentService, true);
            const httpErrorManager = spectator.inject(DotHttpErrorManagerService, true);

            jest.spyOn(uveStore, 'getCurrentTreeNode').mockReturnValue({} as never);
            jest.spyOn(copyContentService, 'copyInPage').mockReturnValue(
                throwError(() => new Error('copy failed'))
            );
            jest.spyOn(httpErrorManager, 'handle').mockReturnValue(of(null));

            spectator.click(spectator.query(byTestId('copy-mode-this-page')) as HTMLElement);
            spectator.detectChanges();
            spectator.click(getCopyConfirmButton());
            flushMicrotasks();
            spectator.detectChanges();

            expect(httpErrorManager.handle).toHaveBeenCalled();
        }));

        describe('confirm button label', () => {
            it('should show "Confirm" when no mode is selected', () => {
                const btn = spectator.query(byTestId('copy-confirm-button'));
                expect(btn?.querySelector('button')?.textContent?.trim()).toBe('Confirm');
            });

            it('should show "Edit All Pages" when ALL_PAGES is selected', () => {
                spectator.click(spectator.query(byTestId('copy-mode-all-pages')) as HTMLElement);
                spectator.detectChanges();

                expect(spectator.component.$confirmLabel()).toBe(
                    'uve.quick-edit.copy-decision.confirm.all-pages'
                );
                const btn = spectator.query(byTestId('copy-confirm-button'));
                expect(btn?.querySelector('button')?.textContent?.trim()).toBe('Edit All Pages');
            });

            it('should show "Copy & Edit" when THIS_PAGE is selected', () => {
                spectator.click(spectator.query(byTestId('copy-mode-this-page')) as HTMLElement);
                spectator.detectChanges();

                expect(spectator.component.$confirmLabel()).toBe(
                    'uve.quick-edit.copy-decision.confirm.this-page'
                );
                const btn = spectator.query(byTestId('copy-confirm-button'));
                expect(btn?.querySelector('button')?.textContent?.trim()).toBe('Copy & Edit');
            });

            it('should return the correct key for each CopyMode value', () => {
                expect(CopyMode.ALL_PAGES).toBe('all-pages');
                expect(CopyMode.THIS_PAGE).toBe('this-page');
            });
        });

        it('should reset the copy decision when the contentlet identifier changes', fakeAsync(() => {
            // confirm decision so form shows
            spectator.click(spectator.query(byTestId('copy-mode-all-pages')) as HTMLElement);
            spectator.detectChanges();
            spectator.click(getCopyConfirmButton());
            spectator.detectChanges();
            expect(spectator.query('form')).toBeTruthy();

            // switch to a different contentlet that also spans multiple pages
            fixture.componentRef.setInput('data', {
                ...mockMultiPageEditData,
                contentlet: { ...mockMultiPageContentlet, identifier: 'new-multi-page-id' }
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query(byTestId('copy-mode-all-pages'))).toBeTruthy();
            expect(spectator.query('form')).toBeFalsy();
        }));
    });

    describe('optimistic updates', () => {
        let uveStore: InstanceType<typeof UVEStore>;
        let optimisticSave: UveOptimisticSaveService;

        beforeEach(() => {
            jest.clearAllMocks();
            uveStore = spectator.inject(UVEStore, true);
            optimisticSave = spectator.inject(UveOptimisticSaveService, true);
        });

        it('should call updateIframeOptimistically on headless page form changes', fakeAsync(() => {
            jest.spyOn(uveStore, 'editorActiveContentlet').mockReturnValue(
                EDIT_ACTION_PAYLOAD_MOCK
            );
            jest.spyOn(uveStore, 'pageType').mockReturnValue(PageType.HEADLESS);

            spectator.component.$contentletForm()?.patchValue({ testField: 'optimistic' });
            spectator.detectChanges();

            expect(optimisticSave.updateIframeOptimistically).toHaveBeenCalled();
        }));

        it('should not call updateIframeOptimistically on traditional page form changes', fakeAsync(() => {
            jest.spyOn(uveStore, 'editorActiveContentlet').mockReturnValue(
                EDIT_ACTION_PAYLOAD_MOCK
            );
            jest.spyOn(uveStore, 'pageType').mockReturnValue(PageType.TRADITIONAL);

            spectator.component.$contentletForm()?.patchValue({ testField: 'no optimistic' });
            spectator.detectChanges();

            expect(optimisticSave.updateIframeOptimistically).not.toHaveBeenCalled();
        }));
    });
});
