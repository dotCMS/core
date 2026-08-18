import { type JSONContent } from '@tiptap/core';

/** Root-level document-stat attrs stamped onto the emitted value for legacy-parity output. */
export const DOC_STAT_ATTRS = ['charCount', 'wordCount', 'readingTime'] as const;

/**
 * Strips the document-stat attrs (`charCount` / `wordCount` / `readingTime`) that the editor stamps
 * onto its emitted value for legacy parity.
 *
 * `editor.getJSON()` never carries these root attrs, so a round-tripped value would otherwise never
 * compare equal to the editor's own document — making the "did the value actually change?" guard
 * permanently false once there is any text. That fires a `setContent` on every value round-trip,
 * which rebuilds the doc and clobbers the current selection (e.g. a just-clicked contentlet card,
 * #36985). Comparing on the stats-free shape keeps the guard symmetric with what is emitted.
 *
 * Returns the input untouched when there are no root attrs; drops the `attrs` key entirely when only
 * stats were present, so the shape matches a plain `editor.getJSON()`.
 */
export function stripDocStats(json: JSONContent): JSONContent {
    const attrs = json?.attrs;
    if (!attrs) {
        return json;
    }

    const rest = { ...attrs };
    for (const key of DOC_STAT_ATTRS) {
        delete rest[key];
    }

    const next = { ...json };
    if (Object.keys(rest).length > 0) {
        next.attrs = rest;
    } else {
        delete next.attrs;
    }

    return next;
}
