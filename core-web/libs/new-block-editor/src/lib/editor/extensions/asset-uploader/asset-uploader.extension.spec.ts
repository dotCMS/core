import { vi } from 'vitest';

import { Editor } from '@tiptap/core';
import { Slice } from '@tiptap/pm/model';
import StarterKit from '@tiptap/starter-kit';

import {
    type MediaUploaders,
    createAssetUploaderExtension,
    handleMediaDrop,
    handleMediaPaste
} from './asset-uploader.extension';

import { type UploadedAudio, type UploadedImage } from '../../services/dot-upload.service';
import { pasteHTML } from '../../testing/editor.testing';
import { Audio, DOT_AUDIO_NODE_NAME } from '../nodes/audio.extension';
import { DOT_IMAGE_NODE_NAME, DotImage } from '../nodes/image.extension';
import { UPLOAD_PLACEHOLDER_NODE_NAME } from '../nodes/upload-placeholder.extension';

const PNG_DATA_URL = 'data:image/png;base64,iVBORw0KGgo=';

const UPLOADED_IMAGE: UploadedImage = {
    src: '/dA/inode-1',
    data: {
        identifier: 'id-1',
        inode: 'inode-1',
        languageId: 1,
        title: 'shot',
        asset: '/dA/inode-1'
    }
};

const UPLOADED_AUDIO: UploadedAudio = {
    src: '/dA/inode-9',
    data: {
        identifier: 'id-9',
        inode: 'inode-9',
        languageId: 1,
        title: 'song',
        asset: '/dA/inode-9'
    },
    mimeType: 'audio/mpeg'
};

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

/** An editor with the uploader wired; `withImages: false` models a field whose Allowed Blocks exclude images. */
function buildEditor(uploaders: MediaUploaders = {}, { withImages = true } = {}): Editor {
    return new Editor({
        extensions: [
            StarterKit,
            Audio,
            ...(withImages ? [DotImage] : []),
            createAssetUploaderExtension({
                uploaders,
                placeholderCopy: { uploading: () => 'Uploading' }
            })
        ],
        content: '<p>seed</p>'
    });
}

function nodeTypes(editor: Editor): string[] {
    return editor.getJSON().content?.map((n) => n.type ?? '') ?? [];
}

function hasNodeType(editor: Editor, name: string): boolean {
    let found = false;
    editor.state.doc.descendants((node) => {
        if (node.type.name === name) {
            found = true;
            return false;
        }
        return true;
    });
    return found;
}

function imageNodes(editor: Editor) {
    return editor.getJSON().content?.filter((n) => n.type === DOT_IMAGE_NODE_NAME) ?? [];
}

function buildDropEvent(files: File[]): DragEvent {
    return {
        dataTransfer: { files },
        preventDefault: vi.fn(),
        clientX: 0,
        clientY: 0
    } as unknown as DragEvent;
}

/**
 * The shape ProseMirror hands to `handlePaste`: a ClipboardEvent whose `clipboardData` exposes
 * `files` and `getData`. jsdom has no DataTransfer, so it is faked like `dataTransfer` above.
 */
function buildPasteEvent(files: File[], text = '', html = ''): ClipboardEvent {
    return {
        clipboardData: {
            files,
            getData: (type: string) => (type === 'text/html' ? html : text)
        },
        preventDefault: vi.fn()
    } as unknown as ClipboardEvent;
}

describe('createAssetUploaderExtension', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('registers the upload placeholder node itself', () => {
        editor = buildEditor();

        expect(editor.schema.nodes[UPLOAD_PLACEHOLDER_NODE_NAME]).toBeDefined();
    });

    it('wires drop and paste into the view', async () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor({ uploadImage });
        vi.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
        const file = new File(['x'], 'shot.png', { type: 'image/png' });

        const dropped = editor.view.someProp('handleDrop', (fn) =>
            fn(editor.view, buildDropEvent([file]), Slice.empty, false)
        );
        expect(dropped).toBe(true);

        const pasted = editor.view.someProp('handlePaste', (fn) =>
            fn(editor.view, buildPasteEvent([file]), Slice.empty)
        );
        expect(pasted).toBe(true);

        await flushPromises();

        expect(uploadImage).toHaveBeenCalledTimes(2);
        expect(imageNodes(editor)).toHaveLength(2);
    });
});

describe('handleMediaDrop', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('returns false when no droppable media files are present', () => {
        editor = buildEditor();
        const event = buildDropEvent([new File(['x'], 'notes.txt', { type: 'text/plain' })]);

        expect(handleMediaDrop(editor, editor.view, event, false, {})).toBe(false);
        expect(event.preventDefault).not.toHaveBeenCalled();
    });

    it('returns false for an internal move so ProseMirror relocates the nodes itself', () => {
        editor = buildEditor();
        const event = buildDropEvent([new File(['x'], 'shot.png', { type: 'image/png' })]);

        expect(handleMediaDrop(editor, editor.view, event, true, {})).toBe(false);
    });

    it('handles an audio file: inserts a placeholder then replaces it with a dotAudio node', async () => {
        editor = buildEditor();
        // jsdom has no layout, so resolve the drop position via the selection fallback.
        vi.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
        const audioFile = new File(['x'], 'song.mp3', { type: 'audio/mpeg' });
        const event = buildDropEvent([audioFile]);
        const uploadAudio = vi.fn().mockResolvedValue(UPLOADED_AUDIO);

        const handled = handleMediaDrop(editor, editor.view, event, false, { uploadAudio });

        expect(handled).toBe(true);
        expect(event.preventDefault).toHaveBeenCalled();
        // Placeholder shows immediately, before the upload resolves.
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(true);

        await flushPromises();

        expect(uploadAudio).toHaveBeenCalledWith(audioFile);
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        const audioNode = editor.getJSON().content?.find((n) => n.type === DOT_AUDIO_NODE_NAME);
        expect(audioNode?.attrs?.['src']).toBe('/dA/inode-9');
        expect(audioNode?.attrs?.['title']).toBe('song');
        expect(audioNode?.attrs?.['mimeType']).toBe('audio/mpeg');
    });

    it('removes the placeholder when the audio upload fails', async () => {
        editor = buildEditor();
        vi.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
        const event = buildDropEvent([new File(['x'], 'song.mp3', { type: 'audio/mpeg' })]);
        const uploadAudio = vi.fn().mockRejectedValue(new Error('boom'));
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        handleMediaDrop(editor, editor.view, event, false, { uploadAudio });
        await flushPromises();

        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(hasNodeType(editor, DOT_AUDIO_NODE_NAME)).toBe(false);
    });

    it('swallows an image drop when the field does not allow images: no upload, no stuck placeholder, no browser navigation', () => {
        editor = buildEditor({}, { withImages: false });
        vi.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
        const event = buildDropEvent([new File(['x'], 'shot.png', { type: 'image/png' })]);
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);

        const handled = handleMediaDrop(editor, editor.view, event, false, { uploadImage });

        expect(handled).toBe(true);
        expect(event.preventDefault).toHaveBeenCalled();
        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
    });
});

/**
 * Parity with the legacy block-editor's `AssetUploader` paste handler (#38165): a file on the
 * clipboard is uploaded as a dotAsset, an image URL pasted as text becomes an image, and text that
 * merely travels with an image (Word) is left to the normal paste.
 */
describe('handleMediaPaste', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('returns false when the clipboard carries no media file and no image URL', () => {
        editor = buildEditor();
        const uploadImage = vi.fn();
        const uploaders = { uploadImage };

        expect(
            handleMediaPaste(editor, editor.view, buildPasteEvent([], 'plain text'), uploaders)
        ).toBe(false);
        expect(
            handleMediaPaste(
                editor,
                editor.view,
                buildPasteEvent([new File(['x'], 'notes.txt', { type: 'text/plain' })]),
                uploaders
            )
        ).toBe(false);
        expect(handleMediaPaste(editor, editor.view, undefined, uploaders)).toBe(false);
        expect(uploadImage).not.toHaveBeenCalled();
    });

    it('uploads a pasted image file: placeholder first, then the dotImage with identifier data', async () => {
        editor = buildEditor();
        const file = new File(['x'], 'image.png', { type: 'image/png' });
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);

        const handled = handleMediaPaste(editor, editor.view, buildPasteEvent([file]), {
            uploadImage
        });

        expect(handled).toBe(true);
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(true);

        await flushPromises();

        expect(uploadImage).toHaveBeenCalledWith(file);
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        const [image] = imageNodes(editor);
        expect(image?.attrs?.['src']).toBe('/dA/inode-1');
        expect(image?.attrs?.['data']).toEqual(UPLOADED_IMAGE.data);
    });

    it('uploads a pasted file even when the browser also exposes an <img> in text/html (Copy Image)', async () => {
        editor = buildEditor();
        const file = new File(['x'], 'image.png', { type: 'image/png' });
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        const event = buildPasteEvent([file], '', '<img src="https://other.site/a.png">');

        expect(handleMediaPaste(editor, editor.view, event, { uploadImage })).toBe(true);

        await flushPromises();

        expect(uploadImage).toHaveBeenCalledWith(file);
        expect(imageNodes(editor)[0]?.attrs?.['src']).toBe('/dA/inode-1');
    });

    it('leaves the paste alone when the HTML already carries a data: image, so transformPasted owns it', () => {
        editor = buildEditor();
        const file = new File(['x'], 'image.png', { type: 'image/png' });
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        const event = buildPasteEvent([file], '', `<img src="${PNG_DATA_URL}">`);

        expect(handleMediaPaste(editor, editor.view, event, { uploadImage })).toBe(false);
        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
    });

    it('leaves the paste alone when the HTML carries a data: image even if the text is an image URL', () => {
        editor = buildEditor();
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        const event = buildPasteEvent(
            [],
            'https://dotcms.com/images/logo.png',
            `<img src="${PNG_DATA_URL}">`
        );

        expect(handleMediaPaste(editor, editor.view, event, { uploadImage })).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });

    it('leaves the paste alone when text travels with the image (Word formatting image)', () => {
        editor = buildEditor();
        const file = new File(['x'], 'image.png', { type: 'image/png' });
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        const event = buildPasteEvent([file], 'Some text from Word');

        expect(handleMediaPaste(editor, editor.view, event, { uploadImage })).toBe(false);
        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
    });

    it('turns a pasted image URL into a dotImage without uploading', () => {
        editor = buildEditor();
        const uploadImage = vi.fn();
        const event = buildPasteEvent([], 'https://dotcms.com/images/logo.png');

        expect(handleMediaPaste(editor, editor.view, event, { uploadImage })).toBe(true);

        expect(uploadImage).not.toHaveBeenCalled();
        expect(imageNodes(editor)[0]?.attrs?.['src']).toBe('https://dotcms.com/images/logo.png');
    });

    it('does nothing when the field does not allow images', () => {
        editor = buildEditor({}, { withImages: false });
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        const file = new File(['x'], 'image.png', { type: 'image/png' });

        expect(
            handleMediaPaste(editor, editor.view, buildPasteEvent([file]), { uploadImage })
        ).toBe(false);
        expect(
            handleMediaPaste(editor, editor.view, buildPasteEvent([], 'https://dotcms.com/a.png'), {
                uploadImage
            })
        ).toBe(false);
        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
    });

    it('removes the placeholder when the upload fails', async () => {
        editor = buildEditor();
        const uploadImage = vi.fn().mockRejectedValue(new Error('boom'));
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        handleMediaPaste(
            editor,
            editor.view,
            buildPasteEvent([new File(['x'], 'image.png', { type: 'image/png' })]),
            { uploadImage }
        );
        await flushPromises();

        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });
});

/**
 * Base64 images inside pasted HTML or Markdown used to be dropped by `DotImage`'s parse rule
 * (#38165). Through the paste pipeline they now become upload placeholders, get uploaded, and end
 * up as identifier-based `dotImage` nodes.
 */
describe('pasted base64 images', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('leaves a paste without data: images alone', () => {
        const uploadImage = vi.fn();
        editor = buildEditor({ uploadImage });

        pasteHTML(editor, '<p>hello</p><img src="https://dotcms.com/a.png">');

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)[0]?.attrs?.['src']).toBe('https://dotcms.com/a.png');
    });

    it('swaps a pasted data: image for a placeholder, then for the uploaded dotImage', async () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor({ uploadImage });

        pasteHTML(editor, `<p>intro</p><img src="${PNG_DATA_URL}" alt="shot">`);

        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(true);
        expect(imageNodes(editor)).toHaveLength(0);
        expect(uploadImage).toHaveBeenCalledTimes(1);
        const file = uploadImage.mock.calls[0][0] as File;
        expect(file.type).toBe('image/png');
        expect(file.name).toBe('pasted-image.png');

        await flushPromises();

        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        const [image] = imageNodes(editor);
        expect(image?.attrs?.['src']).toBe('/dA/inode-1');
        expect(image?.attrs?.['alt']).toBe('shot');
        expect(image?.attrs?.['data']).toEqual(UPLOADED_IMAGE.data);
        expect(editor.getText()).toContain('intro');
    });

    it('handles every data: image in the paste, including ones nested inside other blocks', () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor({ uploadImage });

        pasteHTML(
            editor,
            `<img src="${PNG_DATA_URL}"><blockquote><p>q</p><img src="${PNG_DATA_URL}"></blockquote>`
        );

        expect(uploadImage).toHaveBeenCalledTimes(2);
        let placeholders = 0;
        editor.state.doc.descendants((node) => {
            if (node.type.name === UPLOAD_PLACEHOLDER_NODE_NAME) placeholders++;
            return true;
        });
        expect(placeholders).toBe(2);
    });

    it('splits a paragraph around a mid-sentence image, as any block dotImage does', () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor({ uploadImage });
        editor.commands.setContent('<p></p>');

        pasteHTML(editor, `<p>before <img src="${PNG_DATA_URL}"> after</p>`);

        expect(nodeTypes(editor)).toEqual(['paragraph', UPLOAD_PLACEHOLDER_NODE_NAME, 'paragraph']);
        expect(editor.getText()).toContain('before');
        expect(editor.getText()).toContain('after');
    });

    it('handles an unencoded SVG data URL whose src contains ">" characters', () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor({ uploadImage });
        editor.commands.setContent('<p></p>');
        const svg = "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg'><rect/></svg>";

        pasteHTML(editor, `<p>a</p><img alt="shape" src="${svg}"><p>b</p>`);

        expect(uploadImage).toHaveBeenCalledTimes(1);
        expect((uploadImage.mock.calls[0][0] as File).type).toBe('image/svg+xml');
        expect(nodeTypes(editor)).toEqual(['paragraph', UPLOAD_PLACEHOLDER_NODE_NAME, 'paragraph']);
    });

    it('removes the placeholder when the upload fails', async () => {
        const uploadImage = vi.fn().mockRejectedValue(new Error('boom'));
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        editor = buildEditor({ uploadImage });

        pasteHTML(editor, `<img src="${PNG_DATA_URL}">`);
        await flushPromises();

        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });

    it('drops an undecodable data: image without uploading, as the schema parser always did', () => {
        const uploadImage = vi.fn();
        editor = buildEditor({ uploadImage });

        pasteHTML(editor, '<p>x</p><img src="data:image/png;base64,***">');

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });

    it('does nothing when the field does not allow images', () => {
        const uploadImage = vi.fn();
        editor = buildEditor({ uploadImage }, { withImages: false });

        pasteHTML(editor, `<p>x</p><img src="${PNG_DATA_URL}">`);

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(editor.getText()).toContain('x');
    });

    it('does nothing when no image uploader is available', () => {
        editor = buildEditor({});

        pasteHTML(editor, `<img src="${PNG_DATA_URL}">`);

        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });

    it('uploads a base64 image dropped as rich HTML too: ProseMirror runs transformPasted on drops', async () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor({ uploadImage });
        // jsdom has no layout; land the drop at the end of the seed paragraph.
        vi.spyOn(editor.view, 'posAtCoords').mockReturnValue({ pos: 5, inside: 0 });
        const html = `<p>dragged</p><img src="${PNG_DATA_URL}" alt="shot">`;
        // A drag of selected page content: text/html on the transfer, no files.
        const drop = new Event('drop', { bubbles: true, cancelable: true });
        Object.defineProperty(drop, 'dataTransfer', {
            value: {
                files: [],
                types: ['text/html'],
                getData: (type: string) => (type === 'text/html' ? html : '')
            }
        });
        Object.assign(drop, { clientX: 0, clientY: 0 });

        editor.view.dom.dispatchEvent(drop);

        expect(uploadImage).toHaveBeenCalledTimes(1);
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(true);
        expect(editor.getText()).toContain('dragged');

        await flushPromises();

        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        const [image] = imageNodes(editor);
        expect(image?.attrs?.['src']).toBe('/dA/inode-1');
        expect(image?.attrs?.['data']).toEqual(UPLOADED_IMAGE.data);
    });

    it('leaves setContent on the schema parser: a data: image in loaded HTML is still dropped', () => {
        const uploadImage = vi.fn();
        editor = buildEditor({ uploadImage });

        editor.commands.setContent(`<p>stored</p><img src="${PNG_DATA_URL}">`);

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, UPLOAD_PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });
});
