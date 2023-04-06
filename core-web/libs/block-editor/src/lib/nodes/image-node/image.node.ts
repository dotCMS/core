import { Image } from '@tiptap/extension-image';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { getImageAttr, imageElement, imageLinkElement } from './helpers';

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
            insertImage: (attrs: DotCMSContentlet | string, position?: number) => ReturnType;
        };
    }
}

export const ImageNode = Image.extend({
    name: 'dotImage',

    addAttributes() {
        return {
            src: {
                default: null,
                parseHTML: (element) => element.getAttribute('src'),
                renderHTML: (attributes) => ({ src: attributes.src || attributes.data?.asset })
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
            insertImage:
                (attrs, position) =>
                ({ chain, state }) => {
                    const { selection } = state;
                    const { head } = selection;
                    const node = {
                        attrs: getImageAttr(attrs),
                        type: ImageNode.name
                    };

                    return chain()
                        .insertContentAt(position ?? head, node)
                        .run();
                }
        };
    },

    renderHTML({ HTMLAttributes }) {
        const { style = '', href = null } = HTMLAttributes || {};

        return [
            'div',
            { style, class: 'node-container' },
            href
                ? imageLinkElement(this.options.HTMLAttributes, HTMLAttributes)
                : imageElement(this.options.HTMLAttributes, HTMLAttributes)
        ];
    }
});
