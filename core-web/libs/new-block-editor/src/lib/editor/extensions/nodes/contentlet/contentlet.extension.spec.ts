import type { Injector } from '@angular/core';

import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { DOT_CONTENTLET_NODE_NAME, createDotContentlet } from './contentlet.extension';

function buildEditor(): Editor {
    // Content stays free of `dotContent` nodes so the Angular node view never mounts —
    // it needs a real Angular Injector/ApplicationRef, unavailable in a plain Jest test.
    return new Editor({
        extensions: [StarterKit, createDotContentlet({} as Injector)],
        content: '<p></p>'
    });
}

describe('Contentlet extension', () => {
    let editor: Editor;

    afterEach(() => {
        editor?.destroy();
    });

    it('registers under the immutable `dotContent` node name', () => {
        expect(DOT_CONTENTLET_NODE_NAME).toBe('dotContent');
        editor = buildEditor();
        expect(editor.schema.nodes[DOT_CONTENTLET_NODE_NAME]).toBeDefined();
    });

    it('is an atom node with no contentDOM', () => {
        editor = buildEditor();
        expect(editor.schema.nodes[DOT_CONTENTLET_NODE_NAME].isAtom).toBe(true);
    });

    it('does not declare itself draggable at the node-spec level (regression for #36976)', () => {
        editor = buildEditor();
        expect(editor.schema.nodes[DOT_CONTENTLET_NODE_NAME].spec.draggable).toBeFalsy();
    });
});
