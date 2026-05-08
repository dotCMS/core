import { Node, mergeAttributes } from '@tiptap/core';

/** TipTap node name for embedded dotCMS videos (slash menu → video). */
export const DOT_VIDEO_NODE_NAME = 'dotVideo' as const;

export interface DotVideoData {
    identifier: string;
    inode: string;
    languageId: number;
    title: string;
    asset: string;
}

export const Video = Node.create({
    name: DOT_VIDEO_NODE_NAME,
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            src: { default: null },
            title: { default: null },
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
