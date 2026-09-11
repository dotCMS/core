import { type JSONContent } from '@tiptap/core';

/**
 * Story Block bodies used across the #36985 specs.
 *
 * Every fixture is the SAME logical document — heading, paragraph, embedded contentlet,
 * paragraph — differing only in the shape of the stored JSON. That is the whole point: the
 * defect is triggered by shape, not by content, so a control and a broken fixture must be
 * indistinguishable once parsed.
 *
 * Shapes are specified in `specs/36985-block-editor-selection-guard/data-model.md`.
 */

/** Identifier of the embedded contentlet; arbitrary, but stable across fixtures. */
export const EMBEDDED_IDENTIFIER = '792c388ae83a66c86418955f3bcb9be4';

const dotContentNode = (): JSONContent => ({
    type: 'dotContent',
    attrs: {
        data: {
            identifier: EMBEDDED_IDENTIFIER,
            languageId: 1,
            inode: '88b0c40d-7b45-461a-9e91-ee5187c4c799',
            title: 'Embedded card (36985 fixture)',
            contentType: 'blockEditorRepro36985',
            baseType: 'CONTENT'
        }
    }
});

/**
 * The shape the CURRENT editor emits: root stats spelled correctly, `indent: 0` present on
 * heading and paragraph, attrs in schema-declaration order (`textAlign`, `indent`, `level`).
 *
 * Round-trips byte-identically, so the guard reports "unchanged" and a click-made selection
 * survives. This is the control — it must behave correctly BEFORE and AFTER the fix.
 */
export const CONTROL_BODY: JSONContent = {
    type: 'doc',
    attrs: { charCount: 118, wordCount: 20, readingTime: 1 },
    content: [
        {
            type: 'heading',
            attrs: { textAlign: 'left', indent: 0, level: 2 },
            content: [{ type: 'text', text: 'Heading above the card' }]
        },
        {
            type: 'paragraph',
            attrs: { textAlign: 'left', indent: 0 },
            content: [{ type: 'text', text: 'Some text before the contentlet.' }]
        },
        dotContentNode(),
        {
            type: 'paragraph',
            attrs: { textAlign: 'left', indent: 0 },
            content: [{ type: 'text', text: 'Some text after the contentlet.' }]
        }
    ]
};

/**
 * Legacy shape, carrying BOTH historical triggers at once:
 *
 * - root `chartCount` — the typo the legacy editor wrote until #26025 (2023-09-11). `stripDocStats`
 *   strips `charCount`, never `chartCount`, and the schema does not declare it, so it survives on
 *   the stored side and vanishes on the editor side.
 * - no `indent` — the legacy editor only began declaring it in #32235 (2025-05-27), so the schema
 *   fills the default on parse.
 *
 * Because it carries both, one assertion against this fixture covers AC-001 and AC-002.
 */
export const BROKEN_BODY: JSONContent = {
    type: 'doc',
    attrs: { chartCount: 118, wordCount: 20, readingTime: 1 },
    content: [
        {
            type: 'heading',
            attrs: { textAlign: 'left', level: 2 },
            content: [{ type: 'text', text: 'Heading above the card' }]
        },
        {
            type: 'paragraph',
            attrs: { textAlign: 'left' },
            content: [{ type: 'text', text: 'Some text before the contentlet.' }]
        },
        dotContentNode(),
        {
            type: 'paragraph',
            attrs: { textAlign: 'left' },
            content: [{ type: 'text', text: 'Some text after the contentlet.' }]
        }
    ]
};

/**
 * The LIVE divergence, not a historical one: the legacy editor configures `TextAlign` with
 * `['heading','paragraph','listItem','dotImage']` while the new editor uses
 * `['heading','paragraph']`, so `listItem.attrs.textAlign` is written today and dropped on parse
 * today. This is what makes the flag round-trip in spec §Reproduction Option A reproduce.
 */
export const LIST_TEXT_ALIGN_BODY: JSONContent = {
    type: 'doc',
    content: [
        {
            type: 'paragraph',
            attrs: { textAlign: 'left', indent: 0 },
            content: [{ type: 'text', text: 'Intro' }]
        },
        {
            type: 'bulletList',
            content: [
                {
                    type: 'listItem',
                    attrs: { textAlign: null },
                    content: [
                        {
                            type: 'paragraph',
                            attrs: { textAlign: 'left', indent: 0 },
                            content: [{ type: 'text', text: 'one' }]
                        }
                    ]
                },
                {
                    type: 'listItem',
                    attrs: { textAlign: null },
                    content: [
                        {
                            type: 'paragraph',
                            attrs: { textAlign: 'left', indent: 0 },
                            content: [{ type: 'text', text: 'two' }]
                        }
                    ]
                }
            ]
        },
        dotContentNode()
    ]
};

/**
 * Semantically identical to {@link CONTROL_BODY}, but with every `attrs` object re-serialized in
 * ALPHABETICAL key order (`indent`, `level`, `textAlign`) instead of schema order.
 *
 * This is what `/api/v1/content/<id>` returns. It means a byte comparison fails for content that
 * is otherwise perfectly current — the trigger that would break every document at once if any
 * host, importer or migration normalized key order. The JSP escapes it today only because it
 * reads the raw stored string.
 */
export const ALPHABETICAL_KEY_ORDER_BODY: JSONContent = {
    type: 'doc',
    attrs: { charCount: 118, readingTime: 1, wordCount: 20 },
    content: [
        {
            type: 'heading',
            attrs: { indent: 0, level: 2, textAlign: 'left' },
            content: [{ type: 'text', text: 'Heading above the card' }]
        },
        {
            type: 'paragraph',
            attrs: { indent: 0, textAlign: 'left' },
            content: [{ type: 'text', text: 'Some text before the contentlet.' }]
        },
        dotContentNode(),
        {
            type: 'paragraph',
            attrs: { indent: 0, textAlign: 'left' },
            content: [{ type: 'text', text: 'Some text after the contentlet.' }]
        }
    ]
};

/** A genuinely different document — the guard must still report "changed" for this. */
export const DIFFERENT_BODY: JSONContent = {
    type: 'doc',
    content: [
        {
            type: 'paragraph',
            attrs: { textAlign: 'left', indent: 0 },
            content: [{ type: 'text', text: 'Completely different content.' }]
        }
    ]
};

/** Deep clone, so a spec can hand a NEW object reference carrying EQUAL content (Contract A row A4). */
export const cloneBody = (body: JSONContent): JSONContent =>
    JSON.parse(JSON.stringify(body)) as JSONContent;
