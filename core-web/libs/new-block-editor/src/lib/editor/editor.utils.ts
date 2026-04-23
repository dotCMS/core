import { Editor } from '@tiptap/core';
import { Slice } from '@tiptap/pm/model';
import { EditorView } from '@tiptap/pm/view';

import { DOT_IMAGE_NODE_NAME } from './extensions/nodes/image.extension';
import {
    insertUploadPlaceholders,
    replacePlaceholder,
    removePlaceholder
} from './extensions/nodes/upload-placeholder.extension';
import { DOT_VIDEO_NODE_NAME } from './extensions/nodes/video.extension';
import { type UploadedImage, type UploadedVideo } from './services/dot-cms-upload.service';

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
