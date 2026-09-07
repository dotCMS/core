import { Editor, Node } from '@tiptap/core';
import { NodeSelection } from '@tiptap/pm/state';
import StarterKit from '@tiptap/starter-kit';

import {
    createBlockGutterDragHandle,
    getViewDragging,
    patchAtomDragToNodeSelection,
    setViewDragging
} from './block-gutter.extension';

/** Minimal atom block — no Angular node view — for drag-move unit tests. */
const TestAtom = Node.create({
    name: 'testAtom',
    group: 'block',
    atom: true,
    addAttributes() {
        return { id: { default: null } };
    },
    parseHTML() {
        return [{ tag: 'div[data-test-atom]' }];
    },
    renderHTML({ HTMLAttributes }) {
        return ['div', { 'data-test-atom': '', ...HTMLAttributes }];
    }
});

function buildAtomEditor(): Editor {
    return new Editor({
        extensions: [StarterKit, createBlockGutterDragHandle('Add block'), TestAtom],
        content: {
            type: 'doc',
            content: [
                { type: 'testAtom', attrs: { id: 'aaa' } },
                { type: 'testAtom', attrs: { id: 'bbb' } },
                { type: 'testAtom', attrs: { id: 'ccc' } }
            ]
        }
    });
}

describe('block gutter atom drag move fix (#36976)', () => {
    let editor: Editor;

    afterEach(() => {
        editor?.destroy();
    });

    it('registers the blockGutter extension (drag handle + atom move fix)', () => {
        editor = buildAtomEditor();
        expect(editor.extensionManager.extensions.some((ext) => ext.name === 'blockGutter')).toBe(
            true
        );
        expect(editor.extensionManager.extensions.some((ext) => ext.name === 'dragHandle')).toBe(
            true
        );
    });

    it('patchAtomDragToNodeSelection rewrites view.dragging to a NodeSelection for atoms', () => {
        editor = buildAtomEditor();
        let thirdPos = -1;
        editor.state.doc.forEach((child, offset) => {
            if (child.attrs['id'] === 'ccc') thirdPos = offset;
        });
        expect(thirdPos).toBeGreaterThanOrEqual(0);

        const node = editor.state.doc.nodeAt(thirdPos);
        expect(node?.type.name).toBe('testAtom');
        expect(node?.isAtom).toBe(true);

        setViewDragging(editor.view, {
            slice: editor.state.doc.slice(thirdPos, thirdPos + node!.nodeSize),
            move: true,
            node: undefined
        });

        expect(patchAtomDragToNodeSelection(editor.view, thirdPos)).toBe(true);

        const dragging = getViewDragging(editor.view);
        expect(dragging?.move).toBe(true);
        expect(dragging?.node).toBeInstanceOf(NodeSelection);
        expect(dragging?.node?.from).toBe(thirdPos);
        expect(editor.state.selection).toBeInstanceOf(NodeSelection);
        expect(editor.state.selection.from).toBe(thirdPos);
    });

    it('patchAtomDragToNodeSelection is a no-op for non-atom blocks', () => {
        editor = new Editor({
            extensions: [StarterKit, createBlockGutterDragHandle('Add block')],
            content: '<p>Hello</p><p>World</p>'
        });
        setViewDragging(editor.view, {
            slice: editor.state.doc.slice(0, 7),
            move: true,
            node: undefined
        });
        expect(patchAtomDragToNodeSelection(editor.view, 0)).toBe(false);
        expect(getViewDragging(editor.view)?.node).toBeUndefined();
    });
});
