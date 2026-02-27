import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';

import {
    parseFieldValues,
    getQuickEditFields,
    isQuickEditSupportedField,
    QUICK_EDIT_SUPPORTED_FIELDS
} from './contentlet-form.utils';

describe('ContentletFormUtils', () => {
    describe('parseFieldValues', () => {
        it('should return empty array for undefined input', () => {
            const result = parseFieldValues(undefined);
            expect(result).toEqual([]);
        });

        it('should return empty array for empty string', () => {
            const result = parseFieldValues('');
            expect(result).toEqual([]);
        });

        it('should parse single label|value pair', () => {
            const input = 'Red|red';
            const result = parseFieldValues(input);

            expect(result).toEqual([{ label: 'Red', value: 'red' }]);
        });

        it('should parse multiple label|value pairs', () => {
            const input = 'Red|red\nBlue|blue\nGreen|green';
            const result = parseFieldValues(input);

            expect(result).toEqual([
                { label: 'Red', value: 'red' },
                { label: 'Blue', value: 'blue' },
                { label: 'Green', value: 'green' }
            ]);
        });

        it('should use label as value when pipe is missing', () => {
            const input = 'Red\nBlue\nGreen';
            const result = parseFieldValues(input);

            expect(result).toEqual([
                { label: 'Red', value: 'Red' },
                { label: 'Blue', value: 'Blue' },
                { label: 'Green', value: 'Green' }
            ]);
        });

        it('should handle mixed format (some with pipe, some without)', () => {
            const input = 'Red|red\nBlue\nGreen|green';
            const result = parseFieldValues(input);

            expect(result).toEqual([
                { label: 'Red', value: 'red' },
                { label: 'Blue', value: 'Blue' },
                { label: 'Green', value: 'green' }
            ]);
        });

        it('should trim whitespace from labels and values', () => {
            const input = '  Red  |  red  \n  Blue  |  blue  ';
            const result = parseFieldValues(input);

            expect(result).toEqual([
                { label: 'Red', value: 'red' },
                { label: 'Blue', value: 'blue' }
            ]);
        });

        it('should filter out empty lines', () => {
            const input = 'Red|red\n\nBlue|blue\n\n\nGreen|green';
            const result = parseFieldValues(input);

            expect(result).toEqual([
                { label: 'Red', value: 'red' },
                { label: 'Blue', value: 'blue' },
                { label: 'Green', value: 'green' }
            ]);
        });

        it('should handle empty label or value by using the other', () => {
            const input = '|red\nBlue|';
            const result = parseFieldValues(input);

            expect(result).toEqual([
                { label: 'red', value: 'red' },
                { label: 'Blue', value: 'Blue' }
            ]);
        });

        it('should handle completely empty lines with just pipe', () => {
            const input = '|\n||\n   |   ';
            const result = parseFieldValues(input);

            expect(result).toEqual([
                { label: '', value: '' },
                { label: '', value: '' },
                { label: '', value: '' }
            ]);
        });
    });

    describe('getQuickEditFields', () => {
        it('should return empty array for empty layout', () => {
            const layout: DotCMSContentTypeLayoutRow[] = [];
            const result = getQuickEditFields(layout);

            expect(result).toEqual([]);
        });

        it('should extract supported text field', () => {
            const layout: DotCMSContentTypeLayoutRow[] = [
                {
                    divider: null,
                    columns: [
                        {
                            columnDivider: null,
                            fields: [
                                {
                                    clazz: DotCMSClazzes.TEXT,
                                    name: 'Title',
                                    variable: 'title',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: true,
                                    values: ''
                                } as DotCMSContentTypeField
                            ]
                        }
                    ]
                }
            ];

            const result = getQuickEditFields(layout);

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({
                clazz: DotCMSClazzes.TEXT,
                name: 'Title',
                variable: 'title',
                regexCheck: '',
                dataType: 'TEXT',
                readOnly: false,
                required: true,
                values: ''
            });
        });

        it('should extract all supported field types', () => {
            const layout: DotCMSContentTypeLayoutRow[] = [
                {
                    divider: null,
                    columns: [
                        {
                            columnDivider: null,
                            fields: [
                                {
                                    clazz: DotCMSClazzes.TEXT,
                                    name: 'Text Field',
                                    variable: 'textField',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: ''
                                } as DotCMSContentTypeField,
                                {
                                    clazz: DotCMSClazzes.TEXTAREA,
                                    name: 'Textarea Field',
                                    variable: 'textareaField',
                                    regexCheck: '',
                                    dataType: 'LONG_TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: ''
                                } as DotCMSContentTypeField,
                                {
                                    clazz: DotCMSClazzes.CHECKBOX,
                                    name: 'Checkbox Field',
                                    variable: 'checkboxField',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: 'Option1|opt1\nOption2|opt2'
                                } as DotCMSContentTypeField,
                                {
                                    clazz: DotCMSClazzes.SELECT,
                                    name: 'Select Field',
                                    variable: 'selectField',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: 'A|a\nB|b'
                                } as DotCMSContentTypeField,
                                {
                                    clazz: DotCMSClazzes.RADIO,
                                    name: 'Radio Field',
                                    variable: 'radioField',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: 'Yes|yes\nNo|no'
                                } as DotCMSContentTypeField,
                                {
                                    clazz: DotCMSClazzes.MULTI_SELECT,
                                    name: 'Multi-Select Field',
                                    variable: 'multiSelectField',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: 'One|1\nTwo|2'
                                } as DotCMSContentTypeField
                            ]
                        }
                    ]
                }
            ];

            const result = getQuickEditFields(layout);

            expect(result).toHaveLength(6);
            expect(result.map((f) => f.clazz)).toEqual([
                DotCMSClazzes.TEXT,
                DotCMSClazzes.TEXTAREA,
                DotCMSClazzes.CHECKBOX,
                DotCMSClazzes.SELECT,
                DotCMSClazzes.RADIO,
                DotCMSClazzes.MULTI_SELECT
            ]);
        });

        it('should filter out unsupported field types', () => {
            const layout: DotCMSContentTypeLayoutRow[] = [
                {
                    divider: null,
                    columns: [
                        {
                            columnDivider: null,
                            fields: [
                                {
                                    clazz: DotCMSClazzes.TEXT,
                                    name: 'Title',
                                    variable: 'title',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: true,
                                    values: ''
                                } as DotCMSContentTypeField,
                                {
                                    clazz: DotCMSClazzes.IMAGE,
                                    name: 'Image',
                                    variable: 'image',
                                    regexCheck: '',
                                    dataType: 'SYSTEM',
                                    readOnly: false,
                                    required: false,
                                    values: ''
                                } as DotCMSContentTypeField,
                                {
                                    clazz: DotCMSClazzes.FILE,
                                    name: 'File',
                                    variable: 'file',
                                    regexCheck: '',
                                    dataType: 'SYSTEM',
                                    readOnly: false,
                                    required: false,
                                    values: ''
                                } as DotCMSContentTypeField
                            ]
                        }
                    ]
                }
            ];

            const result = getQuickEditFields(layout);

            // Should only include TEXT field, not IMAGE or FILE
            expect(result).toHaveLength(1);
            expect(result[0].clazz).toBe(DotCMSClazzes.TEXT);
        });

        it('should flatten nested structure correctly', () => {
            const layout: DotCMSContentTypeLayoutRow[] = [
                {
                    divider: null,
                    columns: [
                        {
                            columnDivider: null,
                            fields: [
                                {
                                    clazz: DotCMSClazzes.TEXT,
                                    name: 'Field 1',
                                    variable: 'field1',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: ''
                                } as DotCMSContentTypeField
                            ]
                        },
                        {
                            columnDivider: null,
                            fields: [
                                {
                                    clazz: DotCMSClazzes.TEXTAREA,
                                    name: 'Field 2',
                                    variable: 'field2',
                                    regexCheck: '',
                                    dataType: 'LONG_TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: ''
                                } as DotCMSContentTypeField
                            ]
                        }
                    ]
                },
                {
                    divider: null,
                    columns: [
                        {
                            columnDivider: null,
                            fields: [
                                {
                                    clazz: DotCMSClazzes.SELECT,
                                    name: 'Field 3',
                                    variable: 'field3',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: 'A|a'
                                } as DotCMSContentTypeField
                            ]
                        }
                    ]
                }
            ];

            const result = getQuickEditFields(layout);

            expect(result).toHaveLength(3);
            expect(result.map((f) => f.variable)).toEqual(['field1', 'field2', 'field3']);
        });

        it('should handle rows with null or undefined columns', () => {
            const layout: DotCMSContentTypeLayoutRow[] = [
                {
                    divider: null,
                    columns: null
                },
                {
                    divider: null,
                    columns: undefined
                },
                {
                    divider: null,
                    columns: [
                        {
                            columnDivider: null,
                            fields: [
                                {
                                    clazz: DotCMSClazzes.TEXT,
                                    name: 'Field',
                                    variable: 'field',
                                    regexCheck: '',
                                    dataType: 'TEXT',
                                    readOnly: false,
                                    required: false,
                                    values: ''
                                } as DotCMSContentTypeField
                            ]
                        }
                    ]
                }
            ];

            const result = getQuickEditFields(layout);

            expect(result).toHaveLength(1);
            expect(result[0].variable).toBe('field');
        });
    });

    describe('isQuickEditSupportedField', () => {
        it('should return true for TEXT field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.TEXT)).toBe(true);
        });

        it('should return true for TEXTAREA field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.TEXTAREA)).toBe(true);
        });

        it('should return true for CHECKBOX field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.CHECKBOX)).toBe(true);
        });

        it('should return true for SELECT field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.SELECT)).toBe(true);
        });

        it('should return true for RADIO field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.RADIO)).toBe(true);
        });

        it('should return true for MULTI_SELECT field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.MULTI_SELECT)).toBe(true);
        });

        it('should return false for IMAGE field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.IMAGE)).toBe(false);
        });

        it('should return false for FILE field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.FILE)).toBe(false);
        });

        it('should return false for BINARY field', () => {
            expect(isQuickEditSupportedField(DotCMSClazzes.BINARY)).toBe(false);
        });

        it('should return false for undefined', () => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            expect(isQuickEditSupportedField(undefined as any)).toBe(false);
        });
    });

    describe('QUICK_EDIT_SUPPORTED_FIELDS constant', () => {
        it('should contain exactly 6 supported field types', () => {
            expect(QUICK_EDIT_SUPPORTED_FIELDS).toHaveLength(6);
        });

        it('should contain all expected field types', () => {
            expect(QUICK_EDIT_SUPPORTED_FIELDS).toContain(DotCMSClazzes.TEXT);
            expect(QUICK_EDIT_SUPPORTED_FIELDS).toContain(DotCMSClazzes.TEXTAREA);
            expect(QUICK_EDIT_SUPPORTED_FIELDS).toContain(DotCMSClazzes.CHECKBOX);
            expect(QUICK_EDIT_SUPPORTED_FIELDS).toContain(DotCMSClazzes.SELECT);
            expect(QUICK_EDIT_SUPPORTED_FIELDS).toContain(DotCMSClazzes.RADIO);
            expect(QUICK_EDIT_SUPPORTED_FIELDS).toContain(DotCMSClazzes.MULTI_SELECT);
        });
    });
});
