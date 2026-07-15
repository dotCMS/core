import { JSONContent } from '@tiptap/core';

import { getBlockMap, purifyNodeTree, removeInvalidNodes } from './parser.utils';

describe('parser.utils', () => {
    describe('getBlockMap', () => {
        it('should always include the basic nodes (paragraph, text, doc, hardBreak)', () => {
            const map = getBlockMap(['heading']);

            expect(map.paragraph).toBe(true);
            expect(map.text).toBe(true);
            expect(map.doc).toBe(true);
            expect(map.hardBreak).toBe(true);
        });

        it('should expand related content dependencies (e.g. table)', () => {
            const map = getBlockMap(['table']);

            expect(map.table).toBe(true);
            expect(map.tableRow).toBe(true);
            expect(map.tableHeader).toBe(true);
            expect(map.tableCell).toBe(true);
        });

        it('should add plain allowed blocks as-is', () => {
            const map = getBlockMap(['heading', 'blockquote']);

            expect(map.heading).toBe(true);
            expect(map.blockquote).toBe(true);
        });
    });

    describe('purifyNodeTree', () => {
        it('should drop nodes whose type is not in the block map', () => {
            const content: JSONContent[] = [
                { type: 'paragraph', content: [{ type: 'text', text: 'keep' }] },
                { type: 'blockquote', content: [{ type: 'text', text: 'drop' }] }
            ];

            const result = purifyNodeTree(content, getBlockMap(['paragraph']));

            expect(result).toHaveLength(1);
            expect(result[0].type).toBe('paragraph');
        });

        it('should preserve hardBreak nodes nested inside a paragraph', () => {
            const content: JSONContent[] = [
                {
                    type: 'paragraph',
                    content: [
                        { type: 'text', text: 'line 1' },
                        { type: 'hardBreak' },
                        { type: 'text', text: 'line 2' }
                    ]
                }
            ];

            const result = purifyNodeTree(content, getBlockMap(['paragraph']));

            expect(result[0].content).toHaveLength(3);
            expect(result[0].content[1].type).toBe('hardBreak');
        });
    });

    describe('removeInvalidNodes', () => {
        // Regression for #35985: hard breaks were stripped when re-opening
        // content for editing on a Block Editor field with allowed-block
        // restrictions (allowedBlocks.length > 1).
        it('should keep hardBreak nodes on a restricted-blocks field', () => {
            const allowedBlocks = ['heading', 'paragraph', 'orderedList'];
            const content: JSONContent = {
                type: 'doc',
                content: [
                    {
                        type: 'paragraph',
                        content: [
                            { type: 'text', text: '123 Main St' },
                            { type: 'hardBreak' },
                            { type: 'text', text: 'Suite 100' },
                            { type: 'hardBreak' },
                            { type: 'text', text: 'Springfield' }
                        ]
                    }
                ]
            };

            const result = removeInvalidNodes(content, allowedBlocks);

            const paragraph = result[0];
            expect(paragraph.type).toBe('paragraph');

            const hardBreaks = paragraph.content.filter((node) => node.type === 'hardBreak');
            expect(hardBreaks).toHaveLength(2);
        });

        it('should keep paragraph nodes via the basic-node fallback even when not in allowedBlocks', () => {
            const allowedBlocks = ['heading', 'orderedList'];
            const content: JSONContent = {
                type: 'doc',
                content: [{ type: 'paragraph', content: [{ type: 'text', text: 'plain text' }] }]
            };

            const result = removeInvalidNodes(content, allowedBlocks);

            expect(result).toHaveLength(1);
            expect(result[0].type).toBe('paragraph');
        });

        it('should still strip nodes that are neither basic nor allowed', () => {
            const allowedBlocks = ['heading', 'paragraph'];
            const content: JSONContent = {
                type: 'doc',
                content: [
                    { type: 'paragraph', content: [{ type: 'text', text: 'keep' }] },
                    { type: 'codeBlock', content: [{ type: 'text', text: 'drop' }] }
                ]
            };

            const result = removeInvalidNodes(content, allowedBlocks);

            expect(result).toHaveLength(1);
            expect(result[0].type).toBe('paragraph');
        });
    });
});
