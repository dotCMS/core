/* eslint-disable @typescript-eslint/no-explicit-any */

import { describe, expect, it, jest } from '@jest/globals';

type SpyInstance = ReturnType<typeof jest.spyOn>;

import {
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotLanguage,
    UI_STORAGE_KEY
} from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { MOCK_CONTENTTYPE_2_TABS, MOCK_FORM_CONTROL_FIELDS } from './edit-content.mock';
import * as functionsUtil from './functions.util';
import {
    createPaths,
    generatePreviewUrl,
    getFieldVariablesParsed,
    getStoredUIState,
    isFilteredType,
    isValidJson,
    sortLocalesTranslatedFirst,
    stringToJson
} from './functions.util';
import { CALENDAR_FIELD_TYPES, JSON_FIELD_MOCK, MULTIPLE_TABS_MOCK } from './mocks';

import { FLATTENED_FIELD_TYPES } from '../models/dot-edit-content-field.constant';
import {
    DotEditContentFieldSingleSelectableDataType,
    FIELD_TYPES
} from '../models/dot-edit-content-field.enum';
import { NON_FORM_CONTROL_FIELD_TYPES } from '../models/dot-edit-content-form.enum';

describe('Utils Functions', () => {
    const originalWarn = console.warn;

    beforeAll(() => {
        console.warn = jest.fn();
    });

    afterAll(() => {
        console.warn = originalWarn;
    });

    const {
        castSingleSelectableValue,
        getSingleSelectableFieldOptions,
        getFinalCastedValue,
        isFlattenedField,
        isCalendarField,
        processCalendarFieldValue,
        processFieldValue
    } = functionsUtil;

    describe('castSingleSelectableValue', () => {
        describe('null/undefined/empty handling', () => {
            it('should return null for null value', () => {
                expect(castSingleSelectableValue(null, '')).toBeNull();
            });

            it('should return null for undefined value', () => {
                expect(castSingleSelectableValue(undefined, '')).toBeNull();
            });

            it('should return null for empty string', () => {
                expect(castSingleSelectableValue('', '')).toBeNull();
            });
        });

        describe('Boolean', () => {
            const type = DotEditContentFieldSingleSelectableDataType.BOOL;

            it('should handle boolean true directly', () => {
                expect(castSingleSelectableValue(true, type)).toBe(true);
            });

            it('should handle boolean false directly', () => {
                expect(castSingleSelectableValue(false, type)).toBe(false);
            });

            it('should handle string "true"', () => {
                expect(castSingleSelectableValue('true', type)).toBe(true);
            });

            it('should handle string "false"', () => {
                expect(castSingleSelectableValue('false', type)).toBe(false);
            });

            it('should handle uppercase "TRUE"', () => {
                expect(castSingleSelectableValue('TRUE', type)).toBe(true);
            });

            it('should handle mixed case "tRuE"', () => {
                expect(castSingleSelectableValue('tRuE', type)).toBe(true);
            });

            it('should handle string with spaces', () => {
                expect(castSingleSelectableValue('  true  ', type)).toBe(true);
            });

            it('should return false for non-boolean strings', () => {
                expect(castSingleSelectableValue('hello', type)).toBe(false);
            });

            it('should handle number 1 as false', () => {
                expect(castSingleSelectableValue(1, type)).toBe(false);
            });

            it('should handle number 0 as false', () => {
                expect(castSingleSelectableValue(0, type)).toBe(false);
            });
        });

        describe('Numeric', () => {
            describe('INTEGER type', () => {
                const type = DotEditContentFieldSingleSelectableDataType.INTEGER;

                it('should handle number directly', () => {
                    expect(castSingleSelectableValue(42, type)).toBe(42);
                });

                it('should handle numeric string', () => {
                    expect(castSingleSelectableValue('42', type)).toBe(42);
                });

                it('should handle string with spaces', () => {
                    expect(castSingleSelectableValue('  42  ', type)).toBe(42);
                });

                it('should return null for invalid number', () => {
                    expect(castSingleSelectableValue('not a number', type)).toBeNull();
                });

                it('should handle zero', () => {
                    expect(castSingleSelectableValue(0, type)).toBe(0);
                });

                it('should handle negative numbers', () => {
                    expect(castSingleSelectableValue(-42, type)).toBe(-42);
                });
            });

            describe('FLOAT type', () => {
                const type = DotEditContentFieldSingleSelectableDataType.FLOAT;

                it('should handle float directly', () => {
                    expect(castSingleSelectableValue(3.14, type)).toBe(3.14);
                });

                it('should handle float string', () => {
                    expect(castSingleSelectableValue('3.14', type)).toBe(3.14);
                });

                it('should handle string with spaces', () => {
                    expect(castSingleSelectableValue('  3.14  ', type)).toBe(3.14);
                });

                it('should return null for invalid float', () => {
                    expect(castSingleSelectableValue('not a float', type)).toBeNull();
                });

                it('should handle negative floats', () => {
                    expect(castSingleSelectableValue(-3.14, type)).toBe(-3.14);
                });

                it('should handle zero', () => {
                    expect(castSingleSelectableValue(0.0, type)).toBe(0);
                });
            });
        });

        describe('Default string handling', () => {
            it('should convert number to string for unknown type', () => {
                expect(castSingleSelectableValue(42, 'unknown')).toBe('42');
            });

            it('should convert boolean to string for unknown type', () => {
                expect(castSingleSelectableValue(true, 'unknown')).toBe('true');
            });

            it('should handle object by converting to string', () => {
                const obj = { test: 'value' };
                expect(castSingleSelectableValue(obj, 'unknown')).toBe(String(obj));
            });

            it('should return string as is for unknown type', () => {
                expect(castSingleSelectableValue('test', 'unknown')).toBe('test');
            });
        });
    });

    describe('getSingleSelectableFieldOptions', () => {
        describe('Empty and null values', () => {
            it('should return an empty array if options is empty', () => {
                expect(getSingleSelectableFieldOptions('', 'Some type')).toEqual([]);
            });

            it('should return an empty array if options is null', () => {
                expect(getSingleSelectableFieldOptions(null as any, 'Some type')).toEqual([]);
            });

            it('should return an empty array if options is undefined', () => {
                expect(getSingleSelectableFieldOptions(undefined as any, 'Some type')).toEqual([]);
            });

            it('should return an empty array if options is only whitespace', () => {
                expect(getSingleSelectableFieldOptions('   ', 'Some type')).toEqual([]);
            });
        });

        describe('Multi-line pipe format (dotCMS standard)', () => {
            it('should handle standard dotCMS format from documentation', () => {
                const options = 'foo|1\r\nbar|2\r\nthird item|c\r\nThis one is a ghost.|boo';
                expect(getSingleSelectableFieldOptions(options, 'text')).toEqual([
                    { label: 'foo', value: '1' },
                    { label: 'bar', value: '2' },
                    { label: 'third item', value: 'c' },
                    { label: 'This one is a ghost.', value: 'boo' }
                ]);
            });

            it('should handle radio field format from documentation', () => {
                const options = 'FirstLabel|1\r\nSecondLabel|2\r\nThirdLabel|foo';
                expect(getSingleSelectableFieldOptions(options, 'text')).toEqual([
                    { label: 'FirstLabel', value: '1' },
                    { label: 'SecondLabel', value: '2' },
                    { label: 'ThirdLabel', value: 'foo' }
                ]);
            });

            it('should support different line break formats', () => {
                expect(
                    getSingleSelectableFieldOptions('label1|value1\nlabel2|value2', 'text')
                ).toEqual([
                    { label: 'label1', value: 'value1' },
                    { label: 'label2', value: 'value2' }
                ]);

                expect(
                    getSingleSelectableFieldOptions('label1|value1\rlabel2|value2', 'text')
                ).toEqual([
                    { label: 'label1', value: 'value1' },
                    { label: 'label2', value: 'value2' }
                ]);
            });

            it('should handle options without explicit values (label as value)', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        'some label\r\nsome label 2\r\nsome label 3|i have value',
                        'text'
                    )
                ).toEqual([
                    { label: 'some label', value: 'some label' },
                    { label: 'some label 2', value: 'some label 2' },
                    { label: 'some label 3', value: 'i have value' }
                ]);
            });

            it('should handle options without explicit values (label as value) for boolean type', () => {
                expect(getSingleSelectableFieldOptions('True\r\nFalse', 'text')).toEqual([
                    { label: 'True', value: 'True' },
                    { label: 'False', value: 'False' }
                ]);
            });

            it('should trim labels and values', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        ' some label \r\n     some label 2 \r\n    some label 3     | i have value ',
                        'text'
                    )
                ).toEqual([
                    { label: 'some label', value: 'some label' },
                    { label: 'some label 2', value: 'some label 2' },
                    { label: 'some label 3', value: 'i have value' }
                ]);
            });
        });

        describe('Special case: checkbox without label', () => {
            it('should handle "|true" case for checkbox without label', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        '|true',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toEqual([{ label: '', value: true }]);
            });

            it('should handle "|false" case for checkbox without label', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        '|false',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toEqual([{ label: '', value: false }]);
            });

            it('should handle "|value" case with string type', () => {
                expect(getSingleSelectableFieldOptions('|somevalue', 'text')).toEqual([
                    { label: '', value: 'somevalue' }
                ]);
            });

            it('should handle "|1" case with integer type', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        '|1',
                        DotEditContentFieldSingleSelectableDataType.INTEGER
                    )
                ).toEqual([{ label: '', value: 1 }]);
            });
        });

        describe('Simple comma format (when no pipes present)', () => {
            it('should handle simple comma-separated values', () => {
                expect(getSingleSelectableFieldOptions('1,2,3', 'text')).toEqual([
                    { label: '1', value: '1' },
                    { label: '2', value: '2' },
                    { label: '3', value: '3' }
                ]);
            });

            it('should handle comma-separated values with spaces', () => {
                expect(getSingleSelectableFieldOptions('value1, value2, value3', 'text')).toEqual([
                    { label: 'value1', value: 'value1' },
                    { label: 'value2', value: 'value2' },
                    { label: 'value3', value: 'value3' }
                ]);
            });

            it('should handle single value without commas', () => {
                expect(getSingleSelectableFieldOptions('single value', 'text')).toEqual([
                    { label: 'single value', value: 'single value' }
                ]);
            });

            it('should filter out empty values in comma format', () => {
                expect(getSingleSelectableFieldOptions('value1,,value3,', 'text')).toEqual([
                    { label: 'value1', value: 'value1' },
                    { label: 'value3', value: 'value3' }
                ]);
            });
        });

        describe('Data type casting', () => {
            describe.each([
                {
                    description: 'INTEGER type casting',
                    optionsString: 'some label\r\n3\r\nsome label 3|i have value\r\nfour|4',
                    dataType: DotEditContentFieldSingleSelectableDataType.INTEGER,
                    expected: [
                        { label: '3', value: 3 },
                        { label: 'four', value: 4 }
                    ]
                },
                {
                    description: 'FLOAT type casting',
                    optionsString:
                        'some label\r\n3.14\r\nsome label 3|i have value\r\nfour dot five|4.5',
                    dataType: DotEditContentFieldSingleSelectableDataType.FLOAT,
                    expected: [
                        { label: '3.14', value: 3.14 },
                        { label: 'four dot five', value: 4.5 }
                    ]
                },
                {
                    description: 'BOOL type casting',
                    optionsString: 'some label\r\nfalse\r\nsome label 3|true\r\ntrue|true',
                    dataType: DotEditContentFieldSingleSelectableDataType.BOOL,
                    expected: [
                        { label: 'some label', value: false },
                        { label: 'false', value: false },
                        { label: 'some label 3', value: true },
                        { label: 'true', value: true }
                    ]
                }
            ])('$description', ({ optionsString, dataType, expected }) => {
                it(`should cast values correctly for ${dataType}`, () => {
                    expect(getSingleSelectableFieldOptions(optionsString, dataType)).toEqual(
                        expected
                    );
                });
            });

            it('should handle comma format with integer casting', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        '1,2,3',
                        DotEditContentFieldSingleSelectableDataType.INTEGER
                    )
                ).toEqual([
                    { label: '1', value: 1 },
                    { label: '2', value: 2 },
                    { label: '3', value: 3 }
                ]);
            });

            it('should handle comma format with boolean casting', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        'true,false',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toEqual([
                    { label: 'true', value: true },
                    { label: 'false', value: false }
                ]);
            });
        });

        describe('Edge cases and error handling', () => {
            it('should filter out invalid values that cannot be cast', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        'valid|1\r\ninvalid|notanumber\r\nvalid2|2',
                        DotEditContentFieldSingleSelectableDataType.INTEGER
                    )
                ).toEqual([
                    { label: 'valid', value: 1 },
                    { label: 'valid2', value: 2 }
                ]);
            });

            it('should handle empty lines in multi-line format', () => {
                expect(
                    getSingleSelectableFieldOptions('label1|value1\r\n\r\nlabel2|value2', 'text')
                ).toEqual([
                    { label: 'label1', value: 'value1' },
                    { label: 'label2', value: 'value2' }
                ]);
            });

            it('should handle mixed formats (pipes take precedence)', () => {
                // When pipes are present, comma splitting should not occur
                expect(
                    getSingleSelectableFieldOptions('label1|value1,label2|value2', 'text')
                ).toEqual([
                    { label: 'label1|value1', value: 'label1|value1' },
                    { label: 'label2|value2', value: 'label2|value2' }
                ]);
            });

            it('should handle labels with special characters', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        'Special & Label!|value1\r\n@#$%|value2',
                        'text'
                    )
                ).toEqual([
                    { label: 'Special & Label!', value: 'value1' },
                    { label: '@#$%', value: 'value2' }
                ]);
            });

            it('should handle values with spaces', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        'Label 1|Value with spaces\r\nLabel 2|Another value',
                        'text'
                    )
                ).toEqual([
                    { label: 'Label 1', value: 'Value with spaces' },
                    { label: 'Label 2', value: 'Another value' }
                ]);
            });
        });

        describe('Real-world dotCMS scenarios', () => {
            it('should handle checkbox default values scenario from documentation', () => {
                // Setup options as per documentation
                const options = 'foo|1\r\nbar|2\r\nthird item|c\r\nThis one is a ghost.|boo';
                const result = getSingleSelectableFieldOptions(options, 'text');

                // Verify we can find the default values: 1,c,boo
                const values = result.map((option) => option.value);
                expect(values).toContain('1');
                expect(values).toContain('c');
                expect(values).toContain('boo');
            });

            it('should handle select field with empty first option', () => {
                expect(
                    getSingleSelectableFieldOptions('|\r\nOption 1|1\r\nOption 2|2', 'text')
                ).toEqual([
                    { label: 'Option 1', value: '1' },
                    { label: 'Option 2', value: '2' }
                ]);
            });

            it('should handle multi-select field format', () => {
                expect(
                    getSingleSelectableFieldOptions(
                        '|\r\nFirst Option|first\r\nSecond Option|second',
                        'text'
                    )
                ).toEqual([
                    { label: 'First Option', value: 'first' },
                    { label: 'Second Option', value: 'second' }
                ]);
            });
        });
    });

    describe('getFinalCastedValue', () => {
        describe('DATE_AND_TIME and TIME fields', () => {
            it.each(['Date-and-Time', 'Time'])(
                'should return the original value for %s field',
                (fieldType) => {
                    const value = '2021-09-01T18:00:00.000Z';
                    const field = { fieldType } as DotCMSContentTypeField;

                    // DATE_AND_TIME and TIME fields return the original value without conversion
                    expect(getFinalCastedValue(value, field)).toEqual(value);
                }
            );

            it.each(['Date-and-Time', 'Time'])(
                "should return 'now' string as-is for %s field",
                (fieldType) => {
                    const value = 'now';
                    const field = { fieldType } as DotCMSContentTypeField;

                    // DATE_AND_TIME and TIME fields return the original value
                    expect(getFinalCastedValue(value, field)).toEqual(value);
                }
            );

            it.each(['Date-and-Time', 'Time'])(
                'should return undefined for %s field when value is undefined',
                (fieldType) => {
                    const value = undefined;
                    const field = { fieldType } as DotCMSContentTypeField;

                    expect(getFinalCastedValue(value, field)).toEqual(undefined);
                }
            );
        });

        describe('DATE field', () => {
            it('should convert value to string for DATE field', () => {
                const value = '2021-09-01T18:00:00.000Z';
                const field = { fieldType: 'Date', dataType: 'DATE' } as DotCMSContentTypeField;

                // DATE field goes through castSingleSelectableValue which returns String(value)
                expect(getFinalCastedValue(value, field)).toEqual(value);
            });

            it("should convert 'now' to string for DATE field", () => {
                const value = 'now';
                const field = { fieldType: 'Date', dataType: 'DATE' } as DotCMSContentTypeField;

                // DATE field goes through castSingleSelectableValue which returns String(value)
                expect(getFinalCastedValue(value, field)).toEqual(value);
            });

            it('should return undefined for DATE field when value is undefined', () => {
                const value = undefined;
                const field = { fieldType: 'Date', dataType: 'DATE' } as DotCMSContentTypeField;

                expect(getFinalCastedValue(value, field)).toEqual(undefined);
            });
        });

        describe.each([...FLATTENED_FIELD_TYPES])('Flattened Fields', (fieldType) => {
            describe(fieldType, () => {
                it('should return an array of the values', () => {
                    const value = 'value1,value2,value3';
                    const field = { fieldType } as DotCMSContentTypeField;

                    expect(getFinalCastedValue(value, field)).toEqual([
                        'value1',
                        'value2',
                        'value3'
                    ]);
                });

                it('should trim the values', () => {
                    const value = ' value1 , value2 , value3 ';
                    const field = { fieldType } as DotCMSContentTypeField;

                    expect(getFinalCastedValue(value, field)).toEqual([
                        'value1',
                        'value2',
                        'value3'
                    ]);
                });

                it('should return undefined if the value is undefined', () => {
                    const value = undefined;
                    const field = { fieldType } as DotCMSContentTypeField;

                    expect(getFinalCastedValue(value, field)).toEqual(undefined);
                });
            });
        });

        describe('No special field', () => {
            it('should call castSingleSelectableValue', () => {
                const value = 'value1';
                const field = {
                    fieldType: 'something',
                    dataType: 'something'
                } as DotCMSContentTypeField;

                const castSingleSelectableValueMock = jest.spyOn(
                    functionsUtil,
                    'castSingleSelectableValue'
                );

                getFinalCastedValue(value, field);

                expect(castSingleSelectableValueMock).toHaveBeenCalledWith(value, field.dataType);
            });
        });

        it('should return undefined value', () => {
            const value = undefined;
            const field = {
                fieldType: 'something',
                dataType: 'something'
            } as DotCMSContentTypeField;

            const res = getFinalCastedValue(value, field);
            expect(res).toBeUndefined();
        });

        it('should return a JSON value', () => {
            const value = {
                attrs: {},
                content: [],
                type: 'doc'
            };
            const field = {
                fieldType: 'Story-Block',
                dataType: 'something'
            } as DotCMSContentTypeField;

            const res = getFinalCastedValue(value, field);
            expect(res).toEqual(value);
        });

        describe('JSON Field', () => {
            it('should return a JSON value as string keeping the format', () => {
                const value = {
                    value1: 'value1',
                    value2: 'value2',
                    value3: 'value3'
                };

                const field = {
                    fieldType: JSON_FIELD_MOCK.fieldType
                } as DotCMSContentTypeField;

                const res = getFinalCastedValue(value, field);
                const formattedValue = JSON.stringify(value, null, 2);
                expect(res).toBe(formattedValue);
            });
        });

        describe('Form Tabs', () => {
            it('should transform layout to tabs', () => {
                const expected = [
                    {
                        title: 'first tab',
                        layout: [
                            {
                                divider: {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                                    contentTypeId: 'd46d6404125ac27e6ab68fad09266241',
                                    dataType: 'SYSTEM',
                                    fieldType: 'Row',
                                    fieldTypeLabel: 'Row',
                                    fieldVariables: [],
                                    fixed: false,
                                    forceIncludeInApi: false,
                                    iDate: 1697051073000,
                                    id: 'a31ea895f80eb0a3754e4a2292e09a52',
                                    indexed: false,
                                    listed: false,
                                    modDate: 1697051077000,
                                    name: 'fields-0',
                                    readOnly: false,
                                    required: false,
                                    searchable: false,
                                    sortOrder: 0,
                                    unique: false,
                                    variable: 'fields0'
                                },
                                columns: []
                            }
                        ]
                    },
                    {
                        title: 'New Tab',
                        layout: []
                    }
                ];
                const res = functionsUtil.transformLayoutToTabs('first tab', MULTIPLE_TABS_MOCK);

                expect(res).toEqual(expected);
            });
        });
    });

    describe('isValidJson', () => {
        it('should return true for a valid object JSON', () => {
            expect(isValidJson('{ "key": "value", "value": "value" }')).toBe(true);
        });

        it('should return false for a numeric string', () => {
            expect(isValidJson('1')).toBe(false);
        });

        it('should return false for an incomplete JSON', () => {
            expect(isValidJson('{key: value')).toBe(false);
        });

        it('should return false for a non-JSON string', () => {
            expect(isValidJson('{{')).toBe(false);
        });

        it('should return false for an array JSON', () => {
            expect(isValidJson('["item1", "item2"]')).toBe(false);
        });

        it('should return false for the string "null"', () => {
            expect(isValidJson('null')).toBe(false);
        });

        it('should return true for an empty JSON object', () => {
            expect(isValidJson('{}')).toBe(true);
        });

        it('should return true for a valid nested object JSON', () => {
            expect(isValidJson('{ "key": { "nestedKey": "nestedValue" } }')).toBe(true);
        });

        it('should return false for a boolean string value', () => {
            expect(isValidJson('true')).toBe(false);
        });
    });

    describe('StringToJson function', () => {
        it('should return parsed object when provided with valid JSON string', () => {
            const jsonString = '{ "key": "value" }';

            const result = stringToJson(jsonString);

            expect(result).toEqual({ key: 'value' });
        });

        it('should return empty object when provided with invalid JSON string', () => {
            const jsonString = '{ key: value }';

            const result = stringToJson(jsonString);

            expect(result).toEqual({});
        });

        it('should return empty object when provided JSON string is empty', () => {
            const jsonString = '';

            const result = stringToJson(jsonString);

            expect(result).toEqual({});
        });
    });

    describe('getFieldVariablesParsed function', () => {
        it('should parse an array of DotCMSContentTypeFieldVariable objects correctly', () => {
            const fieldVariables: DotCMSContentTypeFieldVariable[] = [
                {
                    clazz: 'class1',
                    fieldId: 'fieldId1',
                    id: 'id1',
                    key: 'key1',
                    value: 'value1'
                },
                {
                    clazz: 'class2',
                    fieldId: 'fieldId2',
                    id: 'id2',
                    key: 'key2',
                    value: 'value2'
                }
            ];
            const result = getFieldVariablesParsed(fieldVariables);
            expect(result).toEqual({
                key1: 'value1',
                key2: 'value2'
            });
        });

        it('should return an empty object when the provided fieldVariables array is undefined', () => {
            const fieldVariables: DotCMSContentTypeFieldVariable[] | undefined = undefined;
            const result = getFieldVariablesParsed(
                fieldVariables as unknown as DotCMSContentTypeFieldVariable[]
            );
            expect(result).toEqual({});
        });
    });

    describe('createPaths function', () => {
        it('with the root path', () => {
            const path = 'nico.demo.ts';
            const paths = createPaths(path);
            expect(paths).toStrictEqual(['nico.demo.ts/']);
        });

        it('with a single level', () => {
            const path = 'nico.demo.ts/demo';
            const paths = createPaths(path);
            expect(paths).toStrictEqual(['nico.demo.ts/', 'nico.demo.ts/demo/']);
        });

        it('with a single level ending in slash', () => {
            const path = 'nico.demo.ts/demo/';
            const paths = createPaths(path);
            expect(paths).toStrictEqual(['nico.demo.ts/', 'nico.demo.ts/demo/']);
        });

        it('with two levels ', () => {
            const path = 'nico.demo.ts/demo/demo2';
            const paths = createPaths(path);
            expect(paths).toStrictEqual([
                'nico.demo.ts/',
                'nico.demo.ts/demo/',
                'nico.demo.ts/demo/demo2/'
            ]);
        });

        it('with three levels', () => {
            const path = 'nico.demo.ts/demo/demo2/demo3';
            const paths = createPaths(path);
            expect(paths).toStrictEqual([
                'nico.demo.ts/',
                'nico.demo.ts/demo/',
                'nico.demo.ts/demo/demo2/',
                'nico.demo.ts/demo/demo2/demo3/'
            ]);
        });

        it('with an empty path', () => {
            const path = '';
            const paths = createPaths(path);
            expect(paths).toStrictEqual([]);
        });
    });

    describe('UI State Storage', () => {
        beforeEach(() => {
            sessionStorage.clear();
            // eslint-disable-next-line @typescript-eslint/no-empty-function
            jest.spyOn(console, 'warn').mockImplementation(() => {});
        });

        afterEach(() => {
            jest.restoreAllMocks();
        });

        describe('getStoredUIState', () => {
            it('should return default state when sessionStorage is empty', () => {
                const state = getStoredUIState();
                expect(state).toEqual({
                    activeTab: 0,
                    isSidebarOpen: true,
                    activeSidebarTab: 0,
                    isBetaMessageVisible: true
                });
            });

            it('should return stored state from sessionStorage', () => {
                const mockState = {
                    activeTab: 2,
                    isSidebarOpen: false,
                    activeSidebarTab: 1,
                    isBetaMessageVisible: true
                };
                sessionStorage.setItem(UI_STORAGE_KEY, JSON.stringify(mockState));

                const state = getStoredUIState();
                expect(state).toEqual(mockState);
            });

            it('should return default state and warn when sessionStorage has invalid JSON', () => {
                sessionStorage.setItem(UI_STORAGE_KEY, 'invalid-json');

                const state = getStoredUIState();
                expect(state).toEqual({
                    activeTab: 0,
                    isSidebarOpen: true,
                    activeSidebarTab: 0,
                    isBetaMessageVisible: true
                });
                expect(console.warn).toHaveBeenCalledWith(
                    'Error reading UI state from sessionStorage:',
                    expect.any(Error)
                );
            });

            it('should return default state and warn when sessionStorage throws error', () => {
                const mockGetItem = jest.fn(() => {
                    throw new Error('Storage error');
                });

                Object.defineProperty(window, 'sessionStorage', {
                    value: {
                        getItem: mockGetItem
                    }
                });

                const state = getStoredUIState();
                expect(state).toEqual({
                    activeTab: 0,
                    isSidebarOpen: true,
                    activeSidebarTab: 0,
                    isBetaMessageVisible: true
                });
                expect(console.warn).toHaveBeenCalledWith(
                    'Error reading UI state from sessionStorage:',
                    expect.any(Error)
                );
            });
        });
    });

    describe('isFilteredType', () => {
        it('should correctly identify filtered and non-filtered field types', () => {
            const allFields = MOCK_CONTENTTYPE_2_TABS.fields;
            const nonFormControlFieldTypes = Object.values(NON_FORM_CONTROL_FIELD_TYPES);

            // Verify that none of the form control fields are filtered types
            MOCK_FORM_CONTROL_FIELDS.forEach((field) => {
                expect(isFilteredType(field)).toBe(false);
            });

            const filteredFields = allFields.filter((field) => {
                return !isFilteredType(field);
            });

            expect(filteredFields.length).toBe(MOCK_FORM_CONTROL_FIELDS.length);

            filteredFields.forEach((field) => {
                const fieldType = field.fieldType as NON_FORM_CONTROL_FIELD_TYPES;
                expect(nonFormControlFieldTypes.includes(fieldType)).toBe(false);
            });
        });
    });

    describe('sortLocalesTranslatedFirst', () => {
        it('sorts locales with translated ones first', () => {
            const locales = [
                { languageCode: 'en', translated: true },
                { languageCode: 'fr', translated: false },
                { languageCode: 'es', translated: true },
                { languageCode: 'de', translated: false }
            ] as DotLanguage[];

            const result = sortLocalesTranslatedFirst(locales);

            expect(result).toEqual([
                { languageCode: 'en', translated: true },
                { languageCode: 'es', translated: true },
                { languageCode: 'fr', translated: false },
                { languageCode: 'de', translated: false }
            ]);
        });

        it('returns an empty array when input is empty', () => {
            const locales: DotLanguage[] = [];

            const result = sortLocalesTranslatedFirst(locales);

            expect(result).toEqual([]);
        });

        it('returns the same array when all locales are translated', () => {
            const locales = [
                { languageCode: 'en', translated: true },
                { languageCode: 'es', translated: true }
            ] as DotLanguage[];

            const result = sortLocalesTranslatedFirst(locales);

            expect(result).toEqual(locales);
        });

        it('returns the same array when no locales are translated', () => {
            const locales = [
                { languageCode: 'fr', translated: false },
                { languageCode: 'de', translated: false }
            ] as DotLanguage[];

            const result = sortLocalesTranslatedFirst(locales);

            expect(result).toEqual(locales);
        });
    });

    describe('generatePreviewUrl', () => {
        it('should generate the correct preview URL when all attributes are present', () => {
            const contentlet = createFakeContentlet({
                URL_MAP_FOR_CONTENT: '/blog/post/5-snow-sports-to-try-this-winter',
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                languageId: 1
            });

            const expectedUrl =
                'http://localhost/dotAdmin/#/edit-page/content?url=%2Fblog%2Fpost%2F5-snow-sports-to-try-this-winter%3Fhost_id%3D48190c8c-42c4-46af-8d1a-0cd5db894797&language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&mode=EDIT_MODE';

            expect(generatePreviewUrl(contentlet)).toBe(expectedUrl);
        });

        it('should return an empty string if URL_MAP_FOR_CONTENT is missing', () => {
            const contentlet = createFakeContentlet({
                URL_MAP_FOR_CONTENT: undefined,
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                languageId: 1
            });

            expect(generatePreviewUrl(contentlet)).toBe('');
        });

        it('should return an empty string if host is missing', () => {
            const contentlet = createFakeContentlet({
                URL_MAP_FOR_CONTENT: '/blog/post/5-snow-sports-to-try-this-winter',
                host: undefined,
                languageId: 1
            });

            expect(generatePreviewUrl(contentlet)).toBe('');
        });

        it('should return an empty string if languageId is missing', () => {
            const contentlet = createFakeContentlet({
                URL_MAP_FOR_CONTENT: '/blog/post/5-snow-sports-to-try-this-winter',
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                languageId: undefined
            });

            expect(generatePreviewUrl(contentlet)).toBe('');
        });
    });

    describe('prepareContentletForCopy', () => {
        it('should prepare a contentlet for copying by setting locked to false and removing lockedBy', () => {
            // Arrange
            const contentlet = createFakeContentlet({
                locked: true,
                lockedBy: {
                    firstName: 'John',
                    lastName: 'Doe',
                    userId: 'user123'
                }
            });

            // Act
            const result = functionsUtil.prepareContentletForCopy(contentlet);

            // Assert
            expect(result).toEqual({
                ...contentlet,
                locked: false,
                lockedBy: undefined
            });
        });
    });

    describe('Form Field Processing Utilities', () => {
        describe('isFlattenedField', () => {
            it.each(FLATTENED_FIELD_TYPES)(
                'should return true for flattened field type: %s',
                (fieldType) => {
                    const field = { fieldType } as unknown as DotCMSContentTypeField;
                    const arrayValue = ['value1', 'value2', 'value3'];

                    expect(isFlattenedField(arrayValue, field)).toBe(true);
                }
            );

            it('should return false for non-array values even with flattened field types', () => {
                const field = {
                    fieldType: FIELD_TYPES.CHECKBOX
                } as unknown as DotCMSContentTypeField;
                const stringValue = 'not an array';

                expect(isFlattenedField(stringValue, field)).toBe(false);
            });

            it('should return false for array values with non-flattened field types', () => {
                const field = { fieldType: FIELD_TYPES.TEXT } as unknown as DotCMSContentTypeField;
                const arrayValue = ['value1', 'value2'];

                expect(isFlattenedField(arrayValue, field)).toBe(false);
            });

            it('should return false for null/undefined values', () => {
                const field = {
                    fieldType: FIELD_TYPES.CHECKBOX
                } as unknown as DotCMSContentTypeField;

                expect(isFlattenedField(null, field)).toBe(false);
                expect(isFlattenedField(undefined, field)).toBe(false);
            });
        });

        describe('isCalendarField', () => {
            it.each(CALENDAR_FIELD_TYPES)(
                'should return true for calendar field type: %s',
                (fieldType) => {
                    const field = { fieldType } as unknown as DotCMSContentTypeField;

                    expect(isCalendarField(field)).toBe(true);
                }
            );

            it('should return false for non-calendar field types', () => {
                const nonCalendarTypes = [
                    FIELD_TYPES.TEXT,
                    FIELD_TYPES.TEXTAREA,
                    FIELD_TYPES.CHECKBOX
                ];

                nonCalendarTypes.forEach((fieldType) => {
                    const field = { fieldType } as unknown as DotCMSContentTypeField;
                    expect(isCalendarField(field)).toBe(false);
                });
            });
        });

        describe('processCalendarFieldValue', () => {
            const fieldName = 'testField';

            describe('null/undefined handling', () => {
                it('should return null for null value', () => {
                    expect(processCalendarFieldValue(null, fieldName)).toBeNull();
                });

                it('should return undefined for undefined value', () => {
                    expect(processCalendarFieldValue(undefined, fieldName)).toBeUndefined();
                });

                it('should return null for empty string', () => {
                    expect(processCalendarFieldValue('', fieldName)).toBeNull();
                });
            });

            describe('Date object handling', () => {
                it('should convert Date objects to timestamps', () => {
                    const date = new Date('2025-01-15T10:30:00Z');
                    const expectedTimestamp = date.getTime();

                    expect(processCalendarFieldValue(date, fieldName)).toBe(expectedTimestamp);
                });

                it('should handle current date', () => {
                    const now = new Date();
                    const expectedTimestamp = now.getTime();

                    expect(processCalendarFieldValue(now, fieldName)).toBe(expectedTimestamp);
                });
            });

            describe('number handling', () => {
                it('should return numeric values as-is', () => {
                    const timestamp = 1737021000000;

                    expect(processCalendarFieldValue(timestamp, fieldName)).toBe(timestamp);
                });

                it('should handle zero timestamp', () => {
                    expect(processCalendarFieldValue(0, fieldName)).toBe(0);
                });

                it('should handle negative timestamps', () => {
                    const negativeTimestamp = -1737021000000;

                    expect(processCalendarFieldValue(negativeTimestamp, fieldName)).toBe(
                        negativeTimestamp
                    );
                });
            });

            describe('string handling', () => {
                it('should convert valid numeric strings to numbers', () => {
                    const stringTimestamp = '1737021000000';
                    const expectedNumber = 1737021000000;

                    expect(processCalendarFieldValue(stringTimestamp, fieldName)).toBe(
                        expectedNumber
                    );
                });

                it('should handle string with leading/trailing spaces', () => {
                    const stringTimestamp = '  1737021000000  ';
                    const expectedNumber = 1737021000000;

                    expect(processCalendarFieldValue(stringTimestamp, fieldName)).toBe(
                        expectedNumber
                    );
                });

                it('should return null for invalid string timestamps', () => {
                    const invalidString = 'not-a-number';

                    expect(processCalendarFieldValue(invalidString, fieldName)).toBeNull();
                });

                it('should return null for empty string after trim', () => {
                    const emptyString = '   ';

                    expect(processCalendarFieldValue(emptyString, fieldName)).toBeNull();
                });
            });

            describe('unexpected value handling', () => {
                it('should return null for unexpected value types', () => {
                    const objectValue = { timestamp: 1737021000000 };

                    expect(processCalendarFieldValue(objectValue as any, fieldName)).toBeNull();
                });

                it('should return null for boolean values', () => {
                    expect(processCalendarFieldValue(true as any, fieldName)).toBeNull();
                    expect(processCalendarFieldValue(false as any, fieldName)).toBeNull();
                });
            });

            describe('console logging', () => {
                let consoleWarnSpy: SpyInstance;
                let consoleErrorSpy: SpyInstance;

                beforeEach(() => {
                    // eslint-disable-next-line @typescript-eslint/no-empty-function
                    consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
                    // eslint-disable-next-line @typescript-eslint/no-empty-function
                    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
                });

                afterEach(() => {
                    consoleWarnSpy.mockRestore();
                    consoleErrorSpy.mockRestore();
                });

                it('should log warning for string timestamp conversion', () => {
                    processCalendarFieldValue('1737021000000', fieldName);

                    expect(consoleWarnSpy).toHaveBeenCalledWith(
                        `Calendar field ${fieldName} received string timestamp, converted to number:`,
                        { original: '1737021000000', converted: 1737021000000 }
                    );
                });

                it('should log warning for invalid string timestamps', () => {
                    processCalendarFieldValue('invalid', fieldName);

                    expect(consoleWarnSpy).toHaveBeenCalledWith(
                        `Calendar field ${fieldName} has invalid timestamp string:`,
                        'invalid'
                    );
                });

                it('should log error for unexpected value types', () => {
                    const unexpectedValue = { test: 'value' };
                    processCalendarFieldValue(unexpectedValue as any, fieldName);

                    expect(consoleErrorSpy).toHaveBeenCalledWith(
                        `Calendar field ${fieldName} received unexpected value:`,
                        { value: unexpectedValue, type: 'object' }
                    );
                });
            });
        });

        describe('processFieldValue', () => {
            describe('flattened fields', () => {
                it('should join array values with commas for flattened fields', () => {
                    const field = {
                        fieldType: FIELD_TYPES.CHECKBOX,
                        variable: 'checkboxField'
                    } as unknown as DotCMSContentTypeField;
                    const arrayValue = ['option1', 'option2', 'option3'];

                    expect(processFieldValue(arrayValue, field)).toBe('option1,option2,option3');
                });

                it('should handle empty arrays', () => {
                    const field = {
                        fieldType: FIELD_TYPES.MULTI_SELECT,
                        variable: 'multiSelectField'
                    } as unknown as DotCMSContentTypeField;
                    const emptyArray: string[] = [];

                    expect(processFieldValue(emptyArray, field)).toBe('');
                });

                it('should handle single-item arrays', () => {
                    const field = {
                        fieldType: FIELD_TYPES.TAG,
                        variable: 'tagField'
                    } as unknown as DotCMSContentTypeField;
                    const singleItemArray = ['onlyOption'];

                    expect(processFieldValue(singleItemArray, field)).toBe('onlyOption');
                });
            });

            describe('calendar fields', () => {
                it('should process Date objects to timestamps for calendar fields', () => {
                    const field = {
                        fieldType: FIELD_TYPES.DATE,
                        variable: 'dateField'
                    } as unknown as DotCMSContentTypeField;
                    const date = new Date('2025-01-15T10:30:00Z');
                    const expectedTimestamp = date.getTime();

                    expect(processFieldValue(date, field)).toBe(expectedTimestamp);
                });

                it('should process string timestamps for calendar fields', () => {
                    const field = {
                        fieldType: FIELD_TYPES.DATE_AND_TIME,
                        variable: 'datetimeField'
                    } as unknown as DotCMSContentTypeField;
                    const stringTimestamp = '1737021000000';

                    expect(processFieldValue(stringTimestamp, field)).toBe(1737021000000);
                });

                it('should handle null values for calendar fields', () => {
                    const field = {
                        fieldType: FIELD_TYPES.TIME,
                        variable: 'timeField'
                    } as unknown as DotCMSContentTypeField;

                    expect(processFieldValue(null, field)).toBeNull();
                });
            });

            describe('other field types', () => {
                it('should return string values as-is for text fields', () => {
                    const field = {
                        fieldType: FIELD_TYPES.TEXT,
                        variable: 'textField'
                    } as unknown as DotCMSContentTypeField;
                    const stringValue = 'some text';

                    expect(processFieldValue(stringValue, field)).toBe(stringValue);
                });

                it('should return number values as-is for numeric fields', () => {
                    const field = {
                        fieldType: FIELD_TYPES.TEXT,
                        variable: 'numericField'
                    } as unknown as DotCMSContentTypeField;
                    const numberValue = 42;

                    expect(processFieldValue(numberValue, field)).toBe(numberValue);
                });

                it('should return null/undefined values as-is', () => {
                    const field = {
                        fieldType: FIELD_TYPES.TEXTAREA,
                        variable: 'textareaField'
                    } as unknown as DotCMSContentTypeField;

                    expect(processFieldValue(null, field)).toBeNull();
                    expect(processFieldValue(undefined, field)).toBeUndefined();
                });
            });

            describe('edge cases', () => {
                it('should handle fields without specific processing', () => {
                    const field = {
                        fieldType: FIELD_TYPES.WYSIWYG,
                        variable: 'wysiwygField'
                    } as unknown as DotCMSContentTypeField;
                    const complexValue = { html: '<p>content</p>' };

                    expect(processFieldValue(complexValue as any, field)).toBe(complexValue);
                });

                it('should handle unknown field types gracefully', () => {
                    const field = {
                        fieldType: 'UNKNOWN_TYPE' as any,
                        variable: 'unknownField'
                    } as unknown as DotCMSContentTypeField;
                    const value = 'some value';

                    expect(processFieldValue(value, field)).toBe(value);
                });
            });
        });
    });
});
