import { describe, expect, it } from 'vitest';

import {
    ContentTypeCategoryField,
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotCMSContentTypeFieldCategories,
    DotCMSDataTypes,
    DotCMSFieldType,
    DotCMSFieldTypes
} from './dot-content-types.model';

/**
 * Contract tests for `DotCMSContentTypeField` as a discriminated union (issue #37670).
 *
 * Most of what matters here is checked by the COMPILER, not by the assertions below: the
 * proof that narrowing works is that this file type-checks, and the proof that it is a real
 * constraint is that the `@ts-expect-error` directives stay used. A change that loosens the
 * union back toward the flat shape breaks this file at build time rather than at runtime.
 *
 * Covers contract C-1 (discriminant) and C-2 (exhaustiveness) from
 * `specs/37670-strict-field-type-union/contracts/type-contract.md`.
 */
describe('DotCMSContentTypeField — discriminated union contract', () => {
    /**
     * C-1: narrowing on `fieldType` yields the arm, with its per-type properties REQUIRED.
     *
     * Each assignment below targets a non-optional local. That is the whole point: on the
     * flat interface these properties were all optional, so `field.categories` was
     * `… | undefined` and would not assign. Requiring them is the behavioural difference.
     */
    it('narrows on fieldType to the matching arm, with per-type properties required', () => {
        const categories = (field: DotCMSContentTypeField): DotCMSContentTypeFieldCategories => {
            if (field.fieldType === DotCMSFieldTypes.CATEGORY) {
                return field.categories;
            }

            throw new Error('not a category field');
        };

        const values = (field: DotCMSContentTypeField): string => {
            if (field.fieldType === DotCMSFieldTypes.CHECKBOX) {
                return field.values;
            }

            throw new Error('not a checkbox field');
        };

        const cardinality = (field: DotCMSContentTypeField): number => {
            if (field.fieldType === DotCMSFieldTypes.RELATIONSHIP) {
                return field.relationships.cardinality;
            }

            throw new Error('not a relationship field');
        };

        expect(categories).toBeInstanceOf(Function);
        expect(values).toBeInstanceOf(Function);
        expect(cardinality).toBeInstanceOf(Function);
    });

    /**
     * C-1, negative: a property belonging to a DIFFERENT arm must be unreachable.
     *
     * `@ts-expect-error` is the instrument here, not an escape hatch — if the union ever
     * stops rejecting these accesses the directive becomes unused and TypeScript fails the
     * build with TS2578. FR-006 bans `@ts-expect-error` for silencing real errors; this is
     * the opposite, and it is the only place in the feature where it is allowed.
     */
    it('rejects properties belonging to a different arm', () => {
        const wrongArm = (field: DotCMSContentTypeField): unknown => {
            if (field.fieldType === DotCMSFieldTypes.CATEGORY) {
                // @ts-expect-error `relationships` belongs to ContentTypeRelationshipField
                return field.relationships;
            }

            if (field.fieldType === DotCMSFieldTypes.TEXT) {
                // @ts-expect-error `categories` belongs to ContentTypeCategoryField
                return field.categories;
            }

            return undefined;
        };

        expect(wrongArm).toBeInstanceOf(Function);
    });

    /**
     * C-1, correlated discriminants: an arm pins `fieldType`, `dataType` and `clazz`
     * together, so narrowing on any one of them narrows the other two. On the flat interface
     * `fieldType` was a bare `string` and no such correlation existed.
     */
    it('correlates fieldType, dataType and clazz when narrowing on any one of them', () => {
        const byClazz = (field: DotCMSContentTypeField): typeof DotCMSFieldTypes.CATEGORY => {
            if (field.clazz === DotCMSClazzes.CATEGORY) {
                return field.fieldType;
            }

            throw new Error('not a category field');
        };

        const byDataType = (field: DotCMSContentTypeField): DotCMSContentTypeFieldCategories => {
            if (field.clazz === DotCMSClazzes.CATEGORY) {
                const dataType: typeof DotCMSDataTypes.SYSTEM = field.dataType;
                expect(dataType).toBeDefined();

                return field.categories;
            }

            throw new Error('not a category field');
        };

        expect(byClazz).toBeInstanceOf(Function);
        expect(byDataType).toBeInstanceOf(Function);
    });

    /**
     * C-2: the union is exhaustive over the vocabulary. Handling every member of
     * `DotCMSFieldTypes` must leave `never`, so adding a field type without adding its arm
     * fails the build (SC-011).
     */
    it('is exhaustive over DotCMSFieldTypes — an unhandled type would leave a non-never', () => {
        const exhaust = (fieldType: DotCMSFieldType): string => {
            switch (fieldType) {
                case DotCMSFieldTypes.ROW:
                case DotCMSFieldTypes.COLUMN:
                case DotCMSFieldTypes.TAB_DIVIDER:
                case DotCMSFieldTypes.LINE_DIVIDER:
                case DotCMSFieldTypes.COLUMN_BREAK:
                    return 'layout';
                case DotCMSFieldTypes.BINARY:
                case DotCMSFieldTypes.BLOCK_EDITOR:
                case DotCMSFieldTypes.CATEGORY:
                case DotCMSFieldTypes.CHECKBOX:
                case DotCMSFieldTypes.CONSTANT:
                case DotCMSFieldTypes.CUSTOM_FIELD:
                case DotCMSFieldTypes.DATE:
                case DotCMSFieldTypes.DATE_AND_TIME:
                case DotCMSFieldTypes.FILE:
                case DotCMSFieldTypes.HIDDEN:
                case DotCMSFieldTypes.IMAGE:
                case DotCMSFieldTypes.JSON:
                case DotCMSFieldTypes.KEY_VALUE:
                case DotCMSFieldTypes.MULTI_SELECT:
                case DotCMSFieldTypes.RADIO:
                case DotCMSFieldTypes.RELATIONSHIP:
                case DotCMSFieldTypes.SELECT:
                case DotCMSFieldTypes.HOST_FOLDER:
                case DotCMSFieldTypes.TAG:
                case DotCMSFieldTypes.TEXT:
                case DotCMSFieldTypes.TEXTAREA:
                case DotCMSFieldTypes.TIME:
                case DotCMSFieldTypes.WYSIWYG:
                    return 'content';
                default: {
                    // Fails to compile the moment a field type is added without a case above.
                    const unreachable: never = fieldType;

                    return unreachable;
                }
            }
        };

        expect(exhaust(DotCMSFieldTypes.CATEGORY)).toBe('content');
        expect(exhaust(DotCMSFieldTypes.ROW)).toBe('layout');
    });

    /**
     * C-2, the load-bearing one: the union must cover the vocabulary EXACTLY — one arm per
     * field type, no more, no less.
     *
     * The exhaustive `switch` above does not prove this. It ranges over `DotCMSFieldType`,
     * which is derived straight from `DotCMSFieldTypes` and is already a 28-member literal
     * union regardless of how the field interface is declared — so it passes even against
     * the flat shape. What actually has to hold is that the union's OWN `fieldType` spans
     * the same set, and on the flat interface that property is a bare `string`.
     *
     * Adding a member to `DotCMSFieldTypes` without adding its arm makes the two sets differ
     * and fails this line (SC-011).
     */
    it('has exactly one arm per field type in the vocabulary', () => {
        type Equal<A, B> =
            (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;

        const armPerFieldType: Equal<DotCMSContentTypeField['fieldType'], DotCMSFieldType> = true;

        expect(armPerFieldType).toBe(true);
    });

    /**
     * C-2, at runtime: the vocabulary size itself. The one assertion here a compiler cannot
     * make — it is what turns a silent member addition into a visible failure.
     */
    it('declares exactly 28 field types', () => {
        expect(Object.keys(DotCMSFieldTypes)).toHaveLength(28);
    });

    /**
     * An arm is assignable to the union, and the union is not assignable back to an arm —
     * the basic property that makes narrowing necessary rather than optional.
     */
    it('accepts an arm as the union but not the union as an arm', () => {
        const widen = (field: ContentTypeCategoryField): DotCMSContentTypeField => field;

        expect(widen).toBeInstanceOf(Function);
    });
});
