import { describe, expect, it, jest } from '@jest/globals';

import {
    DotCMSContentTypeFieldVariable,
    DotCMSDataTypes,
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

import { NON_FORM_CONTROL_FIELD_TYPES } from '../models/dot-edit-content-field.constant';

describe('Utils Functions', () => {
    const originalWarn = console.warn;

    beforeAll(() => {
        console.warn = jest.fn();
    });

    afterAll(() => {
        console.warn = originalWarn;
    });

    const { castSingleSelectableValue, getSingleSelectableFieldOptions } = functionsUtil;

    describe('castSingleSelectableValue', () => {
        describe('null/undefined/empty handling', () => {
            it('should return null for null value', () => {
                expect(castSingleSelectableValue(null, DotCMSDataTypes.BOOLEAN)).toBeNull();
            });

            it('should return null for undefined value', () => {
                expect(castSingleSelectableValue(undefined, DotCMSDataTypes.BOOLEAN)).toBeNull();
            });

            it('should return null for empty string', () => {
                expect(castSingleSelectableValue('', DotCMSDataTypes.BOOLEAN)).toBeNull();
            });
        });

        describe('Boolean', () => {
            const type = DotCMSDataTypes.BOOLEAN;

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
                const type = DotCMSDataTypes.INTEGER;

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
                const type = DotCMSDataTypes.FLOAT;

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
            const unknownType = 'unknown' as unknown as DotCMSDataTypes;

            it('should convert number to string for unknown type', () => {
                expect(castSingleSelectableValue(42, unknownType)).toBe('42');
            });

            it('should convert boolean to string for unknown type', () => {
                expect(castSingleSelectableValue(true, unknownType)).toBe('true');
            });

            it('should handle object by converting to string', () => {
                const obj = { test: 'value' };
                expect(castSingleSelectableValue(obj, unknownType)).toBe(String(obj));
            });

            it('should return string as is for unknown type', () => {
                expect(castSingleSelectableValue('test', unknownType)).toBe('test');
            });
        });
    });

    describe('getSingleSelectableFieldOptions', () => {
        it('should return an array of objects with label and value', () => {
            expect(
                getSingleSelectableFieldOptions('some label|some value', DotCMSDataTypes.TEXT)
            ).toEqual([
                {
                    label: 'some label',
                    value: 'some value'
                }
            ]);
        });

        it('should return an array of objects with label and value even if value is not provided', () => {
            expect(getSingleSelectableFieldOptions('some label', DotCMSDataTypes.TEXT)).toEqual([
                {
                    label: 'some label',
                    value: 'some label'
                }
            ]);
        });

        it('should return an empty array if options is empty', () => {
            expect(getSingleSelectableFieldOptions('', DotCMSDataTypes.TEXT)).toEqual([]);
        });

        it('should support multiline options', () => {
            expect(
                getSingleSelectableFieldOptions(
                    'some label\r\nsome label 2\r\nsome label 3|i have value',
                    DotCMSDataTypes.TEXT
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
                    DotCMSDataTypes.TEXT
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
                dataType: DotCMSDataTypes.INTEGER,
                expected: [
                    {
                        label: '3',
                        value: 3
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
                dataType: DotCMSDataTypes.FLOAT,
                expected: [
                    {
                        label: '3.14',
                        value: 3.14
                    },
                    {
                        label: 'four dot five',
                        value: 4.5
                    }
                ]
            },
            {
                optionsString: 'some label\r\nfalse\r\nsome label 3|true\r\ntrue|true',
                dataType: DotCMSDataTypes.BOOLEAN,
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
                    activeSidebarTab: 0
                });
            });

            it('should return stored state from sessionStorage', () => {
                const mockState = {
                    activeTab: 2,
                    isSidebarOpen: false,
                    activeSidebarTab: 1
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
                    activeSidebarTab: 0
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
                    activeSidebarTab: 0
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
                const fieldType = field.fieldType;
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
});
