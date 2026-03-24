import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { Component, input } from '@angular/core';
import { ComponentFixture, fakeAsync, flushMicrotasks } from '@angular/core/testing';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotEditContentBinaryFieldComponent,
    DotEditContentService,
    DotFileFieldComponent,
    DotTagFieldComponent
} from '@dotcms/edit-content';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    ContentletEditData,
    DotUveContentletQuickEditComponent
} from './dot-uve-contentlet-quick-edit.component';

import { UveOptimisticSaveService } from '../../../services/uve-optimistic-save/uve-optimistic-save.service';
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

const mockContentletEditData: ContentletEditData = {
    container: {
        identifier: 'container-123',
        uuid: 'uuid-123',
        acceptTypes: 'test',
        maxContentlets: 1
    },
    contentlet: mockContentlet,
    fields: [
        {
            name: 'Test Field',
            variable: 'testField',
            clazz: DotCMSClazzes.TEXT,
            required: true,
            readOnly: false,
            dataType: 'TEXT',
            fieldVariables: [],
            fieldType: 'TEXT'
        }
    ]
};

describe('DotUveContentletQuickEditComponent', () => {
    let spectator: Spectator<DotUveContentletQuickEditComponent>;
    let fixture: ComponentFixture<DotUveContentletQuickEditComponent>;

    const createComponent = createComponentFactory({
        component: DotUveContentletQuickEditComponent,
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
            mockProvider(DotEditContentService)
        ],
        providers: [
            mockProvider(UVEStore, {
                editorActiveContentlet: jest.fn().mockReturnValue(null),
                pageType: jest.fn().mockReturnValue(PageType.HEADLESS),
                addCurrentPageToHistory: jest.fn(),
                setUveStatus: jest.fn(),
                pageReload: jest.fn(),
                saveQuickEditFields: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(MessageService),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'message.content.saved': 'Saved',
                    'message.content.note.already.published': 'Already published',
                    'editpage.content.update.contentlet.error': 'Error updating contentlet'
                })
            }
        ]
    });

    beforeEach(fakeAsync(() => {
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
        it('should render the form when fields are provided', () => {
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
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'different-id' },
                fields: [{ ...mockContentletEditData.fields[0], variable: 'rebuiltField' }]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('#rebuiltField')).toBeTruthy();
            expect(spectator.query('#testField')).toBeFalsy();
        }));
    });

    describe('empty state', () => {
        it('should show the empty state when there are no fields', () => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                fields: []
            });
            spectator.detectChanges();

            expect(spectator.query('form')).toBeFalsy();
            expect(spectator.query('.font-bold')).toHaveText('Select a contentlet');
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
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'optional-id' },
                fields: [
                    {
                        ...mockContentletEditData.fields[0],
                        required: false,
                        variable: 'optionalField'
                    }
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('label')).not.toHaveClass('p-label-input-required');
        }));
    });

    describe('loading state', () => {
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

    describe('TEXT field', () => {
        it('should render a text input', () => {
            expect(spectator.query('#testField')).toBeTruthy();
        });

        it('should render a readOnly TEXT field as disabled', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'readonly-id' },
                fields: [
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
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect((spectator.query('#readOnlyField') as HTMLInputElement)?.disabled).toBe(true);
        }));
    });

    describe('TEXTAREA field', () => {
        it('should render a textarea', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'textarea-id' },
                fields: [
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
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('#description')).toBeTruthy();
        }));
    });

    describe('CHECKBOX field', () => {
        it('should render a binary checkbox when there are no options', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'checkbox-binary-id' },
                fields: [
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
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('p-checkbox')).toBeTruthy();
        }));

        it('should render one checkbox per option when options are provided', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'checkbox-options-id' },
                fields: [
                    {
                        name: 'Colors',
                        variable: 'colors',
                        clazz: DotCMSClazzes.CHECKBOX,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Checkbox',
                        options: [
                            { label: 'Red', value: 'red' },
                            { label: 'Blue', value: 'blue' }
                        ]
                    }
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.queryAll('p-checkbox').length).toBe(2);
        }));
    });

    describe('SELECT field', () => {
        it('should render a p-select', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'select-id' },
                fields: [
                    {
                        name: 'Status',
                        variable: 'status',
                        clazz: DotCMSClazzes.SELECT,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Select',
                        options: [
                            { label: 'Active', value: 'active' },
                            { label: 'Inactive', value: 'inactive' }
                        ]
                    }
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('p-select')).toBeTruthy();
        }));
    });

    describe('RADIO field', () => {
        it('should render one radio button per option', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'radio-id' },
                fields: [
                    {
                        name: 'Priority',
                        variable: 'priority',
                        clazz: DotCMSClazzes.RADIO,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Radio',
                        options: [
                            { label: 'High', value: 'high' },
                            { label: 'Low', value: 'low' }
                        ]
                    }
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.queryAll('p-radiobutton').length).toBe(2);
        }));
    });

    describe('MULTI_SELECT field', () => {
        it('should render a p-multiSelect', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'multiselect-id' },
                fields: [
                    {
                        name: 'Tags',
                        variable: 'categories',
                        clazz: DotCMSClazzes.MULTI_SELECT,
                        required: false,
                        readOnly: false,
                        dataType: 'TEXT',
                        fieldVariables: [],
                        fieldType: 'Multi-Select',
                        options: [
                            { label: 'News', value: 'news' },
                            { label: 'Sports', value: 'sports' }
                        ]
                    }
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query('p-multiselect')).toBeTruthy();
        }));
    });

    describe('IMAGE and FILE fields', () => {
        it('should render dot-file-field with vertical=true for IMAGE fields', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'image-id' },
                fields: [
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
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            const fileFieldInstance = spectator.query(MockDotFileFieldComponent);
            expect(fileFieldInstance).toBeTruthy();
            expect(fileFieldInstance?.vertical()).toBe(true);
        }));

        it('should render dot-file-field with vertical=true for FILE fields', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'file-id' },
                fields: [
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
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            const fileFieldInstance = spectator.query(MockDotFileFieldComponent);
            expect(fileFieldInstance).toBeTruthy();
            expect(fileFieldInstance?.vertical()).toBe(true);
        }));

        it('should render dot-edit-content-binary-field for BINARY fields', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'binary-id' },
                fields: [
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
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query(MockDotEditContentBinaryFieldComponent)).toBeTruthy();
        }));
    });

    describe('TAG field', () => {
        it('should render dot-tag-field for TAG fields', fakeAsync(() => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'tag-id' },
                fields: [
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
                ]
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
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
                contentlet: { ...mockContentlet, identifier: 'tag-error-id' },
                fields: [
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
                ]
            });
            spectator.detectChanges();
            flushMicrotasks();
            spectator.detectChanges();

            expect(spectator.query(MockDotTagFieldComponent)?.hasError()).toBe(true);
        }));
    });
});
