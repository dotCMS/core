import { Editor, Node } from '@tiptap/core';
import type { Node as PMNode } from '@tiptap/pm/model';

/** Media kind shown in the placeholder UI and stored on the node. */
export type UploadPlaceholderMediaType = 'image' | 'video' | 'audio';

/** Payload used when inserting one or more upload placeholders. */
export type UploadPlaceholderItem = {
    id: string;
    mediaType: UploadPlaceholderMediaType;
};

/** Pre-translated copy needed by the placeholder node view. */
export type UploadPlaceholderCopy = {
    /** Localized label/aria text formatted with the media type, e.g. "Uploading image…". */
    uploading: (mediaType: UploadPlaceholderMediaType) => string;
};

const PLACEHOLDER_NODE_NAME = 'uploadPlaceholder' as const;

/**
 * Locates the document position of an `uploadPlaceholder` node by its `id` attribute.
 *
 * @param doc - ProseMirror document to search.
 * @param placeholderId - `attrs.id` of the placeholder to find.
 * @returns Start position of the node, or `null` if not found.
 */
function findUploadPlaceholderPosition(doc: PMNode, placeholderId: string): number | null {
    let targetPos: number | null = null;

    doc.descendants((node, pos) => {
        if (node.type.name === PLACEHOLDER_NODE_NAME && node.attrs['id'] === placeholderId) {
            targetPos = pos;
            return false;
        }
        return true;
    });

    return targetPos;
}

/**
 * Material Symbol name for the placeholder row (host app must load the font).
 *
 * @param mediaType - Whether we are uploading an image, video, or audio file.
 * @returns Ligature text for `material-symbols-outlined`.
 */
function createUploadPlaceholderIcon(mediaType: UploadPlaceholderMediaType): HTMLElement {
    const icon = document.createElement('span');
    icon.className = 'material-symbols-outlined upload-placeholder__icon';
    icon.setAttribute('aria-hidden', 'true');
    icon.textContent =
        mediaType === 'video' ? 'videocam' : mediaType === 'audio' ? 'audiotrack' : 'image';
    return icon;
}

/**
 * Visible “Uploading …” label next to the icon.
 *
 * @param mediaType - Drives the copy (`image` / `video`).
 * @param copy - Pre-translated copy provider.
 */
function createUploadPlaceholderLabel(
    mediaType: UploadPlaceholderMediaType,
    copy: UploadPlaceholderCopy
): HTMLElement {
    const label = document.createElement('span');
    label.className = 'upload-placeholder__label';
    label.textContent = copy.uploading(mediaType);
    return label;
}

/**
 * Indeterminate progress bar track (animated via global `.upload-placeholder__bar` CSS).
 */
function createUploadPlaceholderProgressBar(): HTMLElement {
    const barTrack = document.createElement('span');
    barTrack.className = 'upload-placeholder__bar';
    return barTrack;
}

/**
 * Root DOM for the node view: non-editable status row with icon, label, and bar.
 *
 * @param mediaType - Image vs video (icon + copy).
 * @param copy - Pre-translated copy provider.
 * @returns The `.upload-placeholder` element.
 */
function createUploadPlaceholderDom(
    mediaType: UploadPlaceholderMediaType,
    copy: UploadPlaceholderCopy
): HTMLElement {
    const dom = document.createElement('div');
    dom.className = 'upload-placeholder';
    dom.setAttribute('contenteditable', 'false');
    dom.setAttribute('aria-label', copy.uploading(mediaType));
    dom.setAttribute('role', 'status');

    dom.append(
        createUploadPlaceholderIcon(mediaType),
        createUploadPlaceholderLabel(mediaType, copy),
        createUploadPlaceholderProgressBar()
    );

    return dom;
}

/**
 * Atomic block node shown while a file uploads; replaced or removed when the upload finishes.
 *
 * - Not selectable/draggable; `contenteditable="false"` in the node view.
 * - Serializes as a `div` with `data-upload-id` and `data-media-type` for HTML export.
 *
 * @param copy - Pre-translated copy provider for the placeholder UI.
 */
export function createUploadPlaceholderExtension(copy: UploadPlaceholderCopy) {
    return Node.create({
        name: PLACEHOLDER_NODE_NAME,
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
                const mediaType = node.attrs['mediaType'] as UploadPlaceholderMediaType;
                return { dom: createUploadPlaceholderDom(mediaType, copy) };
            };
        }
    });
}

/**
 * Inserts one or more upload placeholder nodes at `pos` (e.g. where a drop occurred).
 *
 * @param editor - Active TipTap editor.
 * @param pos - Document position to insert at.
 * @param placeholders - Temp ids and media types for each row.
 */
export function insertUploadPlaceholders(
    editor: Editor,
    pos: number,
    placeholders: UploadPlaceholderItem[]
): void {
    const content = placeholders.map(({ id, mediaType }) => ({
        type: PLACEHOLDER_NODE_NAME,
        attrs: { id, mediaType }
    }));
    editor.chain().focus().insertContentAt(pos, content).run();
}

/**
 * Replaces the placeholder node matching `placeholderId` with final TipTap content (e.g. image/video node).
 *
 * @param editor - Active TipTap editor.
 * @param placeholderId - `attrs.id` of the placeholder to replace.
 * @param content - JSON content or node spec passed to `insertContent`.
 */
export function replacePlaceholder(editor: Editor, placeholderId: string, content: object): void {
    const targetPos = findUploadPlaceholderPosition(editor.state.doc, placeholderId);
    if (targetPos !== null) {
        editor.chain().setNodeSelection(targetPos).insertContent(content).run();
    }
}

/**
 * Deletes the placeholder node matching `placeholderId` (e.g. on upload error).
 *
 * @param editor - Active TipTap editor.
 * @param placeholderId - `attrs.id` of the placeholder to remove.
 */
export function removePlaceholder(editor: Editor, placeholderId: string): void {
    const targetPos = findUploadPlaceholderPosition(editor.state.doc, placeholderId);
    if (targetPos !== null) {
        editor.chain().setNodeSelection(targetPos).deleteSelection().run();
    }
}
