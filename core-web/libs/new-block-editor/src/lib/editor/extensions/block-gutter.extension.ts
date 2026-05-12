import { autoUpdate, computePosition, shift } from '@floating-ui/dom';

import type { Editor } from '@tiptap/core';
import { DragHandle, defaultComputePositionConfig } from '@tiptap/extension-drag-handle';
import type { Node as ProseMirrorNode } from '@tiptap/pm/model';

/**
 * Shared mutable state for the block gutter (drag grip + add button), updated on each hover.
 *
 * @property editor - Active TipTap editor, or `null` before first `onNodeChange`.
 * @property pos - Document position of the hovered block; `-1` when none.
 * @property nodeSize - `node.nodeSize` for the hovered block (used to insert below).
 */
type GutterState = {
    editor: Editor | null;
    pos: number;
    nodeSize: number;
    /** Root element from `createGutterRoot` (same node TipTap positions with floating-ui). */
    wrapper: HTMLElement | null;
};

/**
 * Payload passed to `onNodeChange` by `@tiptap/extension-drag-handle`.
 * `pos` is present at runtime but missing from the package typings.
 */
type DragHandleNodeChangePayload = {
    editor: Editor;
    node: ProseMirrorNode | null;
    pos?: number;
};

/**
 * Finds the last valid text cursor position inside a block (end of inline content),
 * or the start of an empty textblock.
 *
 * @param doc - Current ProseMirror document.
 * @param blockPos - Document position of the block node.
 * @returns A valid caret position inside the block, or `null` if none applies.
 */
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

/** SVG markup for the six-dot drag grip (injected into the draggable handle). */
const DRAG_GRIP_SVG = `<svg viewBox="0 0 10 16" width="10" height="16" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <circle cx="2" cy="3"  r="1.5" fill="currentColor"/>
          <circle cx="8" cy="3"  r="1.5" fill="currentColor"/>
          <circle cx="2" cy="8"  r="1.5" fill="currentColor"/>
          <circle cx="8" cy="8"  r="1.5" fill="currentColor"/>
          <circle cx="2" cy="13" r="1.5" fill="currentColor"/>
          <circle cx="8" cy="13" r="1.5" fill="currentColor"/>
        </svg>`;

/** SVG markup for the “add block” (+) control. */
const ADD_ICON_SVG = `<svg viewBox="0 0 10 10" width="10" height="10" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
          <path d="M5 1v8M1 5h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>`;

/**
 * Builds the draggable grip element TipTap attaches drag listeners to.
 *
 * @returns The `.drag-handle` root element (single child of the gutter wrapper).
 */
function createDragGripElement(): HTMLElement {
    const dragEl = document.createElement('div');
    dragEl.className = 'drag-handle';
    dragEl.setAttribute('aria-hidden', 'true');
    dragEl.style.cursor = 'grab';
    dragEl.innerHTML = DRAG_GRIP_SVG;
    return dragEl;
}

/**
 * Handles primary-button down on the “+” control: opens the slash menu on an empty line,
 * or inserts a new paragraph below the block and opens slash when the block has text.
 *
 * @param state - Gutter state (must hold current `editor`, `pos`, `nodeSize`).
 * @param event - `mousedown` from the add button (default prevented to avoid focus quirks).
 */
function onAddBlockButtonMouseDown(state: GutterState, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

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
}

/**
 * Creates the non-draggable “+” button and wires slash / new-paragraph behavior.
 *
 * @param state - Shared gutter state (read on each click).
 * @param addBlockAriaLabel - Pre-translated aria-label for the button.
 * @returns Configured `.add-block-btn` element.
 */
function createAddBlockButton(state: GutterState, addBlockAriaLabel: string): HTMLElement {
    const addBtn = document.createElement('button');
    addBtn.type = 'button';
    addBtn.className = 'add-block-btn';
    addBtn.setAttribute('aria-label', addBlockAriaLabel);
    addBtn.setAttribute('draggable', 'false');
    addBtn.innerHTML = ADD_ICON_SVG;
    addBtn.addEventListener('dragstart', (e) => {
        e.preventDefault();
        e.stopPropagation();
    });
    addBtn.addEventListener('mousedown', (e) => onAddBlockButtonMouseDown(state, e));
    return addBtn;
}

/**
 * Root element returned by DragHandle `render()`: wrapper + grip + add button.
 *
 * @param state - Shared gutter state passed through to the add button handler.
 * @param addBlockAriaLabel - Pre-translated aria-label for the add button.
 * @returns `.drag-handle-wrapper` element (visibility toggled by the extension).
 */
function createGutterRoot(state: GutterState, addBlockAriaLabel: string): HTMLElement {
    const root = document.createElement('div');
    root.className = 'drag-handle-wrapper';
    root.style.visibility = 'hidden';
    root.appendChild(createDragGripElement());
    root.appendChild(createAddBlockButton(state, addBlockAriaLabel));
    state.wrapper = root;
    return root;
}

/** Deep-clone a DOM node and copy computed styles so the drag bitmap matches the editor. */
function cloneBlockElementWithStyles(node: HTMLElement): HTMLElement {
    const cloned = node.cloneNode(true) as HTMLElement;
    const sources = [node, ...Array.from(node.getElementsByTagName('*'))] as HTMLElement[];
    const targets = [cloned, ...Array.from(cloned.getElementsByTagName('*'))] as HTMLElement[];
    sources.forEach((source, i) => {
        const computed = getComputedStyle(source);
        let cssText = '';
        for (let j = 0; j < computed.length; j += 1) {
            const prop = computed[j];
            cssText += `${prop}:${computed.getPropertyValue(prop)};`;
        }
        targets[i].style.cssText = cssText;
    });
    return cloned;
}

/**
 * After TipTap’s `dragHandler` runs, replaces the drag bitmap with an offscreen **clone** of the
 * block (TipTap already does this, but its `setDragImage` X offset uses the clone wrapper’s
 * viewport rect, which breaks for horizontally centered editors). Using the **live** `nodeDOM`
 * as the drag image (`setDragImage(blockEl, …)`) looked wrong: huge opaque snapshot, bad Y
 * anchor. We mirror TipTap’s clone approach, align offsets to the real block rect, add vertical
 * offset so the cursor stays on the grip, and slightly reduce opacity for a Notion-like ghost.
 */
function createFixDragImageOffsetHandler(
    state: GutterState,
    getIsDragHandleDrag: () => boolean
): (e: DragEvent) => void {
    return (e: DragEvent) => {
        if (!getIsDragHandleDrag() || state.pos < 0 || !state.editor || !e.dataTransfer) return;
        const blockEl = state.editor.view.nodeDOM(state.pos) as HTMLElement | null;
        if (!blockEl) return;

        const blockRect = blockEl.getBoundingClientRect();
        const previewRoot = document.createElement('div');
        previewRoot.style.cssText =
            'position:fixed;left:0;top:-10000px;pointer-events:none;opacity:0.72;z-index:2147483647;';
        previewRoot.appendChild(cloneBlockElementWithStyles(blockEl));
        document.body.appendChild(previewRoot);

        const w = Math.max(1, blockRect.width);
        const h = Math.max(1, blockRect.height);
        const offsetX = Math.min(Math.max(0, e.clientX - blockRect.left), w);
        const offsetY = Math.min(Math.max(0, e.clientY - blockRect.top), h);
        e.dataTransfer.setDragImage(previewRoot, offsetX, offsetY);

        const removePreview = (): void => {
            previewRoot.remove();
        };
        document.addEventListener('dragend', removePreview, { once: true });
        document.addEventListener('drop', removePreview, { once: true });
    };
}

/**
 * Attaches `listener` to the editor container’s parent for `dragstart`, at most once.
 * Bubble order ensures our fix runs after TipTap’s handle `dragstart`.
 *
 * @param editor - TipTap editor (uses `view.dom.parentElement`).
 * @param listener - Typically {@link createFixDragImageOffsetHandler}'s return value.
 * @param registered - Mutable flag; set to `true` after the first successful add.
 */
function ensureParentDragStartListener(
    editor: Editor | undefined,
    listener: (e: DragEvent) => void,
    registered: { current: boolean }
): void {
    if (registered.current || !editor?.view.dom.parentElement) return;
    editor.view.dom.parentElement.addEventListener('dragstart', listener);
    registered.current = true;
}

/** Must match `DragHandle.configure({ computePositionConfig })` so our updates align with TipTap. */
const GUTTER_COMPUTE_POSITION_CONFIG = {
    ...defaultComputePositionConfig,
    middleware: [shift({ padding: 8 })]
};

/** Reads the hovered block's DOM and applies floating-ui's position to the wrapper. */
function computeAndApplyGutterPosition(state: GutterState): void {
    const { editor, wrapper, pos } = state;
    if (!editor || !wrapper || pos < 0) return;

    const dom = editor.view.nodeDOM(pos);
    if (!dom || dom.nodeType !== Node.ELEMENT_NODE) return;

    const virtualRef = {
        getBoundingClientRect: () => (dom as Element).getBoundingClientRect()
    };

    void computePosition(virtualRef, wrapper, GUTTER_COMPUTE_POSITION_CONFIG).then((val) => {
        if (!state.wrapper) return;
        Object.assign(state.wrapper.style, {
            position: val.strategy,
            left: `${val.x}px`,
            top: `${val.y}px`
        });
    });
}

/**
 * Owns the floating-ui `autoUpdate` lifecycle for the gutter wrapper.
 *
 * `schedule()` defers to the next frame because TipTap calls `onNodeChange` BEFORE
 * `repositionDragHandle` + `showHandle`. Running `computePosition` while the wrapper is
 * still `visibility: hidden` can give floating-ui wrong floating dimensions and leave
 * `top` too high (clipped under a sticky toolbar). One frame later, `showHandle` has
 * run and the wrapper is in its proper layout state.
 *
 * `tearDown()` is called when the gutter hides (no hovered block) and on editor destroy
 * so scroll listeners do not leak.
 */
function createGutterAutoPositioner(state: GutterState): {
    schedule: () => void;
    tearDown: () => void;
} {
    let dispose: (() => void) | null = null;

    const tearDown = (): void => {
        dispose?.();
        dispose = null;
    };

    const sync = (): void => {
        tearDown();
        const { editor, wrapper, pos } = state;
        if (!editor || !wrapper || pos < 0) return;

        const referenceEl = editor.view.nodeDOM(pos);
        if (!referenceEl || referenceEl.nodeType !== Node.ELEMENT_NODE) return;

        dispose = autoUpdate(
            referenceEl as Element,
            wrapper,
            () => computeAndApplyGutterPosition(state),
            { ancestorScroll: true, ancestorResize: true, elementResize: true }
        );
    };

    const schedule = (): void => {
        if (state.pos < 0) {
            tearDown();
        } else {
            requestAnimationFrame(sync);
        }
    };

    return { schedule, tearDown };
}

/**
 * Configures TipTap’s {@link DragHandle} with a two-part gutter: draggable grip + “+” button.
 *
 * - One wrapper from `render()`; TipTap attaches drag behavior to that root’s draggable child.
 * - Grip is first so the add control stays inside the padded gutter and is not clipped.
 * - Add button is `draggable="false"` and stops `dragstart` so it never starts a block drag.
 * - Floating UI `shift` keeps the gutter on-screen; `autoUpdate` re-runs positioning on scroll
 *   (TipTap’s plugin only recomputes when the hovered *node* changes, and `document` scroll misses
 *   inner scroll containers because scroll events do not bubble).
 *
 * @param addBlockAriaLabel - Pre-translated aria-label for the “+” button.
 * @returns A configured `DragHandle` extension ready for `Editor` extensions array.
 */
export function createBlockGutterDragHandle(addBlockAriaLabel: string) {
    const state: GutterState = { editor: null, pos: -1, nodeSize: 0, wrapper: null };
    const positioner = createGutterAutoPositioner(state);

    let isDragHandleDrag = false;
    const dragImageListenerRegistered = { current: false };
    let editorEventsHooked = false;

    const fixDragImageOffset = createFixDragImageOffsetHandler(state, () => isDragHandleDrag);

    /**
     * Resets the wrapper's stale absolute layout when no valid tracked block exists.
     *
     * The wrapper is `position: absolute` inside the editor's `position: relative;
     * overflow-y: auto` scroll container, so any non-zero `top` extends `scrollHeight`.
     * If the tracked block was removed by a doc edit (select-all-delete) OR the mouse
     * left the editor (TipTap's `mouseleave` clears `state.pos` and sets visibility
     * hidden but keeps the stale `top`), and the user then edits via keyboard,
     * the wrapper's `top` from a prior positioning is what leaves a phantom scrollbar.
     *
     * Invariant: after any doc transaction, if we aren't actively tracking a real block,
     * the wrapper's absolute layout must be `top: 0; left: 0`.
     */
    const resetWrapperIfStale = (editor: Editor): void => {
        if (!state.wrapper) return;

        // Keep the gutter in place if we're actively tracking a block that still exists.
        if (state.pos >= 0) {
            const docSize = editor.state.doc.content.size;
            const stillThere = state.pos < docSize && editor.view.nodeDOM(state.pos) != null;
            if (stillThere) return;
        }

        // No valid tracked block — collapse the wrapper to the origin so its layout
        // contribution to scrollHeight goes away. TipTap's plugin will reposition on
        // the next mousemove.
        state.pos = -1;
        state.nodeSize = 0;
        state.wrapper.style.top = '0';
        state.wrapper.style.left = '0';
        state.wrapper.style.visibility = 'hidden';
        positioner.tearDown();
    };

    /** Registers one-shot lifecycle listeners on the active editor (idempotent). */
    const hookEditorEventsOnce = (editor: Editor): void => {
        if (editorEventsHooked) return;
        editor.on('update', ({ editor: ed }) => resetWrapperIfStale(ed));
        editor.on('destroy', () => {
            positioner.tearDown();
            editorEventsHooked = false;
        });
        editorEventsHooked = true;
    };

    return DragHandle.configure({
        computePositionConfig: GUTTER_COMPUTE_POSITION_CONFIG,
        onNodeChange: (raw) => {
            const payload = raw as DragHandleNodeChangePayload;
            state.editor = payload.editor;
            state.pos = payload.pos ?? -1;
            state.nodeSize = payload.node?.nodeSize ?? 0;

            ensureParentDragStartListener(
                payload.editor,
                fixDragImageOffset,
                dragImageListenerRegistered
            );
            hookEditorEventsOnce(payload.editor);
            positioner.schedule();
        },
        onElementDragStart: () => {
            isDragHandleDrag = true;
            document.documentElement.style.setProperty('cursor', 'grabbing', 'important');
        },
        onElementDragEnd: () => {
            isDragHandleDrag = false;
            document.documentElement.style.removeProperty('cursor');
        },
        render: () => createGutterRoot(state, addBlockAriaLabel)
    });
}
