import { describe, expect, it, jest } from '@jest/globals';

import {
    DotCMSContentTypeField,
    DotCMSContentTypeFieldVariable,
    DotLanguage
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
import { DotEditContentFieldSingleSelectableDataType } from '../models/dot-edit-content-field.enum';
import { NON_FORM_CONTROL_FIELD_TYPES } from '../models/dot-edit-content-form.enum';
import { UI_STORAGE_KEY } from '../models/dot-edit-content.constant';

describe('Utils Functions', () => {
    const { castSingleSelectableValue, getSingleSelectableFieldOptions, getFinalCastedValue } =
        functionsUtil;

    describe('castSingleSelectableValue', () => {
        it('should return null if value is empty', () => {
            expect(castSingleSelectableValue('', '')).toBeNull();
        });

        it('should return value if type is not Bool or Number', () => {
            expect(castSingleSelectableValue('Some value', 'Some other type')).toBe('Some value');
        });

        describe('Boolean', () => {
            it('should return true if value is true', () => {
                expect(
                    castSingleSelectableValue(
                        'true',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(true);
            });

            it('should return true if value is TRUE', () => {
                expect(
                    castSingleSelectableValue(
                        'TRUE',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(true);
            });

            it('should return true if value is True', () => {
                expect(
                    castSingleSelectableValue(
                        'True',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(true);
            });

            it('should return true if value is TrUE', () => {
                expect(
                    castSingleSelectableValue(
                        'TrUE',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(true);
            });

            it('should return true if value has spaces on the sides', () => {
                expect(
                    castSingleSelectableValue(
                        '        true      ',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(true);
            });

            it('should return false if value is false', () => {
                expect(
                    castSingleSelectableValue(
                        'false',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(false);
            });

            it('should return false for any random string that is not "true"', () => {
                expect(
                    castSingleSelectableValue(
                        (Math.random() * 10).toString(36), // This return some random stuff like 6.kuh34iuh12
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(false);
            });

            it('should return false for any random number that is not "true"', () => {
                expect(
                    castSingleSelectableValue(
                        (Math.random() * 10).toString(), // Random number from 0 to 10
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBe(false);
            });
        });

        describe.each([
            {
                dataType: DotEditContentFieldSingleSelectableDataType.INTEGER
            },
            {
                dataType: DotEditContentFieldSingleSelectableDataType.FLOAT
            }
        ])('Numeric DataTypes', ({ dataType }) => {
            describe(dataType, () => {
                let number = 0;

                beforeEach(() => {
                    number =
                        dataType == DotEditContentFieldSingleSelectableDataType.INTEGER
                            ? Math.random() * 10
                            : Math.random(); // To generate a float or integer number
                });

                it('should return number if value is number', () => {
                    expect(castSingleSelectableValue(number.toString(), dataType)).toBe(number);
                });

                it('should return number if value is number and has spaces on the sides', () => {
                    expect(
                        castSingleSelectableValue(`      ${number.toString()}   `, dataType)
                    ).toBe(number);
                });

                it('should return NaN if value is not a number', () => {
                    const randomString = number.toString(36);

                    expect(castSingleSelectableValue(randomString, dataType)).toBe(NaN);
                });
            });
        });
    });

    describe('getSingleSelectableFieldOptions', () => {
        it('should return an array of objects with label and value', () => {
            expect(getSingleSelectableFieldOptions('some label|some value', 'Some type')).toEqual([
                {
                    label: 'some label',
                    value: 'some value'
                }
            ]);
        });

        it('should return an array of objects with label and value even if value is not provided', () => {
            expect(getSingleSelectableFieldOptions('some label', 'Some type')).toEqual([
                {
                    label: 'some label',
                    value: 'some label'
                }
            ]);
        });

        it('should return an empty array if options is empty', () => {
            expect(getSingleSelectableFieldOptions('', 'Some type')).toEqual([]);
        });

        it('should support multiline options', () => {
            expect(
                getSingleSelectableFieldOptions(
                    'some label\r\nsome label 2\r\nsome label 3|i have value',
                    'Some type'
                )
            ).toEqual([
                {
                    label: 'some label',
                    value: 'some label'
                },
                {
                    label: 'some label 2',
                    value: 'some label 2'
                },
                {
                    label: 'some label 3',
                    value: 'i have value'
                }
            ]);
        });

        it('should trim the values', () => {
            expect(
                getSingleSelectableFieldOptions(
                    ' some label \r\n     some label 2 \r\n    some label 3     | i have value ',
                    'Some type'
                )
            ).toEqual([
                {
                    label: 'some label',
                    value: 'some label'
                },
                {
                    label: 'some label 2',
                    value: 'some label 2'
                },
                {
                    label: 'some label 3',
                    value: 'i have value'
                }
            ]);
        });
        describe.each([
            {
                optionsString: 'some label\r\n3\r\nsome label 3|i have value\r\nfour|4',
                dataType: DotEditContentFieldSingleSelectableDataType.INTEGER,
                expected: [
                    {
                        label: 'some label',
                        value: NaN
                    },
                    {
                        label: '3',
                        value: 3
                    },
                    {
                        label: 'some label 3',
                        value: NaN
                    },
                    {
                        label: 'four',
                        value: 4
                    }
                ]
            },
            {
                optionsString:
                    'some label\r\n3.14\r\nsome label 3|i have value\r\nfour dot five|4.5',
                dataType: DotEditContentFieldSingleSelectableDataType.FLOAT,
                expected: [
                    {
                        label: 'some label',
                        value: NaN
                    },
                    {
                        label: '3.14',
                        value: 3.14
                    },
                    {
                        label: 'some label 3',
                        value: NaN
                    },
                    {
                        label: 'four dot five',
                        value: 4.5
                    }
                ]
            },
            {
                optionsString: 'some label\r\nfalse\r\nsome label 3|true\r\ntrue|true',
                dataType: DotEditContentFieldSingleSelectableDataType.BOOL,
                expected: [
                    {
                        label: 'some label',
                        value: false
                    },
                    {
                        label: 'false',
                        value: false
                    },
                    {
                        label: 'some label 3',
                        value: true
                    },
                    {
                        label: 'true',
                        value: true
                    }
                ]
            }
        ])(
            'should cast the values when a type is passed',
            ({ optionsString, dataType, expected }) => {
                it(`should cast for ${dataType} `, () => {
                    expect(getSingleSelectableFieldOptions(optionsString, dataType)).toEqual(
                        expected
                    );
                });
            }
        );
    });

    describe('getFinalCastedValue', () => {
        describe.each([...CALENDAR_FIELD_TYPES])('Calendar Fields', (fieldType) => {
            describe(fieldType, () => {
                it('should parse the date if the value is a valid date', () => {
                    const value = '2021-09-01T18:00:00.000Z';
                    const field = { fieldType } as DotCMSContentTypeField;

                    expect((getFinalCastedValue(value, field) as Date).toDateString()).toEqual(
                        new Date(value).toDateString()
                    );
                });

                it("should return Date.now if the value is 'now'", () => {
                    const value = 'now';
                    const field = { fieldType } as DotCMSContentTypeField;

                    expect((getFinalCastedValue(value, field) as Date).toDateString()).toEqual(
                        new Date().toDateString()
                    );
                });

                it('should return undefined if the value is undefined', () => {
                    const value = undefined;
                    const field = { fieldType } as DotCMSContentTypeField;

                    expect(getFinalCastedValue(value, field)).toEqual(undefined);
                });
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
            localStorage.clear();
            jest.spyOn(console, 'warn').mockImplementation(() => {});
        });

        afterEach(() => {
            jest.restoreAllMocks();
        });

        describe('getStoredUIState', () => {
            it('should return default state when localStorage is empty', () => {
                const state = getStoredUIState();
                expect(state).toEqual({
                    activeTab: 0,
                    isSidebarOpen: true,
                    activeSidebarTab: 0
                });
            });

            it('should return stored state from localStorage', () => {
                const mockState = {
                    activeTab: 2,
                    isSidebarOpen: false,
                    activeSidebarTab: 1
                };
                localStorage.setItem(UI_STORAGE_KEY, JSON.stringify(mockState));

                const state = getStoredUIState();
                expect(state).toEqual(mockState);
            });

            it('should return default state and warn when localStorage has invalid JSON', () => {
                localStorage.setItem(UI_STORAGE_KEY, 'invalid-json');

                const state = getStoredUIState();
                expect(state).toEqual({
                    activeTab: 0,
                    isSidebarOpen: true,
                    activeSidebarTab: 0
                });
                expect(console.warn).toHaveBeenCalledWith(
                    'Error reading UI state from localStorage:',
                    expect.any(Error)
                );
            });

            it('should return default state and warn when localStorage throws error', () => {
                const mockGetItem = jest.fn(() => {
                    throw new Error('Storage error');
                });

                Object.defineProperty(window, 'localStorage', {
                    value: {
                        getItem: mockGetItem
                    }
                });

                const state = getStoredUIState();
                expect(state).toEqual({
                    activeTab: 0,
                    isSidebarOpen: true,
                    activeSidebarTab: 0
                });
                expect(console.warn).toHaveBeenCalledWith(
                    'Error reading UI state from localStorage:',
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
});
