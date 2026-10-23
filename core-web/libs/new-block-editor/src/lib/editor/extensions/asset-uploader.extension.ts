import { Editor, Extension } from '@tiptap/core';
import {
    DOMParser,
    Fragment,
    type Node as PMNode,
    Slice,
    type TagParseRule
} from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { EditorView } from '@tiptap/pm/view';

import { DOT_AUDIO_NODE_NAME } from './nodes/audio.extension';
import { DOT_IMAGE_NODE_NAME } from './nodes/image.extension';
import {
    type UploadPlaceholderCopy,
    type UploadPlaceholderMediaType,
    UPLOAD_PLACEHOLDER_NODE_NAME,
    createUploadPlaceholderExtension,
    createUploadPlaceholderId,
    insertUploadPlaceholders,
    removePlaceholder,
    replacePlaceholder
} from './nodes/upload-placeholder.extension';
import { DOT_VIDEO_NODE_NAME } from './nodes/video.extension';

import {
    type UploadedAudio,
    type UploadedImage,
    type UploadedVideo
} from '../services/dot-upload.service';
import { dataUrlToFile, isDataUrl } from '../utils/data-url.utils';
import { isImageUrl } from '../utils/url.utils';

/** Upload callbacks the editor host supplies; a missing one means that media kind is not uploaded. */
export interface MediaUploaders {
    uploadImage?: (file: File) => Promise<UploadedImage>;
    uploadVideo?: (file: File) => Promise<UploadedVideo>;
    uploadAudio?: (file: File) => Promise<UploadedAudio>;
}

/** Options for {@link createAssetUploaderExtension}. */
export interface AssetUploaderOptions {
    /** How each media kind reaches dotCMS. */
    uploaders: MediaUploaders;
    /** Pre-translated copy for the "Uploading…" placeholder row. */
    placeholderCopy: UploadPlaceholderCopy;
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
    moved: boolean,
    uploaders: MediaUploaders
): boolean {
    if (moved) return false;

    const files = Array.from(event.dataTransfer?.files ?? []);
    if (!files.some((file) => mediaTypeOf(file))) return false;

    event.preventDefault();
    const dropResult = view.posAtCoords({ left: event.clientX, top: event.clientY });
    const pos = dropResult?.pos ?? view.state.selection.from;

    uploadMediaFiles(editor, pos, files, uploaders);

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
 * - HTML carrying a base64 image wins over a file too: `transformPasted` (below) has already
 *   swapped it for a placeholder and started its upload, and consuming the paste here would
 *   discard that slice and leave the upload orphaned.
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
 * Paste-only parse rule: lets `<img src="data:image/…">` become a `dotImage` long enough for
 * `transformPasted` to swap it out. `DotImage`'s own rule excludes `data:` sources on purpose,
 * and that stays true for stored content and `setContent`, which never use this parser.
 */
const DATA_URL_IMAGE_RULE: TagParseRule = {
    tag: 'img[src^="data:image/"]',
    node: DOT_IMAGE_NODE_NAME,
    getAttrs: (element) => ({
        src: element.getAttribute('src'),
        alt: element.getAttribute('alt')
    })
};

/**
 * Everything that turns a file into a dotAsset inside the editor, in one place — the successor
 * of the legacy block-editor's `AssetUploader` extension (#38165).
 *
 * - Registers the `uploadPlaceholder` node that stands in while a file uploads.
 * - `handleDrop`: dropped image, video and audio files upload at the drop point.
 * - `handlePaste`: clipboard files and pasted image URLs, see {@link handleMediaPaste}.
 * - `clipboardParser` + `transformPasted`: base64 images in pasted HTML (and Markdown routed
 *   through `view.pasteHTML`) are kept as `dotImage` nodes by a paste-only parser, then swapped
 *   for placeholder nodes while their decoded files upload. Stored content and `setContent`
 *   keep the schema parser, which drops `data:` images as it always did. Only active when the
 *   field allows images and an image uploader exists.
 *
 * Placeholders are only ever minted here as nodes, never parsed from markup, so one cannot be
 * injected or duplicated through the clipboard.
 */
export function createAssetUploaderExtension({ uploaders, placeholderCopy }: AssetUploaderOptions) {
    return Extension.create({
        name: 'assetUploader',

        addExtensions() {
            return [createUploadPlaceholderExtension(placeholderCopy)];
        },

        addProseMirrorPlugins() {
            const { editor } = this;
            const { schema } = editor;
            const { uploadImage } = uploaders;
            const placeholderType = schema.nodes[UPLOAD_PLACEHOLDER_NODE_NAME];
            const uploadsPastedImages =
                Boolean(uploadImage) &&
                isMediaTypeAllowed(editor, 'image') &&
                Boolean(placeholderType);

            /** Replaces every `data:` `dotImage` in the fragment with a placeholder, recursively. */
            const swapDataUrlImages = (fragment: Fragment): Fragment => {
                const nodes: PMNode[] = [];
                fragment.forEach((node) => {
                    const src: string = node.attrs['src'] ?? '';
                    if (node.type.name !== DOT_IMAGE_NODE_NAME || !isDataUrl(src) || !uploadImage) {
                        nodes.push(node.isLeaf ? node : node.copy(swapDataUrlImages(node.content)));

                        return;
                    }

                    const file = dataUrlToFile(src, 'pasted-image');
                    // Undecodable: dropped, as the schema parser always did.
                    if (!file) return;

                    const id = createUploadPlaceholderId('image');
                    uploadImageIntoPlaceholder(
                        editor,
                        id,
                        file,
                        node.attrs['alt'] ?? file.name,
                        uploadImage
                    );
                    nodes.push(placeholderType.create({ id, mediaType: 'image' }));
                });

                return Fragment.from(nodes);
            };

            return [
                new Plugin({
                    key: new PluginKey('assetUploader'),
                    props: {
                        handleDrop: (view, event, _slice, moved) =>
                            handleMediaDrop(editor, view, event as DragEvent, moved, uploaders),
                        handlePaste: (view, event) =>
                            handleMediaPaste(editor, view, event, uploaders),
                        ...(uploadsPastedImages
                            ? {
                                  clipboardParser: new DOMParser(schema, [
                                      DATA_URL_IMAGE_RULE,
                                      ...DOMParser.fromSchema(schema).rules
                                  ]),
                                  transformPasted: (slice: Slice) =>
                                      new Slice(
                                          swapDataUrlImages(slice.content),
                                          slice.openStart,
                                          slice.openEnd
                                      )
                              }
                            : {})
                    }
                })
            ];
        }
    });
}
