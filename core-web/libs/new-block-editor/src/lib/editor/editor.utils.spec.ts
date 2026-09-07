import { Editor } from '@tiptap/core';
import { Slice } from '@tiptap/pm/model';
import StarterKit from '@tiptap/starter-kit';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { handleMediaDrop, insertDotAudioFromContentlet } from './editor.utils';
import { Audio, DOT_AUDIO_NODE_NAME } from './extensions/nodes/audio.extension';
import { createUploadPlaceholderExtension } from './extensions/nodes/upload-placeholder.extension';
import { type UploadedAudio } from './services/dot-upload.service';

const PLACEHOLDER_NODE_NAME = 'uploadPlaceholder';

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

function buildEditor(): Editor {
    return new Editor({
        extensions: [
            StarterKit,
            Audio,
            createUploadPlaceholderExtension({ uploading: () => 'Uploading' })
        ],
        content: '<p>seed</p>'
    });
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
        preventDefault: jest.fn(),
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
        jest.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
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
        const uploadAudio = jest.fn().mockResolvedValue(uploaded);

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
        jest.spyOn(editor.view, 'posAtCoords').mockReturnValue(null);
        const event = buildDropEvent([new File(['x'], 'song.mp3', { type: 'audio/mpeg' })]);
        const uploadAudio = jest.fn().mockRejectedValue(new Error('boom'));
        jest.spyOn(console, 'error').mockImplementation(() => undefined);

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
