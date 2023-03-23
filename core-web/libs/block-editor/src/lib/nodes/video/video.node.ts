import { mergeAttributes, Node } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        videoBlock: {
            setVideo: (attrs: DotCMSContentlet | string) => ReturnType;
        };
    }
}

export const VideoNode = Node.create({
    name: 'video',

    addAttributes() {
        return {
            src: {
                default: null,
                parseHTML: (element) => element.getAttribute('src'),
                renderHTML: (attributes) => ({ src: attributes.src })
            },
            mineType: {
                default: null,
                parseHTML: (element) => element.getAttribute('mineType'),
                renderHTML: (attributes) => ({ mineType: attributes.mineType })
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
            orientation: {
                default: null,
                parseHTML: (element) => element.getAttribute('orientation'),
                renderHTML: ({ height, width }) => ({
                    orientation: height > width ? 'vertical' : 'horizontal'
                })
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
            setVideo:
                (attrs) =>
                ({ commands }) => {
                    return commands.insertContent({
                        type: this.name,
                        attrs: getVideoAttrs(attrs)
                    });
                }
        };
    },

    renderHTML({ HTMLAttributes }) {
        const { orientation = 'horizontal' } = HTMLAttributes;

        return [
            'div',
            { class: 'node-container' },
            [
                'video',
                mergeAttributes(HTMLAttributes, {
                    controls: true,
                    class: `${orientation}-video`
                })
            ]
        ];
    }
});

const getVideoAttrs = (attrs: DotCMSContentlet | string) => {
    if (typeof attrs === 'string') {
        return { src: attrs };
    }

    const { assetMetaData, asset, mineType, fileAsset } = attrs;
    const { width = 'auto', height = 'auto', contentType } = assetMetaData || {};
    const orientation = height > width ? 'vertical' : 'horizontal';

    return {
        src: fileAsset || asset,
        data: {
            ...attrs
        },
        width,
        height,
        mineType: mineType || contentType,
        orientation
    };
};
