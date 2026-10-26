import { Editor } from '@tiptap/core';
import { Fragment, type Node as PMNode, type TagParseRule } from '@tiptap/pm/model';
import { EditorView } from '@tiptap/pm/view';

import {
    type MediaUploaders,
    hasMediaFile,
    canUpload,
    prepareAssets,
    prepareDataUrlImage
} from './pending-asset';
import { insertAndUploadAssets, uploadAsset } from './upload-asset';

import { isDataUrl } from '../../utils/data-url.utils';
import { isImageUrl } from '../../utils/url.utils';
import { DOT_IMAGE_NODE_NAME } from '../nodes/image.extension';
import { UPLOAD_PLACEHOLDER_NODE_NAME } from '../nodes/upload-placeholder.extension';

/**
 * ProseMirror `handleDrop` hook: uploads dropped image, video and audio files at the drop point.
 *
 * Returns `true` (and prevents the browser default) whenever the drop carries media files, even
 * when none of them can be uploaded here — left to the browser, a dropped file navigates the tab
 * away from the editor.
 */
export function handleMediaDrop(
    editor: Editor,
    view: EditorView,
    event: DragEvent,
    moved: boolean,
    uploaders: MediaUploaders
): boolean {
    if (moved) return false;

    const files = Array.from(event.dataTransfer?.files ?? []);
    if (!hasMediaFile(files)) return false;

    event.preventDefault();
    const dropResult = view.posAtCoords({ left: event.clientX, top: event.clientY });
    const pos = dropResult?.pos ?? view.state.selection.from;
    insertAndUploadAssets(editor, pos, prepareAssets(editor, files, uploaders), uploaders);

    return true;
}

/**
 * ProseMirror `handlePaste` hook, restoring what the legacy block-editor's `AssetUploader` did
 * (#38165):
 *
 * - A media file on the clipboard (a screenshot, a file copied in Finder, a browser's
 *   "Copy Image") is uploaded as a dotAsset at the cursor, exactly like a drop.
 * - Plain text that is an image URL becomes an image node.
 * - Any other text wins over a file: Word puts a rendering of the formatted text on the
 *   clipboard as an image file next to the text, and the author wants the text.
 * - HTML carrying a base64 image wins over a file and over an image URL: {@link swapDataUrlImages}
 *   has already replaced it with a placeholder and started its upload, and consuming the paste
 *   here would discard that slice and leave the upload orphaned.
 *
 * @returns `true` when the paste was consumed; `false` lets ProseMirror paste as usual.
 */
export function handleMediaPaste(
    editor: Editor,
    view: EditorView,
    event: ClipboardEvent | undefined,
    uploaders: MediaUploaders
): boolean {
    const clipboard = event?.clipboardData;
    if (!clipboard) return false;

    const text = (clipboard.getData('text/plain') || '').trim();
    const pastedImageUrl = isImageUrl(text);
    if (text && !pastedImageUrl) return false;

    // Checked before the image-URL branch too: that branch also consumes the paste.
    if (/data:image\//i.test(clipboard.getData('text/html') || '')) return false;

    if (pastedImageUrl) {
        if (!editor.schema.nodes[DOT_IMAGE_NODE_NAME]) return false;

        editor
            .chain()
            .focus()
            .insertContent({
                type: DOT_IMAGE_NODE_NAME,
                attrs: { src: text, alt: null, title: null }
            })
            .run();

        return true;
    }

    const files = Array.from(clipboard.files ?? []);

    return insertAndUploadAssets(
        editor,
        view.state.selection.from,
        prepareAssets(editor, files, uploaders),
        uploaders
    );
}

/**
 * Paste-only parse rule: lets `<img src="data:image/…">` become a `dotImage` long enough for
 * `transformPasted` to swap it out. `DotImage`'s own rule excludes `data:` sources on purpose,
 * and that stays true for stored content and `setContent`, which never use this parser.
 */
export const DATA_URL_IMAGE_RULE: TagParseRule = {
    tag: 'img[src^="data:image/"]',
    node: DOT_IMAGE_NODE_NAME,
    getAttrs: (element) => ({
        src: element.getAttribute('src'),
        alt: element.getAttribute('alt')
    })
};

/** True when the paste-only parser and the base64 swap should be active for this editor. */
export function uploadsPastedImages(editor: Editor, uploaders: MediaUploaders): boolean {
    return (
        canUpload(editor, 'image', uploaders) &&
        Boolean(editor.schema.nodes[UPLOAD_PLACEHOLDER_NODE_NAME])
    );
}

/**
 * `transformPasted` body: walks a pasted fragment and replaces every `dotImage` whose `src` is a
 * `data:` URL with an upload placeholder node, starting that image's upload. Images that cannot
 * be prepared (undecodable, or uploads unavailable) are dropped, as the schema parser always did.
 */
export function swapDataUrlImages(
    editor: Editor,
    fragment: Fragment,
    uploaders: MediaUploaders
): Fragment {
    const placeholderType = editor.schema.nodes[UPLOAD_PLACEHOLDER_NODE_NAME];
    const nodes: PMNode[] = [];

    fragment.forEach((node) => {
        const src: string = node.attrs['src'] ?? '';
        if (node.type.name !== DOT_IMAGE_NODE_NAME || !isDataUrl(src)) {
            nodes.push(
                node.isLeaf ? node : node.copy(swapDataUrlImages(editor, node.content, uploaders))
            );

            return;
        }

        const asset = prepareDataUrlImage(editor, src, node.attrs['alt'] ?? null, uploaders);
        if (!asset) return;

        nodes.push(placeholderType.create({ id: asset.id, mediaType: asset.mediaType }));
        void uploadAsset(editor, asset, uploaders);
    });

    return Fragment.from(nodes);
}
