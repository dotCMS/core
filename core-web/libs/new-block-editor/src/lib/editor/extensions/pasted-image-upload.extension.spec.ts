import { vi } from 'vitest';

import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { DOT_IMAGE_NODE_NAME, DotImage } from './nodes/image.extension';
import {
    UPLOAD_PLACEHOLDER_NODE_NAME,
    createUploadPlaceholderExtension
} from './nodes/upload-placeholder.extension';
import { createPastedImageUploadExtension } from './pasted-image-upload.extension';

import { type UploadedImage } from '../services/dot-upload.service';
import { pasteHTML } from '../testing/editor.testing';

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

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

function buildEditor(
    uploadImage: ((file: File) => Promise<UploadedImage>) | undefined,
    { withImages = true } = {}
): Editor {
    return new Editor({
        extensions: [
            StarterKit,
            ...(withImages ? [DotImage] : []),
            createUploadPlaceholderExtension({ uploading: () => 'Uploading' }),
            createPastedImageUploadExtension(uploadImage)
        ],
        content: '<p></p>'
    });
}

function nodeTypes(editor: Editor): string[] {
    return editor.getJSON().content?.map((n) => n.type ?? '') ?? [];
}

function imageNodes(editor: Editor) {
    return editor.getJSON().content?.filter((n) => n.type === DOT_IMAGE_NODE_NAME) ?? [];
}

function hasPlaceholder(editor: Editor): boolean {
    return nodeTypes(editor).includes(UPLOAD_PLACEHOLDER_NODE_NAME);
}

/**
 * Base64 images inside pasted HTML or Markdown used to be dropped by `DotImage`'s parse rule
 * (#38165). Through the paste pipeline they now become upload placeholders, get uploaded, and end
 * up as identifier-based `dotImage` nodes.
 */
describe('createPastedImageUploadExtension', () => {
    let editor: Editor;

    afterEach(() => editor?.destroy());

    it('leaves a paste without data: images alone', () => {
        const uploadImage = vi.fn();
        editor = buildEditor(uploadImage);

        pasteHTML(editor, '<p>hello</p><img src="https://dotcms.com/a.png">');

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasPlaceholder(editor)).toBe(false);
        expect(imageNodes(editor)[0]?.attrs?.['src']).toBe('https://dotcms.com/a.png');
    });

    it('swaps a pasted data: image for a placeholder, then for the uploaded dotImage', async () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor(uploadImage);

        pasteHTML(editor, `<p>intro</p><img src="${PNG_DATA_URL}" alt="shot">`);

        expect(hasPlaceholder(editor)).toBe(true);
        expect(imageNodes(editor)).toHaveLength(0);
        expect(uploadImage).toHaveBeenCalledTimes(1);
        const file = uploadImage.mock.calls[0][0] as File;
        expect(file.type).toBe('image/png');
        expect(file.name).toBe('pasted-image.png');

        await flushPromises();

        expect(hasPlaceholder(editor)).toBe(false);
        const [image] = imageNodes(editor);
        expect(image?.attrs?.['src']).toBe('/dA/inode-1');
        expect(image?.attrs?.['alt']).toBe('shot');
        expect(image?.attrs?.['data']).toEqual(UPLOADED_IMAGE.data);
        expect(editor.getText()).toContain('intro');
    });

    it('handles every data: image in the paste, including ones nested inside other blocks', () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor(uploadImage);

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
        editor = buildEditor(uploadImage);

        pasteHTML(editor, `<p>before <img src="${PNG_DATA_URL}"> after</p>`);

        expect(nodeTypes(editor)).toEqual(['paragraph', UPLOAD_PLACEHOLDER_NODE_NAME, 'paragraph']);
        expect(editor.getText()).toContain('before');
        expect(editor.getText()).toContain('after');
    });

    it('handles an unencoded SVG data URL whose src contains ">" characters', () => {
        const uploadImage = vi.fn().mockResolvedValue(UPLOADED_IMAGE);
        editor = buildEditor(uploadImage);
        const svg = "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg'><rect/></svg>";

        pasteHTML(editor, `<p>a</p><img alt="shape" src="${svg}"><p>b</p>`);

        expect(uploadImage).toHaveBeenCalledTimes(1);
        expect((uploadImage.mock.calls[0][0] as File).type).toBe('image/svg+xml');
        expect(nodeTypes(editor)).toEqual(['paragraph', UPLOAD_PLACEHOLDER_NODE_NAME, 'paragraph']);
    });

    it('removes the placeholder when the upload fails', async () => {
        const uploadImage = vi.fn().mockRejectedValue(new Error('boom'));
        vi.spyOn(console, 'error').mockImplementation(() => undefined);
        editor = buildEditor(uploadImage);

        pasteHTML(editor, `<img src="${PNG_DATA_URL}">`);
        await flushPromises();

        expect(hasPlaceholder(editor)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });

    it('drops an undecodable data: image without uploading, as the schema parser always did', () => {
        const uploadImage = vi.fn();
        editor = buildEditor(uploadImage);

        pasteHTML(editor, '<p>x</p><img src="data:image/png;base64,***">');

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasPlaceholder(editor)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });

    it('does nothing when the field does not allow images', () => {
        const uploadImage = vi.fn();
        editor = buildEditor(uploadImage, { withImages: false });

        pasteHTML(editor, `<p>x</p><img src="${PNG_DATA_URL}">`);

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasPlaceholder(editor)).toBe(false);
        expect(editor.getText()).toContain('x');
    });

    it('does nothing when no uploader is available', () => {
        editor = buildEditor(undefined);

        pasteHTML(editor, `<img src="${PNG_DATA_URL}">`);

        expect(hasPlaceholder(editor)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });

    it('leaves setContent on the schema parser: a data: image in loaded HTML is still dropped', () => {
        const uploadImage = vi.fn();
        editor = buildEditor(uploadImage);

        editor.commands.setContent(`<p>stored</p><img src="${PNG_DATA_URL}">`);

        expect(uploadImage).not.toHaveBeenCalled();
        expect(hasPlaceholder(editor)).toBe(false);
        expect(imageNodes(editor)).toHaveLength(0);
    });
});
