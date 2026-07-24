import { Node } from '@tiptap/core';

import {
    parseUnknownBlockOriginalNode,
    renderUnknownBlockOriginalNode,
    UNKNOWN_BLOCK_NODE_NAME
} from '../../utils/unknown-block.utils';

export const UnsupportedBlock = Node.create({
    name: UNKNOWN_BLOCK_NODE_NAME,
    group: 'block',
    atom: true,
    selectable: true,
    draggable: true,

    addAttributes() {
        return {
            originalType: {
                default: null,
                parseHTML: (element) => element.getAttribute('data-original-type'),
                renderHTML: (attributes) =>
                    attributes.originalType ? { 'data-original-type': attributes.originalType } : {}
            },
            originalNode: {
                default: null,
                parseHTML: (element) =>
                    parseUnknownBlockOriginalNode(element.getAttribute('data-original-node'))
                        .originalNode,
                renderHTML: (attributes) => renderUnknownBlockOriginalNode(attributes)
            },
            originalNodeRaw: {
                default: null,
                parseHTML: (element) =>
                    parseUnknownBlockOriginalNode(element.getAttribute('data-original-node'))
                        .originalNodeRaw,
                renderHTML: () => ({})
            }
        };
    },

    parseHTML() {
        return [{ tag: `div[data-node-view-wrapper="${UNKNOWN_BLOCK_NODE_NAME}"]` }];
    },

    renderHTML({ HTMLAttributes, node }) {
        const originalType = node.attrs['originalType'] || 'unknown';

        return [
            'div',
            {
                ...HTMLAttributes,
                'data-node-view-wrapper': UNKNOWN_BLOCK_NODE_NAME,
                contenteditable: 'false',
                class: 'dot-unsupported-block'
            },
            `Unsupported block (${originalType})`
        ];
    }
});
