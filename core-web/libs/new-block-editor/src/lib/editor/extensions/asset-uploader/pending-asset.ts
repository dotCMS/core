import { Editor } from '@tiptap/core';

import {
    type UploadedAudio,
    type UploadedImage,
    type UploadedVideo
} from '../../services/dot-upload.service';
import { dataUrlToFile } from '../../utils/data-url.utils';
import { DOT_AUDIO_NODE_NAME } from '../nodes/audio.extension';
import { DOT_IMAGE_NODE_NAME } from '../nodes/image.extension';
import {
    type UploadPlaceholderMediaType,
    createUploadPlaceholderId
} from '../nodes/upload-placeholder.extension';
import { DOT_VIDEO_NODE_NAME } from '../nodes/video.extension';

/** Kinds of asset the editor can upload; each becomes its own node once published. */
export type AssetMediaType = UploadPlaceholderMediaType;

/** Upload callbacks the editor host supplies; a missing one means that media kind is not uploaded. */
export interface MediaUploaders {
    uploadImage?: (file: File) => Promise<UploadedImage>;
    uploadVideo?: (file: File) => Promise<UploadedVideo>;
    uploadAudio?: (file: File) => Promise<UploadedAudio>;
}

/**
 * A file the editor has decided to upload: what it is, the placeholder that stands in for it
 * while the upload runs, and the text to keep on the final node.
 */
export interface PendingAsset {
    /** Placeholder id, also the key the upload result is matched back on. */
    readonly id: string;
    readonly mediaType: AssetMediaType;
    readonly file: File;
    /** Alt text (images) or title (video/audio) for the node that replaces the placeholder. */
    readonly label: string;
}

/** Node that each media kind becomes once uploaded. Absent from the schema when the field disallows it. */
const NODE_NAME_BY_MEDIA_TYPE: Record<AssetMediaType, string> = {
    image: DOT_IMAGE_NODE_NAME,
    video: DOT_VIDEO_NODE_NAME,
    audio: DOT_AUDIO_NODE_NAME
};

/** Which uploader serves each media kind. */
const UPLOADER_KEY_BY_MEDIA_TYPE: Record<AssetMediaType, keyof MediaUploaders> = {
    image: 'uploadImage',
    video: 'uploadVideo',
    audio: 'uploadAudio'
};

/** The media kind a file belongs to, or `null` when it is not an image, video or audio file. */
export function mediaTypeOf(file: File): AssetMediaType | null {
    if (file.type.startsWith('image/')) return 'image';
    if (file.type.startsWith('video/')) return 'video';
    if (file.type.startsWith('audio/')) return 'audio';

    return null;
}

/** True when at least one file is an image, video or audio file, whatever the field allows. */
export function hasMediaFile(files: File[]): boolean {
    return files.some((file) => mediaTypeOf(file) !== null);
}

/**
 * True when this media kind can actually be uploaded here: the field's schema has the node it
 * would become AND the host supplied an uploader for it. Anything else must be skipped up front;
 * a placeholder that nothing can replace would otherwise sit in the document forever.
 */
export function canUpload(
    editor: Editor,
    mediaType: AssetMediaType,
    uploaders: MediaUploaders
): boolean {
    return (
        Boolean(editor.schema.nodes[NODE_NAME_BY_MEDIA_TYPE[mediaType]]) &&
        Boolean(uploaders[UPLOADER_KEY_BY_MEDIA_TYPE[mediaType]])
    );
}

/** File name without its extension, used as the title of uploaded video and audio. */
function titleOf(file: File): string {
    return file.name.replace(/\.[^.]+$/, '');
}

/** Builds the pending asset for one file of a known, uploadable kind. */
function toPendingAsset(file: File, mediaType: AssetMediaType): PendingAsset {
    return {
        id: createUploadPlaceholderId(mediaType),
        mediaType,
        file,
        label: mediaType === 'image' ? file.name : titleOf(file)
    };
}

/**
 * Turns dropped or pasted files into the assets the editor will upload, in order. Files that
 * are not media, or whose kind {@link canUpload} rejects, are left out.
 */
export function prepareAssets(
    editor: Editor,
    files: File[],
    uploaders: MediaUploaders
): PendingAsset[] {
    const assets: PendingAsset[] = [];
    files.forEach((file) => {
        const mediaType = mediaTypeOf(file);
        if (mediaType && canUpload(editor, mediaType, uploaders)) {
            assets.push(toPendingAsset(file, mediaType));
        }
    });

    return assets;
}

/**
 * Turns a base64 image found in pasted content into a pending asset, or `null` when images
 * cannot be uploaded here or the data URL does not decode.
 *
 * @param alt The image's alt text, kept on the uploaded node; falls back to the file name.
 */
export function prepareDataUrlImage(
    editor: Editor,
    dataUrl: string,
    alt: string | null,
    uploaders: MediaUploaders
): PendingAsset | null {
    if (!canUpload(editor, 'image', uploaders)) return null;

    const file = dataUrlToFile(dataUrl, 'pasted-image');
    if (!file) return null;

    return { ...toPendingAsset(file, 'image'), label: alt || file.name };
}
