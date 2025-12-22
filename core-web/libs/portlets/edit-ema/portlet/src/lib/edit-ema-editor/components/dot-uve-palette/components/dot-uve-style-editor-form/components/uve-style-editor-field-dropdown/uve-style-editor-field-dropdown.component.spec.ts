import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Dropdown } from 'primeng/dropdown';

import { StyleEditorFieldSchema, StyleEditorRadioOptionObject } from '@dotcms/uve';

import { UveStyleEditorFieldDropdownComponent } from './uve-style-editor-field-dropdown.component';

@Component({
    standalone: false,
    selector: 'dot-custom-host',
    template: ''
})
export class MockFormComponent {
    // Host Props
    formGroup: FormGroup;
    field: StyleEditorFieldSchema;
}

const createMockField = (
    id: string,
    label: string,
    options: StyleEditorRadioOptionObject[]
): StyleEditorFieldSchema => ({
    id,
    label,
    type: 'dropdown',
    config: {
        options
    }
});

describe('UveStyleEditorFieldDropdownComponent', () => {
    let spectator: SpectatorHost<UveStyleEditorFieldDropdownComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: UveStyleEditorFieldDropdownComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    describe('rendering', () => {
        it('should render the field label', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' },
                { label: 'Helvetica', value: 'Helvetica' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const label = spectator.query('.field-label');
            expect(label).toBeTruthy();
            expect(label.textContent.trim()).toBe('Font Family');
        });

        it('should render the dropdown component', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' },
                { label: 'Helvetica', value: 'Helvetica' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(dropdown).toBeTruthy();
        });

        it('should set correct inputId on dropdown', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(dropdown.inputId).toBe('test-field');
        });

        it('should set correct optionLabel and optionValue on dropdown', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(dropdown.optionLabel).toBe('label');
            expect(dropdown.optionValue).toBe('value');
        });

        it('should set showClear to false on dropdown', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(dropdown.showClear).toBe(false);
        });
    });

    describe('options computation', () => {
        it('should compute options from field config', () => {
            const options: StyleEditorRadioOptionObject[] = [
                { label: 'Arial', value: 'Arial' },
                { label: 'Helvetica', value: 'Helvetica' },
                { label: 'Times New Roman', value: 'Times New Roman' }
            ];
            const field = createMockField('test-field', 'Font Family', options);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$options()).toEqual(options);
        });

        it('should pass options to dropdown', () => {
            const options: StyleEditorRadioOptionObject[] = [
                { label: 'Arial', value: 'Arial' },
                { label: 'Helvetica', value: 'Helvetica' }
            ];
            const field = createMockField('test-field', 'Font Family', options);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(dropdown.options).toEqual(options);
        });

        it('should return empty array when options are not provided', () => {
            const field: StyleEditorFieldSchema = {
                id: 'test-field',
                label: 'Font Family',
                type: 'dropdown',
                config: {}
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$options()).toEqual([]);
        });

        it('should return empty array when config is not provided', () => {
            const field: StyleEditorFieldSchema = {
                id: 'test-field',
                label: 'Font Family',
                type: 'dropdown',
                config: {}
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl()
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$options()).toEqual([]);
        });
    });

    describe('form integration', () => {
        it('should bind dropdown to form control', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' },
                { label: 'Helvetica', value: 'Helvetica' }
            ]);

            const formControl = new FormControl('Arial');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: formControl
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(dropdown.value).toBe('Arial');
            expect(formControl.value).toBe('Arial');
        });

        it('should update form control when dropdown value changes', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' },
                { label: 'Helvetica', value: 'Helvetica' }
            ]);

            const formControl = new FormControl('Arial');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: formControl
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(formControl.value).toBe('Arial');

            formControl.setValue('Helvetica');
            spectator.detectChanges();

            expect(dropdown.value).toBe('Helvetica');
            expect(formControl.value).toBe('Helvetica');
        });

        it('should handle null form control value', () => {
            const field = createMockField('test-field', 'Font Family', [
                { label: 'Arial', value: 'Arial' }
            ]);

            const formControl = new FormControl(null);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-dropdown [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: formControl
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const dropdown = spectator.query(Dropdown);
            expect(formControl.value).toBeNull();
            expect(dropdown.value).toBeNull();
        });
    });
});
