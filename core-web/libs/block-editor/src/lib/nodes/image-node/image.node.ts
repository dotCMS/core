import { Image } from '@tiptap/extension-image';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { addImageLanguageId, getImageAttr, imageElement, imageLinkElement } from './helpers';

import { contentletToJSON } from '../../shared';

const FLOAT_STYLES: Record<'left' | 'right', string> = {
    left: 'float: left; width: 50%; margin: 0 1rem 1rem 0;',
    right: 'float: right; width: 50%; margin: 0 0 1rem 1rem;'
};

const IMG_ONLY_ATTRS = new Set(['textWrap', 'textAlign']);

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        ImageBlock: {
            setImageLink: (attributes: { href: string; target?: string }) => ReturnType;
            unsetImageLink: () => ReturnType;
            insertImage: (attrs: DotCMSContentlet | string, position?: number) => ReturnType;
            setImageTextWrap: (value: 'left' | 'right' | null) => ReturnType;
        };
    }
}

export const ImageNode = Image.extend({
    name: 'dotImage',

    addOptions() {
        return {
            inline: false,
            selectable: true,
            allowBase64: true,
            // v3 added `resize` as a required option on ImageOptions; preserve v2 default behavior.
            resize: false,
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
            },
            textWrap: {
                default: null,
                parseHTML: (element) => element.getAttribute('textwrap'),
                renderHTML: (attributes) => ({ textwrap: attributes.textWrap })
            },
            textAlign: {
                default: null,
                parseHTML: (element) => element.getAttribute('textalign'),
                renderHTML: (attributes) => ({ textalign: attributes.textAlign })
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
                },
            setImageTextWrap:
                (value) =>
                ({ commands, editor }) => {
                    const currentTextWrap = editor.getAttributes(ImageNode.name).textWrap;
                    const resolvedWrap = currentTextWrap === value ? null : value;

                    return commands.updateAttributes(ImageNode.name, {
                        textWrap: resolvedWrap,
                        textAlign: null
                    });
                }
        };
    },

    renderHTML({ HTMLAttributes }) {
        const { href = null, textWrap, textAlign } = HTMLAttributes || {};

        let divStyle: string | undefined;

        if (textWrap === 'left' || textWrap === 'right') {
            divStyle = FLOAT_STYLES[textWrap];
        } else if (textAlign) {
            divStyle = `text-align: ${textAlign};`;
        }

        return [
            'figure',
            { style: divStyle },
            href
                ? imageLinkElement(this.options.HTMLAttributes, HTMLAttributes)
                : imageElement(this.options.HTMLAttributes, HTMLAttributes)
        ];
    },

    addNodeView() {
        return ({ node, HTMLAttributes }) => {
            const hasImageLink = !!HTMLAttributes.href;
            const img = document.createElement('img');
            img.classList.add(`dot-image`);
            Object.entries(HTMLAttributes).forEach(([key, value]) => {
                if (IMG_ONLY_ATTRS.has(key)) {
                    return;
                }

                if (typeof value === 'object' && value !== null) {
                    value = JSON.stringify(value);
                }

                img.setAttribute(key, value as string);
            });

            let inner: HTMLElement;
            if (hasImageLink) {
                const a = document.createElement('a');
                a.setAttribute('href', HTMLAttributes.href);
                a.setAttribute('target', HTMLAttributes.target);
                a.appendChild(img);
                inner = a;
            } else {
                inner = img;
            }

            const wrapper = document.createElement('div');
            // Use node.attrs (camelCase) not HTMLAttributes — renderHTML lowercases keys to textwrap/textalign
            const textWrap = node.attrs['textWrap'];
            const textAlign = node.attrs['textAlign'];

            if (textWrap === 'left' || textWrap === 'right') {
                wrapper.style.cssText = FLOAT_STYLES[textWrap];
                wrapper.classList.add('has-float');
            } else if (textAlign) {
                wrapper.style.textAlign = textAlign;
            }

            const align = textWrap ?? textAlign;
            if (align) {
                wrapper.classList.add(`dot-node-${align}`);
            }
            wrapper.appendChild(inner);

            node.toJSON = contentletToJSON.bind({ node });

            return {
                dom: wrapper,
                node
            };
        };
    }
});
