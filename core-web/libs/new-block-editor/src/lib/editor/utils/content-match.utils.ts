import { type Editor, type JSONContent } from '@tiptap/core';
import { Node as PMNode } from '@tiptap/pm/model';

import { preserveUnknownNodesInDocument } from './unknown-block.utils';

/**
 * True when `incoming` represents the document the editor already holds — meaning the caller
 * must NOT call `setContent`.
 *
 * The comparison is **structural**, never textual. Both sides are normalized through the
 * editor's own schema before being compared, so the three things that made the previous
 * byte-wise version answer "changed" on unchanged content stop mattering (#36985):
 *
 * - **schema defaults** that the stored document predates (`indent`, added to the legacy editor
 *   only in #32235);
 * - **undeclared attrs** the schema drops on parse (the legacy `chartCount` typo, written until
 *   #26025; `listItem.textAlign`, which the legacy editor still writes today);
 * - **attribute key order**, which `/api/v1/content` emits alphabetically while the schema
 *   declares `textAlign, indent, level`. That last one breaks *current* content, not just old
 *   content, and is the reason this must not be a string comparison at any point.
 *
 * Fails closed: if the incoming value cannot be deserialized, returns `false` so the caller
 * loads it. Since #37175 that path is defensive rather than routine — unknown **marks** are now
 * preserved as `dotUnsupportedMark` just as unknown **nodes** are preserved as
 * `dotUnsupportedBlock`, so both round-trip and compare equal instead of aborting the parse. The
 * `catch` remains because a malformed document can still fail `fromJSON` for other reasons, and
 * blanking the field is the failure mode #37145 was about.
 */
export function contentMatchesEditorDocument(
    editor: Editor,
    incoming: string | JSONContent | JSONContent[]
): boolean {
    if (typeof incoming === 'string') {
        const trimmed = incoming.trimStart();
        if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
            // Not Block Editor JSON — the JSP's showdown fallback hands over HTML.
            return incoming === editor.getHTML();
        }

        try {
            return matchesDocument(editor, JSON.parse(incoming) as JSONContent | JSONContent[]);
        } catch {
            return false;
        }
    }

    return matchesDocument(editor, incoming);
}

function matchesDocument(editor: Editor, incoming: JSONContent | JSONContent[]): boolean {
    try {
        // Unknown node types become the `dotUnsupportedBlock` placeholder and unknown marks the
        // `dotUnsupportedMark` placeholder (#37175), exactly as the load path does, so both sides
        // hold the same shape. Applied for BOTH entry points — the string branch used to skip
        // this, which is why custom blocks never compared equal.
        const prepared = preserveUnknownNodesInDocument(
            incoming,
            new Set(Object.keys(editor.schema.nodes)),
            new Set(Object.keys(editor.schema.marks))
        );

        // Some hosts (the UVE side panel) pass a bare array of nodes rather than a document.
        const asDocument = Array.isArray(prepared) ? { type: 'doc', content: prepared } : prepared;

        // `Fragment.eq`, deliberately NOT `Node.eq`. `Node.eq` also compares the document node's
        // own attrs, which would reintroduce sensitivity to the root doc stats this fix exists to
        // ignore. The two happen to behave identically today only because
        // `schema.nodes.doc.spec.attrs` is null, so `fromJSON` discards root attrs outright —
        // a coincidence, not a guarantee. If doc attrs are ever declared for the emit path,
        // `Node.eq` would silently start failing while this stays correct.
        return PMNode.fromJSON(editor.schema, asDocument).content.eq(editor.state.doc.content);
    } catch {
        // Deliberately swallowed without logging: Story Block bodies are customer content.
        return false;
    }
}
