import { vi } from 'vitest';

import { Editor, type EditorOptions } from '@tiptap/core';
import { Slice } from '@tiptap/pm/model';
import StarterKit from '@tiptap/starter-kit';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    handleMediaDrop,
    handleMediaPaste,
    insertDotAudioFromContentlet,
    transformPastedDataUrlImages
} from './editor.utils';
import { Audio, DOT_AUDIO_NODE_NAME } from './extensions/nodes/audio.extension';
import { DOT_IMAGE_NODE_NAME, DotImage } from './extensions/nodes/image.extension';
import { createUploadPlaceholderExtension } from './extensions/nodes/upload-placeholder.extension';
import { type UploadedAudio, type UploadedImage } from './services/dot-upload.service';
import { pasteHTML } from './testing/editor.testing';

const PLACEHOLDER_NODE_NAME = 'uploadPlaceholder';

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

function buildEditor(options: Partial<EditorOptions> = {}): Editor {
    return new Editor({
        extensions: [
            StarterKit,
            Audio,
            DotImage,
            createUploadPlaceholderExtension({ uploading: () => 'Uploading' })
        ],
        content: '<p>seed</p>',
        ...options
    });
}

/** An editor whose field disallows images: no `dotImage` node in the schema. */
function buildEditorWithoutImages(): Editor {
    return new Editor({
        extensions: [
            StarterKit,
            Audio,
            createUploadPlaceholderExtension({ uploading: () => 'Uploading' })
        ],
        content: '<p>seed</p>'
    });
}

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

function imageNodes(editor: Editor) {
    return editor.getJSON().content?.filter((n) => n.type === DOT_IMAGE_NODE_NAME) ?? [];
}

/**
 * The shape ProseMirror hands to `handlePaste`: a ClipboardEvent whose `clipboardData` exposes
 * `files` and `getData`. jsdom has no DataTransfer, so it is faked the same way the drop spec fakes
 * `dataTransfer`.
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

function buildDropEvent(files: File[]): DragEvent {
    return {
        dataTransfer: { files },
        preventDefault: vi.fn(),
        clientX: 0,
        clientY: 0
    } as unknown as DragEvent;
}

describe('insertDotAudioFromContentlet', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('inserts a dotAudio node mapping the contentlet onto the node attrs', () => {
        editor = buildEditor();
        const contentlet = {
            identifier: 'id-1',
            inode: 'inode-1',
            languageId: 2,
            title: 'My Podcast',
            mimeType: 'audio/mpeg'
        } as unknown as DotCMSContentlet;

        insertDotAudioFromContentlet(editor, contentlet);

        const audioNode = editor.getJSON().content?.find((n) => n.type === DOT_AUDIO_NODE_NAME);
        expect(audioNode).toBeDefined();
        expect(audioNode?.attrs?.['src']).toBe('/dA/inode-1');
        expect(audioNode?.attrs?.['title']).toBe('My Podcast');
        expect(audioNode?.attrs?.['mimeType']).toBe('audio/mpeg');
        expect(audioNode?.attrs?.['data']).toEqual({
            identifier: 'id-1',
            inode: 'inode-1',
            languageId: 2,
            title: 'My Podcast',
            asset: '/dA/inode-1'
        });
    });
});

describe('handleMediaDrop — audio', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('returns false when no droppable media files are present', () => {
        editor = buildEditor();
        const event = buildDropEvent([new File(['x'], 'notes.txt', { type: 'text/plain' })]);

        const handled = handleMediaDrop(editor, editor.view, event, Slice.empty, false);

        expect(handled).toBe(false);
        expect(event.preventDefault).not.toHaveBeenCalled();
    });

    it('handles an audio file: inserts a placeholder then replaces it with a dotAudio node', async () => {
        editor = buildEditor();
        // jsdom has no layout, so resolve the drop position via the selection fallback.
        vi.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
        const audioFile = new File(['x'], 'song.mp3', { type: 'audio/mpeg' });
        const event = buildDropEvent([audioFile]);

        const uploaded: UploadedAudio = {
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
        const uploadAudio = vi.fn().mockResolvedValue(uploaded);

        const handled = handleMediaDrop(
            editor,
            editor.view,
            event,
            Slice.empty,
            false,
            undefined,
            undefined,
            uploadAudio
        );

        expect(handled).toBe(true);
        expect(event.preventDefault).toHaveBeenCalled();
        // Placeholder shows immediately, before the upload resolves.
        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(true);

        await flushPromises();

        expect(uploadAudio).toHaveBeenCalledWith(audioFile);
        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
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

        handleMediaDrop(
            editor,
            editor.view,
            event,
            Slice.empty,
            false,
            undefined,
            undefined,
            uploadAudio
        );

        await flushPromises();

        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(hasNodeType(editor, DOT_AUDIO_NODE_NAME)).toBe(false);
    });
});

describe('handleMediaDrop — schema gating', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('swallows an image drop when the field does not allow images: no upload, no stuck placeholder, no browser navigation', () => {
        editor = buildEditorWithoutImages();
        vi.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
        const event = buildDropEvent([new File(['x'], 'shot.png', { type: 'image/png' })]);
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);

        const handled = handleMediaDrop(
            editor,
            editor.view,
            event,
            Slice.empty,
            false,
            uploadImage
        );

        expect(handled).toBe(true);
        expect(event.preventDefault).toHaveBeenCalled();
        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
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

        expect(
            handleMediaPaste(editor, editor.view, buildPasteEvent([], 'plain text'), {
                uploadImage
            })
        ).toBe(false);
        expect(
            handleMediaPaste(
                editor,
                editor.view,
                buildPasteEvent([new File(['x'], 'notes.txt', { type: 'text/plain' })]),
                { uploadImage }
            )
        ).toBe(false);
        expect(handleMediaPaste(editor, editor.view, undefined, { uploadImage })).toBe(false);
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
        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(true);

        await flushPromises();

        expect(uploadImage).toHaveBeenCalledWith(file);
        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
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

    it('leaves the paste alone when text travels with the image (Word formatting image)', () => {
        editor = buildEditor();
        const file = new File(['x'], 'image.png', { type: 'image/png' });
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        const event = buildPasteEvent([file], 'Some text from Word');

        expect(handleMediaPaste(editor, editor.view, event, { uploadImage })).toBe(false);
        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
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
        editor = buildEditorWithoutImages();
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
        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
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

        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });
});

/**
 * Base64 images inside pasted HTML or Markdown used to be dropped by `DotImage`'s parse rule
 * (#38165). They are swapped for upload placeholders before ProseMirror parses the HTML, uploaded,
 * and then replaced by the resulting dotImage.
 */
describe('transformPastedDataUrlImages', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('returns the HTML untouched when it has no data: images', () => {
        editor = buildEditor();
        const uploadImage = vi.fn();
        const html = '<p>hello <img src="https://dotcms.com/a.png"></p>';

        expect(transformPastedDataUrlImages(editor, html, uploadImage)).toBe(html);
        expect(uploadImage).not.toHaveBeenCalled();
    });

    it('returns the HTML untouched when the field does not allow images', () => {
        editor = buildEditorWithoutImages();
        const uploadImage = vi.fn();
        const html = `<p>hello</p><img src="${PNG_DATA_URL}">`;

        expect(transformPastedDataUrlImages(editor, html, uploadImage)).toBe(html);
        expect(uploadImage).not.toHaveBeenCalled();
    });

    it('swaps each data: image for an upload placeholder and uploads the decoded file', () => {
        editor = buildEditor();
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        const html = `<p>a</p><img src="${PNG_DATA_URL}" alt="one"><p>b</p><img src="${PNG_DATA_URL}">`;

        const out = transformPastedDataUrlImages(editor, html, uploadImage);

        expect(out).not.toContain('data:image');
        expect(out.match(/data-upload-id="/g)).toHaveLength(2);
        expect(out).toContain('<p>a</p>');
        expect(uploadImage).toHaveBeenCalledTimes(2);
        const file = uploadImage.mock.calls[0][0] as File;
        expect(file.type).toBe('image/png');
        expect(file.name).toMatch(/\.png$/);
    });

    it('through the real paste pipeline: placeholder lands in the doc, then becomes the uploaded dotImage', async () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor({
            editorProps: {
                transformPastedHTML: (html) =>
                    transformPastedDataUrlImages(editor, html, uploadImage)
            }
        });

        pasteHTML(editor, `<p>intro</p><img src="${PNG_DATA_URL}" alt="shot">`);

        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(true);
        expect(imageNodes(editor)).toHaveLength(0);

        await flushPromises();

        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
        const [image] = imageNodes(editor);
        expect(image?.attrs?.['src']).toBe('/dA/inode-1');
        expect(image?.attrs?.['data']).toEqual(UPLOADED_IMAGE.data);
        expect(editor.getText()).toContain('intro');
    });

    it('removes the placeholder when the upload fails', async () => {
        const uploadImage = vi.fn().mockRejectedValue(new Error('boom'));
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        editor = buildEditor({
            editorProps: {
                transformPastedHTML: (html) =>
                    transformPastedDataUrlImages(editor, html, uploadImage)
            }
        });

        pasteHTML(editor, `<img src="${PNG_DATA_URL}">`);
        await flushPromises();

        expect(hasNodeType(editor, PLACEHOLDER_NODE_NAME)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });
});
