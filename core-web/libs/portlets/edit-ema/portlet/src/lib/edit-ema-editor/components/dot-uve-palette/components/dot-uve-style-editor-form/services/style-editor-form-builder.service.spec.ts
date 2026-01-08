import { TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';

import { StyleEditorFormSchema } from '@dotcms/uve';

import { StyleEditorFormBuilderService } from './style-editor-form-builder.service';
import { STYLE_EDITOR_FIELD_TYPES } from '../../../../../../shared/consts';

const createMockSchema = (): StyleEditorFormSchema => ({
    contentType: 'test-content-type',
    sections: [
        {
            title: 'Typography',
            fields: [
                {
                    id: 'font-size',
                    label: 'Font Size',
                    type: 'input',
                    config: {
                        inputType: 'number',
                        defaultValue: 16
                    }
                },
                {
                    id: 'font-family',
                    label: 'Font Family',
                    type: 'dropdown',
                    config: {
                        options: [
                            { label: 'Arial', value: 'Arial' },
                            { label: 'Helvetica', value: 'Helvetica' }
                        ],
                        defaultValue: 'Arial'
                    }
                }
            ]
        },
        {
            title: 'Text Decoration',
            fields: [
                {
                    id: 'text-decoration',
                    label: 'Text Decoration',
                    type: 'checkboxGroup',
                    config: {
                        options: [
                            { label: 'Underline', value: 'underline' },
                            { label: 'Overline', value: 'overline' }
                        ],
                        defaultValue: {
                            underline: true,
                            overline: false
                        }
                    }
                },
                {
                    id: 'alignment',
                    label: 'Alignment',
                    type: 'radio',
                    config: {
                        options: [
                            { label: 'Left', value: 'left' },
                            { label: 'Right', value: 'right' }
                        ],
                        defaultValue: 'left'
                    }
                }
            ]
        }
    ]
});

describe('StyleEditorFormBuilderService', () => {
    let service: StyleEditorFormBuilderService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ReactiveFormsModule],
            providers: [StyleEditorFormBuilderService]
        });
        service = TestBed.inject(StyleEditorFormBuilderService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('buildForm', () => {
        it('should build form with default values when initialValues is not provided', () => {
            const schema = createMockSchema();
            const form = service.buildForm(schema);

            expect(form).toBeInstanceOf(FormGroup);
            expect(form.get('font-size')?.value).toBe(16);
            expect(form.get('font-family')?.value).toBe('Arial');
            expect(form.get('alignment')?.value).toBe('left');

            const textDecorationGroup = form.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(true);
            expect(textDecorationGroup.get('overline')?.value).toBe(false);
        });

        it('should use initial values when provided for input field', () => {
            const schema = createMockSchema();
            const initialValues = {
                'font-size': 24
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('font-size')?.value).toBe(24);
            expect(form.get('font-family')?.value).toBe('Arial'); // Should use default
        });

        it('should use initial values when provided for dropdown field', () => {
            const schema = createMockSchema();
            const initialValues = {
                'font-family': 'Helvetica'
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('font-family')?.value).toBe('Helvetica');
            expect(form.get('font-size')?.value).toBe(16); // Should use default
        });

        it('should use initial values when provided for radio field', () => {
            const schema = createMockSchema();
            const initialValues = {
                alignment: 'right'
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('alignment')?.value).toBe('right');
        });

        it('should use initial values when provided for checkboxGroup field', () => {
            const schema = createMockSchema();
            const initialValues = {
                'text-decoration': {
                    underline: false,
                    overline: true
                }
            };

            const form = service.buildForm(schema, initialValues);
            const textDecorationGroup = form.get('text-decoration') as FormGroup;

            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(true);
        });

        it('should use initial values for all fields when provided', () => {
            const schema = createMockSchema();
            const initialValues = {
                'font-size': 20,
                'font-family': 'Helvetica',
                'text-decoration': {
                    underline: false,
                    overline: true
                },
                alignment: 'right'
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('font-size')?.value).toBe(20);
            expect(form.get('font-family')?.value).toBe('Helvetica');
            expect(form.get('alignment')?.value).toBe('right');

            const textDecorationGroup = form.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(true);
        });

        it('should use default values for fields not in initialValues', () => {
            const schema = createMockSchema();
            const initialValues = {
                'font-size': 20
                // Other fields should use defaults
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('font-size')?.value).toBe(20);
            expect(form.get('font-family')?.value).toBe('Arial');
            expect(form.get('alignment')?.value).toBe('left');

            const textDecorationGroup = form.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(true);
            expect(textDecorationGroup.get('overline')?.value).toBe(false);
        });

        it('should handle empty initialValues object', () => {
            const schema = createMockSchema();
            const initialValues = {};

            const form = service.buildForm(schema, initialValues);

            // Should use all defaults
            expect(form.get('font-size')?.value).toBe(16);
            expect(form.get('font-family')?.value).toBe('Arial');
            expect(form.get('alignment')?.value).toBe('left');
        });

        it('should handle partial checkboxGroup initial values', () => {
            const schema = createMockSchema();
            const initialValues = {
                'text-decoration': {
                    underline: false
                    // overline not provided, should use default
                }
            };

            const form = service.buildForm(schema, initialValues);
            const textDecorationGroup = form.get('text-decoration') as FormGroup;

            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(false); // Default from schema
        });
    });
});
