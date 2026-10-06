import { Editor } from '@tiptap/core';
import StarterKit from '@tiptap/starter-kit';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { Audio, DOT_AUDIO_NODE_NAME, audioMetaAttrsFromContentlet } from './audio.extension';

function buildEditor(content = ''): Editor {
    return new Editor({
        extensions: [StarterKit, Audio],
        content
    });
}

describe('Audio extension', () => {
    let editor: Editor;

    afterEach(() => {
        editor?.destroy();
    });

    it('registers under the immutable `dotAudio` node name', () => {
        expect(DOT_AUDIO_NODE_NAME).toBe('dotAudio');
        editor = buildEditor();
        expect(editor.schema.nodes[DOT_AUDIO_NODE_NAME]).toBeDefined();
    });

    it('defaults src / title / mimeType / data to null', () => {
        editor = buildEditor('<audio src="/dA/abc"></audio>');
        const node = editor.state.doc.child(0);

        expect(node.type.name).toBe(DOT_AUDIO_NODE_NAME);
        expect(node.attrs['src']).toBe('/dA/abc');
        expect(node.attrs['title']).toBeNull();
        expect(node.attrs['mimeType']).toBeNull();
        expect(node.attrs['data']).toBeNull();
    });

    it('parses an <audio src> tag into a dotAudio node', () => {
        editor = buildEditor('<audio src="/dA/song"></audio>');
        const json = editor.getJSON();

        expect(json.content?.[0]?.type).toBe(DOT_AUDIO_NODE_NAME);
        expect(json.content?.[0]?.attrs?.['src']).toBe('/dA/song');
    });

    it('renders an <audio> element with native controls', () => {
        editor = buildEditor('<audio src="/dA/song"></audio>');
        const html = editor.getHTML();

        expect(html).toContain('<audio');
        expect(html).toContain('controls');
        expect(html).toContain('src="/dA/song"');
    });

    it('wraps the audio in a selectable .audio-block container', () => {
        // A bare <audio controls> captures every click on its native controls, so the atom
        // node could never be selected/deleted with the mouse. The node view wraps it in a
        // container that provides a clickable, non-interactive selection surface.
        editor = buildEditor('<audio src="/dA/song"></audio>');
        const wrapper = editor.view.dom.querySelector('.audio-block');

        expect(wrapper).toBeTruthy();
        expect(wrapper?.querySelector('audio')).toBeTruthy();
    });

    it('toggles is-selected on the wrapper as the node is selected/deselected', () => {
        // Trailing paragraph gives the selection somewhere to land that isn't the atom node,
        // so we can observe both the selected and the deselected state.
        editor = buildEditor('<audio src="/dA/song"></audio><p>after</p>');
        const wrapper = editor.view.dom.querySelector('.audio-block');

        editor.commands.setNodeSelection(0);
        expect(wrapper?.classList.contains('is-selected')).toBe(true);

        editor.commands.setTextSelection(editor.state.doc.content.size);
        expect(wrapper?.classList.contains('is-selected')).toBe(false);
    });

    it('serializes the mimeType attribute only when present', () => {
        editor = buildEditor();
        editor
            .chain()
            .insertContent({
                type: DOT_AUDIO_NODE_NAME,
                attrs: { src: '/dA/song', mimeType: 'audio/mpeg' }
            })
            .run();

        // HTML attribute names are lowercased by the DOM when set/serialized (both `setAttribute`
        // and `getAttribute` normalize case for HTML elements in an HTML document), so the
        // rendered markup carries `mimetype`, not `mimeType`, even though the node attr and
        // `parseHTML`/`renderHTML` keys stay camelCase.
        expect(editor.getHTML()).toContain('mimetype="audio/mpeg"');
    });
});

describe('audioMetaAttrsFromContentlet', () => {
    it('prefers the contentlet mimeType when set', () => {
        const contentlet = { mimeType: 'audio/mpeg' } as unknown as DotCMSContentlet;
        expect(audioMetaAttrsFromContentlet(contentlet)).toEqual({ mimeType: 'audio/mpeg' });
    });

    it('falls back to the file metadata contentType', () => {
        const contentlet = {
            metaData: { contentType: 'audio/wav' }
        } as unknown as DotCMSContentlet;
        expect(audioMetaAttrsFromContentlet(contentlet)).toEqual({ mimeType: 'audio/wav' });
    });

    it('returns null when no mime information is available', () => {
        expect(audioMetaAttrsFromContentlet({} as DotCMSContentlet)).toEqual({ mimeType: null });
    });
});
