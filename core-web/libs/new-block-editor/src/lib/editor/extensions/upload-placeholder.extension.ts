import { Editor, Node } from '@tiptap/core';

export const UploadPlaceholderExtension = Node.create({
    name: 'uploadPlaceholder',
    group: 'block',
    atom: true,
    selectable: false,
    draggable: false,

    addAttributes() {
        return {
            id: { default: null },
            mediaType: { default: 'image' }
        };
    },

    renderHTML({ HTMLAttributes }) {
        return [
            'div',
            {
                'data-upload-id': HTMLAttributes['id'],
                'data-media-type': HTMLAttributes['mediaType']
            }
        ];
    },

    addNodeView() {
        return ({ node }) => {
            const mediaType = node.attrs['mediaType'] as 'image' | 'video';

            const dom = document.createElement('div');
            dom.className = 'upload-placeholder';
            dom.setAttribute('contenteditable', 'false');
            dom.setAttribute('aria-label', `Uploading ${mediaType}…`);
            dom.setAttribute('role', 'status');

            const icon = document.createElement('span');
            icon.className = 'material-symbols-outlined upload-placeholder__icon';
            icon.setAttribute('aria-hidden', 'true');
            icon.textContent = mediaType === 'video' ? 'videocam' : 'image';

            const label = document.createElement('span');
            label.className = 'upload-placeholder__label';
            label.textContent = `Uploading ${mediaType}…`;

            const barTrack = document.createElement('span');
            barTrack.className = 'upload-placeholder__bar';

            dom.append(icon, label, barTrack);

            return { dom };
        };
    }
});

// ── Helpers ──────────────────────────────────────────────────────────────────

export function insertUploadPlaceholders(
    editor: Editor,
    pos: number,
    placeholders: Array<{ id: string; mediaType: 'image' | 'video' }>
): void {
    const content = placeholders.map(({ id, mediaType }) => ({
        type: 'uploadPlaceholder',
        attrs: { id, mediaType }
    }));
    editor.chain().focus().insertContentAt(pos, content).run();
}

export function replacePlaceholder(editor: Editor, placeholderId: string, content: object): void {
    let targetPos: number | null = null;

    editor.state.doc.descendants((node, pos) => {
        if (node.type.name === 'uploadPlaceholder' && node.attrs['id'] === placeholderId) {
            targetPos = pos;
            return false;
        }
        return true;
    });

    if (targetPos !== null) {
        editor.chain().setNodeSelection(targetPos).insertContent(content).run();
    }
}

export function removePlaceholder(editor: Editor, placeholderId: string): void {
    let targetPos: number | null = null;

    editor.state.doc.descendants((node, pos) => {
        if (node.type.name === 'uploadPlaceholder' && node.attrs['id'] === placeholderId) {
            targetPos = pos;
            return false;
        }
        return true;
    });

    if (targetPos !== null) {
        editor.chain().setNodeSelection(targetPos).deleteSelection().run();
    }
}
