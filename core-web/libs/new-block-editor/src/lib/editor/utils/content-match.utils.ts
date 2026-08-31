import { type Editor, type JSONContent } from '@tiptap/core';

import { stripDocStats } from './doc-stats.utils';
import { preserveUnknownNodesInDocument } from './unknown-block.utils';

/**
 * True when `incoming` represents the document the editor already holds — meaning the caller
 * must NOT call `setContent`.
 *
 * NOTE: this is the byte-wise implementation lifted verbatim out of `editor.component.ts` so it
 * can be tested. It is deliberately unchanged at this commit; the structural comparison replaces
 * the body in the next one.
 */
export function contentMatchesEditorDocument(
    editor: Editor,
    incoming: string | JSONContent | JSONContent[]
): boolean {
    const currentJson = JSON.stringify(stripDocStats(editor.getJSON()));

    if (typeof incoming === 'string') {
        const trimmed = incoming.trimStart();
        if (trimmed.startsWith('{')) {
            try {
                return JSON.stringify(stripDocStats(JSON.parse(incoming))) === currentJson;
            } catch {
                return false;
            }
        }

        return incoming === editor.getHTML();
    }

    return (
        JSON.stringify(
            stripDocStats(
                preserveUnknownNodesInDocument(
                    incoming,
                    new Set(Object.keys(editor.schema.nodes))
                ) as JSONContent
            )
        ) === currentJson
    );
}
