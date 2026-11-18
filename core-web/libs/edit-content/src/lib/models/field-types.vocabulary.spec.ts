import { describe, expect, it } from 'vitest';

import { DotCMSFieldType, DotCMSFieldTypes } from '@dotcms/dotcms-models';

import { resolutionValue } from '../components/dot-edit-content-form/dot-edit-content-form-resolutions';

/**
 * Contract tests for the single field-type vocabulary (issue #37670, FR-005).
 *
 * `libs/edit-content` used to declare its own `FIELD_TYPES` enum and a `FIELD_TYPES_CONST`
 * object carrying the same string values as `DotCMSFieldTypes` in `@dotcms/dotcms-models` —
 * three declarations of one set. Because the content-model union discriminates on
 * `DotCMSFieldTypes` while this library compared against its own enum, the two were distinct
 * types and every comparison needed an `as FIELD_TYPES` assertion. Collapsing them is what
 * makes those assertions removable rather than merely relocatable.
 *
 * Most of this is checked by the COMPILER; the runtime assertions guard the counts.
 */
describe('field-type vocabulary', () => {
    /**
     * FR-008 / contract C-2: a map that must cover every field type is exhaustive by
     * construction. Keyed by the shared vocabulary, `resolutionValue` must carry all 28
     * entries — the local enum had 24, missing the four layout types, so this fails to
     * compile until the map is completed.
     */
    it('resolutionValue covers every field type in the shared vocabulary', () => {
        const everyFieldType = Object.values(DotCMSFieldTypes) as DotCMSFieldType[];

        for (const fieldType of everyFieldType) {
            expect(resolutionValue[fieldType]).toBeInstanceOf(Function);
        }

        expect(Object.keys(resolutionValue)).toHaveLength(everyFieldType.length);
    });

    /**
     * The four layout types the removed local enum never carried. Listed explicitly because
     * they are the ones most likely to be forgotten: nothing in the edit form renders a row
     * or a column as a field, so a missing entry would only surface at runtime.
     */
    it('includes the four layout types the old local enum lacked', () => {
        expect(resolutionValue[DotCMSFieldTypes.ROW]).toBeInstanceOf(Function);
        expect(resolutionValue[DotCMSFieldTypes.COLUMN]).toBeInstanceOf(Function);
        expect(resolutionValue[DotCMSFieldTypes.TAB_DIVIDER]).toBeInstanceOf(Function);
        expect(resolutionValue[DotCMSFieldTypes.COLUMN_BREAK]).toBeInstanceOf(Function);
    });

    /**
     * The swap is value-identical, which is what makes it runtime-inert: every string the
     * removed enum defined is still produced by the surviving vocabulary. If this ever fails,
     * a comparison somewhere changed meaning rather than merely changing its import.
     */
    it('preserves the string values the removed enum defined', () => {
        expect(DotCMSFieldTypes.TEXT).toBe('Text');
        expect(DotCMSFieldTypes.BLOCK_EDITOR).toBe('Story-Block');
        expect(DotCMSFieldTypes.HOST_FOLDER).toBe('Host-Folder');
        expect(DotCMSFieldTypes.CONSTANT).toBe('Constant-Field');
        expect(DotCMSFieldTypes.CUSTOM_FIELD).toBe('Custom-Field');
        expect(DotCMSFieldTypes.DATE_AND_TIME).toBe('Date-and-Time');
        expect(DotCMSFieldTypes.HIDDEN).toBe('Hidden-Field');
        expect(DotCMSFieldTypes.JSON).toBe('JSON-Field');
        expect(DotCMSFieldTypes.KEY_VALUE).toBe('Key-Value');
        expect(DotCMSFieldTypes.MULTI_SELECT).toBe('Multi-Select');
        expect(DotCMSFieldTypes.LINE_DIVIDER).toBe('Line_divider');
    });
});
