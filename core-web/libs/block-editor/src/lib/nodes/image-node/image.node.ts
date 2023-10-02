import { Image } from '@tiptap/extension-image';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { addImageLanguageId, getImageAttr, imageElement, imageLinkElement } from './helpers';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        ImageBlock: {
            /**
             * Set Image Link mark
             */
            setImageLink: (attributes: { href: string; target?: string }) => ReturnType;
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
            href: {
                default: null,
                parseHTML: (element) => element.getAttribute('href'),
                renderHTML: (attributes) => ({ href: attributes.href })
            },
            data: {
                default: null,
                parseHTML: (element) => element.getAttribute('data'),
                renderHTML: (attributes) => ({ data: JSON.stringify(attributes.data) })
            },
            target: {
                default: null,
                parseHTML: (element) => element.getAttribute('target'),
                renderHTML: (attributes) => ({ target: attributes.target })
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
        const { href = null, style } = HTMLAttributes || {};

        return [
            'div',
            { class: 'image-container', style },
            href
                ? imageLinkElement(this.options.HTMLAttributes, HTMLAttributes)
                : imageElement(this.options.HTMLAttributes, HTMLAttributes)
        ];
    }
});
