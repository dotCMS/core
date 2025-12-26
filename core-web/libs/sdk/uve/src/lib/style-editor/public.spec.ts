import { normalizeForm } from './internal';
import { styleEditorField, defineStyleEditorSchema } from './public';
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
                placeholder: 'Enter font size',
                defaultValue: 16
            };

            const result = styleEditorField.input(config);

            expect(result).toEqual({
                type: 'input',
                ...config
            });
            expect(result.type).toBe('input');
            expect(result.inputType).toBe('number');
            expect(result.defaultValue).toBe(16);
        });

        it('should create an input field with text type and string defaultValue', () => {
            const config = {
                id: 'font-name',
                label: 'Font Name',
                inputType: 'text' as const,
                placeholder: 'Enter font name',
                defaultValue: 'Arial'
            };

            const result = styleEditorField.input(config);

            expect(result).toEqual({
                type: 'input',
                ...config
            });
            expect(result.type).toBe('input');
            expect(result.inputType).toBe('text');
            expect(result.defaultValue).toBe('Arial');
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
            expect(result.defaultValue).toBeUndefined();
        });

        it('should create an input field without placeholder', () => {
            const config = {
                id: 'font-size',
                label: 'Font Size',
                inputType: 'number' as const,
                defaultValue: 16
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
                placeholder: 'Custom placeholder',
                defaultValue: 'Custom value'
            };

            const result = styleEditorField.input(config);

            expect(result.label).toBe('Custom Field');
            expect(result.inputType).toBe('text');
            expect(result.placeholder).toBe('Custom placeholder');
            expect(result.defaultValue).toBe('Custom value');
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
                ],
                defaultValue: 'light'
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

        it('should create a dropdown field without defaultValue', () => {
            const config = {
                id: 'font-family',
                label: 'Font Family',
                options: [
                    { label: 'Arial', value: 'arial' },
                    { label: 'Helvetica', value: 'helvetica' }
                ]
            };

            const result = styleEditorField.dropdown(config);

            expect(result.defaultValue).toBeUndefined();
        });
    });

    describe('radio', () => {
        it('should create a radio field with string options', () => {
            const config = {
                id: 'alignment',
                label: 'Alignment',
                options: ['Left', 'Center', 'Right'],
                defaultValue: 'Left'
            };

            const result = styleEditorField.radio(config);

            expect(result).toEqual({
                type: 'radio',
                ...config
            });
            expect(result.type).toBe('radio');
            expect(result.options).toEqual(['Left', 'Center', 'Right']);
            expect(result.defaultValue).toBe('Left');
        });

        it('should create a radio field with object options', () => {
            const config = {
                id: 'theme',
                label: 'Theme',
                options: [
                    { label: 'Light', value: 'light' },
                    { label: 'Dark', value: 'dark' }
                ],
                defaultValue: 'light'
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
                ],
                defaultValue: 'light'
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
                    { label: 'Dark', value: 'dark' },
                    'Auto'
                ],
                defaultValue: 'light'
            };

            const result = styleEditorField.radio(config);

            expect(result.options).toHaveLength(3);
            expect(result.options[0]).toHaveProperty('imageURL');
            expect(result.options[2]).toBe('Auto');
        });

        it('should create a radio field without defaultValue', () => {
            const config = {
                id: 'alignment',
                label: 'Alignment',
                options: ['Left', 'Center', 'Right']
            };

            const result = styleEditorField.radio(config);

            expect(result.defaultValue).toBeUndefined();
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
                ],
                defaultValue: 'light'
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
                    { label: 'Underline', key: 'underline', value: true },
                    { label: 'Overline', key: 'overline', value: false },
                    { label: 'Line Through', key: 'line-through', value: false }
                ]
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.options).toEqual([
                { label: 'Underline', key: 'underline', value: true },
                { label: 'Overline', key: 'overline', value: false },
                { label: 'Line Through', key: 'line-through', value: false }
            ]);
            expect(result.type).toBe('checkboxGroup');
        });

        it('should create a checkbox group field with all options checked', () => {
            const config = {
                id: 'text-decoration',
                label: 'Text Decoration',
                options: [
                    { label: 'Underline', key: 'underline', value: true },
                    { label: 'Overline', key: 'overline', value: true }
                ]
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.options).toEqual([
                { label: 'Underline', key: 'underline', value: true },
                { label: 'Overline', key: 'overline', value: true }
            ]);
        });

        it('should create a checkbox group field with all options unchecked', () => {
            const config = {
                id: 'text-decoration',
                label: 'Text Decoration',
                options: [
                    { label: 'Underline', key: 'underline', value: false },
                    { label: 'Overline', key: 'overline', value: false }
                ]
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.options).toEqual([
                { label: 'Underline', key: 'underline', value: false },
                { label: 'Overline', key: 'overline', value: false }
            ]);
        });

        it('should create a checkbox group field with mixed checked states', () => {
            const config = {
                id: 'type-settings',
                label: 'Type settings',
                options: [
                    { label: 'Bold', key: 'bold', value: true },
                    { label: 'Italic', key: 'italic', value: false },
                    { label: 'Underline', key: 'underline', value: true },
                    { label: 'Strikethrough', key: 'strikethrough', value: false }
                ]
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.options).toEqual([
                { label: 'Bold', key: 'bold', value: true },
                { label: 'Italic', key: 'italic', value: false },
                { label: 'Underline', key: 'underline', value: true },
                { label: 'Strikethrough', key: 'strikethrough', value: false }
            ]);
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
                                inputType: 'number',
                                defaultValue: 16
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
                            inputType: 'number',
                            defaultValue: 16
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
                            config: { inputType: 'number', defaultValue: 16 }
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
                            inputType: 'number',
                            defaultValue: 16
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
