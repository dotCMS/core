import type { Editor } from '@tiptap/core';

import type { EditorDialogManagerService } from './services/editor-dialog-manager.service';

/**
 * Handles clicks on rich content inside ProseMirror (link edit dialog).
 * Kept outside the component to keep EditorComponent focused on lifecycle and wiring.
 */
export function handleEditorProseMirrorClick(
    event: MouseEvent,
    editor: Editor,
    dialogManager: EditorDialogManagerService
): void {
    const anchor = (event.target as HTMLElement).closest('a[href]');
    if (!anchor) return;

    const href = anchor.getAttribute('href') ?? '';
    const displayText = anchor.textContent?.trim() ?? '';

    let anchorPos: number;
    try {
        anchorPos = editor.view.posAtDOM(anchor, 0);
    } catch {
        anchorPos = editor.state.selection.from;
    }

    event.preventDefault();

    dialogManager.openLink(() => anchor.getBoundingClientRect(), {
        initialValues: { href, displayText },
        linkEl: anchor as HTMLElement,
        anchorPos
    });
}
