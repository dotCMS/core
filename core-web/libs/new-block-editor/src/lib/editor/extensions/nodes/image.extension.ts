import { mergeAttributes } from '@tiptap/core';
import Image from '@tiptap/extension-image';

/** TipTap node name for embedded dotCMS images (slash menu → image). */
export const DOT_IMAGE_NODE_NAME = 'dotImage' as const;

export interface DotImageData {
    identifier: string;
    inode: string;
    languageId: number;
    title: string;
    asset: string;
}

function appendLanguageId(src: string, languageId: number | undefined): string {
    if (!src || !languageId) return src;
    return src.includes('language_id') ? src : `${src}?language_id=${languageId}`;
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        dotImage: {
            setImageTextWrap: (value: 'left' | 'right') => ReturnType;
        };
    }
}

export const DotImage = Image.extend({
    // 'dotImage' matches the old block-editor node name — existing stored content uses this key.
    name: DOT_IMAGE_NODE_NAME,

    addAttributes() {
        return {
            ...this.parent?.(),
            src: {
                default: null,
                parseHTML: (element) => element.getAttribute('src'),
                renderHTML: (attributes) => ({
                    src: appendLanguageId(
                        attributes.src || attributes.data?.asset,
                        attributes.data?.languageId
                    )
                })
            },
            alt: {
                default: null,
                parseHTML: (element) => element.getAttribute('alt'),
                renderHTML: (attributes) => ({
                    alt: attributes.alt || attributes.data?.title || null
                })
            },
            title: {
                default: null,
                parseHTML: (element) => element.getAttribute('title'),
                renderHTML: (attributes) => ({
                    title: attributes.title || attributes.data?.title || null
                })
            },
            data: {
                default: null,
                parseHTML: (element) => {
                    const raw = element.getAttribute('data');
                    if (!raw) return null;
                    try {
                        return JSON.parse(raw) as DotImageData;
                    } catch {
                        return null;
                    }
                },
                renderHTML: (attributes) =>
                    attributes.data ? { data: JSON.stringify(attributes.data) } : {}
            },
            textWrap: {
                default: null,
                // Read from the parent <figure>'s class — set by renderHTML()
                parseHTML: (element) => {
                    const figure = element.closest('figure');
                    if (!figure) return null;
                    if (figure.classList.contains('image-wrap-left')) return 'left';
                    if (figure.classList.contains('image-wrap-right')) return 'right';
                    return null;
                },
                // textWrap goes on <figure>, not on <img> — return empty object
                renderHTML: () => ({})
            }
        };
    },

    parseHTML() {
        return [
            // Primary: our serialized format — <figure><img src="…"></figure>
            { tag: 'figure img[src]' },
            // Fallback: bare <img> tags from other sources / old content
            { tag: this.options.allowBase64 ? 'img[src]' : 'img[src]:not([src^="data:"])' }
        ];
    },

    renderHTML({ HTMLAttributes }) {
        const { textWrap, ...imgAttrs } = HTMLAttributes;
        const figAttrs: Record<string, string> = {};
        if (textWrap) figAttrs['class'] = `image-wrap-${textWrap}`;

        return [
            'figure',
            figAttrs,
            ['img', mergeAttributes(this.options.HTMLAttributes, imgAttrs)]
        ];
    },

    addNodeView() {
        return ({ node }) => {
            const figure = document.createElement('figure');
            const img = document.createElement('img');

            // Apply all attrs except textWrap to the <img>
            const { textWrap, ...imgAttrs } = node.attrs as Record<string, unknown>;
            Object.entries(imgAttrs).forEach(([key, value]) => {
                if (value == null) return;
                img.setAttribute(
                    key,
                    typeof value === 'object' ? JSON.stringify(value) : String(value)
                );
            });

            // Apply wrap class to <figure> — CSS drives the float, not inline styles
            figure.className = textWrap ? `image-wrap-${textWrap}` : '';

            figure.appendChild(img);

            return {
                dom: figure,
                selectNode() {
                    figure.classList.add('is-selected');
                },
                deselectNode() {
                    figure.classList.remove('is-selected');
                }
            };
        };
    },

    addCommands() {
        return {
            ...this.parent?.(),
            setImageTextWrap:
                (value) =>
                ({ commands, editor }) => {
                    const current = editor.getAttributes('dotImage').textWrap;
                    // Toggle: clicking the same direction again clears it
                    return commands.updateAttributes('dotImage', {
                        textWrap: current === value ? null : value
                    });
                }
        };
    }
});
