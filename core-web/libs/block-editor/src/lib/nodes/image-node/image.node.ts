import { mergeAttributes } from '@tiptap/core';
import { Image } from '@tiptap/extension-image';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        ImageBlock: {
            /**
             * Set Image Link mark
             */
            setImageLink: (attributes: { href: string }) => ReturnType;
            /**
             * Unset Image Link mark
             */
            unsetImageLink: () => ReturnType;
        };
    }
}

export const ImageNode = Image.extend({
    addAttributes() {
        return {
            // Extend Attributes: https://tiptap.dev/guide/custom-extensions#extend-existing-attributes
            ...this.parent?.(),
            href: {
                default: null,
                parseHTML: (element) => element.getAttribute('href'),
                renderHTML: (attributes) => {
                    return { href: attributes.href };
                }
            },
            data: {
                default: null,
                parseHTML: (element) => ({
                    data: element.getAttribute('data')
                }),
                renderHTML: (attributes) => {
                    return { data: attributes.data };
                }
            }
        };
    },

    addCommands() {
        return {
            ...this.parent?.(),
            setImageLink:
                (attributes) =>
                ({ commands }) => {
                    return commands.updateAttributes('image', attributes);
                },
            unsetImageLink:
                () =>
                ({ commands }) => {
                    return commands.updateAttributes('image', { href: '' });
                }
        };
    },

    parseHTML() {
        return [
            {
                tag: this.options.allowBase64 ? 'img[src]' : 'img[src]:not([src^="data:"])'
            }
        ];
    },

    renderHTML({ HTMLAttributes }) {
        const { style = '', href = null } = HTMLAttributes || {};

        if (href) {
            return [
                'div',
                { style },
                [
                    'a',
                    { href },
                    ['img', mergeAttributes(this.options.HTMLAttributes, HTMLAttributes)]
                ]
            ];
        }

        return [
            'div',
            { style },
            ['img', mergeAttributes(this.options.HTMLAttributes, HTMLAttributes)]
        ];
    }
});
