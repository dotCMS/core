import '@testing-library/jest-dom';
import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { StyleEditorFieldSchema } from '@dotcms/uve';

import { UveStyleEditorFieldInputComponent } from './uve-style-editor-field-input.component';

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
    inputType?: 'text' | 'number',
    placeholder?: string
): StyleEditorFieldSchema => ({
    id,
    label,
    type: 'input',
    config: {
        ...(inputType && { inputType }),
        ...(placeholder && { placeholder })
    }
});

describe('UveStyleEditorFieldInputComponent', () => {
    let spectator: SpectatorHost<UveStyleEditorFieldInputComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: UveStyleEditorFieldInputComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule, InputTextModule],
        detectChanges: false
    });

    describe('rendering', () => {
        it('should render the field label', () => {
            const field = createMockField('test-field', 'Font Size');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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
            expect(label.textContent.trim()).toBe('Font Size');
        });

        it('should render the input element', () => {
            const field = createMockField('test-field', 'Font Size');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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

            const input = spectator.query('input');
            expect(input).toBeTruthy();
        });

        it('should set correct id on input element', () => {
            const field = createMockField('test-field', 'Font Size');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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

            const input = spectator.query('input');
            expect(input.getAttribute('id')).toBe('test-field');
        });

        it('should set correct for attribute on label', () => {
            const field = createMockField('test-field', 'Font Size');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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
            expect(label.getAttribute('for')).toBe('test-field');
        });

        it('should have uve-input class on input element', () => {
            const field = createMockField('test-field', 'Font Size');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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

            const input = spectator.query('input');
            expect(input.classList.contains('uve-input')).toBe(true);
        });
    });

    describe('form integration', () => {
        it('should bind input to form control', () => {
            const field = createMockField('test-field', 'Font Size');

            const formControl = new FormControl('16');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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

            const input = spectator.query('input') as HTMLInputElement;
            expect(input.value).toBe('16');
            expect(formControl.value).toBe('16');
        });

        it('should update form control when input value changes', () => {
            const field = createMockField('test-field', 'Font Size');

            const formControl = new FormControl('16');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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

            const input = spectator.query('input') as HTMLInputElement;
            expect(formControl.value).toBe('16');

            spectator.typeInElement('20', input);
            spectator.detectChanges();

            expect(formControl.value).toBe('20');
        });

        it('should handle empty form control value', () => {
            const field = createMockField('test-field', 'Font Size');

            const formControl = new FormControl('');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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

            const input = spectator.query('input') as HTMLInputElement;
            expect(formControl.value).toBe('');
            expect(input.value).toBe('');
        });

        it('should handle null form control value', () => {
            const field = createMockField('test-field', 'Font Size');

            const formControl = new FormControl(null);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
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

            const input = spectator.query('input') as HTMLInputElement;
            expect(formControl.value).toBeNull();
            expect(input.value).toBe('');
        });
    });

    describe('field configuration', () => {
        it('should work with text inputType', () => {
            const field = createMockField('test-field', 'Font Name', 'text');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl('Arial')
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const input = spectator.query('input') as HTMLInputElement;
            expect(input).toBeTruthy();
            expect(input.value).toBe('Arial');
        });

        it('should work with number inputType', () => {
            const field = createMockField('test-field', 'Font Size', 'number');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl(16)
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const input = spectator.query('input') as HTMLInputElement;
            expect(input).toBeTruthy();
            expect(input.value).toBe('16');
        });

        it('should work without inputType in config', () => {
            const field: StyleEditorFieldSchema = {
                id: 'test-field',
                label: 'Font Size',
                type: 'input',
                config: {}
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-input [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: new FormControl('test')
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const input = spectator.query('input') as HTMLInputElement;
            expect(input).toBeTruthy();
            expect(input.value).toBe('test');
        });
    });
});
