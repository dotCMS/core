import { vi } from 'vitest';

import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { type PendingAsset } from './pending-asset';
import { insertAndUploadAssets, uploadAsset } from './upload-asset';

import { Audio, DOT_AUDIO_NODE_NAME } from '../nodes/audio.extension';
import { DOT_IMAGE_NODE_NAME, DotImage } from '../nodes/image.extension';
import {
    UPLOAD_PLACEHOLDER_NODE_NAME,
    createUploadPlaceholderExtension,
    insertUploadPlaceholders
} from '../nodes/upload-placeholder.extension';
import { DOT_VIDEO_NODE_NAME, Video } from '../nodes/video.extension';

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

function buildEditor(): Editor {
    return new Editor({
        extensions: [
            StarterKit,
            DotImage,
            Video,
            Audio,
            createUploadPlaceholderExtension({ uploading: () => 'Uploading' })
        ],
        content: '<p></p>'
    });
}

function pending(mediaType: PendingAsset['mediaType'], label = 'thing'): PendingAsset {
    return {
        id: `${mediaType}-1`,
        mediaType,
        file: new File(['x'], `thing.${mediaType}`, { type: `${mediaType}/x` }),
        label
    };
}

function nodeOfType(editor: Editor, type: string) {
    return editor.getJSON().content?.find((n) => n.type === type);
}

function hasPlaceholder(editor: Editor): boolean {
    return editor.getJSON().content?.some((n) => n.type === UPLOAD_PLACEHOLDER_NODE_NAME) ?? false;
}

describe('uploadAsset', () => {
    let editor: Editor;

    beforeEach(() => {
        editor = buildEditor();
    });

    afterEach(() => editor?.destroy());

    it('replaces the placeholder with a dotImage carrying src, alt and data', async () => {
        const asset = pending('image', 'shot');
        insertUploadPlaceholders(editor, 1, [asset]);
        const uploadImage = vi.fn().mockResolvedValue({
            src: '/dA/i1',
            data: { identifier: 'id', inode: 'i1', languageId: 1, title: 't', asset: '/dA/i1' }
        });

        await uploadAsset(editor, asset, { uploadImage });

        expect(uploadImage).toHaveBeenCalledWith(asset.file);
        expect(hasPlaceholder(editor)).toBe(false);
        expect(nodeOfType(editor, DOT_IMAGE_NODE_NAME)?.attrs).toMatchObject({
            src: '/dA/i1',
            alt: 'shot',
            data: { identifier: 'id' }
        });
    });

    it('replaces the placeholder with a dotVideo carrying title and metadata', async () => {
        const asset = pending('video', 'clip');
        insertUploadPlaceholders(editor, 1, [asset]);
        const uploadVideo = vi.fn().mockResolvedValue({
            src: '/dA/v1',
            data: { identifier: 'id', inode: 'v1', languageId: 1, title: 't', asset: '/dA/v1' },
            mimeType: 'video/mp4',
            width: 640,
            height: 480,
            orientation: 'horizontal'
        });

        await uploadAsset(editor, asset, { uploadVideo });

        expect(nodeOfType(editor, DOT_VIDEO_NODE_NAME)?.attrs).toMatchObject({
            src: '/dA/v1',
            title: 'clip',
            mimeType: 'video/mp4',
            width: 640,
            height: 480,
            orientation: 'horizontal'
        });
    });

    it('replaces the placeholder with a dotAudio carrying title and mimeType', async () => {
        const asset = pending('audio', 'song');
        insertUploadPlaceholders(editor, 1, [asset]);
        const uploadAudio = vi.fn().mockResolvedValue({
            src: '/dA/a1',
            data: { identifier: 'id', inode: 'a1', languageId: 1, title: 't', asset: '/dA/a1' },
            mimeType: 'audio/mpeg'
        });

        await uploadAsset(editor, asset, { uploadAudio });

        expect(nodeOfType(editor, DOT_AUDIO_NODE_NAME)?.attrs).toMatchObject({
            src: '/dA/a1',
            title: 'song',
            mimeType: 'audio/mpeg'
        });
    });

    it('removes the placeholder when the upload fails', async () => {
        const asset = pending('image');
        insertUploadPlaceholders(editor, 1, [asset]);
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        await uploadAsset(editor, asset, {
            uploadImage: vi.fn().mockRejectedValue(new Error('x'))
        });

        expect(hasPlaceholder(editor)).toBe(false);
        expect(nodeOfType(editor, DOT_IMAGE_NODE_NAME)).toBeUndefined();
    });

    it('removes the placeholder when no uploader exists for that kind', async () => {
        const asset = pending('video');
        insertUploadPlaceholders(editor, 1, [asset]);
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        await uploadAsset(editor, asset, {});

        expect(hasPlaceholder(editor)).toBe(false);
    });
});

describe('insertAndUploadAssets', () => {
    let editor: Editor;

    beforeEach(() => {
        editor = buildEditor();
    });

    afterEach(() => editor?.destroy());

    it('returns false and inserts nothing for an empty list', () => {
        expect(insertAndUploadAssets(editor, 1, [], {})).toBe(false);
        expect(hasPlaceholder(editor)).toBe(false);
    });

    it('inserts all placeholders at once, then uploads each asset', async () => {
        const image = { ...pending('image'), id: 'img-1' };
        const audio = { ...pending('audio'), id: 'aud-1' };
        const uploadImage = vi.fn().mockResolvedValue({
            src: '/dA/i1',
            data: { identifier: 'id', inode: 'i1', languageId: 1, title: 't', asset: '/dA/i1' }
        });
        const uploadAudio = vi.fn().mockResolvedValue({
            src: '/dA/a1',
            data: { identifier: 'id', inode: 'a1', languageId: 1, title: 't', asset: '/dA/a1' },
            mimeType: 'audio/mpeg'
        });

        const handled = insertAndUploadAssets(editor, 1, [image, audio], {
            uploadImage,
            uploadAudio
        });

        expect(handled).toBe(true);
        expect(
            editor.getJSON().content?.filter((n) => n.type === UPLOAD_PLACEHOLDER_NODE_NAME)
        ).toHaveLength(2);

        await flushPromises();

        expect(hasPlaceholder(editor)).toBe(false);
        expect(nodeOfType(editor, DOT_IMAGE_NODE_NAME)).toBeDefined();
        expect(nodeOfType(editor, DOT_AUDIO_NODE_NAME)).toBeDefined();
    });
});
