import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Checkbox } from 'primeng/checkbox';

import { StyleEditorFieldSchema, StyleEditorRadioOptionObject } from '@dotcms/uve';

import { UveStyleEditorFieldCheckboxGroupComponent } from './uve-style-editor-field-checkbox-group.component';

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
    type: 'checkboxGroup',
    config: {
        options
    }
});

describe('UveStyleEditorFieldCheckboxGroupComponent', () => {
    let spectator: SpectatorHost<UveStyleEditorFieldCheckboxGroupComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: UveStyleEditorFieldCheckboxGroupComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    describe('rendering', () => {
        it('should render the field label', () => {
            const field = createMockField('test-field', 'Text Decoration', [
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' }
            ]);

            const checkboxFormGroup = new FormGroup({
                underline: new FormControl(false),
                overline: new FormControl(false)
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const label = spectator.query('.field-label');
            expect(label).toBeTruthy();
            expect(label.textContent.trim()).toBe('Text Decoration');
        });

        it('should render checkboxes for each option', () => {
            const field = createMockField('test-field', 'Text Decoration', [
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' },
                { label: 'Line Through', value: 'line-through' }
            ]);

            const checkboxFormGroup = new FormGroup({
                underline: new FormControl(false),
                overline: new FormControl(false),
                'line-through': new FormControl(false)
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes.length).toBe(3);
        });

        it('should render checkboxes with correct labels', () => {
            const field = createMockField('test-field', 'Text Decoration', [
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' }
            ]);

            const checkboxFormGroup = new FormGroup({
                underline: new FormControl(false),
                overline: new FormControl(false)
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            // PrimeNG Checkbox does not expose a stable "label" API on the component instance.
            // Assert through DOM aria-labels (we bind [ariaLabel]="option.label").
            expect(spectator.query('input[aria-label="Underline"]')).toBeTruthy();
            expect(spectator.query('input[aria-label="Overline"]')).toBeTruthy();
        });

        it('should render checkboxes with correct inputId', () => {
            const field = createMockField('test-field', 'Text Decoration', [
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' }
            ]);

            const checkboxFormGroup = new FormGroup({
                underline: new FormControl(false),
                overline: new FormControl(false)
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const checkboxes = spectator.queryAll(Checkbox);
            expect(checkboxes[0].inputId).toBe('test-field-underline');
            expect(checkboxes[1].inputId).toBe('test-field-overline');
        });
    });

    describe('options computation', () => {
        it('should compute options from field config', () => {
            const options: StyleEditorRadioOptionObject[] = [
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' }
            ];
            const field = createMockField('test-field', 'Text Decoration', options);

            const checkboxFormGroup = new FormGroup({
                underline: new FormControl(false),
                overline: new FormControl(false)
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            expect(spectator.component.$options()).toEqual(options);
        });

        it('should return empty array when options are not provided', () => {
            const field: StyleEditorFieldSchema = {
                id: 'test-field',
                label: 'Text Decoration',
                type: 'checkboxGroup',
                config: {}
            };

            const checkboxFormGroup = new FormGroup({});

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
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
                label: 'Text Decoration',
                type: 'checkboxGroup',
                config: {}
            };

            const checkboxFormGroup = new FormGroup({});

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
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
        it('should bind checkboxes to form controls', () => {
            const field = createMockField('test-field', 'Text Decoration', [
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' }
            ]);

            const checkboxFormGroup = new FormGroup({
                underline: new FormControl(true),
                overline: new FormControl(false)
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            const checkboxes = spectator.queryAll(Checkbox);
            const checkedCheckboxes = checkboxes.filter((checkbox) => checkbox.checked);
            expect(checkedCheckboxes.length).toBe(1);
        });

        it('should have form controls correctly initialized', () => {
            const field = createMockField('test-field', 'Text Decoration', [
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' }
            ]);

            const checkboxFormGroup = new FormGroup({
                underline: new FormControl(true),
                overline: new FormControl(false)
            });

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-checkbox-group [field]="field" />
                </form>`,
                {
                    hostProps: {
                        formGroup: new FormGroup({
                            [field.id]: checkboxFormGroup
                        }),
                        field
                    }
                }
            );
            spectator.detectChanges();

            expect(checkboxFormGroup.get('underline')?.value).toBe(true);
            expect(checkboxFormGroup.get('overline')?.value).toBe(false);
        });
    });
});
