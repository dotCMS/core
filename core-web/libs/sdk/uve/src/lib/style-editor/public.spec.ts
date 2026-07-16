import { styleEditorField } from './public';

describe('styleEditorField', () => {
    describe('input', () => {
        it('should create an input field with number type', () => {
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

        it('should create an input field with text type', () => {
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
        it('should create a radio field with object options', () => {
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

        it('should preserve columns config', () => {
            const config = {
                id: 'layout',
                label: 'Layout',
                columns: 2 as const,
                options: [
                    { label: 'Left', value: 'left' },
                    { label: 'Right', value: 'right' }
                ]
            };

            const result = styleEditorField.radio(config);

            expect(result.columns).toBe(2);
        });
    });

    describe('checkboxGroup', () => {
        it('should create a checkbox group field', () => {
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
