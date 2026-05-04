import { Editor } from '@tiptap/core';
import { Slice } from '@tiptap/pm/model';
import { EditorView } from '@tiptap/pm/view';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { type DotImageData, DOT_IMAGE_NODE_NAME } from './extensions/nodes/image.extension';
import {
    insertUploadPlaceholders,
    replacePlaceholder,
    removePlaceholder
} from './extensions/nodes/upload-placeholder.extension';
import { type DotVideoData, DOT_VIDEO_NODE_NAME } from './extensions/nodes/video.extension';
import { type UploadedImage, type UploadedVideo } from './services/dot-upload.service';

export function handleMediaDrop(
    editor: Editor,
    view: EditorView,
    event: DragEvent,
    _slice: Slice,
    moved: boolean,
    uploadImage?: (file: File) => Promise<UploadedImage>,
    uploadVideo?: (file: File) => Promise<UploadedVideo>
): boolean {
    if (moved) return false;

    const allFiles = Array.from(event.dataTransfer?.files ?? []);
    const imageFiles = allFiles.filter((f) => f.type.startsWith('image/'));
    const videoFiles = allFiles.filter((f) => f.type.startsWith('video/'));

    if (!imageFiles.length && !videoFiles.length) return false;

    event.preventDefault();
    const dropResult = view.posAtCoords({ left: event.clientX, top: event.clientY });
    const pos = dropResult?.pos ?? view.state.selection.from;

    // Build placeholder descriptors for all files
    const imagePlaceholders = imageFiles.map((_, i) => ({
        id: `img-${Date.now()}-${i}`,
        mediaType: 'image' as const
    }));
    const videoPlaceholders = videoFiles.map((_, i) => ({
        id: `vid-${Date.now()}-${i}`,
        mediaType: 'video' as const
    }));

    // Insert all placeholders in one transaction — gives immediate feedback
    insertUploadPlaceholders(editor, pos, [...imagePlaceholders, ...videoPlaceholders]);

    // ── Images ──────────────────────────────────────────────────────────────
    imageFiles.forEach((file, i) => {
        const { id } = imagePlaceholders[i];

        if (uploadImage) {
            uploadImage(file)
                .then(({ src, data }) =>
                    replacePlaceholder(editor, id, {
                        type: DOT_IMAGE_NODE_NAME,
                        attrs: { src, alt: file.name, data }
                    })
                )
                .catch((err) => {
                    console.error('Image drop upload failed', err);
                    removePlaceholder(editor, id);
                });
        } else {
            const reader = new FileReader();
            reader.onload = () => {
                replacePlaceholder(editor, id, {
                    type: DOT_IMAGE_NODE_NAME,
                    attrs: { src: reader.result as string, alt: file.name }
                });
            };
            reader.readAsDataURL(file);
        }
    });

    // ── Videos ──────────────────────────────────────────────────────────────
    videoFiles.forEach((file, i) => {
        const { id } = videoPlaceholders[i];

        if (uploadVideo) {
            uploadVideo(file)
                .then(({ src, data }) => {
                    const title = file.name.replace(/\.[^.]+$/, '');
                    replacePlaceholder(editor, id, {
                        type: DOT_VIDEO_NODE_NAME,
                        attrs: { src, title, data }
                    });
                })
                .catch((err) => {
                    console.error('Video drop upload failed', err);
                    removePlaceholder(editor, id);
                });
        } else {
            removePlaceholder(editor, id);
        }
    });

    return true;
}

/**
 * Maps a dotCMS contentlet onto a `dotImage` node and inserts it at the editor's current
 * selection. Shared by the AI Image flow ({@link EditorModalService.openAiImage})
 * and the dotCMS browser-selector flow inside `<dot-image-insert-dialog>`. Both paths produce a
 * `DotCMSContentlet` that needs to land in the editor with the same `data` shape so the
 * image toolbar (alignment, wrap, link, properties) keeps working uniformly.
 */
export function insertDotImageFromContentlet(editor: Editor, contentlet: DotCMSContentlet): void {
    const data: DotImageData = {
        identifier: contentlet.identifier,
        inode: contentlet.inode,
        languageId: (contentlet as { languageId?: number }).languageId ?? 1,
        title: contentlet.title ?? '',
        asset: `/dA/${contentlet.inode}`
    };

    editor
        .chain()
        .focus()
        .insertContent({
            type: DOT_IMAGE_NODE_NAME,
            attrs: {
                src: data.asset,
                title: data.title || null,
                alt: data.title || null,
                data
            }
        })
        .run();
}

/**
 * Maps a dotCMS contentlet onto a `dotVideo` node and inserts it at the editor's current
 * selection. Used by the dotCMS browser-selector flow inside `<dot-video-dialog>` —
 * mirrors {@link insertDotImageFromContentlet} so both media nodes share the same
 * `data` shape (identifier, inode, languageId, title, asset).
 */
export function insertDotVideoFromContentlet(editor: Editor, contentlet: DotCMSContentlet): void {
    const data: DotVideoData = {
        identifier: contentlet.identifier,
        inode: contentlet.inode,
        languageId: (contentlet as { languageId?: number }).languageId ?? 1,
        title: contentlet.title ?? '',
        asset: `/dA/${contentlet.inode}`
    };

    editor
        .chain()
        .focus()
        .insertContent({
            type: DOT_VIDEO_NODE_NAME,
            attrs: {
                src: data.asset,
                title: data.title || null,
                data
            }
        })
        .run();
}
