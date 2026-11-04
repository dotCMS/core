import { vi } from 'vitest';

import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { canUpload, mediaTypeOf, prepareAssets, prepareDataUrlImage } from './pending-asset';

import { Audio } from '../nodes/audio.extension';
import { DotImage } from '../nodes/image.extension';

const PNG_DATA_URL = 'data:image/png;base64,iVBORw0KGgo=';

/** Schema with images and audio but no video, so each gate can be exercised. */
function buildEditor(): Editor {
    return new Editor({ extensions: [StarterKit, DotImage, Audio], content: '<p></p>' });
}

const png = new File(['x'], 'shot.png', { type: 'image/png' });
const mp4 = new File(['x'], 'clip.mp4', { type: 'video/mp4' });
const mp3 = new File(['x'], 'song.mp3', { type: 'audio/mpeg' });
const txt = new File(['x'], 'notes.txt', { type: 'text/plain' });

describe('mediaTypeOf', () => {
    it('classifies by mime family and rejects everything else', () => {
        expect(mediaTypeOf(png)).toBe('image');
        expect(mediaTypeOf(mp4)).toBe('video');
        expect(mediaTypeOf(mp3)).toBe('audio');
        expect(mediaTypeOf(txt)).toBeNull();
    });
});

describe('canUpload', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('needs both the node in the schema and an uploader from the host', () => {
        editor = buildEditor();
        const uploadImage = vi.fn();
        const uploadVideo = vi.fn();

        expect(canUpload(editor, 'image', { uploadImage })).toBe(true);
        expect(canUpload(editor, 'image', {})).toBe(false);
        // Video node is not in this schema, even with an uploader.
        expect(canUpload(editor, 'video', { uploadVideo })).toBe(false);
        expect(canUpload(editor, 'audio', {})).toBe(false);
    });
});

describe('prepareAssets', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('keeps only uploadable media, in order, each with its own placeholder id and label', () => {
        editor = buildEditor();
        const uploaders = { uploadImage: vi.fn(), uploadVideo: vi.fn(), uploadAudio: vi.fn() };

        const assets = prepareAssets(editor, [txt, png, mp4, mp3], uploaders);

        expect(assets.map((a) => a.mediaType)).toEqual(['image', 'audio']);
        expect(assets[0].file).toBe(png);
        expect(assets[0].label).toBe('shot.png');
        expect(assets[1].label).toBe('song');
        expect(assets[0].id).not.toBe(assets[1].id);
        expect(assets[0].id).toMatch(/^img-/);
        expect(assets[1].id).toMatch(/^aud-/);
    });

    it('returns nothing when the host cannot upload that kind', () => {
        editor = buildEditor();

        expect(prepareAssets(editor, [png], {})).toEqual([]);
    });
});

describe('prepareDataUrlImage', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('decodes the data URL into a file and keeps the alt text as the label', () => {
        editor = buildEditor();

        const asset = prepareDataUrlImage(editor, PNG_DATA_URL, 'shot', { uploadImage: vi.fn() });

        expect(asset?.mediaType).toBe('image');
        expect(asset?.file.type).toBe('image/png');
        expect(asset?.file.name).toBe('pasted-image.png');
        expect(asset?.label).toBe('shot');
    });

    it('falls back to the file name when there is no alt', () => {
        editor = buildEditor();

        const asset = prepareDataUrlImage(editor, PNG_DATA_URL, null, { uploadImage: vi.fn() });

        expect(asset?.label).toBe('pasted-image.png');
    });

    it('returns null when images cannot be uploaded here or the URL does not decode', () => {
        editor = buildEditor();

        expect(prepareDataUrlImage(editor, PNG_DATA_URL, null, {})).toBeNull();
        expect(
            prepareDataUrlImage(editor, 'data:image/png;base64,***', null, { uploadImage: vi.fn() })
        ).toBeNull();
    });
});
