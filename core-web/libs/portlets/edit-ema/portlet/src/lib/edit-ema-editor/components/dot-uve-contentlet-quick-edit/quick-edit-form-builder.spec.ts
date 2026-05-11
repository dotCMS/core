import { describe, expect, it, jest, beforeEach } from '@jest/globals';

import { FormBuilder } from '@angular/forms';

import { DotCMSClazzes, DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    buildQuickEditFormGroup,
    buildValidators,
    coerceFieldValue,
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
    });
});
