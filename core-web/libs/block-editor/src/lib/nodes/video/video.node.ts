import { mergeAttributes, Node } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        videoBlock: {
            insertVideo: (attrs: DotCMSContentlet | string, position?: number) => ReturnType;
        };
    }
}

export const VideoNode = Node.create({
    name: 'dotVideo',

    addAttributes() {
        return {
            src: {
                default: null,
                parseHTML: (element) => element.getAttribute('src'),
                renderHTML: (attributes) => ({ src: attributes.src })
            },
            mimeType: {
                default: null,
                parseHTML: (element) => element.getAttribute('mimeType'),
                renderHTML: (attributes) => ({ mimeType: attributes.mimeType })
            },
            width: {
                default: null,
                parseHTML: (element) => element.getAttribute('width'),
                renderHTML: (attributes) => ({ width: attributes.width })
            },
            height: {
                default: null,
                parseHTML: (element) => element.getAttribute('height'),
                renderHTML: (attributes) => ({ height: attributes.height })
            },
            data: {
                default: null,
                parseHTML: (element) => element.getAttribute('data'),
                renderHTML: (attributes) => ({ data: attributes.data })
            }
        };
    },

    parseHTML() {
        return [
            {
                tag: 'video'
            }
        ];
    },

    addOptions() {
        return {
            inline: false,
            allowBase64: false,
            HTMLAttributes: {}
        };
    },

    inline() {
        return this.options.inline;
    },

    group() {
        return 'block';
    },

    draggable: true,

    addCommands() {
        return {
            ...this.parent?.(),
            insertVideo:
                (attrs, position) =>
                ({ commands, state }) => {
                    const { selection } = state;
                    const { head } = selection;

                    return commands.insertContentAt(position ?? head, {
                        type: this.name,
                        attrs: getVideoAttrs(attrs)
                    });
                }
        };
    },

    renderHTML({ HTMLAttributes }) {
        return [
            'div',
            { class: 'node-container' },
            [
                'video',
                mergeAttributes(HTMLAttributes, {
                    controls: true
                })
            ]
        ];
    }
});

const getVideoAttrs = (attrs: DotCMSContentlet | string) => {
    if (typeof attrs === 'string') {
        return { src: attrs };
    }

    const { assetMetaData, asset, mimeType, fileAsset } = attrs;
    const { width = 'auto', height = 'auto', contentType } = assetMetaData || {};

    return {
        src: fileAsset || asset,
        data: {
            ...attrs
        },
        width,
        height,
        mimeType: mimeType || contentType
    };
};
