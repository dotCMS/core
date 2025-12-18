import { SpectatorHost, createHostFactory } from '@ngneat/spectator';

import { Component } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { RadioButton } from 'primeng/radiobutton';

import { StyleEditorFieldSchema, StyleEditorRadioOptionObject } from '@dotcms/uve';

import { UveStyleEditorFieldRadioComponent } from './uve-style-editor-field-radio.component';

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
    options: StyleEditorRadioOptionObject[],
    columns?: 1 | 2
): StyleEditorFieldSchema => ({
    id,
    label,
    type: 'radio',
    config: {
        options,
        ...(columns && { columns })
    }
});

describe('UveStyleEditorFieldRadioComponent', () => {
    let spectator: SpectatorHost<UveStyleEditorFieldRadioComponent, MockFormComponent>;

    const createHost = createHostFactory({
        component: UveStyleEditorFieldRadioComponent,
        host: MockFormComponent,
        imports: [ReactiveFormsModule],
        detectChanges: false
    });

    describe('rendering', () => {
        it('should render the field label', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' },
                { label: 'Right', value: 'right' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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
            expect(label.textContent.trim()).toBe('Alignment');
        });

        it('should render radio buttons for each option', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' },
                { label: 'Center', value: 'center' },
                { label: 'Right', value: 'right' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const radioButtons = spectator.queryAll(RadioButton);
            expect(radioButtons.length).toBe(3);
        });

        it('should render radio buttons with correct labels', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' },
                { label: 'Right', value: 'right' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const radioButtons = spectator.queryAll(RadioButton);
            expect(radioButtons[0].label).toBe('Left');
            expect(radioButtons[1].label).toBe('Right');
        });

        it('should render radio buttons with correct inputId', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' },
                { label: 'Right', value: 'right' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const radioButtons = spectator.queryAll(RadioButton);
            expect(radioButtons[0].inputId).toBe('test-field-left');
            expect(radioButtons[1].inputId).toBe('test-field-right');
        });
    });

    describe('options computation', () => {
        it('should compute options from field config', () => {
            const options: StyleEditorRadioOptionObject[] = [
                { label: 'Left', value: 'left' },
                { label: 'Right', value: 'right' }
            ];
            const field = createMockField('test-field', 'Alignment', options);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

        it('should return empty array when options are not provided', () => {
            const field: StyleEditorFieldSchema = {
                id: 'test-field',
                label: 'Alignment',
                type: 'radio',
                config: {}
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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
                label: 'Alignment',
                type: 'radio',
                config: {}
            };

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

    describe('radio image detection', () => {
        it('should detect radio image when first option has imageURL', () => {
            const field = createMockField('test-field', 'Layout', [
                {
                    label: 'Left Layout',
                    value: 'left',
                    imageURL: 'https://example.com/left.png',
                    width: 80,
                    height: 50
                },
                { label: 'Right Layout', value: 'right' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            expect(spectator.component.$hasRadioImage()).toBe(true);
            expect(spectator.query('.field-radio-image')).toBeTruthy();
        });

        it('should not detect radio image when no options have imageURL', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' },
                { label: 'Right', value: 'right' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            expect(spectator.component.$hasRadioImage()).toBe(false);
            expect(spectator.query('.field-radio-image')).toBeFalsy();
        });

        it('should render image radio inputs when hasRadioImage is true', () => {
            const field = createMockField('test-field', 'Layout', [
                {
                    label: 'Left Layout',
                    value: 'left',
                    imageURL: 'https://example.com/left.png',
                    width: 80,
                    height: 50
                }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const imageInputs = spectator.queryAll('input.radio-image-input');
            expect(imageInputs.length).toBe(1);
            expect(imageInputs[0].getAttribute('type')).toBe('radio');
        });

        it('should render regular radio buttons when hasRadioImage is false', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const radioButtons = spectator.queryAll(RadioButton);
            expect(radioButtons.length).toBe(1);
            expect(spectator.queryAll('input.radio-image-input').length).toBe(0);
        });
    });

    describe('columns computation', () => {
        it('should compute columns from field config', () => {
            const field = createMockField(
                'test-field',
                'Layout',
                [{ label: 'Left', value: 'left' }],
                2
            );

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            expect(spectator.component.$columns()).toBe(2);
        });

        it('should default to 1 column when columns are not provided', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' }
            ]);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            expect(spectator.component.$columns()).toBe(1);
        });

        it('should apply grid-template-columns style when hasRadioImage is true', () => {
            const field = createMockField(
                'test-field',
                'Layout',
                [
                    {
                        label: 'Left Layout',
                        value: 'left',
                        imageURL: 'https://example.com/left.png'
                    },
                    {
                        label: 'Right Layout',
                        value: 'right',
                        imageURL: 'https://example.com/right.png'
                    }
                ],
                2
            );

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const radioImageGroup = spectator.query('.radio-image-group');
            expect(radioImageGroup).toBeTruthy();
            expect(radioImageGroup.getAttribute('style')).toContain(
                'grid-template-columns: repeat(2, 1fr)'
            );
        });
    });

    describe('form integration', () => {
        it('should bind radio buttons to form control', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' },
                { label: 'Right', value: 'right' }
            ]);

            const formControl = new FormControl('left');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const radioButtons = spectator.queryAll(RadioButton);
            const checkedRadio = radioButtons.find((radio) => radio.checked);
            expect(checkedRadio).toBeTruthy();
            expect(checkedRadio.value).toBe('left');
            expect(formControl.value).toBe('left');
        });

        it('should update form control when radio button is selected', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' },
                { label: 'Right', value: 'right' }
            ]);

            const formControl = new FormControl('left');

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            expect(formControl.value).toBe('left');

            formControl.setValue('right');
            spectator.detectChanges();

            const radioButtons = spectator.queryAll(RadioButton);
            const checkedRadio = radioButtons.find((radio) => radio.checked);
            expect(checkedRadio.value).toBe('right');
            expect(formControl.value).toBe('right');
        });

        it('should handle null form control value', () => {
            const field = createMockField('test-field', 'Alignment', [
                { label: 'Left', value: 'left' }
            ]);

            const formControl = new FormControl(null);

            spectator = createHost(
                `<form [formGroup]="formGroup">
                    <dot-uve-style-editor-field-radio [field]="field" />
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

            const radioButtons = spectator.queryAll(RadioButton);
            const checkedRadios = radioButtons.filter((radio) => radio.checked);
            expect(checkedRadios.length).toBe(0);
            expect(formControl.value).toBeNull();
        });
    });
});
