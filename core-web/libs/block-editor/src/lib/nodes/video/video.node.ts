import { mergeAttributes, Node } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { contentletToJSON } from '../../shared';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        videoBlock: {
            insertVideo: (attrs: DotCMSContentlet | string, position?: number) => ReturnType;
        };
    }
}

const getVideoAttrs = (attrs: DotCMSContentlet | string) => {
    if (typeof attrs === 'string') {
        return { src: attrs };
    }

    const { assetMetaData, asset, mimeType, fileAsset } = attrs;
    const { width = 'auto', height = 'auto', contentType } = assetMetaData || {};
    const orientation = height > width ? 'vertical' : 'horizontal';

    return {
        src: fileAsset || asset,
        data: {
            ...attrs
        },
        width,
        height,
        mimeType: mimeType || contentType,
        orientation
    };
};

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
                renderHTML: (attributes) => ({ data: JSON.stringify(attributes.data) })
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

    /**
     * Return the node for the renderHTML method
     *
     * @param {*} { HTMLAttributes }
     * @return {*}
     */
    renderHTML({ HTMLAttributes }) {
        return [
            'div',
            [
                'video',
                mergeAttributes(HTMLAttributes, {
                    controls: true
                })
            ]
        ];
    },

    /**
     * Return the node view for Block Editor in Development mode
     *
     * @return {*}
     */
    addNodeView() {
        return ({ node, HTMLAttributes }) => {
            const dom = document.createElement('div');
            dom.classList.add('video-container');

            const video = document.createElement('video');
            video.controls = true;

            Object.entries(HTMLAttributes).forEach(([key, value]) => {
                if (typeof value === 'object' && value !== null) {
                    value = JSON.stringify(value);
                }

                video.setAttribute(key, value);
            });

            dom.appendChild(video);

            // Override toJSON method to include the contentlet data
            node.toJSON = contentletToJSON.bind({ node });

            return {
                dom,
                node
            };
        };
    }
});
