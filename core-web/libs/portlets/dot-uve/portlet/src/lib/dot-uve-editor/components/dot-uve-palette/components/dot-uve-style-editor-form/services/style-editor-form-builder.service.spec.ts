import { TestBed } from '@angular/core/testing';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { StyleEditorFormSchema } from '@dotcms/uve';

import { StyleEditorFormBuilderService } from './style-editor-form-builder.service';

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
                        inputType: 'number'
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
                        ]
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
                        ]
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
                        ]
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
        it('should build form with empty values when initialValues is not provided', () => {
            const schema = createMockSchema();
            const form = service.buildForm(schema);

            expect(form).toBeInstanceOf(FormGroup);
            expect(form.get('font-size')?.value).toBeNull();
            expect(form.get('font-family')?.value).toBeNull();
            expect(form.get('alignment')?.value).toBeNull();

            const textDecorationGroup = form.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(false);
        });

        it('should use initial values when provided for input field', () => {
            const schema = createMockSchema();
            const initialValues = {
                'font-size': 24
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('font-size')?.value).toBe(24);
            expect(form.get('font-family')?.value).toBeNull(); // Should be empty when no initial value
        });

        it('should use initial values when provided for dropdown field', () => {
            const schema = createMockSchema();
            const initialValues = {
                'font-family': 'Helvetica'
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('font-family')?.value).toBe('Helvetica');
            expect(form.get('font-size')?.value).toBeNull(); // Should be empty when no initial value
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

        it('should use empty values for fields not in initialValues', () => {
            const schema = createMockSchema();
            const initialValues = {
                'font-size': 20
                // Other fields should be empty
            };

            const form = service.buildForm(schema, initialValues);

            expect(form.get('font-size')?.value).toBe(20);
            expect(form.get('font-family')?.value).toBeNull();
            expect(form.get('alignment')?.value).toBeNull();

            const textDecorationGroup = form.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(false);
        });

        it('should handle empty initialValues object', () => {
            const schema = createMockSchema();
            const initialValues = {};

            const form = service.buildForm(schema, initialValues);

            // Should all be empty
            expect(form.get('font-size')?.value).toBeNull();
            expect(form.get('font-family')?.value).toBeNull();
            expect(form.get('alignment')?.value).toBeNull();

            const textDecorationGroup = form.get('text-decoration') as FormGroup;
            expect(textDecorationGroup.get('underline')?.value).toBe(false);
            expect(textDecorationGroup.get('overline')?.value).toBe(false);
        });

        it('should handle partial checkboxGroup initial values', () => {
            const schema = createMockSchema();
            const initialValues = {
                'text-decoration': {
                    underline: true
                    // overline not provided, should be false
                }
            };

            const form = service.buildForm(schema, initialValues);
            const textDecorationGroup = form.get('text-decoration') as FormGroup;

            expect(textDecorationGroup.get('underline')?.value).toBe(true);
            expect(textDecorationGroup.get('overline')?.value).toBe(false);
        });
    });
});
