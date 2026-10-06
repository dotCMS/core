import { Node, mergeAttributes } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { getFileMetadata } from '@dotcms/utils';

/** TipTap node name for embedded dotCMS videos (slash menu → video). */
export const DOT_VIDEO_NODE_NAME = 'dotVideo' as const;

export interface DotVideoData {
    identifier: string;
    inode: string;
    languageId: number;
    title: string;
    asset: string;
}

/** The `dotVideo` metadata/layout attributes derived from a contentlet. */
export interface DotVideoMetaAttrs {
    mimeType: string | null;
    width: number | null;
    height: number | null;
    orientation: 'vertical' | 'horizontal' | null;
}

/**
 * Derives the `dotVideo` `mimeType` / `width` / `height` / `orientation` attributes from a
 * contentlet, for parity with the legacy block-editor's `getVideoAttrs`. Resolves file metadata
 * via the canonical {@link getFileMetadata} (handles both `metaData` and `assetMetaData` shapes) —
 * the legacy code read `assetMetaData` only, missing FileAsset / per-field shapes. Each value is
 * null when unavailable so it never serializes a bogus attribute.
 */
export function videoMetaAttrsFromContentlet(contentlet: DotCMSContentlet): DotVideoMetaAttrs {
    const meta = getFileMetadata(contentlet);
    const width = meta.width ?? null;
    const height = meta.height ?? null;

    return {
        mimeType: contentlet.mimeType ?? meta.contentType ?? null,
        width,
        height,
        orientation:
            width != null && height != null ? (height > width ? 'vertical' : 'horizontal') : null
    };
}

export const Video = Node.create({
    name: DOT_VIDEO_NODE_NAME,
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            src: { default: null },
            title: { default: null },
            // mimeType / width / height / orientation are preserved for round-trip parity with the
            // legacy block-editor's `dotVideo` node. Existing stored content carries these keys, and
            // server-side rendering may read them to size the <video>; without declaring them here
            // TipTap would silently drop them when an old document is re-saved in the new editor.
            // Rendered conditionally so a null value never serializes as e.g. width="null".
            mimeType: {
                default: null,
                parseHTML: (el) => el.getAttribute('mimeType'),
                renderHTML: (attrs) => (attrs.mimeType ? { mimeType: attrs.mimeType } : {})
            },
            width: {
                default: null,
                parseHTML: (el) => el.getAttribute('width'),
                renderHTML: (attrs) => (attrs.width ? { width: attrs.width } : {})
            },
            height: {
                default: null,
                parseHTML: (el) => el.getAttribute('height'),
                renderHTML: (attrs) => (attrs.height ? { height: attrs.height } : {})
            },
            orientation: {
                default: null,
                parseHTML: (el) => el.getAttribute('orientation'),
                renderHTML: (attrs) => (attrs.orientation ? { orientation: attrs.orientation } : {})
            },
            data: {
                default: null,
                parseHTML: (el) => {
                    const raw = el.getAttribute('data');
                    if (!raw) return null;
                    try {
                        return JSON.parse(raw) as DotVideoData;
                    } catch {
                        return null;
                    }
                },
                renderHTML: ({ data }: { data: DotVideoData | null }) => {
                    if (!data) return {};
                    return { data: JSON.stringify(data) };
                }
            }
        };
    },

    parseHTML() {
        return [{ tag: 'video[src]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return [
            'video',
            mergeAttributes({ controls: true, class: 'w-full rounded' }, HTMLAttributes)
        ];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('video');
            dom.setAttribute('controls', '');
            dom.classList.add('w-full', 'rounded');

            const resolvedSrc =
                (node.attrs.src as string | null) ??
                (node.attrs.data as DotVideoData | null)?.asset ??
                null;
            if (resolvedSrc) {
                dom.setAttribute('src', resolvedSrc);
            }

            if (node.attrs.title) {
                dom.setAttribute('title', String(node.attrs.title));
            }

            return {
                dom,
                selectNode() {
                    dom.classList.add('is-selected');
                },
                deselectNode() {
                    dom.classList.remove('is-selected');
                }
            };
        };
    }
});
