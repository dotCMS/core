import { mergeAttributes, Node } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        videoBlock: {
            setVideo: (attributes: { href: string }) => ReturnType;
        };
    }
}

export const CustomNode = Node.create({
    name: 'customNode',

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
                    return commands.insertContent({
                        type: this.name,
                        attrs
                    });
                }
        };
    },

    renderHTML() {
        // eslint-disable-next-line no-console

        return [
            'div',
            { class: 'node-container' },
            [
                'video',
                mergeAttributes(
                    {},
                    {
                        width: '400px',
                        height: 'auto',
                        controls: true,
                        src: 'https://www.w3schools.com/tags/movie.mp4'
                    }
                )
            ]
        ];
    }
});
