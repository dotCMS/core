import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Component, input } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSClazzes, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotEditContentBinaryFieldComponent, DotFileFieldComponent } from '@dotcms/edit-content';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    ContentletEditData,
    DotUveContentletQuickEditComponent
} from './dot-uve-contentlet-quick-edit.component';

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
                        imports: [DotFileFieldComponent, DotEditContentBinaryFieldComponent]
                    },
                    add: {
                        imports: [MockDotFileFieldComponent, MockDotEditContentBinaryFieldComponent]
                    }
                }
            ]
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.common.cancel': 'Cancel',
                    'dot.common.save': 'Save'
                })
            }
        ]
    });

    const mockContentletEditData: ContentletEditData = {
        container: {
            identifier: 'container-123',
            uuid: 'uuid-123',
            acceptTypes: 'test',
            maxContentlets: 1
        },
        contentlet: {
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
        } as DotCMSContentlet,
        fields: [
            {
                name: 'Test Field',
                variable: 'testField',
                clazz: DotCMSClazzes.TEXT,
                required: true,
                readOnly: false,
                dataType: 'TEXT'
            }
        ]
    };

    beforeEach(() => {
        spectator = createComponent({
            props: {
                data: mockContentletEditData,
                loading: false
            }
        });
        fixture = spectator.fixture;
        spectator.detectChanges(); // Trigger effect to build form
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should build form when data is provided', () => {
        spectator.detectChanges(); // Ensure form is built and rendered
        const formElement = spectator.query('form');
        expect(formElement).toBeTruthy();

        const testFieldInput =
            spectator.query('input[formcontrolname="testField"]') || spectator.query('#testField');
        expect(testFieldInput).toBeTruthy();
    });

    it('should display form fields', () => {
        const label = spectator.query('label');
        expect(label).toHaveText('Test Field');
    });

    it('should emit submit event when form is valid and submitted', () => {
        spectator.detectChanges(); // Ensure form is built and rendered
        let emittedData: Record<string, unknown> | undefined;
        spectator.component.submit.subscribe((data) => (emittedData = data));

        const input = (spectator.query('input[formcontrolname="testField"]') ||
            spectator.query('#testField')) as HTMLInputElement;
        expect(input).toBeTruthy();
        spectator.typeInElement('test value', input);
        spectator.detectChanges();

        const saveBtn = spectator
            .query('[data-testid="quick-edit-save-btn"]')
            ?.querySelector('button');
        spectator.click(saveBtn as HTMLElement);
        spectator.detectChanges();

        expect(emittedData).toBeDefined();
        expect(emittedData?.['testField']).toBe('test value');
    });

    it('should emit cancel event when cancel button is clicked', () => {
        let cancelEmitted = false;
        spectator.component.cancel.subscribe(() => (cancelEmitted = true));

        const cancelBtn = spectator
            .query('[data-testid="quick-edit-cancel-btn"]')
            ?.querySelector('button');
        expect(cancelBtn).toBeTruthy();
        spectator.click(cancelBtn as HTMLElement);

        expect(cancelEmitted).toBe(true);
    });

    it('should disable buttons when loading', () => {
        fixture.componentRef.setInput('loading', true);
        spectator.detectChanges();

        const cancelButton = spectator
            .query('[data-testid="quick-edit-cancel-btn"]')
            ?.querySelector('button') as HTMLButtonElement;
        const submitButton = spectator
            .query('[data-testid="quick-edit-save-btn"]')
            ?.querySelector('button') as HTMLButtonElement;

        expect(cancelButton.disabled).toBe(true);
        expect(submitButton.disabled).toBe(true);
    });

    it('should display empty state when no fields', () => {
        fixture.componentRef.setInput('data', {
            ...mockContentletEditData,
            fields: []
        });
        spectator.detectChanges();

        expect(spectator.query('form')).toBeFalsy();
        expect(spectator.query('.font-bold')).toHaveText('Select a contentlet');
    });

    it('should mark required fields with CSS class', () => {
        const label = spectator.query('label');
        expect(label).toHaveClass('p-label-input-required');
    });

    it('should emit cancel event on Escape key', () => {
        let cancelEmitted = false;
        spectator.component.cancel.subscribe(() => (cancelEmitted = true));

        const form = spectator.query('form');
        expect(form).toBeTruthy();

        if (form) {
            form.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
            spectator.detectChanges();
        }

        expect(cancelEmitted).toBe(true);
    });

    it('should include inode in form if contentlet has inode', () => {
        const inodeInput = spectator.query('input[formcontrolname="inode"]');
        expect(inodeInput).toBeTruthy();
        expect((inodeInput as HTMLInputElement).value).toBe('inode-123');
    });

    describe('IMAGE and FILE fields', () => {
        it('should render dot-file-field with vertical=true for IMAGE fields', () => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
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

            const fileFieldInstance = spectator.query(MockDotFileFieldComponent);
            expect(fileFieldInstance).toBeTruthy();
            expect(fileFieldInstance?.vertical()).toBe(true);
        });

        it('should render dot-file-field with vertical=true for FILE fields', () => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
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

            const fileFieldInstance = spectator.query(MockDotFileFieldComponent);
            expect(fileFieldInstance).toBeTruthy();
            expect(fileFieldInstance?.vertical()).toBe(true);
        });

        it('should render dot-edit-content-binary-field for BINARY fields', () => {
            fixture.componentRef.setInput('data', {
                ...mockContentletEditData,
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

            expect(spectator.query('dot-edit-content-binary-field')).toBeTruthy();
        });
    });

    it('should not submit form when invalid', () => {
        let emittedData: Record<string, unknown> | undefined;
        spectator.component.submit.subscribe((data) => (emittedData = data));

        const input = spectator.query('input[formcontrolname="testField"]') as HTMLInputElement;
        spectator.typeInElement('', input); // Clear required field to make form invalid
        spectator.detectChanges();

        const submitButton = spectator
            .query('[data-testid="quick-edit-save-btn"]')
            ?.querySelector('button') as HTMLButtonElement;
        expect(submitButton.disabled).toBe(true); // Should be disabled when form is invalid

        expect(emittedData).toBeUndefined();
    });
});
