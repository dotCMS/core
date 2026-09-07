import { type Injector, runInInjectionContext } from '@angular/core';

import { Editor, type JSONContent } from '@tiptap/core';

import { type DotMessageService } from '@dotcms/data-access';

import { type SlashMenuService } from '../components/slash-menu/slash-menu.service';
import { createEditorExtensions } from '../extensions/editor-extensions';

/**
 * Builds a REAL TipTap editor with the dotCMS extension set, for tests that need behaviour rather
 * than schema.
 *
 * `buildEditorSchema` is the right tool when only the shape matters. It is the wrong tool here:
 * the #37340 defect lives in an `appendTransaction` and in input/paste rules, none of which exist
 * in a schema. A test that cannot type cannot see this bug.
 *
 * The caller owns the returned editor and MUST `destroy()` it — an undestroyed editor keeps a
 * ProseMirror view and its plugins alive across tests.
 */
export function createTestEditor(
    injector: Injector,
    options: { allowedBlocks?: string[]; content?: JSONContent | string } = {}
): Editor {
    const dotMessageService = { get: (key: string) => key } as unknown as DotMessageService;

    // The slash-command extension calls these during plugin construction; a bare `{}` throws.
    const menuService = {
        attachEditor: () => undefined,
        detachEditor: () => undefined,
        filterItems: () => [],
        open: () => undefined,
        update: () => undefined,
        close: () => undefined,
        handleKeyDown: () => false
    } as unknown as SlashMenuService;

    const extensions = runInInjectionContext(injector, () =>
        createEditorExtensions(menuService, options.allowedBlocks, injector, dotMessageService)
    );

    return new Editor({ extensions, content: options.content ?? '<p></p>' });
}

/**
 * Simulates an author typing, one character at a time.
 *
 * `insertContent` does NOT fire input rules — TipTap registers those on the view's
 * `handleTextInput` prop, which only a real text-input event reaches. Rules like `:copyright:` and
 * `:) ` match against the text *before* the cursor, so the characters must arrive individually or
 * the pattern never completes.
 *
 * Falls back to a plain insert when no rule handles the character, which is what ProseMirror's own
 * input handling does.
 */
export function typeText(editor: Editor, text: string): void {
    for (const char of Array.from(text)) {
        const { view } = editor;
        const { from, to } = view.state.selection;
        const handled = view.someProp('handleTextInput', (fn) => fn(view, from, to, char));

        if (!handled) {
            view.dispatch(view.state.tr.insertText(char, from, to));
        }
    }
}

/** Places the cursor at a document position. */
export function placeCursor(editor: Editor, pos: number): void {
    editor.commands.setTextSelection(pos);
}

/** The inline children of the first block, flattened for assertions. */
export function inlineNodes(editor: Editor): JSONContent[] {
    const json = editor.getJSON();

    return ((json.content?.[0] as JSONContent)?.content ?? []) as JSONContent[];
}

/** Every node type present anywhere in the document — the cheapest "did we create one?" check. */
export function nodeTypes(editor: Editor): string[] {
    const types: string[] = [];
    editor.state.doc.descendants((node) => {
        types.push(node.type.name);

        return true;
    });

    return types;
}

/** True when the document contains at least one `emoji` node. The core assertion of #37340. */
export function hasEmojiNode(editor: Editor): boolean {
    return nodeTypes(editor).includes('emoji');
}

/** Concatenated text of the whole document, so tests can assert the character survived. */
export function docText(editor: Editor): string {
    return editor.state.doc.textBetween(0, editor.state.doc.content.size, '', '');
}

/**
 * Pastes plain text through the real paste pipeline — paste rules, parse rules and all.
 *
 * `view.pasteText` defaults its event to `new ClipboardEvent('paste')`, which jsdom does not
 * implement. ProseMirror only forwards that event to `handlePaste`, so a plain `Event` is
 * sufficient and avoids polyfilling a global.
 */
export function pasteText(editor: Editor, text: string): void {
    editor.view.pasteText(text, new Event('paste') as ClipboardEvent);
}

/** Pastes HTML through the real paste pipeline. See {@link pasteText} on the event. */
export function pasteHTML(editor: Editor, html: string): void {
    editor.view.pasteHTML(html, new Event('paste') as ClipboardEvent);
}
