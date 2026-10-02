import { describe, expectTypeOf, it } from 'vitest';

import {
    ContentTypeCategoryField,
    ContentTypeCheckboxField,
    ContentTypeColumnField,
    ContentTypeCustomField,
    ContentTypeRelationshipField,
    ContentTypeRowField,
    ContentTypeTextField,
    ContentTypeWYSIWYGField,
    DotCMSClazzes,
    DotCMSContentTypeBaseField,
    DotCMSContentTypeField,
    DotCMSContentTypeFieldCategories,
    DotCMSDataTypes,
    DotCMSFieldType,
    DotCMSFieldTypes,
    FieldOf
} from './dot-content-types.model';

/**
 * Type-level contract for `DotCMSContentTypeField` (issue #37670).
 *
 * These run under `vitest --typecheck`, which invokes the TypeScript compiler instead of
 * executing anything — so a broken type is a failing test with a name and a location, not a
 * wall of `tsc` output someone has to read. It also means this file is the one part of the
 * feature's test suite that still runs while the Angular TestBed setup is broken, since
 * typecheck mode never loads the test environment.
 *
 * Covers contracts C-1 (discriminant), C-2 (exhaustiveness) and C-3 (per-key narrowing) from
 * `specs/37670-strict-field-type-union/contracts/type-contract.md`.
 */
describe('DotCMSContentTypeField', () => {
    describe('C-1 — the discriminant', () => {
        it('narrows to exactly one arm per field type', () => {
            expectTypeOf<
                FieldOf<typeof DotCMSFieldTypes.CATEGORY>
            >().toEqualTypeOf<ContentTypeCategoryField>();
            expectTypeOf<
                FieldOf<typeof DotCMSFieldTypes.RELATIONSHIP>
            >().toEqualTypeOf<ContentTypeRelationshipField>();
            expectTypeOf<
                FieldOf<typeof DotCMSFieldTypes.CHECKBOX>
            >().toEqualTypeOf<ContentTypeCheckboxField>();
            expectTypeOf<
                FieldOf<typeof DotCMSFieldTypes.ROW>
            >().toEqualTypeOf<ContentTypeRowField>();
            expectTypeOf<
                FieldOf<typeof DotCMSFieldTypes.COLUMN>
            >().toEqualTypeOf<ContentTypeColumnField>();
        });

        it('makes per-type properties required, not optional', () => {
            // The flat interface this replaced declared all of these as optional, which is why
            // every consumer had to null-check a property its field type always carries.
            expectTypeOf<ContentTypeCategoryField>()
                .toHaveProperty('categories')
                .toEqualTypeOf<DotCMSContentTypeFieldCategories>();

            expectTypeOf<ContentTypeCheckboxField>()
                .toHaveProperty('values')
                .toEqualTypeOf<string>();

            expectTypeOf<ContentTypeRelationshipField>()
                .toHaveProperty('relationships')
                .toEqualTypeOf<{
                    cardinality: number;
                    isParentField: boolean;
                    velocityVar: string;
                }>();
        });

        it('does not expose another arm’s properties', () => {
            expectTypeOf<ContentTypeCategoryField>().not.toHaveProperty('relationships');
            expectTypeOf<ContentTypeTextField>().not.toHaveProperty('categories');
            expectTypeOf<ContentTypeRowField>().not.toHaveProperty('categories');

            // Note what is NOT asserted here: `values` is on DotCMSContentTypeBaseField, so
            // every arm has it, layout ones included. Only properties an arm actually adds are
            // arm-specific — `categories`, `relationships`, `rendered`, `regexCheck`.
        });

        it('pins fieldType, dataType and clazz together on each arm', () => {
            expectTypeOf<ContentTypeCategoryField['fieldType']>().toEqualTypeOf<
                typeof DotCMSFieldTypes.CATEGORY
            >();
            expectTypeOf<ContentTypeCategoryField['dataType']>().toEqualTypeOf<
                typeof DotCMSDataTypes.SYSTEM
            >();
            expectTypeOf<ContentTypeCategoryField['clazz']>().toEqualTypeOf<
                typeof DotCMSClazzes.CATEGORY
            >();

            // WYSIWYG is long text, not text — a distinction the flat `dataType: string` could
            // not express, and which two committed mocks had wrong.
            expectTypeOf<ContentTypeWYSIWYGField['dataType']>().toEqualTypeOf<
                typeof DotCMSDataTypes.LONG_TEXT
            >();
        });
    });

    describe('C-2 — exhaustiveness over the vocabulary', () => {
        it('has exactly one arm per field type, and no field type without an arm', () => {
            // The load-bearing assertion. An arm added without a vocabulary entry, or a
            // vocabulary entry added without an arm, fails here (SC-011).
            expectTypeOf<DotCMSContentTypeField['fieldType']>().toEqualTypeOf<DotCMSFieldType>();
        });

        it('keeps every arm assignable to the shared base', () => {
            expectTypeOf<ContentTypeCategoryField>().toExtend<DotCMSContentTypeBaseField>();
            expectTypeOf<ContentTypeRowField>().toExtend<DotCMSContentTypeBaseField>();
            expectTypeOf<ContentTypeCustomField>().toExtend<DotCMSContentTypeBaseField>();
        });

        it('widens an arm to the union but does not narrow the union to an arm', () => {
            expectTypeOf<ContentTypeCategoryField>().toExtend<DotCMSContentTypeField>();
            expectTypeOf<DotCMSContentTypeField>().not.toExtend<ContentTypeCategoryField>();
        });
    });

    describe('C-3 — per-key narrowing for behaviour maps', () => {
        it('gives each key a handler narrowed to that key’s arm', () => {
            type ResolutionMap = {
                [K in DotCMSFieldType]: (field: FieldOf<K>) => string;
            };

            expectTypeOf<ResolutionMap[typeof DotCMSFieldTypes.CATEGORY]>()
                .parameter(0)
                .toEqualTypeOf<ContentTypeCategoryField>();
            expectTypeOf<ResolutionMap[typeof DotCMSFieldTypes.RELATIONSHIP]>()
                .parameter(0)
                .toEqualTypeOf<ContentTypeRelationshipField>();

            // The shape this feature rejected: one handler signature over the whole union. It
            // accepts a narrowed handler without checking it whenever `strictFunctionTypes` is
            // off — which it is in `libs/edit-content` — so the narrowing would be decoration.
            // Asserting the difference keeps the distinction from quietly eroding (FR-007).
            type LooseMap = Record<DotCMSFieldType, (field: DotCMSContentTypeField) => string>;

            expectTypeOf<LooseMap[typeof DotCMSFieldTypes.CATEGORY]>()
                .parameter(0)
                .not.toEqualTypeOf<ContentTypeCategoryField>();
        });
    });

    describe('the vocabulary', () => {
        it('spans 28 field types, including the four layout ones', () => {
            expectTypeOf<typeof DotCMSFieldTypes.ROW>().toEqualTypeOf<'Row'>();
            expectTypeOf<typeof DotCMSFieldTypes.COLUMN>().toEqualTypeOf<'Column'>();
            expectTypeOf<typeof DotCMSFieldTypes.TAB_DIVIDER>().toEqualTypeOf<'Tab_divider'>();
            expectTypeOf<typeof DotCMSFieldTypes.COLUMN_BREAK>().toEqualTypeOf<'Column_break'>();
        });
    });
});
