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

    // A linked image renders as <a href><img></a>. Clicking the image must NOT open the link
    // editor — the click selects the `dotImage` node (surfacing the image toolbar group), and the
    // link is edited from the toolbar Link button. Just stop the anchor from navigating. Opening
    // the text-link editor here would show the wrong fields and, on save, replace the image
    // (#36361). Gate on the *clicked* element being the image (not merely any descendant img) so
    // a text click in a mixed anchor still routes to the text-link editor.
    const clickedImage = (event.target as HTMLElement).closest('img');
    if (clickedImage && anchor.contains(clickedImage)) {
        event.preventDefault();
        return;
    }

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
