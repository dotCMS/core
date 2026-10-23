import { Editor } from '@tiptap/core';
import { Slice } from '@tiptap/pm/model';
import { EditorView } from '@tiptap/pm/view';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    type DotAudioData,
    DOT_AUDIO_NODE_NAME,
    audioMetaAttrsFromContentlet
} from './extensions/nodes/audio.extension';
import { type DotImageData, DOT_IMAGE_NODE_NAME } from './extensions/nodes/image.extension';
import {
    type UploadPlaceholderMediaType,
    createUploadPlaceholderId,
    insertUploadPlaceholders,
    replacePlaceholder,
    removePlaceholder,
    uploadPlaceholderHtml
} from './extensions/nodes/upload-placeholder.extension';
import {
    type DotVideoData,
    DOT_VIDEO_NODE_NAME,
    videoMetaAttrsFromContentlet
} from './extensions/nodes/video.extension';
import {
    type UploadedAudio,
    type UploadedImage,
    type UploadedVideo
} from './services/dot-upload.service';
import { dataUrlToFile, isDataUrl } from './utils/data-url.utils';
import { isImageUrl } from './utils/url.utils';

/** Upload callbacks the editor host supplies; a missing one means that media kind is not uploaded. */
export interface MediaUploaders {
    uploadImage?: (file: File) => Promise<UploadedImage>;
    uploadVideo?: (file: File) => Promise<UploadedVideo>;
    uploadAudio?: (file: File) => Promise<UploadedAudio>;
}

/** Node that each media kind becomes once uploaded. Absent from the schema when the field disallows it. */
const NODE_NAME_BY_MEDIA_TYPE: Record<UploadPlaceholderMediaType, string> = {
    image: DOT_IMAGE_NODE_NAME,
    video: DOT_VIDEO_NODE_NAME,
    audio: DOT_AUDIO_NODE_NAME
};

/** The media kind a file belongs to, or `null` when it is not an image, video or audio file. */
function mediaTypeOf(file: File): UploadPlaceholderMediaType | null {
    if (file.type.startsWith('image/')) return 'image';
    if (file.type.startsWith('video/')) return 'video';
    if (file.type.startsWith('audio/')) return 'audio';

    return null;
}

/** True when the field allows the node this media kind would become. */
function isMediaTypeAllowed(editor: Editor, mediaType: UploadPlaceholderMediaType): boolean {
    return Boolean(editor.schema.nodes[NODE_NAME_BY_MEDIA_TYPE[mediaType]]);
}

/** The files that are media AND whose node the field allows, grouped by kind. */
function groupAllowedMediaFiles(
    editor: Editor,
    files: File[]
): Record<UploadPlaceholderMediaType, File[]> {
    const groups: Record<UploadPlaceholderMediaType, File[]> = { image: [], video: [], audio: [] };
    files.forEach((file) => {
        const mediaType = mediaTypeOf(file);
        if (mediaType && isMediaTypeAllowed(editor, mediaType)) {
            groups[mediaType].push(file);
        }
    });

    return groups;
}

/**
 * Uploads one image and swaps its placeholder for the resulting `dotImage`, or removes the
 * placeholder when the upload fails. Shared by the drop, file-paste and base64-paste paths so
 * every pasted or dropped image ends up with the same identifier-based `src` and `data`.
 */
function uploadImageIntoPlaceholder(
    editor: Editor,
    placeholderId: string,
    file: File,
    alt: string,
    uploadImage: (file: File) => Promise<UploadedImage>
): void {
    uploadImage(file)
        .then(({ src, data }) =>
            replacePlaceholder(editor, placeholderId, {
                type: DOT_IMAGE_NODE_NAME,
                attrs: { src, alt, data }
            })
        )
        .catch((err) => {
            console.error('Image upload failed', err);
            removePlaceholder(editor, placeholderId);
        });
}

/**
 * Inserts an upload placeholder per media file at `pos`, uploads each file, and replaces the
 * placeholder with the resulting media node (or removes it on failure). Files that are not media,
 * or whose node the field disallows, are skipped — a placeholder that nothing can replace would
 * otherwise sit in the document forever.
 *
 * @returns `true` when at least one file was taken.
 */
export function uploadMediaFiles(
    editor: Editor,
    pos: number,
    files: File[],
    { uploadImage, uploadVideo, uploadAudio }: MediaUploaders
): boolean {
    const {
        image: imageFiles,
        video: videoFiles,
        audio: audioFiles
    } = groupAllowedMediaFiles(editor, files);

    if (!imageFiles.length && !videoFiles.length && !audioFiles.length) return false;

    // Build placeholder descriptors for all files
    const imagePlaceholders = imageFiles.map(() => ({
        id: createUploadPlaceholderId('image'),
        mediaType: 'image' as const
    }));
    const videoPlaceholders = videoFiles.map(() => ({
        id: createUploadPlaceholderId('video'),
        mediaType: 'video' as const
    }));
    const audioPlaceholders = audioFiles.map(() => ({
        id: createUploadPlaceholderId('audio'),
        mediaType: 'audio' as const
    }));

    // Insert all placeholders in one transaction — gives immediate feedback
    insertUploadPlaceholders(editor, pos, [
        ...imagePlaceholders,
        ...videoPlaceholders,
        ...audioPlaceholders
    ]);

    // ── Images ──────────────────────────────────────────────────────────────
    imageFiles.forEach((file, i) => {
        const { id } = imagePlaceholders[i];

        if (uploadImage) {
            uploadImageIntoPlaceholder(editor, id, file, file.name, uploadImage);
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
                .then(({ src, data, mimeType, width, height, orientation }) => {
                    const title = file.name.replace(/\.[^.]+$/, '');
                    replacePlaceholder(editor, id, {
                        type: DOT_VIDEO_NODE_NAME,
                        attrs: { src, title, data, mimeType, width, height, orientation }
                    });
                })
                .catch((err) => {
                    console.error('Video upload failed', err);
                    removePlaceholder(editor, id);
                });
        } else {
            removePlaceholder(editor, id);
        }
    });

    // ── Audio ───────────────────────────────────────────────────────────────
    audioFiles.forEach((file, i) => {
        const { id } = audioPlaceholders[i];

        if (uploadAudio) {
            uploadAudio(file)
                .then(({ src, data, mimeType }) => {
                    const title = file.name.replace(/\.[^.]+$/, '');
                    replacePlaceholder(editor, id, {
                        type: DOT_AUDIO_NODE_NAME,
                        attrs: { src, title, data, mimeType }
                    });
                })
                .catch((err) => {
                    console.error('Audio upload failed', err);
                    removePlaceholder(editor, id);
                });
        } else {
            removePlaceholder(editor, id);
        }
    });

    return true;
}

/**
 * ProseMirror `handleDrop` hook: uploads dropped image, video and audio files at the drop point.
 *
 * Returns `true` (and prevents the browser default) whenever the drop carries media files, even
 * when the field disallows every one of them — left to the browser, a dropped file navigates
 * the tab away from the editor.
 */
export function handleMediaDrop(
    editor: Editor,
    view: EditorView,
    event: DragEvent,
    _slice: Slice,
    moved: boolean,
    uploadImage?: (file: File) => Promise<UploadedImage>,
    uploadVideo?: (file: File) => Promise<UploadedVideo>,
    uploadAudio?: (file: File) => Promise<UploadedAudio>
): boolean {
    if (moved) return false;

    const files = Array.from(event.dataTransfer?.files ?? []);
    if (!files.some((file) => mediaTypeOf(file))) return false;

    event.preventDefault();
    const dropResult = view.posAtCoords({ left: event.clientX, top: event.clientY });
    const pos = dropResult?.pos ?? view.state.selection.from;

    uploadMediaFiles(editor, pos, files, { uploadImage, uploadVideo, uploadAudio });

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
 * - HTML carrying a base64 image wins over a file too: {@link transformPastedDataUrlImages}
 *   has already started uploading it, and consuming the paste here would discard the slice
 *   holding its placeholder and leave that upload orphaned.
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

    if (pastedImageUrl) {
        if (!isMediaTypeAllowed(editor, 'image')) return false;

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

    if (/data:image\//i.test(clipboard.getData('text/html') || '')) return false;

    return uploadMediaFiles(
        editor,
        view.state.selection.from,
        Array.from(clipboard.files ?? []),
        uploaders
    );
}

/**
 * Matches each `<img …>` tag in clipboard HTML. Quoted attribute values are consumed whole, so
 * a `>` inside the value, legal in serialized HTML and common in an unencoded
 * `data:image/svg+xml,<svg …>` source, does not end the match early.
 */
const IMG_TAG_PATTERN = /<img\b(?:[^>"']|"[^"]*"|'[^']*')*>/gi;

/**
 * Reads `src` and `alt` off a single `<img>` tag without touching the live document: an element
 * in a detached document never fetches its `src`.
 */
function readImgAttributes(tag: string): { src: string | null; alt: string | null } {
    const container = document.implementation.createHTMLDocument('').createElement('div');
    container.innerHTML = tag;
    const img = container.querySelector('img');

    return { src: img?.getAttribute('src') ?? null, alt: img?.getAttribute('alt') ?? null };
}

/**
 * ProseMirror `transformPastedHTML` hook: replaces every base64 (`data:`) image in pasted HTML
 * with an upload placeholder, uploads the decoded file, and swaps the placeholder for the
 * resulting `dotImage` once the upload finishes (#38165).
 *
 * `DotImage` deliberately does not parse `data:` sources, so without this the images in HTML
 * or Markdown pasted from another editor silently vanish. Replacing them before ProseMirror
 * parses the HTML keeps the surrounding content byte-for-byte as pasted; only the `<img>` tags
 * change. The placeholders reach the document synchronously with the paste, well before any
 * upload resolves.
 *
 * @returns The HTML to parse; unchanged when it carries no `data:` images, when images are not
 * allowed in the field, or when no uploader is available.
 */
export function transformPastedDataUrlImages(
    editor: Editor,
    html: string,
    uploadImage?: (file: File) => Promise<UploadedImage>
): string {
    if (!uploadImage || !isMediaTypeAllowed(editor, 'image') || !/data:image\//i.test(html)) {
        return html;
    }

    let pastedCount = 0;

    return html.replace(IMG_TAG_PATTERN, (tag) => {
        const { src, alt } = readImgAttributes(tag);
        if (!src || !isDataUrl(src)) return tag;

        const file = dataUrlToFile(src, `pasted-image-${++pastedCount}`);
        // Undecodable: leave the tag for the parser, which drops it as it always did.
        if (!file) return tag;

        const placeholder = { id: createUploadPlaceholderId('image'), mediaType: 'image' as const };
        uploadImageIntoPlaceholder(editor, placeholder.id, file, alt ?? file.name, uploadImage);

        return uploadPlaceholderHtml(placeholder);
    });
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
                data,
                ...videoMetaAttrsFromContentlet(contentlet)
            }
        })
        .run();
}

/**
 * Maps a dotCMS contentlet onto a `dotAudio` node and inserts it at the editor's current
 * selection. Used by the dotCMS browser-selector flow inside the audio picker — mirrors
 * {@link insertDotVideoFromContentlet} so both media nodes share the same `data` shape
 * (identifier, inode, languageId, title, asset). Audio carries only `mimeType` metadata.
 */
export function insertDotAudioFromContentlet(editor: Editor, contentlet: DotCMSContentlet): void {
    const data: DotAudioData = {
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
            type: DOT_AUDIO_NODE_NAME,
            attrs: {
                src: data.asset,
                title: data.title || null,
                data,
                ...audioMetaAttrsFromContentlet(contentlet)
            }
        })
        .run();
}
