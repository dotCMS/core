import { normalizeForm } from './internal';
import { styleEditorField, defineStyleEditorForm } from './public';
import { StyleEditorForm, StyleEditorFormSchema } from './types';

// Mock the internal normalizeForm function
jest.mock('./internal', () => ({
    normalizeForm: jest.fn()
}));

describe('styleEditorField', () => {
    describe('input', () => {
        it('should create an input field with number type and number defaultValue', () => {
            const config = {
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
        it('should create a dropdown field with string options', () => {
            const config = {
                label: 'Font Family',
                options: ['Arial', 'Helvetica', 'Times New Roman'],
                defaultValue: 'Arial',
                placeholder: 'Select a font'
            };

            const result = styleEditorField.dropdown(config);

            expect(result).toEqual({
                type: 'dropdown',
                ...config
            });
            expect(result.type).toBe('dropdown');
            expect(result.options).toEqual(['Arial', 'Helvetica', 'Times New Roman']);
            expect(result.defaultValue).toBe('Arial');
            expect(result.placeholder).toBe('Select a font');
        });

        it('should create a dropdown field with object options', () => {
            const config = {
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

        it('should create a dropdown field with mixed string and object options', () => {
            const config = {
                label: 'Font Family',
                options: [
                    'Arial',
                    { label: 'Helvetica Neue', value: 'helvetica-neue' },
                    'Times New Roman'
                ],
                defaultValue: 'Arial'
            };

            const result = styleEditorField.dropdown(config);

            expect(result.options).toEqual([
                'Arial',
                { label: 'Helvetica Neue', value: 'helvetica-neue' },
                'Times New Roman'
            ]);
        });

        it('should create a dropdown field without defaultValue', () => {
            const config = {
                label: 'Font Family',
                options: ['Arial', 'Helvetica']
            };

            const result = styleEditorField.dropdown(config);

            expect(result.defaultValue).toBeUndefined();
        });

        it('should create a dropdown field without placeholder', () => {
            const config = {
                label: 'Font Family',
                options: ['Arial', 'Helvetica'],
                defaultValue: 'Arial'
            };

            const result = styleEditorField.dropdown(config);

            expect(result.placeholder).toBeUndefined();
        });
    });

    describe('radio', () => {
        it('should create a radio field with string options', () => {
            const config = {
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
                label: 'Theme',
                options: [
                    {
                        label: 'Light',
                        value: 'light',
                        imageURL: 'https://example.com/light-theme.png',
                        width: 100,
                        height: 60
                    },
                    { label: 'Dark', value: 'dark' }
                ],
                defaultValue: 'light'
            };

            const result = styleEditorField.radio(config);

            expect(result.options[0]).toEqual({
                label: 'Light',
                value: 'light',
                imageURL: 'https://example.com/light-theme.png',
                width: 100,
                height: 60
            });
        });

        it('should create a radio field with mixed string and object options', () => {
            const config = {
                label: 'Theme',
                options: [
                    {
                        label: 'Light',
                        value: 'light',
                        imageURL: 'https://example.com/light-theme.png',
                        width: 100,
                        height: 60
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
                label: 'Alignment',
                options: ['Left', 'Center', 'Right']
            };

            const result = styleEditorField.radio(config);

            expect(result.defaultValue).toBeUndefined();
        });

        it('should handle options with only imageURL', () => {
            const config = {
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
            expect(result.options[0]).not.toHaveProperty('width');
            expect(result.options[0]).not.toHaveProperty('height');
        });
    });

    describe('checkboxGroup', () => {
        it('should create a checkbox group field with string options', () => {
            const config = {
                label: 'Text Decoration',
                options: ['Underline', 'Overline', 'Line Through'],
                defaultValue: {
                    Underline: true,
                    Overline: false,
                    'Line Through': false
                }
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result).toEqual({
                type: 'checkboxGroup',
                ...config
            });
            expect(result.type).toBe('checkboxGroup');
            expect(result.options).toEqual(['Underline', 'Overline', 'Line Through']);
            expect(result.defaultValue).toEqual({
                Underline: true,
                Overline: false,
                'Line Through': false
            });
        });

        it('should create a checkbox group field with object options', () => {
            const config = {
                label: 'Text Decoration',
                options: [
                    { label: 'Underline', value: 'underline' },
                    { label: 'Overline', value: 'overline' },
                    { label: 'Line Through', value: 'line-through' }
                ],
                defaultValue: {
                    underline: true,
                    overline: false,
                    'line-through': false
                }
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.options).toEqual([
                { label: 'Underline', value: 'underline' },
                { label: 'Overline', value: 'overline' },
                { label: 'Line Through', value: 'line-through' }
            ]);
            expect(result.defaultValue).toEqual({
                underline: true,
                overline: false,
                'line-through': false
            });
        });

        it('should create a checkbox group field with mixed string and object options', () => {
            const config = {
                label: 'Text Decoration',
                options: [
                    { label: 'Underline', value: 'underline' },
                    'Bold',
                    { label: 'Italic', value: 'italic' }
                ],
                defaultValue: {
                    underline: true,
                    Bold: false,
                    italic: true
                }
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.options).toHaveLength(3);
            expect(result.defaultValue).toEqual({
                underline: true,
                Bold: false,
                italic: true
            });
        });

        it('should create a checkbox group field without defaultValue', () => {
            const config = {
                label: 'Text Decoration',
                options: [
                    { label: 'Underline', value: 'underline' },
                    { label: 'Overline', value: 'overline' }
                ]
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.defaultValue).toBeUndefined();
        });

        it('should handle empty defaultValue object', () => {
            const config = {
                label: 'Text Decoration',
                options: [{ label: 'Underline', value: 'underline' }],
                defaultValue: {}
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.defaultValue).toEqual({});
        });

        it('should handle defaultValue with all options checked', () => {
            const config = {
                label: 'Text Decoration',
                options: [
                    { label: 'Underline', value: 'underline' },
                    { label: 'Overline', value: 'overline' }
                ],
                defaultValue: {
                    underline: true,
                    overline: true
                }
            };

            const result = styleEditorField.checkboxGroup(config);

            expect(result.defaultValue).toEqual({
                underline: true,
                overline: true
            });
        });
    });
});

describe('defineStyleEditorForm', () => {
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
                            label: 'Font Size',
                            inputType: 'number',
                            defaultValue: 16
                        })
                    ]
                }
            ]
        };

        const result = defineStyleEditorForm(form);

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
                            label: 'Primary Color',
                            options: [{ label: 'Red', value: 'red' }]
                        })
                    ]
                }
            ]
        };

        const result = defineStyleEditorForm(form);

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
                            label: 'Width',
                            config: { inputType: 'number' }
                        },

                        {
                            type: 'input',
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
                            label: 'Width',
                            inputType: 'number'
                        }),

                        styleEditorField.input({
                            label: 'Height',
                            inputType: 'number'
                        })
                    ]
                }
            ]
        };

        const result = defineStyleEditorForm(form);

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

        const result = defineStyleEditorForm(form);

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

        const result = defineStyleEditorForm(form);

        expect(result.contentType).toBe('custom-content-type');
    });
});
