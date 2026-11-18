import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { insertDotAudioFromContentlet } from './editor.utils';
import { Audio, DOT_AUDIO_NODE_NAME } from './extensions/nodes/audio.extension';

function buildEditor(): Editor {
    return new Editor({
        extensions: [StarterKit, Audio],
        content: '<p>seed</p>'
    });
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
