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
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('video');
            dom.setAttribute('controls', '');
            dom.classList.add('w-full', 'rounded');

            if (node.attrs.src) {
                dom.setAttribute('src', String(node.attrs.src));
            }

            if (node.attrs.title) {
                dom.setAttribute('title', String(node.attrs.title));
            }

            return {
                dom,
                selectNode() {
                    dom.classList.add('is-selected');
                },
                deselectNode() {
                    dom.classList.remove('is-selected');
                }
            };
        };
    }
});
