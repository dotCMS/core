import { Node } from '@tiptap/core';

// Maps allowedBlocks keys to their TipTap node names for grid column content.
// Custom nodes such as `aiContent` and `loader` are intentionally absent — they are
// editor-level utilities that do not make sense inside a grid column and are not
// registered as StarterKit node types.
const GRID_COLUMN_CONTENT_MAP: Record<string, string> = {
    heading: 'heading',
    bulletList: 'bulletList',
    orderedList: 'orderedList',
    codeBlock: 'codeBlock',
    blockquote: 'blockquote',
    table: 'table',
    horizontalRule: 'horizontalRule',
    image: 'dotImage',
    video: 'dotVideo',
    dotContent: 'dotContent'
};

const ALL_NODE_TYPES = Object.values(GRID_COLUMN_CONTENT_MAP);
const DEFAULT_CONTENT = `(paragraph | ${ALL_NODE_TYPES.join(' | ')})+`;

/**
 * Builds the TipTap content expression for GridColumn based on the allowed blocks.
 * This is necessary because StarterKit removes disabled node types from the schema at init time,
 * so a static content expression that references a removed node (e.g. `blockquote`) causes
 * a schema validation error. By deriving the expression from `allowedBlocks`, we ensure
 * GridColumn only references nodes that are actually registered in the schema.
 */
function buildGridColumnContent(allowedBlocks: string[]): string {
    if (!allowedBlocks.length) {
        return DEFAULT_CONTENT;
    }

    const headingAllowed = allowedBlocks.some((b) => b.startsWith('heading'));
    // `paragraph` is always included — StarterKit always registers it and
    // DotBlockEditorComponent seeds allowedBlocks with ['paragraph'] at initialization.
    const nodeTypes = ['paragraph'];

    for (const [key, nodeType] of Object.entries(GRID_COLUMN_CONTENT_MAP)) {
        if (key === 'heading') {
            if (headingAllowed) nodeTypes.push(nodeType);
        } else if (allowedBlocks.includes(key)) {
            nodeTypes.push(nodeType);
        }
    }

    return `(${nodeTypes.join(' | ')})+`;
}

export function createGridColumn(allowedBlocks: string[] = []) {
    return Node.create({
        name: 'gridColumn',
        group: 'gridColumn',
        // gridBlock is intentionally excluded to prevent nested grids.
        // Nested grid support requires a more robust resize implementation.
        content: buildGridColumnContent(allowedBlocks),
        isolating: true,

        parseHTML() {
            return [{ tag: 'div[data-type="gridColumn"]' }];
        },

        renderHTML({ HTMLAttributes }) {
            return [
                'div',
                { ...HTMLAttributes, 'data-type': 'gridColumn', class: 'grid-block__column' },
                0
            ];
        }
    });
}

export const GridColumn = createGridColumn();
