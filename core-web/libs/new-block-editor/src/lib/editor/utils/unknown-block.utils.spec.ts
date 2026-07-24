import { JSONContent } from '@tiptap/core';

import {
    preserveUnknownBlockNodes,
    restoreUnknownBlockNodes,
    UNKNOWN_BLOCK_NODE_NAME
} from './unknown-block.utils';

describe('unknown-block.utils', () => {
    it('round-trips unregistered nodes through the unsupported-block placeholder', () => {
        const input: JSONContent[] = [
            {
                type: 'customGallery',
                attrs: {
                    layout: 'single',
                    images: '[1,2]'
                }
            }
        ];

        const preserved = preserveUnknownBlockNodes(input, new Set(['doc', 'paragraph', 'text']));

        expect(preserved).toEqual([
            {
                type: UNKNOWN_BLOCK_NODE_NAME,
                attrs: {
                    originalNode: input[0],
                    originalType: 'customGallery'
                }
            }
        ]);
        expect(restoreUnknownBlockNodes(preserved)).toEqual(input);
    });
});
