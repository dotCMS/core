import { Image } from '@tiptap/extension-image';

import { parseIMGElement, imageLinkElement, imageElement } from './helpers';

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
    priority: 10000,

    addAttributes() {
        return {
            src: {
                default: null,
                parseHTML: (element) => parseIMGElement(element).getAttribute('src'),
                renderHTML: (attributes) => {
                    return { src: attributes.src };
                }
            },
            alt: {
                default: null,
                parseHTML: (element) => parseIMGElement(element).getAttribute('alt'),
                renderHTML: (attributes) => {
                    return { alt: attributes.alt };
                }
            },
            title: {
                default: null,
                parseHTML: (element) => parseIMGElement(element).getAttribute('title'),
                renderHTML: (attributes) => {
                    return { title: attributes.title };
                }
            },
            style: {
                default: null,
                parseHTML: (element) => parseIMGElement(element).getAttribute('style'),
                renderHTML: (attributes) => {
                    return { style: attributes.style };
                }
            },
            href: {
                default: null,
                parseHTML: (element) => parseIMGElement(element).getAttribute('href'),
                renderHTML: (attributes) => {
                    return { href: attributes.href };
                }
            },
            data: {
                default: null,
                parseHTML: (element) => ({
                    data: parseIMGElement(element).getAttribute('data')
                }),
                renderHTML: (attributes) => {
                    return { data: attributes.data };
                }
            }
        };
    },

    parseHTML() {
        return [
            {
                tag: this.options.allowBase64 ? 'img[src]' : 'img[src]:not([src^="data:"])'
            },
            {
                tag: 'p',
                getAttrs: (element) => {
                    if (typeof element === 'string') {
                        return false && null;
                    }

                    // Check if the element has an attribute
                    const isImage = element.querySelector('img');

                    return !!isImage && null;
                }
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
