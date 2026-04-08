import { shift } from '@floating-ui/dom';

import type { Editor } from '@tiptap/core';
import { DragHandle, defaultComputePositionConfig } from '@tiptap/extension-drag-handle';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';

/** Last valid text cursor inside this block (end of inline content), or start of an empty textblock. */
function endPosInsideBlock(doc: ProseMirrorNode, blockPos: number): number | null {
    const block = doc.nodeAt(blockPos);
    if (!block) return null;
    if (block.isTextblock) return blockPos + block.nodeSize - 1;
    if (block.childCount === 0) return null;

    let childPos = blockPos + 1;
    for (let i = 0; i < block.childCount - 1; i++) {
        childPos += block.child(i).nodeSize;
    }
    return endPosInsideBlock(doc, childPos);
}

/**
 * TipTap's drag-handle plugin always attaches `draggable` and drag listeners to the **single**
 * root node from `render()`. We keep one wrapper for layout, but:
 * - Put the grip first (toward the block) so the "+" stays in the padded gutter and is not clipped.
 * - Mark the "+" as non-draggable and cancel `dragstart` so it does not start a block drag.
 * - Use Floating UI `shift` so the gutter stays on-screen (default config has none).
 */
export function createBlockGutterDragHandle() {
    const state: { editor: Editor | null; pos: number; nodeSize: number } = {
        editor: null,
        pos: -1,
        nodeSize: 0
    };

    return DragHandle.configure({
        computePositionConfig: {
            ...defaultComputePositionConfig,
            middleware: [shift({ padding: 8 })]
        },
        onNodeChange: (payload) => {
            const { editor, node } = payload;
            // Runtime includes `pos`; @tiptap/extension-drag-handle types omit it.
            const pos = (payload as { pos?: number }).pos;
            state.editor = editor;
            state.pos = pos ?? -1;
            state.nodeSize = node?.nodeSize ?? 0;
        },
        render() {
            const root = document.createElement('div');
            root.className = 'drag-handle-wrapper';
            root.style.visibility = 'hidden';

            const dragEl = document.createElement('div');
            dragEl.className = 'drag-handle';
            dragEl.setAttribute('aria-hidden', 'true');
            dragEl.innerHTML = `<svg viewBox="0 0 10 16" width="10" height="16" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <circle cx="2" cy="3"  r="1.5" fill="currentColor"/>
          <circle cx="8" cy="3"  r="1.5" fill="currentColor"/>
          <circle cx="2" cy="8"  r="1.5" fill="currentColor"/>
          <circle cx="8" cy="8"  r="1.5" fill="currentColor"/>
          <circle cx="2" cy="13" r="1.5" fill="currentColor"/>
          <circle cx="8" cy="13" r="1.5" fill="currentColor"/>
        </svg>`;

            const addBtn = document.createElement('button');
            addBtn.type = 'button';
            addBtn.className = 'add-block-btn';
            addBtn.setAttribute(
                'aria-label',
                'Add block below, or open block menu on an empty line'
            );
            addBtn.setAttribute('draggable', 'false');
            addBtn.innerHTML = `<svg viewBox="0 0 10 10" width="10" height="10" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <path d="M5 1v8M1 5h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>`;
            addBtn.addEventListener('dragstart', (e) => {
                e.preventDefault();
                e.stopPropagation();
            });
            addBtn.addEventListener('mousedown', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const { editor, pos, nodeSize } = state;
                if (!editor || pos < 0) return;
                const { doc } = editor.state;
                const outer = doc.nodeAt(pos);
                const hasText = outer !== null && outer.textContent.trim().length > 0;

                if (!hasText) {
                    const endInside = endPosInsideBlock(doc, pos);
                    if (endInside !== null) {
                        editor.chain().focus().setTextSelection(endInside).insertContent('/').run();
                        return;
                    }
                }

                const insertPos = pos + nodeSize;
                editor
                    .chain()
                    .focus()
                    .insertContentAt(insertPos, { type: 'paragraph' })
                    .setTextSelection(insertPos + 1)
                    .insertContent('/')
                    .run();
            });

            root.appendChild(dragEl);
            root.appendChild(addBtn);
            return root;
        }
    });
}
