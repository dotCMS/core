import { normalizeForm } from './internal';
import { defineStyleEditorSchema, styleEditorField } from './public';
import { StyleEditorForm, StyleEditorFormSchema } from './types';

// Mock the internal normalizeForm function
jest.mock('./internal', () => ({
    normalizeForm: jest.fn()
}));

describe('styleEditorField', () => {
    describe('input', () => {
        it('should create an input field with number type and number defaultValue', () => {
            const config = {
                id: 'font-size',
                label: 'Font Size',
                inputType: 'number' as const,
                placeholder: 'Enter font size'
            };

            const result = styleEditorField.input(config);

            expect(result).toEqual({
                type: 'input',
                ...config
            });
            expect(result.type).toBe('input');
            expect(result.inputType).toBe('number');
        });

        it('should create an input field with text type and string defaultValue', () => {
            const config = {
                id: 'font-name',
                label: 'Font Name',
                inputType: 'text' as const,
                placeholder: 'Enter font name'
            };

            const result = styleEditorField.input(config);

            expect(result).toEqual({
                type: 'input',
                ...config
            });
            expect(result.type).toBe('input');
            expect(result.inputType).toBe('text');
        });

        it('should create an input field without defaultValue', () => {
            const config = {
                id: 'font-size',
                label: 'Font Size',
                inputType: 'number' as const,
                placeholder: 'Enter font size'
            };

            const result = styleEditorField.input(config);

            expect(result).toEqual({
                type: 'input',
                ...config
            });
        });

        it('should create an input field without placeholder', () => {
            const config = {
                id: 'font-size',
                label: 'Font Size',
                inputType: 'number' as const
            };

            const result = styleEditorField.input(config);

            expect(result).toEqual({
                type: 'input',
                ...config
            });
            expect(result.placeholder).toBeUndefined();
        });

        it('should preserve all properties from config', () => {
            const config = {
                id: 'custom-field',
                label: 'Custom Field',
                inputType: 'text' as const,
                placeholder: 'Custom placeholder'
            };

            const result = styleEditorField.input(config);

            expect(result.label).toBe('Custom Field');
            expect(result.inputType).toBe('text');
            expect(result.placeholder).toBe('Custom placeholder');
        });
    });

    describe('dropdown', () => {
        it('should create a dropdown field with object options', () => {
            const config = {
                id: 'theme',
                label: 'Theme',
                options: [
                    { label: 'Light Theme', value: 'light' },
                    { label: 'Dark Theme', value: 'dark' }
                ]
            };

            const result = styleEditorField.dropdown(config);

            expect(result).toEqual({
                type: 'dropdown',
                ...config
            });
            expect(result.type).toBe('dropdown');
            expect(result.options).toEqual([
                { label: 'Light Theme', value: 'light' },
                { label: 'Dark Theme', value: 'dark' }
            ]);
        });
    });

    describe('radio', () => {
        it('should create a radio field with string options', () => {
            const config = {
                id: 'alignment',
                label: 'Alignment',
                options: [
                    { label: 'Left', value: 'left' },
                    { label: 'Center', value: 'center' },
                    { label: 'Right', value: 'right' }
                ]
            };

            const result = styleEditorField.radio(config);

            expect(result).toEqual({
                type: 'radio',
                ...config
            });
            expect(result.type).toBe('radio');
            expect(result.options).toEqual([
                { label: 'Left', value: 'left' },
                { label: 'Center', value: 'center' },
                { label: 'Right', value: 'right' }
            ]);
        });

        it('should create a radio field with object options', () => {
            const config = {
                id: 'theme',
                label: 'Theme',
                options: [
                    { label: 'Light', value: 'light' },
                    { label: 'Dark', value: 'dark' }
                ]
            };

            const result = styleEditorField.radio(config);

            expect(result.options).toEqual([
                { label: 'Light', value: 'light' },
                { label: 'Dark', value: 'dark' }
            ]);
        });

        it('should create a radio field with options including images', () => {
            const config = {
                id: 'theme',
                label: 'Theme',
                options: [
                    {
                        label: 'Light',
                        value: 'light',
                        imageURL: 'https://example.com/light-theme.png'
                    },
                    { label: 'Dark', value: 'dark' }
                ]
            };

            const result = styleEditorField.radio(config);

            expect(result.options[0]).toEqual({
                label: 'Light',
                value: 'light',
                imageURL: 'https://example.com/light-theme.png'
            });
        });

        it('should create a radio field with mixed string and object options', () => {
            const config = {
                id: 'theme',
                label: 'Theme',
                options: [
                    {
                        label: 'Light',
                        value: 'light',
                        imageURL: 'https://example.com/light-theme.png'
                    },
                    { label: 'Dark', value: 'dark' }
                ]
            };

            const result = styleEditorField.radio(config);

            expect(result.options).toHaveLength(2);
            expect(result.options[0]).toHaveProperty('imageURL');
            expect(result.options[1]).toEqual({ label: 'Dark', value: 'dark' });
        });

        it('should handle options with only imageURL', () => {
            const config = {
                id: 'theme',
                label: 'Theme',
                options: [
                    {
                        label: 'Light',
                        value: 'light',
                        imageURL: 'https://example.com/light.png'
                    }
                ]
            };

            const result = styleEditorField.radio(config);

            expect(result.options[0]).toEqual({
                label: 'Light',
                value: 'light',
                imageURL: 'https://example.com/light.png'
            });
        });
    });

    describe('checkboxGroup', () => {
        it('should create a checkbox group field with new option structure', () => {
            const config = {
                id: 'text-decoration',
                label: 'Text Decoration',
                options: [
                    { label: 'Underline', key: 'underline' },
                    { label: 'Overline', key: 'overline' },
                    { label: 'Line Through', key: 'line-through' }
                ]
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.options).toEqual([
                { label: 'Underline', key: 'underline' },
                { label: 'Overline', key: 'overline' },
                { label: 'Line Through', key: 'line-through' }
            ]);
            expect(result.type).toBe('checkboxGroup');
        });
    });
});

describe('defineStyleEditorSchema', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('should normalize a form with single column section', () => {
        const mockSchema: StyleEditorFormSchema = {
            contentType: 'test-content-type',
            sections: [
                {
                    title: 'Typography',
                    fields: [
                        {
                            type: 'input',
                            id: 'font-size',
                            label: 'Font Size',
                            config: {
                                inputType: 'number'
                            }
                        }
                    ]
                }
            ]
        };

        (normalizeForm as jest.Mock).mockReturnValue(mockSchema);

        const form: StyleEditorForm = {
            contentType: 'test-content-type',
            sections: [
                {
                    title: 'Typography',
                    fields: [
                        styleEditorField.input({
                            id: 'font-size',
                            label: 'Font Size',
                            inputType: 'number'
                        })
                    ]
                }
            ]
        };

        const result = defineStyleEditorSchema(form);

        expect(normalizeForm).toHaveBeenCalledWith(form);
        expect(result).toEqual(mockSchema);
    });

    it('should normalize a form with multiple sections', () => {
        const mockSchema: StyleEditorFormSchema = {
            contentType: 'test-content-type',
            sections: [
                {
                    title: 'Typography',

                    fields: [
                        {
                            type: 'input',
                            id: 'font-size',
                            label: 'Font Size',
                            config: { inputType: 'number' }
                        }
                    ]
                },
                {
                    title: 'Colors',

                    fields: [
                        {
                            type: 'dropdown',
                            id: 'primary-color',
                            label: 'Primary Color',
                            config: { options: [{ label: 'Red', value: 'red' }] }
                        }
                    ]
                }
            ]
        };

        (normalizeForm as jest.Mock).mockReturnValue(mockSchema);

        const form: StyleEditorForm = {
            contentType: 'test-content-type',
            sections: [
                {
                    title: 'Typography',
                    fields: [
                        styleEditorField.input({
                            id: 'font-size',
                            label: 'Font Size',
                            inputType: 'number'
                        })
                    ]
                },
                {
                    title: 'Colors',
                    fields: [
                        styleEditorField.dropdown({
                            id: 'primary-color',
                            label: 'Primary Color',
                            options: [{ label: 'Red', value: 'red' }]
                        })
                    ]
                }
            ]
        };

        const result = defineStyleEditorSchema(form);

        expect(normalizeForm).toHaveBeenCalledWith(form);
        expect(result).toEqual(mockSchema);
    });

    it('should normalize a form with multi-column section', () => {
        const mockSchema: StyleEditorFormSchema = {
            contentType: 'test-content-type',
            sections: [
                {
                    title: 'Layout',

                    fields: [
                        {
                            type: 'input',
                            id: 'width',
                            label: 'Width',
                            config: { inputType: 'number' }
                        },

                        {
                            type: 'input',
                            id: 'height',
                            label: 'Height',
                            config: { inputType: 'number' }
                        }
                    ]
                }
            ]
        };

        (normalizeForm as jest.Mock).mockReturnValue(mockSchema);

        const form: StyleEditorForm = {
            contentType: 'test-content-type',
            sections: [
                {
                    title: 'Layout',

                    fields: [
                        styleEditorField.input({
                            id: 'width',
                            label: 'Width',
                            inputType: 'number'
                        }),

                        styleEditorField.input({
                            id: 'height',
                            label: 'Height',
                            inputType: 'number'
                        })
                    ]
                }
            ]
        };

        const result = defineStyleEditorSchema(form);

        expect(normalizeForm).toHaveBeenCalledWith(form);
        expect(result).toEqual(mockSchema);
    });

    it('should handle empty sections array', () => {
        const mockSchema: StyleEditorFormSchema = {
            contentType: 'test-content-type',
            sections: []
        };

        (normalizeForm as jest.Mock).mockReturnValue(mockSchema);

        const form: StyleEditorForm = {
            contentType: 'test-content-type',
            sections: []
        };

        const result = defineStyleEditorSchema(form);

        expect(normalizeForm).toHaveBeenCalledWith(form);
        expect(result).toEqual(mockSchema);
    });

    it('should preserve contentType in the result', () => {
        const mockSchema: StyleEditorFormSchema = {
            contentType: 'custom-content-type',
            sections: []
        };

        (normalizeForm as jest.Mock).mockReturnValue(mockSchema);

        const form: StyleEditorForm = {
            contentType: 'custom-content-type',
            sections: []
        };

        const result = defineStyleEditorSchema(form);

        expect(result.contentType).toBe('custom-content-type');
    });
});
