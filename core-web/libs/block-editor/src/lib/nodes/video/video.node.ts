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
                    const src = typeof attrs === 'string' ? attrs : attrs.asset;

                    return commands.insertContent({
                        type: this.name,
                        attrs: {
                            src
                        }
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
                    width: '100%',
                    height: 'auto',
                    controls: true
                })
            ]
        ];
    }
});
