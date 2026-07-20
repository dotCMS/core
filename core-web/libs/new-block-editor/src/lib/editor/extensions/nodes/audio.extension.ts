import { Node, mergeAttributes } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { getFileMetadata } from '@dotcms/utils';

/** TipTap node name for embedded dotCMS audio (slash menu → audio). */
export const DOT_AUDIO_NODE_NAME = 'dotAudio' as const;

export interface DotAudioData {
    identifier: string;
    inode: string;
    languageId: number;
    title: string;
    asset: string;
}

/**
 * Derives the `dotAudio` `mimeType` attribute from a contentlet. Resolves file metadata via the
 * canonical {@link getFileMetadata} (handles both `metaData` and `assetMetaData` shapes). Audio has
 * no width/height/orientation — only the mime type is needed. Null when unavailable so it never
 * serializes a bogus attribute.
 */
export function audioMetaAttrsFromContentlet(contentlet: DotCMSContentlet): {
    mimeType: string | null;
} {
    return {
        mimeType: contentlet.mimeType ?? getFileMetadata(contentlet).contentType ?? null
    };
}

export const Audio = Node.create({
    name: DOT_AUDIO_NODE_NAME,
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            src: { default: null },
            title: { default: null },
            // Rendered conditionally so a null value never serializes as e.g. mimeType="null".
            mimeType: {
                default: null,
                parseHTML: (el) => el.getAttribute('mimeType'),
                renderHTML: (attrs) => (attrs.mimeType ? { mimeType: attrs.mimeType } : {})
            },
            data: {
                default: null,
                parseHTML: (el) => {
                    const raw = el.getAttribute('data');
                    if (!raw) return null;
                    try {
                        return JSON.parse(raw) as DotAudioData;
                    } catch {
                        return null;
                    }
                },
                renderHTML: ({ data }: { data: DotAudioData | null }) => {
                    if (!data) return {};
                    return { data: JSON.stringify(data) };
                }
            }
        };
    },

    parseHTML() {
        return [{ tag: 'audio[src]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return ['audio', mergeAttributes({ controls: true, class: 'w-full' }, HTMLAttributes)];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('audio');
            dom.setAttribute('controls', '');
            dom.classList.add('w-full');

            const resolvedSrc =
                (node.attrs.src as string | null) ??
                (node.attrs.data as DotAudioData | null)?.asset ??
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
