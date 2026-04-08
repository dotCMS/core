import { Node, mergeAttributes } from '@tiptap/core';

export const Video = Node.create({
    name: 'video',
    group: 'block',
    atom: true,

    addAttributes() {
        return {
            src: { default: null },
            title: { default: null }
        };
    },

    parseHTML() {
        return [{ tag: 'video[src]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return [
            'video',
            mergeAttributes({ controls: true, class: 'w-full rounded' }, HTMLAttributes)
        ];
    }
});
