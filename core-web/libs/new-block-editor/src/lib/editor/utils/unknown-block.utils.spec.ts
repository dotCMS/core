import { JSONContent } from '@tiptap/core';

import {
    preserveUnknownBlockNodes,
    preserveUnknownNodesInDocument,
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

/**
 * dotCMS stores a Block Editor field as either a `{ type: 'doc', content: [...] }` document
 * or — through some hosts, notably the UVE side panel — a bare array of nodes. Both shapes
 * reach `setContent`, and the legacy editor branches on `Array.isArray` before preserving
 * unknown nodes (`dot-block-editor.component.ts:773`).
 *
 * Spreading an array into an object turns it into `{ 0: node, 1: node, ..., content: undefined }`,
 * which loses the document `type` and makes TipTap throw `RangeError: Unknown node type:
 * undefined` — the field renders blank for ANY content, regardless of marks (#37145).
 */
describe('preserveUnknownNodesInDocument', () => {
    const known = new Set(['doc', 'paragraph', 'text']);

    it('keeps a bare array of nodes as an array', () => {
        const input: JSONContent[] = [
            { type: 'paragraph', content: [{ type: 'text', text: 'first' }] },
            { type: 'paragraph', content: [{ type: 'text', text: 'second' }] }
        ];

        const result = preserveUnknownNodesInDocument(input, known);

        expect(Array.isArray(result)).toBe(true);
        expect(result).toEqual(input);
    });

    it('never turns an array into a plain object carrying an undefined content', () => {
        const input: JSONContent[] = [{ type: 'paragraph' }];

        const result = preserveUnknownNodesInDocument(input, known);

        // The spread bug produced `{ 0: node, content: undefined }`: still an object, and the
        // stray `content` key is what made TipTap throw `Unknown node type: undefined`.
        expect(Array.isArray(result)).toBe(true);
        expect(Object.prototype.hasOwnProperty.call(result, 'content')).toBe(false);
    });

    it('still preserves unknown nodes inside a bare array', () => {
        const unknown: JSONContent = { type: 'customGallery', attrs: { layout: 'single' } };

        const result = preserveUnknownNodesInDocument([unknown], known) as JSONContent[];

        expect(result[0].type).toBe(UNKNOWN_BLOCK_NODE_NAME);
        expect(result[0].attrs).toEqual({
            originalType: 'customGallery',
            originalNode: unknown,
            originalNodeRaw: null
        });
    });

    it('keeps handling a doc-shaped document', () => {
        const doc: JSONContent = {
            type: 'doc',
            content: [{ type: 'paragraph', content: [{ type: 'text', text: 'keep me' }] }]
        };

        expect(preserveUnknownNodesInDocument(doc, known)).toEqual(doc);
    });

    it('preserves sibling document attrs such as the doc stats', () => {
        const doc: JSONContent = {
            type: 'doc',
            attrs: { charCount: 7, wordCount: 2, readingTime: 1 },
            content: [{ type: 'paragraph' }]
        };

        expect(preserveUnknownNodesInDocument(doc, known)).toEqual(doc);
    });
});
