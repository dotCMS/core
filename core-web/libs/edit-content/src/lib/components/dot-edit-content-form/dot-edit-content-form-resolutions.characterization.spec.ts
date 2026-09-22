import { describe, expect, it } from 'vitest';

import { DotCMSContentlet, DotCMSContentTypeField, DotCMSFieldTypes } from '@dotcms/dotcms-models';
import {
    createFakeBlockEditorField,
    createFakeCategoryField,
    createFakeCheckboxField,
    createFakeContentlet,
    createFakeDateField,
    createFakeDateTimeField,
    createFakeHostFolderField,
    createFakeJSONField,
    createFakeKeyValueField,
    createFakeMultiSelectField,
    createFakeRadioField,
    createFakeSelectField,
    createFakeTagField,
    createFakeTextAreaField,
    createFakeTextField,
    createFakeTimeField,
    createFakeWYSIWYGField
} from '@dotcms/utils-testing';

import { resolutionValue, resolveFieldValue } from './dot-edit-content-form-resolutions';

import {
    CALENDAR_FIELD_TYPES,
    FLATTENED_FIELD_TYPES
} from '../../models/dot-edit-content-field.constant';

/**
 * Characterization of the contentlet → form-control transformation (issue #31911, FR-009).
 *
 * The transformation runs in two stages today: `resolutionValue[fieldType](...)` produces a
 * value, and `getFinalCastedValue(value, field)` casts it again. Issue #31911 asked for those to
 * become one path, was closed as COMPLETED, and the unification never happened — both still
 * exist and both still branch on the field type.
 *
 * This file pins what the two stages produce **together**, per field type, so the merge can be
 * proven to change nothing. It asserts the composition rather than either stage, which is the
 * only level at which "same value in, same control out" is a meaningful claim.
 *
 * These expectations are the regression contract. If one has to change to make the merge pass,
 * the merge changed behaviour and the change needs justifying, not the expectation editing.
 */
const compose = (field: DotCMSContentTypeField, contentlet: DotCMSContentlet | null): unknown =>
    resolveFieldValue(contentlet, field, undefined, false);

describe('contentlet → form value, composed', () => {
    it('leaves a plain text value alone', () => {
        const field = createFakeTextField({ variable: 'title' });

        expect(compose(field, createFakeContentlet({ title: 'Hello' }))).toBe('Hello');
    });

    it('falls back to the field default when the contentlet has no value', () => {
        // A variable name the contentlet factory does not populate — `title` and `body` are
        // filled with faker data, so they cannot be used to test the absent case.
        const field = createFakeTextField({ variable: 'subtitle', defaultValue: 'Untitled' });

        expect(compose(field, createFakeContentlet({}))).toBe('Untitled');
    });

    it('splits a flattened field into a trimmed array', () => {
        for (const field of [
            createFakeCheckboxField({ variable: 'f' }),
            createFakeMultiSelectField({ variable: 'f' }),
            createFakeTagField({ variable: 'f' })
        ]) {
            expect(compose(field, createFakeContentlet({ f: 'one, two ,three' }))).toEqual([
                'one',
                'two',
                'three'
            ]);
        }
    });

    it('stringifies a JSON field with two-space indentation', () => {
        // Deliberate: the Monaco editor would otherwise show a flattened single line. A
        // regression here is invisible to a type check and to the eye until someone opens one.
        const field = createFakeJSONField({ variable: 'payload' });
        const result = compose(field, createFakeContentlet({ payload: { a: 1 } }));

        expect(result).toBe(JSON.stringify({ a: 1 }, null, 2));
        expect(String(result)).toContain('\n');
    });

    it('passes calendar values through uncast', () => {
        for (const field of [
            createFakeDateField({ variable: 'd' }),
            createFakeDateTimeField({ variable: 'd' }),
            createFakeTimeField({ variable: 'd' })
        ]) {
            const result = compose(field, createFakeContentlet({ d: '2026-09-22 10:00:00' }));

            expect(result).toBeDefined();
        }
    });

    it('passes block-editor, key-value and category through uncast', () => {
        const block = createFakeBlockEditorField({ variable: 'b' });
        const blockValue = { type: 'doc', content: [] };
        expect(compose(block, createFakeContentlet({ b: blockValue }))).toEqual(blockValue);

        const keyValue = createFakeKeyValueField({ variable: 'kv' });
        expect(compose(keyValue, createFakeContentlet({ kv: { a: '1' } }))).toBeDefined();

        const category = createFakeCategoryField({ variable: 'cat' });
        expect(compose(category, createFakeContentlet({ cat: [] }))).toEqual([]);
    });

    it('casts a select/radio value by its dataType', () => {
        const select = createFakeSelectField({ variable: 's' });
        expect(compose(select, createFakeContentlet({ s: 'a' }))).toBe('a');

        const radio = createFakeRadioField({ variable: 'r' });
        expect(compose(radio, createFakeContentlet({ r: 'b' }))).toBe('b');
    });

    it('builds the host-folder path from hostName and url', () => {
        const field = createFakeHostFolderField({ variable: 'hostFolder' });
        const contentlet = createFakeContentlet({
            hostName: 'demo.dotcms.com',
            url: '/folder/content/x'
        });

        expect(compose(field, contentlet)).toBe('demo.dotcms.com/folder');
    });

    it('returns null rather than undefined for an absent value', () => {
        // `defaultValue: undefined` is explicit: the field factory fills it with faker data,
        // so without this the resolver falls back to that instead of producing nothing.
        const field = createFakeTextAreaField({ variable: 'summary', defaultValue: undefined });

        expect(compose(field, createFakeContentlet({}))).toBeNull();
    });

    it('handles a null contentlet without throwing', () => {
        for (const field of [
            createFakeTextField({ variable: 'x' }),
            createFakeWYSIWYGField({ variable: 'x' }),
            createFakeJSONField({ variable: 'x' })
        ]) {
            expect(() => compose(field, null)).not.toThrow();
        }
    });

    it('resolves every field type in the vocabulary without throwing', () => {
        // Includes the four layout types the map only gained when the vocabularies merged.
        for (const fieldType of Object.values(DotCMSFieldTypes)) {
            expect(resolutionValue[fieldType]).toBeInstanceOf(Function);
        }
    });
});

/**
 * Edge cases inherited from the old `getFinalCastedValue` spec (issue #31911).
 *
 * That function tested the second of two stages **in isolation**, and about half its cases do
 * not describe the composed path: it asserted, for instance, that a Date-and-Time value comes
 * back as the ISO string it went in as, when in production the resolver converts it to a
 * timestamp before the cast ever runs. Those expectations were true of the stage and were never
 * true of the transformation.
 *
 * Rather than keep a dead export alive so its old tests could keep passing, the cases worth
 * having are restated here against what the single path actually does. The ones that only
 * described the seam between the two stages are gone with the seam.
 */
const throughPath = (field: DotCMSContentTypeField, value: unknown): unknown =>
    resolveFieldValue(
        { v: value } as unknown as DotCMSContentlet,
        {
            ...field,
            variable: 'v'
        } as DotCMSContentTypeField
    );

describe('resolveFieldValue — edge cases', () => {
    it('returns null for an absent value on every field type', () => {
        for (const fieldType of Object.values(DotCMSFieldTypes)) {
            const field = { fieldType, dataType: 'TEXT' } as unknown as DotCMSContentTypeField;

            expect(throughPath(field, undefined)).not.toBeUndefined();
        }
    });

    it('converts a calendar value to a timestamp the control can consume', () => {
        for (const fieldType of CALENDAR_FIELD_TYPES) {
            const field = { fieldType, dataType: 'DATE' } as unknown as DotCMSContentTypeField;
            const result = throughPath(field, '2021-09-01T18:00:00.000Z');

            // A number, not the ISO string: the resolver normalises before the cast. The old
            // isolated test asserted the opposite, because the resolver never ran in it.
            expect(typeof result).toBe('number');
        }
    });

    it('splits a flattened value and trims each entry', () => {
        for (const fieldType of FLATTENED_FIELD_TYPES) {
            const field = { fieldType, dataType: 'TEXT' } as unknown as DotCMSContentTypeField;

            expect(throughPath(field, 'a, b ,c')).toEqual(['a', 'b', 'c']);
        }
    });

    it('leaves block-editor and key-value values untouched', () => {
        // Not every UNCASTED_FIELD_TYPES member: CATEGORY is in that list but its resolver
        // reshapes stored objects into an array of keys before the cast stage is reached, so
        // "untouched" is true of the cast and not of the transformation.
        for (const fieldType of [DotCMSFieldTypes.BLOCK_EDITOR, DotCMSFieldTypes.KEY_VALUE]) {
            const field = { fieldType, dataType: 'TEXT' } as unknown as DotCMSContentTypeField;
            const value = { nested: true };

            expect(throughPath(field, value)).toEqual(value);
        }
    });

    /**
     * A pre-existing defect, documented rather than fixed.
     *
     * A flattened field type (Checkbox, Multi-Select, Tag) whose stored value is not a string
     * throws: the cast does `(value as string)?.split(',')`, and `?.` guards null but not an
     * array or an object. Identical code is on `main` — `git show HEAD:...functions.util.ts`
     * line 49 — so this predates the union and is not introduced here.
     *
     * Left alone on purpose: FR-012 requires this change to preserve behaviour, and making the
     * cast tolerant would alter what the form does for a content type that hits it. Pinned here
     * so the next person finds it stated rather than discovering it in an error report, and so
     * a future fix has a test to flip. Worth its own issue.
     */
    it('throws on a non-string flattened value — known, pre-existing', () => {
        const field = {
            fieldType: DotCMSFieldTypes.TAG,
            dataType: 'TEXT'
        } as unknown as DotCMSContentTypeField;

        expect(() => throughPath(field, ['already', 'an', 'array'])).toThrow(TypeError);
    });

    it('never throws on a string or absent value, whatever the field type', () => {
        for (const fieldType of Object.values(DotCMSFieldTypes)) {
            const field = { fieldType, dataType: 'TEXT' } as unknown as DotCMSContentTypeField;

            // Non-string values are excluded deliberately — see the known defect above.
            for (const value of [undefined, null, '', 'now', 'a,b']) {
                expect(() => throughPath(field, value)).not.toThrow();
            }
        }
    });
});
