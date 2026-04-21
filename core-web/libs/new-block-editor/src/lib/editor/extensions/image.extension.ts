import { mergeAttributes } from '@tiptap/core';
import Image from '@tiptap/extension-image';

/** TipTap node name for embedded dotCMS images (slash menu → image). */
export const DOT_IMAGE_NODE_NAME = 'dotImage' as const;

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        dotImage: {
            setImageTextWrap: (value: 'left' | 'right') => ReturnType;
        };
    }
}

export const DotImage = Image.extend({
    // Keep name 'image' — preserves compatibility with setImage(), updateAttributes('image', …),
    // editor.isActive('image'), and existing stored content.
    name: DOT_IMAGE_NODE_NAME,

    addAttributes() {
        return {
            ...this.parent?.(),
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
                    const current = editor.getAttributes('image').textWrap;
                    // Toggle: clicking the same direction again clears it
                    return commands.updateAttributes('image', {
                        textWrap: current === value ? null : value
                    });
                }
        };
    }
});
