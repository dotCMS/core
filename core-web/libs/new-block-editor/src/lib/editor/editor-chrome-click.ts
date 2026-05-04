import type { Editor } from '@tiptap/core';

import type { EditorPopoverService } from './services/editor-popover.service';

/**
 * Handles clicks on rich content inside ProseMirror (opens the link-edit popover).
 * Kept outside the component to keep EditorComponent focused on lifecycle and wiring.
 */
export function handleEditorProseMirrorClick(
    event: MouseEvent,
    editor: Editor,
    popovers: EditorPopoverService
): void {
    const anchor = (event.target as HTMLElement).closest('a[href]');
    if (!anchor) return;

    const href = anchor.getAttribute('href') ?? '';
    const displayText = anchor.textContent?.trim() ?? '';
    const target = anchor.getAttribute('target');
    const title = anchor.getAttribute('title');
    const ariaLabel = anchor.getAttribute('aria-label');
    const rel = anchor.getAttribute('rel');

    let anchorPos: number;
    try {
        anchorPos = editor.view.posAtDOM(anchor, 0);
    } catch {
        anchorPos = editor.state.selection.from;
    }

    event.preventDefault();

    popovers.openLink(() => anchor.getBoundingClientRect(), {
        initialValues: { href, displayText, target, title, ariaLabel, rel },
        linkEl: anchor as HTMLElement,
        anchorPos
    });
}
