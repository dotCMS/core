import { Image } from '@tiptap/extension-image';

import { imageLinkElement, imageElement } from './helpers';

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
    name: 'dotImage',

    addAttributes() {
        return {
            // Extend Attributes: https://tiptap.dev/guide/custom-extensions#extend-existing-attributes
            ...this.parent?.(),
            style: {
                default: null,
                parseHTML: (element) => element.getAttribute('style'),
                renderHTML: (attributes) => ({ style: attributes.style })
            },
            href: {
                default: null,
                parseHTML: (element) => element.getAttribute('href'),
                renderHTML: (attributes) => ({ href: attributes.href })
            },
            data: {
                default: null,
                parseHTML: (element) => ({
                    data: element.getAttribute('data')
                }),
                renderHTML: (attributes) => ({ data: attributes.data })
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

    addCommands() {
        return {
            ...this.parent?.(),
            setImageLink:
                (attributes) =>
                ({ commands }) => {
                    return commands.updateAttributes(this.name, attributes);
                },
            unsetImageLink:
                () =>
                ({ commands }) => {
                    return commands.updateAttributes(this.name, { href: '' });
                }
        };
    },

    renderHTML({ HTMLAttributes }) {
        const { style = '', href = null } = HTMLAttributes || {};

        return [
            'div',
            { style },
            href
                ? imageLinkElement(this.options.HTMLAttributes, HTMLAttributes)
                : imageElement(this.options.HTMLAttributes, HTMLAttributes)
        ];
    }
});
