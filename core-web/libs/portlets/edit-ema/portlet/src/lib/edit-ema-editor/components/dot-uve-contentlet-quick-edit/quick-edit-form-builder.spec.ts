import { describe, expect, it, jest, beforeEach } from '@jest/globals';

import { FormBuilder } from '@angular/forms';

import { DotCMSClazzes, DotCMSDataTypes, DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    buildQuickEditFormGroup,
    buildValidators,
    coerceFieldValue,
    coerceValueToDataType,
    toPageAssetProperties
} from './quick-edit-form-builder';
import { ContentletField } from './types';

const field = (overrides: Partial<ContentletField>): ContentletField =>
    ({
        name: 'Field',
        variable: 'field',
        clazz: DotCMSClazzes.TEXT,
        required: false,
        readOnly: false,
        ...overrides
    }) as ContentletField;

const contentlet = (overrides: Record<string, unknown> = {}): DotCMSContentlet =>
    ({ inode: 'inode-1', identifier: 'id-1', ...overrides }) as unknown as DotCMSContentlet;

describe('quick-edit-form-builder', () => {
    describe('coerceFieldValue', () => {
        it('returns empty string when contentlet is undefined', () => {
            expect(coerceFieldValue(field({ variable: 'title' }), undefined)).toBe('');
        });

        it('returns the raw string value for TEXT fields', () => {
            const value = coerceFieldValue(
                field({ variable: 'title', clazz: DotCMSClazzes.TEXT }),
                contentlet({ title: 'hello' })
            );
            expect(value).toBe('hello');
        });

        describe('CHECKBOX', () => {
            it('splits comma-joined strings into a trimmed array when options exist', () => {
                const value = coerceFieldValue(
                    field({
                        variable: 'tags',
                        clazz: DotCMSClazzes.CHECKBOX,
                        options: [
                            { label: 'A', value: 'a' },
                            { label: 'B', value: 'b' }
                        ]
                    }),
                    contentlet({ tags: 'a, b' })
                );
                expect(value).toEqual(['a', 'b']);
            });

            it('returns [] for empty value with options', () => {
                const value = coerceFieldValue(
                    field({
                        variable: 'tags',
                        clazz: DotCMSClazzes.CHECKBOX,
                        options: [{ label: 'A', value: 'a' }]
                    }),
                    contentlet({})
                );
                expect(value).toEqual([]);
            });

            it('falls through to raw value when no options (binary checkbox)', () => {
                const value = coerceFieldValue(
                    field({ variable: 'flag', clazz: DotCMSClazzes.CHECKBOX }),
                    contentlet({ flag: true })
                );
                expect(value).toBe(true);
            });

            it('falls back to defaultValue (split by comma) when contentlet has no value', () => {
                const value = coerceFieldValue(
                    field({
                        variable: 'tags',
                        clazz: DotCMSClazzes.CHECKBOX,
                        options: [
                            { label: 'A', value: 'a' },
                            { label: 'B', value: 'b' }
                        ],
                        defaultValue: 'a, b'
                    }),
                    contentlet()
                );
                expect(value).toEqual(['a', 'b']);
            });

            it('uses contentlet value over defaultValue when present', () => {
                const value = coerceFieldValue(
                    field({
                        variable: 'tags',
                        clazz: DotCMSClazzes.CHECKBOX,
                        options: [
                            { label: 'A', value: 'a' },
                            { label: 'B', value: 'b' }
                        ],
                        defaultValue: 'a'
                    }),
                    contentlet({ tags: 'b' })
                );
                expect(value).toEqual(['b']);
            });
        });

        describe('MULTI_SELECT', () => {
            it('splits comma-joined string into an array', () => {
                const value = coerceFieldValue(
                    field({ variable: 'tags', clazz: DotCMSClazzes.MULTI_SELECT }),
                    contentlet({ tags: 'a,b ,c' })
                );
                expect(value).toEqual(['a', 'b', 'c']);
            });

            it('returns array as-is when already an array', () => {
                const value = coerceFieldValue(
                    field({ variable: 'tags', clazz: DotCMSClazzes.MULTI_SELECT }),
                    contentlet({ tags: ['a', 'b'] })
                );
                expect(value).toEqual(['a', 'b']);
            });
        });

        describe('IMAGE / FILE', () => {
            it('returns trimmed string when value is a string', () => {
                const value = coerceFieldValue(
                    field({ variable: 'asset', clazz: DotCMSClazzes.IMAGE }),
                    contentlet({ asset: '  identifier-1  ' })
                );
                expect(value).toBe('identifier-1');
            });

            it('extracts identifier from object value', () => {
                const value = coerceFieldValue(
                    field({ variable: 'asset', clazz: DotCMSClazzes.FILE }),
                    contentlet({ asset: { identifier: 'identifier-1' } })
                );
                expect(value).toBe('identifier-1');
            });

            it('returns empty string when nothing usable', () => {
                const value = coerceFieldValue(
                    field({ variable: 'asset', clazz: DotCMSClazzes.IMAGE }),
                    contentlet({ asset: null })
                );
                expect(value).toBe('');
            });
        });

        describe('BINARY', () => {
            it('prefers idPath, then versionPath, then identifier', () => {
                expect(
                    coerceFieldValue(
                        field({ variable: 'bin', clazz: DotCMSClazzes.BINARY }),
                        contentlet({
                            bin: { idPath: 'idp', versionPath: 'vp', identifier: 'id' }
                        })
                    )
                ).toBe('idp');

                expect(
                    coerceFieldValue(
                        field({ variable: 'bin', clazz: DotCMSClazzes.BINARY }),
                        contentlet({ bin: { versionPath: 'vp', identifier: 'id' } })
                    )
                ).toBe('vp');

                expect(
                    coerceFieldValue(
                        field({ variable: 'bin', clazz: DotCMSClazzes.BINARY }),
                        contentlet({ bin: { identifier: 'id' } })
                    )
                ).toBe('id');
            });

            it('returns empty string when no usable key', () => {
                expect(
                    coerceFieldValue(
                        field({ variable: 'bin', clazz: DotCMSClazzes.BINARY }),
                        contentlet({ bin: {} })
                    )
                ).toBe('');
            });
        });

        describe('RADIO', () => {
            it('falls back to defaultValue when contentlet has no value', () => {
                const value = coerceFieldValue(
                    field({
                        variable: 'color',
                        clazz: DotCMSClazzes.RADIO,
                        defaultValue: 'red'
                    }),
                    contentlet()
                );
                expect(value).toBe('red');
            });

            it('uses the contentlet value when present, ignoring defaultValue', () => {
                const value = coerceFieldValue(
                    field({
                        variable: 'color',
                        clazz: DotCMSClazzes.RADIO,
                        defaultValue: 'red'
                    }),
                    contentlet({ color: 'blue' })
                );
                expect(value).toBe('blue');
            });

            it('returns empty string when no value and no defaultValue', () => {
                const value = coerceFieldValue(
                    field({ variable: 'color', clazz: DotCMSClazzes.RADIO }),
                    contentlet()
                );
                expect(value).toBe('');
            });

            it('maps a boolean value to the matching Yes|1/No|0 option string', () => {
                const boolField = field({
                    variable: 'flag',
                    clazz: DotCMSClazzes.RADIO,
                    dataType: DotCMSDataTypes.BOOLEAN,
                    options: [
                        { label: 'Yes', value: '1' },
                        { label: 'No', value: '0' }
                    ]
                });
                expect(coerceFieldValue(boolField, contentlet({ flag: true }))).toBe('1');
                expect(coerceFieldValue(boolField, contentlet({ flag: false }))).toBe('0');
            });
        });

        describe('SELECT', () => {
            const selectBool = (over = {}) =>
                field({
                    variable: 'live',
                    clazz: DotCMSClazzes.SELECT,
                    dataType: DotCMSDataTypes.BOOLEAN,
                    options: [
                        { label: 'Yes', value: '1' },
                        { label: 'No', value: '0' }
                    ],
                    ...over
                });

            it('maps a real boolean from the Page API to the matching option string', () => {
                expect(coerceFieldValue(selectBool(), contentlet({ live: true }))).toBe('1');
                expect(coerceFieldValue(selectBool(), contentlet({ live: false }))).toBe('0');
            });

            it('matches boolean options declared as Yes|true / No|false', () => {
                const f = selectBool({
                    options: [
                        { label: 'Yes', value: 'true' },
                        { label: 'No', value: 'false' }
                    ]
                });
                expect(coerceFieldValue(f, contentlet({ live: true }))).toBe('true');
                expect(coerceFieldValue(f, contentlet({ live: false }))).toBe('false');
            });

            it('stringifies a numeric value so it matches string options', () => {
                const f = field({
                    variable: 'level',
                    clazz: DotCMSClazzes.SELECT,
                    dataType: DotCMSDataTypes.INTEGER,
                    options: [
                        { label: 'One', value: '1' },
                        { label: 'Two', value: '2' }
                    ]
                });
                expect(coerceFieldValue(f, contentlet({ level: 2 }))).toBe('2');
            });

            it('returns a matching string value untouched', () => {
                const f = field({
                    variable: 'color',
                    clazz: DotCMSClazzes.SELECT,
                    options: [
                        { label: 'Red', value: 'red' },
                        { label: 'Blue', value: 'blue' }
                    ]
                });
                expect(coerceFieldValue(f, contentlet({ color: 'blue' }))).toBe('blue');
            });

            it('returns empty string when the contentlet has no value', () => {
                expect(coerceFieldValue(selectBool(), contentlet())).toBe('');
            });
        });
    });

    describe('buildValidators', () => {
        it('returns an empty array when no constraints', () => {
            expect(buildValidators(field({}))).toEqual([]);
        });

        it('adds Validators.required when required is true', () => {
            expect(buildValidators(field({ required: true }))).toHaveLength(1);
        });

        it('adds a pattern validator when regexCheck is a valid regex', () => {
            expect(buildValidators(field({ regexCheck: '^\\d+$' }))).toHaveLength(1);
        });

        it('drops invalid regex and warns instead of throwing', () => {
            const warn = jest.spyOn(console, 'warn').mockImplementation(jest.fn());
            const validators = buildValidators(field({ regexCheck: '[' }));
            expect(validators).toEqual([]);
            expect(warn).toHaveBeenCalled();
            warn.mockRestore();
        });

        it('combines required + regex', () => {
            expect(buildValidators(field({ required: true, regexCheck: '^\\d+$' }))).toHaveLength(
                2
            );
        });
    });

    describe('buildQuickEditFormGroup', () => {
        let fb: FormBuilder;

        beforeEach(() => {
            fb = new FormBuilder();
        });

        it('adds a hidden inode control when contentlet has an inode', () => {
            const form = buildQuickEditFormGroup(fb, [], contentlet({ inode: 'i-1' }));
            expect(form.get('inode')?.value).toBe('i-1');
        });

        it('does not add an inode control when contentlet has no inode', () => {
            const form = buildQuickEditFormGroup(fb, [], { identifier: 'x' } as DotCMSContentlet);
            expect(form.get('inode')).toBeNull();
        });

        it('builds a control per field with the coerced value', () => {
            const form = buildQuickEditFormGroup(
                fb,
                [field({ variable: 'title' })],
                contentlet({ title: 'hi' })
            );
            expect(form.get('title')?.value).toBe('hi');
        });

        it('disables read-only fields', () => {
            const form = buildQuickEditFormGroup(
                fb,
                [field({ variable: 'locked', readOnly: true })],
                contentlet({ locked: 'x' })
            );
            expect(form.get('locked')?.disabled).toBe(true);
        });

        it('attaches required validator', () => {
            const form = buildQuickEditFormGroup(
                fb,
                [field({ variable: 'title', required: true })],
                contentlet({})
            );
            const ctrl = form.get('title');
            expect(ctrl?.invalid).toBe(true);
            expect(ctrl?.errors?.['required']).toBe(true);
        });
    });

    describe('toPageAssetProperties', () => {
        it('passes through values for non-asset fields', () => {
            const result = toPageAssetProperties(
                [field({ variable: 'title', clazz: DotCMSClazzes.TEXT })],
                { title: 'hello' }
            );
            expect(result).toEqual({ title: 'hello' });
        });

        it('wraps IMAGE values as { identifier }', () => {
            const result = toPageAssetProperties(
                [field({ variable: 'photo', clazz: DotCMSClazzes.IMAGE })],
                { photo: 'asset-id' }
            );
            expect(result).toEqual({ photo: { identifier: 'asset-id' } });
        });

        it('wraps FILE values as { identifier }', () => {
            const result = toPageAssetProperties(
                [field({ variable: 'doc', clazz: DotCMSClazzes.FILE })],
                { doc: 'asset-id' }
            );
            expect(result).toEqual({ doc: { identifier: 'asset-id' } });
        });

        it('wraps BINARY values as { idPath }', () => {
            const result = toPageAssetProperties(
                [field({ variable: 'bin', clazz: DotCMSClazzes.BINARY })],
                { bin: 'tmp-path' }
            );
            expect(result).toEqual({ bin: { idPath: 'tmp-path' } });
        });

        it('preserves other keys when wrapping asset fields', () => {
            const result = toPageAssetProperties(
                [
                    field({ variable: 'photo', clazz: DotCMSClazzes.IMAGE }),
                    field({ variable: 'title', clazz: DotCMSClazzes.TEXT })
                ],
                { photo: 'p1', title: 'hi' }
            );
            expect(result).toEqual({
                photo: { identifier: 'p1' },
                title: 'hi'
            });
        });

        it('coerces BOOL option strings ("1"/"0") to real booleans', () => {
            const result = toPageAssetProperties(
                [
                    field({
                        variable: 'live',
                        clazz: DotCMSClazzes.SELECT,
                        dataType: DotCMSDataTypes.BOOLEAN
                    }),
                    field({
                        variable: 'archived',
                        clazz: DotCMSClazzes.RADIO,
                        dataType: DotCMSDataTypes.BOOLEAN
                    })
                ],
                { live: '1', archived: '0' }
            );
            expect(result).toEqual({ live: true, archived: false });
        });

        it('coerces INTEGER string values to numbers', () => {
            const result = toPageAssetProperties(
                [
                    field({
                        variable: 'count',
                        clazz: DotCMSClazzes.TEXT,
                        dataType: DotCMSDataTypes.INTEGER
                    })
                ],
                { count: '5' }
            );
            expect(result).toEqual({ count: 5 });
        });

        it('coerces FLOAT string values to numbers', () => {
            const result = toPageAssetProperties(
                [
                    field({
                        variable: 'price',
                        clazz: DotCMSClazzes.TEXT,
                        dataType: DotCMSDataTypes.FLOAT
                    })
                ],
                { price: '9.99' }
            );
            expect(result).toEqual({ price: 9.99 });
        });

        it('re-joins MULTI_SELECT arrays into a comma string', () => {
            const result = toPageAssetProperties(
                [field({ variable: 'tags', clazz: DotCMSClazzes.MULTI_SELECT })],
                { tags: ['a', 'b', 'c'] }
            );
            expect(result).toEqual({ tags: 'a,b,c' });
        });

        it('re-joins CHECKBOX-with-options arrays into a comma string', () => {
            const result = toPageAssetProperties(
                [
                    field({
                        variable: 'opts',
                        clazz: DotCMSClazzes.CHECKBOX,
                        options: [
                            { label: 'A', value: 'a' },
                            { label: 'B', value: 'b' }
                        ]
                    })
                ],
                { opts: ['a', 'b'] }
            );
            expect(result).toEqual({ opts: 'a,b' });
        });

        it('joins a single-element multi-value array without a trailing comma', () => {
            const result = toPageAssetProperties(
                [field({ variable: 'tags', clazz: DotCMSClazzes.MULTI_SELECT })],
                { tags: ['only'] }
            );
            expect(result).toEqual({ tags: 'only' });
        });

        it('leaves a binary CHECKBOX boolean untouched (not treated as multi-value)', () => {
            const result = toPageAssetProperties(
                [
                    field({
                        variable: 'flag',
                        clazz: DotCMSClazzes.CHECKBOX,
                        dataType: DotCMSDataTypes.BOOLEAN
                    })
                ],
                { flag: true }
            );
            expect(result).toEqual({ flag: true });
        });

        it('leaves untouched typed fields coerced too (not just the edited one)', () => {
            const result = toPageAssetProperties(
                [
                    field({
                        variable: 'featured',
                        clazz: DotCMSClazzes.SELECT,
                        dataType: DotCMSDataTypes.BOOLEAN
                    }),
                    field({ variable: 'title', clazz: DotCMSClazzes.TEXT })
                ],
                { featured: '0', title: 'edited' }
            );
            expect(result).toEqual({ featured: false, title: 'edited' });
        });
    });

    describe('coerceValueToDataType', () => {
        // Mirrors commons-lang3 BooleanUtils.toBoolean(String) (3.18.0):
        // true set (case-insensitive) = true, yes, y, t, on, 1.
        it('maps the commons-lang3 true tokens to boolean true for BOOL', () => {
            for (const token of ['true', 'TRUE', 'yes', 'Yes', 'y', 't', 'on', 'ON', '1']) {
                expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, token)).toBe(true);
            }
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, 1)).toBe(true);
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, true)).toBe(true);
        });

        it('maps everything outside the true set to boolean false for BOOL', () => {
            for (const token of ['0', 'false', 'no', 'n', 'f', 'off', '2', '10', 'maybe', '']) {
                expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, token)).toBe(false);
            }
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, 0)).toBe(false);
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, false)).toBe(false);
        });

        it('does not trim BOOL values (matching commons-lang3)', () => {
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, ' 1')).toBe(false);
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, '1 ')).toBe(false);
        });

        it('parses INTEGER and FLOAT strings to numbers', () => {
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, '42')).toBe(42);
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, '-7')).toBe(-7);
            expect(coerceValueToDataType(DotCMSDataTypes.FLOAT, '3.14')).toBe(3.14);
            expect(coerceValueToDataType(DotCMSDataTypes.FLOAT, '  9.99  ')).toBe(9.99);
        });

        it('leaves numbers as-is when already numeric', () => {
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, 7)).toBe(7);
            expect(coerceValueToDataType(DotCMSDataTypes.FLOAT, 1.5)).toBe(1.5);
        });

        it('does not coerce INTEGER strings that Long.parseLong would reject', () => {
            // decimals, trailing garbage, and whitespace all throw server-side
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, '5.9')).toBe('5.9');
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, '5abc')).toBe('5abc');
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, ' 5')).toBe(' 5');
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, '')).toBe('');
        });

        it('returns TEXT and unknown dataTypes unchanged', () => {
            expect(coerceValueToDataType(DotCMSDataTypes.TEXT, 'hello')).toBe('hello');
            expect(coerceValueToDataType(undefined, 'hello')).toBe('hello');
        });

        it('leaves null, undefined, and array values untouched', () => {
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, null)).toBeNull();
            expect(coerceValueToDataType(DotCMSDataTypes.BOOLEAN, undefined)).toBeUndefined();
            expect(coerceValueToDataType(DotCMSDataTypes.INTEGER, ['a'])).toEqual(['a']);
        });

        it('returns the original value when a FLOAT string cannot be parsed', () => {
            expect(coerceValueToDataType(DotCMSDataTypes.FLOAT, 'abc')).toBe('abc');
        });
    });
});
