import { AngularNodeViewRenderer } from 'ngx-tiptap';

import type { Injector } from '@angular/core';

import { Node, mergeAttributes, type NodeViewRenderer } from '@tiptap/core';

import { DotInlineContentNodeViewComponent } from './inline-content.component';
import {
    type ContentletData,
    type ContentletDataRef,
    DOT_INLINE_CONTENT_NODE_NAME,
    INLINE_CONTENT_HOST_CLASS,
    INLINE_CONTENT_HTML_HOST_TAG
} from './inline-content.types';

export {
    DOT_INLINE_CONTENT_NODE_NAME,
    INLINE_CONTENT_HTML_HOST_TAG,
    type ContentletData,
    type ContentletDataRef
} from './inline-content.types';

/**
 * Inline dotCMS contentlet reference. Lives inside a paragraph's content (Notion-style `@`-mention)
 * and renders as a compact, linked token of the contentlet title. Canonical storage is ProseMirror
 * JSON (`type: dotInlineContent`, `attrs.data`); HTML uses {@link INLINE_CONTENT_HTML_HOST_TAG} plus
 * a `data` JSON attribute (skinny ref) for paste / export. The Angular node view is only for editing.
 *
 * Structurally identical to the block `dotContent` node (same `attrs.data` shape) so the backend's
 * existing Story Block hydration machinery re-hydrates it unchanged — the difference is `inline: true`.
 */
export function createDotInlineContent(injector: Injector) {
    return Node.create({
        name: DOT_INLINE_CONTENT_NODE_NAME,
        inline: true,
        group: 'inline',
        atom: true,
        selectable: true,
        draggable: false,

        addAttributes() {
            return {
                data: {
                    default: null as ContentletData | null,
                    parseHTML: (element) => {
                        const raw = element.getAttribute('data');
                        if (raw) {
                            try {
                                return JSON.parse(raw) as ContentletData;
                            } catch {
                                return null;
                            }
                        }
                        const identifier = element.getAttribute('data-identifier');
                        if (!identifier) return null;
                        const languageIdRaw = element.getAttribute('data-language-id');
                        return {
                            identifier,
                            languageId: languageIdRaw ? Number(languageIdRaw) : 1,
                            inode: element.getAttribute('data-inode') ?? undefined,
                            contentType: element.getAttribute('data-content-type') ?? undefined
                        } as ContentletData;
                    },
                    renderHTML: (attrs) => {
                        if (!attrs['data']) return {};
                        const skinny: ContentletDataRef = {
                            identifier: (attrs['data'] as ContentletData).identifier,
                            languageId: (attrs['data'] as ContentletData).languageId ?? 1
                        };
                        return { data: JSON.stringify(skinny) };
                    }
                }
            };
        },

        parseHTML() {
            return [
                { tag: INLINE_CONTENT_HTML_HOST_TAG },
                { tag: 'span[data-type="dot-inline-content"]' }
            ];
        },

        renderHTML({ HTMLAttributes }) {
            return [
                INLINE_CONTENT_HTML_HOST_TAG,
                mergeAttributes(
                    {
                        'data-type': 'dot-inline-content',
                        class: INLINE_CONTENT_HOST_CLASS
                    },
                    HTMLAttributes
                )
            ];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(DotInlineContentNodeViewComponent, { injector });
        }
    });
}
