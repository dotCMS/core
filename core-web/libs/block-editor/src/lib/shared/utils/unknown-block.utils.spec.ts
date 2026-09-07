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
                    originalType: 'customGallery',
                    originalNode: input[0],
                    originalNodeRaw: null
                }
            }
        ]);
        expect(restoreUnknownBlockNodes(preserved)).toEqual(input);
    });

    it('leaves known nodes untouched', () => {
        const input: JSONContent[] = [
            {
                type: 'paragraph',
                content: [{ type: 'text', text: 'keep me' }]
            }
        ];

        expect(preserveUnknownBlockNodes(input, new Set(['paragraph', 'text']))).toEqual(input);
    });

    it('recurses into known parents and preserves unknown children', () => {
        const unknownChild: JSONContent = { type: 'customGallery', attrs: { layout: 'single' } };
        const input: JSONContent[] = [
            {
                type: 'paragraph',
                content: [unknownChild]
            }
        ];

        expect(preserveUnknownBlockNodes(input, new Set(['paragraph', 'text']))).toEqual([
            {
                type: 'paragraph',
                content: [
                    {
                        type: UNKNOWN_BLOCK_NODE_NAME,
                        attrs: {
                            originalType: 'customGallery',
                            originalNode: unknownChild,
                            originalNodeRaw: null
                        }
                    }
                ]
            }
        ]);
    });

    it('wraps nodes with a missing or non-string type', () => {
        const input = [{ attrs: { foo: 'bar' } }, { type: 123 }] as JSONContent[];
        const preserved = preserveUnknownBlockNodes(input, new Set(['paragraph', 'text']));

        expect(preserved).toEqual([
            {
                type: UNKNOWN_BLOCK_NODE_NAME,
                attrs: {
                    originalType: null,
                    originalNode: input[0],
                    originalNodeRaw: null
                }
            },
            {
                type: UNKNOWN_BLOCK_NODE_NAME,
                attrs: {
                    originalType: null,
                    originalNode: input[1],
                    originalNodeRaw: null
                }
            }
        ]);
    });

    it('keeps corrupted placeholders unchanged during restore', () => {
        const input: JSONContent[] = [
            {
                type: UNKNOWN_BLOCK_NODE_NAME,
                attrs: {
                    originalType: 'customGallery',
                    originalNode: { attrs: { layout: 'single' } },
                    originalNodeRaw: '{"attrs":{"layout":"single"}}'
                }
            }
        ];

        expect(restoreUnknownBlockNodes(input)).toEqual(input);
    });
});
