import { Editor, type JSONContent } from '@tiptap/core';

import { type MediaUploaders, type PendingAsset } from './pending-asset';

import { DOT_AUDIO_NODE_NAME } from '../nodes/audio.extension';
import { DOT_IMAGE_NODE_NAME } from '../nodes/image.extension';
import {
    insertUploadPlaceholders,
    removePlaceholder,
    replacePlaceholder
} from '../nodes/upload-placeholder.extension';
import { DOT_VIDEO_NODE_NAME } from '../nodes/video.extension';

/**
 * Sends the file through the uploader for its kind and shapes the node that will replace the
 * placeholder. The attrs mirror what the asset pickers store, so an uploaded asset and a picked
 * one behave the same in the toolbar.
 *
 * @returns The node content, or `null` when the host has no uploader for this kind.
 */
async function uploadToNode(
    asset: PendingAsset,
    { uploadImage, uploadVideo, uploadAudio }: MediaUploaders
): Promise<JSONContent | null> {
    switch (asset.mediaType) {
        case 'image': {
            if (!uploadImage) return null;
            const { src, data } = await uploadImage(asset.file);

            return { type: DOT_IMAGE_NODE_NAME, attrs: { src, alt: asset.label, data } };
        }
        case 'video': {
            if (!uploadVideo) return null;
            const { src, data, mimeType, width, height, orientation } = await uploadVideo(
                asset.file
            );

            return {
                type: DOT_VIDEO_NODE_NAME,
                attrs: { src, title: asset.label, data, mimeType, width, height, orientation }
            };
        }
        case 'audio': {
            if (!uploadAudio) return null;
            const { src, data, mimeType } = await uploadAudio(asset.file);

            return {
                type: DOT_AUDIO_NODE_NAME,
                attrs: { src, title: asset.label, data, mimeType }
            };
        }
    }
}

/**
 * Uploads one asset and swaps its placeholder for the resulting node, or removes the placeholder
 * when the upload fails or no uploader exists. Every upload path, drop, file paste and base64
 * paste, ends here, so each kind of asset lands in the document in exactly one shape.
 */
export async function uploadAsset(
    editor: Editor,
    asset: PendingAsset,
    uploaders: MediaUploaders
): Promise<void> {
    try {
        const node = await uploadToNode(asset, uploaders);
        if (!node) throw new Error(`No uploader for ${asset.mediaType}`);
        replacePlaceholder(editor, asset.id, node);
    } catch (err) {
        console.error(`${asset.mediaType} upload failed`, err);
        removePlaceholder(editor, asset.id);
    }
}

/**
 * Inserts one placeholder per asset at `pos` in a single transaction, then uploads each asset.
 * Used by drop and by file paste; base64 paste places its placeholders inside the pasted slice
 * instead and only needs {@link uploadAsset}.
 *
 * @returns `true` when at least one asset was taken.
 */
export function insertAndUploadAssets(
    editor: Editor,
    pos: number,
    assets: PendingAsset[],
    uploaders: MediaUploaders
): boolean {
    if (!assets.length) return false;

    insertUploadPlaceholders(
        editor,
        pos,
        assets.map(({ id, mediaType }) => ({ id, mediaType }))
    );
    assets.forEach((asset) => void uploadAsset(editor, asset, uploaders));

    return true;
}
