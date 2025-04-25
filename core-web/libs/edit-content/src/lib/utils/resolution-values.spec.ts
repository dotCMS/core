import { DotCMSDataTypes, DotCMSFieldTypes } from '@dotcms/dotcms-models';
import {
    createFakeCategoryField,
    createFakeContentlet,
    createFakeHostFolderField,
    createFakeLineDividerField,
    createFakeRelationshipField,
    createFakeJSONField,
    createFakeDateField,
    createFakeDateTimeField,
    createFakeTimeField,
    createFakeSelectField,
    createFakeRadioField,
    createFakeCheckboxField,
    createFakeMultiSelectField,
    createFakeTagField,
    createFakeTextField,
    createFakeRowField,
    createFakeColumnField,
    createFakeTabDividerField,
    createFakeColumnBreakField
} from '@dotcms/utils-testing';

import { resolutionValue } from './resolution-values.utils';

describe('Utils', () => {
    describe('resolutionValue', () => {
        // Host Folder Tests
        describe('Host Folder Resolution', () => {
            it('should return hostName when hostName and url exist', () => {
                const contentlet = createFakeContentlet({
                    hostName: 'demo.dotcms.com',
                    url: '/content/generic/index'
                });
                const field = createFakeHostFolderField({ defaultValue: '' });

                expect(resolutionValue[DotCMSFieldTypes.HOST_FOLDER](contentlet, field)).toBe(
                    'demo.dotcms.com'
                );
            });

            it('should return default value when hostName or url is missing', () => {
                const contentlet = createFakeContentlet({ hostName: null, url: null });
                const field = createFakeHostFolderField({ defaultValue: 'default' });

                expect(resolutionValue[DotCMSFieldTypes.HOST_FOLDER](contentlet, field)).toBe(
                    'default'
                );
            });

            it('should return empty string when no default value and no path', () => {
                const contentlet = createFakeContentlet({
                    hostName: null,
                    url: null
                });
                const field = createFakeHostFolderField({ defaultValue: null });

                expect(resolutionValue[DotCMSFieldTypes.HOST_FOLDER](contentlet, field)).toBe('');
            });
        });

        // Category Tests
        describe('Category Resolution', () => {
            it('should return array of category keys', () => {
                const contentlet = createFakeContentlet({
                    categories: [{ key1: 'value1' }, { key2: 'value2' }]
                });
                const field = createFakeCategoryField({ variable: 'categories' });

                expect(resolutionValue[DotCMSFieldTypes.CATEGORY](contentlet, field)).toEqual([
                    'key1',
                    'key2'
                ]);
            });

            it('should return default value when no categories exist', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeCategoryField({ defaultValue: '[]' });

                expect(resolutionValue[DotCMSFieldTypes.CATEGORY](contentlet, field)).toEqual('[]');
            });

            it('should handle non-array values gracefully', () => {
                const contentlet = createFakeContentlet({
                    categories: 'invalid'
                });
                const field = createFakeCategoryField({
                    variable: 'categories',
                    defaultValue: null
                });

                expect(resolutionValue[DotCMSFieldTypes.CATEGORY](contentlet, field)).toEqual([]);
            });
        });

        // Relationship Tests
        describe('Relationship Resolution', () => {
            it('should return comma-separated identifiers', () => {
                const contentlet = createFakeContentlet({
                    relationship: [{ identifier: 'id1' }, { identifier: 'id2' }]
                });
                const field = createFakeRelationshipField({ variable: 'relationship' });

                expect(resolutionValue[DotCMSFieldTypes.RELATIONSHIP](contentlet, field)).toBe(
                    'id1,id2'
                );
            });

            it('should handle empty relationships', () => {
                const contentlet = createFakeContentlet({
                    relationship: []
                });
                const field = createFakeRelationshipField({ variable: 'relationship' });

                const result = resolutionValue[DotCMSFieldTypes.RELATIONSHIP](contentlet, field);
                expect(result).toBe('');
            });
        });

        // Line Divider Tests
        describe('Line Divider Resolution', () => {
            it('should always return empty string', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeLineDividerField();

                expect(resolutionValue[DotCMSFieldTypes.LINE_DIVIDER](contentlet, field)).toBe('');
            });
        });

        // JSON Tests
        describe('JSON Resolution', () => {
            it('should return stringified JSON when value is an object', () => {
                const jsonData = { name: 'John', age: 30 };
                const contentlet = createFakeContentlet({
                    jsonField: jsonData
                });
                const field = createFakeJSONField({ variable: 'jsonField' });

                expect(resolutionValue[DotCMSFieldTypes.JSON](contentlet, field)).toBe(
                    JSON.stringify(jsonData, null, 2)
                );
            });

            it('should return original value when not an object', () => {
                const contentlet = createFakeContentlet({
                    jsonField: 'not-json'
                });
                const field = createFakeJSONField({ variable: 'jsonField' });

                expect(resolutionValue[DotCMSFieldTypes.JSON](contentlet, field)).toBe('not-json');
            });

            it('should return null for null values', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeJSONField({
                    variable: 'missingJsonField',
                    defaultValue: null
                });

                const result = resolutionValue[DotCMSFieldTypes.JSON](contentlet, field);
                expect(result).toBeNull();
            });
        });

        // Date Tests
        describe('Date Resolution', () => {
            it('should parse date strings correctly', () => {
                const dateStr = '2023-05-15';
                const contentlet = createFakeContentlet({
                    dateField: dateStr
                });
                const field = createFakeDateField({ variable: 'dateField' });

                const result = resolutionValue[DotCMSFieldTypes.DATE](contentlet, field) as Date;
                expect(result).toBeInstanceOf(Date);
                expect(result.toISOString().split('T')[0]).toBe(dateStr);
            });

            it('should return null for null/undefined values', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeDateField({
                    variable: 'missingDateField',
                    defaultValue: null
                });

                expect(resolutionValue[DotCMSFieldTypes.DATE](contentlet, field)).toBeNull();
            });

            it('should use default value when field value is missing', () => {
                const defaultDate = '2023-01-01';
                const contentlet = createFakeContentlet();
                const field = createFakeDateField({
                    variable: 'dateField',
                    defaultValue: defaultDate
                });

                const result = resolutionValue[DotCMSFieldTypes.DATE](contentlet, field) as Date;
                expect(result).toBeInstanceOf(Date);
                expect(result.toISOString().split('T')[0]).toBe(defaultDate);
            });

            it('should return today for invalid date strings', () => {
                const contentlet = createFakeContentlet({
                    dateField: 'invalid-date'
                });
                const field = createFakeDateField({ variable: 'dateField' });

                const result = resolutionValue[DotCMSFieldTypes.DATE](contentlet, field) as Date;
                expect(result).toBeInstanceOf(Date);

                // Check if date is today (within 1 day)
                const today = new Date();
                const diff = Math.abs(result.getTime() - today.getTime());
                expect(diff).toBeLessThan(24 * 60 * 60 * 1000);
            });
        });

        // DateTime Tests
        describe('DateTime Resolution', () => {
            it('should parse datetime strings correctly', () => {
                const dateTimeStr = '2021-09-01T18:00:00.000Z';
                const expectedDate = new Date(dateTimeStr).toDateString();
                const contentlet = createFakeContentlet({
                    dateTimeField: dateTimeStr
                });
                const field = createFakeDateTimeField({ variable: 'dateTimeField' });

                const result = resolutionValue[DotCMSFieldTypes.DATE_AND_TIME](
                    contentlet,
                    field
                ) as Date;
                expect(result).toBeInstanceOf(Date);
                expect(result.toDateString()).toEqual(expectedDate);
            });

            it("should return Date.now if the value is 'now'", () => {
                const value = 'now';
                const expectedDate = new Date().toDateString();
                const contentlet = createFakeContentlet({
                    dateTimeField: value
                });
                const field = createFakeDateTimeField({ variable: 'dateTimeField' });

                const result = resolutionValue[DotCMSFieldTypes.DATE_AND_TIME](
                    contentlet,
                    field
                ) as Date;
                expect(result.toDateString()).toEqual(expectedDate);
            });

            it('should return null for null/undefined values', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeDateTimeField({
                    variable: 'missingDateTimeField',
                    defaultValue: null
                });

                expect(
                    resolutionValue[DotCMSFieldTypes.DATE_AND_TIME](contentlet, field)
                ).toBeNull();
            });
        });

        // Time Tests
        describe('Time Resolution', () => {
            it('should parse time strings correctly', () => {
                const timeStr = '14:30:00';
                const contentlet = createFakeContentlet({
                    timeField: timeStr
                });
                const field = createFakeTimeField({ variable: 'timeField' });

                const result = resolutionValue[DotCMSFieldTypes.TIME](contentlet, field);
                expect(result).toBeInstanceOf(Date);
            });

            it('should return null for null/undefined values', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeTimeField({
                    variable: 'missingTimeField',
                    defaultValue: null
                });

                expect(resolutionValue[DotCMSFieldTypes.TIME](contentlet, field)).toBeNull();
            });
        });

        // Select/Radio Tests
        describe('Select/Radio Resolution', () => {
            it('should convert to boolean for boolean dataType', () => {
                const contentlet = createFakeContentlet({
                    selectField: 'true'
                });
                const field = createFakeSelectField({
                    variable: 'selectField',
                    dataType: DotCMSDataTypes.BOOLEAN
                });

                expect(resolutionValue[DotCMSFieldTypes.SELECT](contentlet, field)).toBe(true);
            });

            it('should handle actual boolean values', () => {
                const contentlet = createFakeContentlet({
                    selectField: true
                });
                const field = createFakeSelectField({
                    variable: 'selectField',
                    dataType: DotCMSDataTypes.BOOLEAN
                });

                expect(resolutionValue[DotCMSFieldTypes.SELECT](contentlet, field)).toBe(true);
            });

            it('should convert to number for float/integer dataType', () => {
                const contentlet = createFakeContentlet({
                    selectField: '42'
                });
                const field = createFakeSelectField({
                    variable: 'selectField',
                    dataType: DotCMSDataTypes.INTEGER
                });

                expect(resolutionValue[DotCMSFieldTypes.SELECT](contentlet, field)).toBe(42);
            });

            it('should return null for empty string values', () => {
                const contentlet = createFakeContentlet({
                    selectField: ''
                });
                const field = createFakeSelectField({ variable: 'selectField' });

                expect(resolutionValue[DotCMSFieldTypes.SELECT](contentlet, field)).toBeNull();
            });

            it('should return null for invalid number values', () => {
                const contentlet = createFakeContentlet({
                    selectField: 'not-a-number'
                });
                const field = createFakeSelectField({
                    variable: 'selectField',
                    dataType: DotCMSDataTypes.FLOAT
                });

                expect(resolutionValue[DotCMSFieldTypes.SELECT](contentlet, field)).toBeNull();
            });

            it('should return string value for other dataTypes', () => {
                const contentlet = createFakeContentlet({
                    selectField: 'option1'
                });
                const field = createFakeSelectField({
                    variable: 'selectField',
                    dataType: DotCMSDataTypes.TEXT
                });

                expect(resolutionValue[DotCMSFieldTypes.SELECT](contentlet, field)).toBe('option1');
            });

            it('should work with Radio field type', () => {
                const contentlet = createFakeContentlet({
                    radioField: 'option1'
                });
                const field = createFakeRadioField({ variable: 'radioField' });

                expect(resolutionValue[DotCMSFieldTypes.RADIO](contentlet, field)).toBe('option1');
            });
        });

        // Flattened Field Tests (Checkbox, MultiSelect, Tag)
        describe('Flattened Field Resolution', () => {
            it('should handle array values', () => {
                const contentlet = createFakeContentlet({
                    checkboxField: ['option1', 'option2 ']
                });
                const field = createFakeCheckboxField({ variable: 'checkboxField' });

                expect(resolutionValue[DotCMSFieldTypes.CHECKBOX](contentlet, field)).toEqual([
                    'option1',
                    'option2'
                ]);
            });

            it('should handle comma-separated string values', () => {
                const contentlet = createFakeContentlet({
                    multiSelectField: 'option1,option2 , option3'
                });
                const field = createFakeMultiSelectField({ variable: 'multiSelectField' });

                expect(resolutionValue[DotCMSFieldTypes.MULTI_SELECT](contentlet, field)).toEqual([
                    'option1',
                    'option2',
                    'option3'
                ]);
            });

            it('should return empty array for null/undefined values', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeTagField({
                    variable: 'missingTagField',
                    defaultValue: null
                });

                expect(resolutionValue[DotCMSFieldTypes.TAG](contentlet, field)).toEqual([]);
            });

            it('should handle empty string', () => {
                const contentlet = createFakeContentlet({
                    tagField: ''
                });
                const field = createFakeTagField({ variable: 'tagField' });

                expect(resolutionValue[DotCMSFieldTypes.TAG](contentlet, field)).toEqual([]);
            });
        });

        // Generic Value Resolution Tests
        describe('Generic Value Resolution', () => {
            it('should return field value when present', () => {
                const contentlet = createFakeContentlet({
                    textField: 'Hello World'
                });
                const field = createFakeTextField({ variable: 'textField' });

                expect(resolutionValue[DotCMSFieldTypes.TEXT](contentlet, field)).toBe(
                    'Hello World'
                );
            });

            it('should return default value when field value is missing', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeTextField({
                    variable: 'textField',
                    defaultValue: 'Default Text'
                });

                expect(resolutionValue[DotCMSFieldTypes.TEXT](contentlet, field)).toBe(
                    'Default Text'
                );
            });

            it('should return null when field value and default value are missing', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeTextField({
                    variable: 'missingField',
                    defaultValue: null
                });

                expect(resolutionValue[DotCMSFieldTypes.TEXT](contentlet, field)).toBeNull();
            });
        });

        // Layout Field Tests
        describe('Layout Field Resolution', () => {
            it('should return empty string for ROW field type', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeRowField({ variable: 'rowField' });

                expect(resolutionValue[DotCMSFieldTypes.ROW](contentlet, field)).toBe('');
            });

            it('should return empty string for COLUMN field type', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeColumnField({ variable: 'columnField' });

                expect(resolutionValue[DotCMSFieldTypes.COLUMN](contentlet, field)).toBe('');
            });

            it('should return empty string for TAB_DIVIDER field type', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeTabDividerField({ variable: 'tabDividerField' });

                expect(resolutionValue[DotCMSFieldTypes.TAB_DIVIDER](contentlet, field)).toBe('');
            });

            it('should return empty string for COLUMN_BREAK field type', () => {
                const contentlet = createFakeContentlet();
                const field = createFakeColumnBreakField({ variable: 'columnBreakField' });

                expect(resolutionValue[DotCMSFieldTypes.COLUMN_BREAK](contentlet, field)).toBe('');
            });
        });
    });
});
