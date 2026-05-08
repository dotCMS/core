import { Node } from '@tiptap/core';

/**
 * TipTap node name for AI-generated text blocks. Same as legacy block-editor for storage compat.
 *
 * In the new editor, AI-generated HTML is inserted via `commands.insertContent(html)` so each
 * block becomes a normal editable node (paragraph / heading / list / etc.) — NOT wrapped in an
 * `aiContent` block. This node registration only exists so legacy stored content from the old
 * block editor (which DID wrap in `aiContent`) still parses and renders. Removing the node
 * registration would silently drop those blocks on load — see `CLAUDE.md` "TipTap Node Names
 * Are Immutable".
 */
export const AI_CONTENT_NODE_NAME = 'aiContent' as const;

export const AIContent = Node.create({
    name: AI_CONTENT_NODE_NAME,
    group: 'block',
    inline: false,

    addAttributes() {
        return {
            content: { default: '' }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-ai-content]' }];
    },

    renderHTML() {
        return ['div', { 'data-ai-content': 'true' }];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');
            dom.setAttribute('data-ai-content', 'true');
            dom.classList.add('ai-content-container');
            dom.contentEditable = 'true';

            const inner = document.createElement('div');
            inner.innerHTML = String(node.attrs['content'] ?? '');
            dom.appendChild(inner);

            return { dom };
        };
    }
});
