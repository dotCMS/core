import { AngularNodeViewRenderer } from 'ngx-tiptap';

import type { Injector } from '@angular/core';

import { Node, mergeAttributes, type NodeViewRenderer } from '@tiptap/core';

import { DotContentletNodeViewComponent } from './contentlet.component';
import {
    CONTENTLET_CARD_HOST_CLASS,
    CONTENTLET_HTML_HOST_TAG,
    type ContentletData,
    type ContentletDataRef,
    DOT_CONTENTLET_NODE_NAME
} from './contentlet.types';

export {
    CONTENTLET_HTML_HOST_TAG,
    DOT_CONTENTLET_NODE_NAME,
    type ContentletData,
    type ContentletDataRef,
    type ContentletEditEvent
} from './contentlet.types';

/**
 * Embedded dotCMS contentlet node. Canonical storage is ProseMirror JSON (`type: dotContent`,
 * `attrs.data`). HTML uses {@link CONTENTLET_HTML_HOST_TAG} plus a `data` JSON attribute (skinny
 * ref) for paste / export; the Angular node view is only for editing.
 */
export function createDotContentlet(injector: Injector) {
    return Node.create({
        name: DOT_CONTENTLET_NODE_NAME,
        group: 'block',
        atom: true,
        draggable: true,

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
            return [{ tag: CONTENTLET_HTML_HOST_TAG }, { tag: 'div[data-type="dot-content"]' }];
        },

        renderHTML({ HTMLAttributes }) {
            return [
                CONTENTLET_HTML_HOST_TAG,
                mergeAttributes(
                    {
                        'data-type': 'dot-content',
                        class: CONTENTLET_CARD_HOST_CLASS
                    },
                    HTMLAttributes
                )
            ];
        },

        addNodeView(): NodeViewRenderer {
            return AngularNodeViewRenderer(DotContentletNodeViewComponent, { injector });
        }
    });
}
