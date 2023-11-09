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
    });
});
