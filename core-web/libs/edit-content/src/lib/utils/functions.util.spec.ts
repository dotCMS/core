import { it, describe, expect } from '@jest/globals';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';

import * as functionsUtil from './functions.util';
import { CALENDAR_FIELD_TYPES } from './mocks';

import { FLATTENED_FIELD_TYPES } from '../models/dot-edit-content-field.constant';
import { DotEditContentFieldSingleSelectableDataType } from '../models/dot-edit-content-field.enum';

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
                ).toBeTruthy();
            });

            it('should return false if value is false', () => {
                expect(
                    castSingleSelectableValue(
                        'false',
                        DotEditContentFieldSingleSelectableDataType.BOOL
                    )
                ).toBeFalsy();
            });
        });

        describe('Integer', () => {
            it('should return number if value is number', () => {
                expect(
                    castSingleSelectableValue(
                        '1',
                        DotEditContentFieldSingleSelectableDataType.INTEGER
                    )
                ).toBe(1);
            });

            it('should return NaN if value is not a number', () => {
                expect(
                    castSingleSelectableValue(
                        'a',
                        DotEditContentFieldSingleSelectableDataType.INTEGER
                    )
                ).toBe(NaN);
            });
        });

        describe('Float', () => {
            it('should return number if value is number', () => {
                expect(
                    castSingleSelectableValue(
                        '1.3234',
                        DotEditContentFieldSingleSelectableDataType.FLOAT
                    )
                ).toBe(1.3234);
            });

            it('should return NaN if value is not a number', () => {
                expect(
                    castSingleSelectableValue(
                        'a',
                        DotEditContentFieldSingleSelectableDataType.FLOAT
                    )
                ).toBe(NaN);
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
    });

    describe('should cast the values when a type is passed', () => {
        it('should cast for integer', () => {
            expect(
                getSingleSelectableFieldOptions(
                    'some label\r\n3\r\nsome label 3|i have value\r\nfour|4',
                    DotEditContentFieldSingleSelectableDataType.INTEGER
                )
            ).toEqual([
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
            ]);
        });

        it('should cast for float', () => {
            expect(
                getSingleSelectableFieldOptions(
                    'some label\r\n234.21\r\nsome label 3|i have value\r\nfour dot five|4.5',
                    DotEditContentFieldSingleSelectableDataType.FLOAT
                )
            ).toEqual([
                {
                    label: 'some label',
                    value: NaN
                },
                {
                    label: '234.21',
                    value: 234.21
                },
                {
                    label: 'some label 3',
                    value: NaN
                },
                {
                    label: 'four dot five',
                    value: 4.5
                }
            ]);
        });

        it('should cast for bool', () => {
            expect(
                getSingleSelectableFieldOptions(
                    'some label\r\nfalse\r\nsome label 3|true\r\ntrue|true',
                    DotEditContentFieldSingleSelectableDataType.BOOL
                )
            ).toEqual([
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
            ]);
        });
    });

    describe('getFinalCastedValue', () => {
        describe.each([...CALENDAR_FIELD_TYPES])('Calendar Fields', (fieldType) => {
            it('should parse the date if the value is a valid date', () => {
                const value = '2021-09-01T18:00:00.000Z';
                const field = { fieldType } as DotCMSContentTypeField;

                expect(getFinalCastedValue(value, field)).toEqual(new Date(value));
            });

            it("should return Date.now if the value is 'now'", () => {
                const value = 'now';
                const field = { fieldType } as DotCMSContentTypeField;

                expect(getFinalCastedValue(value, field)).toEqual(new Date());
            });

            it('should return undefined if the value is undefined', () => {
                const value = undefined;
                const field = { fieldType } as DotCMSContentTypeField;

                expect(getFinalCastedValue(value, field)).toEqual(undefined);
            });
        });

        describe.each([...FLATTENED_FIELD_TYPES])('Flattened Fields', (fieldType) => {
            it('should return an array of the values', () => {
                const value = 'value1,value2,value3';
                const field = { fieldType } as DotCMSContentTypeField;

                expect(getFinalCastedValue(value, field)).toEqual(['value1', 'value2', 'value3']);
            });

            it('should trim the values', () => {
                const value = ' value1 , value2 , value3 ';
                const field = { fieldType } as DotCMSContentTypeField;

                expect(getFinalCastedValue(value, field)).toEqual(['value1', 'value2', 'value3']);
            });

            it('should return undefined if the value is undefined', () => {
                const value = undefined;
                const field = { fieldType } as DotCMSContentTypeField;

                expect(getFinalCastedValue(value, field)).toEqual(undefined);
            });
        });

        describe('No special type', () => {
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
    });
});
