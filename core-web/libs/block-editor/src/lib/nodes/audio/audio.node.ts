import { mergeAttributes, Node } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { contentletToJSON } from '../../shared';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        audioBlock: {
            insertAudio: (attrs: DotCMSContentlet | string, position?: number) => ReturnType;
        };
    }
}

const getAudioAttrs = (attrs: DotCMSContentlet | string) => {
    if (typeof attrs === 'string') {
        return { src: attrs };
    }

    const { assetMetaData, asset, mimeType, fileAsset } = attrs;
    const { contentType } = assetMetaData || {};

    return {
        src: fileAsset || asset,
        data: {
            ...attrs
        },
        mimeType: mimeType || contentType
    };
};

export const AudioNode = Node.create({
    name: 'dotAudio',

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
                tag: 'audio'
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
            insertAudio:
                (attrs, position) =>
                ({ commands, state }) => {
                    const { selection } = state;
                    const { head } = selection;

                    return commands.insertContentAt(position ?? head, {
                        type: this.name,
                        attrs: getAudioAttrs(attrs)
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
                'audio',
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
            dom.classList.add('audio-container');

            const audio = document.createElement('audio');
            audio.controls = true;

            Object.entries(HTMLAttributes).forEach(([key, value]) => {
                if (typeof value === 'object' && value !== null) {
                    value = JSON.stringify(value);
                }

                audio.setAttribute(key, value);
            });

            dom.appendChild(audio);

            // Override toJSON method to include the contentlet data
            node.toJSON = contentletToJSON.bind({ node });

            return {
                dom,
                node
            };
        };
    }
});
