import type { Editor } from '@tiptap/core';

import type { ImageDialogService } from './components/image/image-dialog.service';
import type { LinkDialogService } from './components/link/link-dialog.service';

/**
 * Handles clicks on rich content inside ProseMirror (image / link edit dialogs).
 * Kept outside the component to keep EditorComponent focused on lifecycle and wiring.
 */
export function handleEditorProseMirrorClick(
    event: MouseEvent,
    editor: Editor,
    _imageDialog: ImageDialogService,
    linkDialog: LinkDialogService
): void {
    // TODO: Image click-to-edit disabled — use the toolbar "Edit image properties" button instead.
    // const img = (event.target as HTMLElement).closest('img') as HTMLImageElement | null;
    // if (img) {
    //     const src = img.getAttribute('src') ?? '';
    //     const title = img.getAttribute('title') ?? '';
    //     const alt = img.getAttribute('alt') ?? '';
    //     const rect = img.getBoundingClientRect();
    //
    //     let imgPos: number;
    //     try {
    //         imgPos = editor.view.posAtDOM(img, 0);
    //     } catch {
    //         return;
    //     }
    //
    //     event.preventDefault();
    //
    //     imageDialog.open(
    //         (newSrc, newTitle, newAlt) => {
    //             editor
    //                 .chain()
    //                 .focus()
    //                 .setNodeSelection(imgPos)
    //                 .updateAttributes('image', {
    //                     src: newSrc,
    //                     title: newTitle || null,
    //                     alt: newAlt || null
    //                 })
    //                 .run();
    //         },
    //         () => rect,
    //         { src, title, alt }
    //     );
    //     return;
    // }

    const anchor = (event.target as HTMLElement).closest('a[href]');
    if (!anchor) return;

    const href = anchor.getAttribute('href') ?? '';
    const displayText = anchor.textContent?.trim() ?? '';
    const rect = anchor.getBoundingClientRect();

    let anchorPos: number;
    try {
        anchorPos = editor.view.posAtDOM(anchor, 0);
    } catch {
        anchorPos = editor.state.selection.from;
    }

    event.preventDefault();

    linkDialog.open(
        (newHref, newDisplayText) => {
            editor
                .chain()
                .focus()
                .setTextSelection(anchorPos)
                .extendMarkRange('link')
                .insertContent({
                    type: 'text',
                    text: newDisplayText ?? newHref,
                    marks: [{ type: 'link', attrs: { href: newHref } }]
                })
                .run();
        },
        () => rect,
        { href, displayText },
        anchor as HTMLElement
    );
}
