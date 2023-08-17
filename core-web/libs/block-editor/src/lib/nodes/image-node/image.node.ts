import { Image } from '@tiptap/extension-image';

import { addImageLanguageId, getImageAttr, imageElement, imageLinkElement } from './helpers';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

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
            addDotImage: (attrs: DotCMSContentlet | string) => ReturnType;
        };
    }
}

export const ImageNode = Image.extend({
    name: 'dotImage',

    addOptions() {
        return {
            inline: false,
            allowBase64: true,
            HTMLAttributes: {}
        };
    },

    addAttributes() {
        return {
            src: {
                default: null,
                parseHTML: (element) => element.getAttribute('src'),
                renderHTML: (attributes) => ({
                    src: addImageLanguageId(
                        attributes.src || attributes.data?.asset,
                        attributes.data?.languageId
                    )
                })
            },
            alt: {
                default: null,
                parseHTML: (element) => element.getAttribute('alt'),
                renderHTML: (attributes) => ({ alt: attributes.alt || attributes.data?.title })
            },
            title: {
                default: null,
                parseHTML: (element) => element.getAttribute('title'),
                renderHTML: (attributes) => ({ title: attributes.title || attributes.data?.title })
            },
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
                },
            addDotImage:
                (attrs) =>
                ({ chain, state }) => {
                    const { selection } = state;
                    const node = {
                        attrs: getImageAttr(attrs),
                        type: ImageNode.name
                    };

                    return chain().insertContentAt(selection.head, node).run();
                }
        };
    },

    renderHTML({ HTMLAttributes }) {
        const { style = '', href = null } = HTMLAttributes || {};

        return [
            'div',
            { style, class: 'node-container node-image' },
            href
                ? imageLinkElement(this.options.HTMLAttributes, HTMLAttributes)
                : imageElement(this.options.HTMLAttributes, HTMLAttributes)
        ];
    }
});
