import { Editor } from '@tiptap/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import {
    type DotAudioData,
    DOT_AUDIO_NODE_NAME,
    audioMetaAttrsFromContentlet
} from './extensions/nodes/audio.extension';
import { type DotImageData, DOT_IMAGE_NODE_NAME } from './extensions/nodes/image.extension';
import {
    type DotVideoData,
    DOT_VIDEO_NODE_NAME,
    videoMetaAttrsFromContentlet
} from './extensions/nodes/video.extension';

/**
 * Maps a dotCMS contentlet onto a `dotImage` node and inserts it at the editor's current
 * selection. Shared by the AI Image flow ({@link EditorModalService.openAiImage})
 * and the dotCMS browser-selector flow inside `<dot-image-insert-dialog>`. Both paths produce a
 * `DotCMSContentlet` that needs to land in the editor with the same `data` shape so the
 * image toolbar (alignment, wrap, link, properties) keeps working uniformly.
 */
export function insertDotImageFromContentlet(editor: Editor, contentlet: DotCMSContentlet): void {
    const data: DotImageData = {
        identifier: contentlet.identifier,
        inode: contentlet.inode,
        languageId: (contentlet as { languageId?: number }).languageId ?? 1,
        title: contentlet.title ?? '',
        asset: `/dA/${contentlet.inode}`
    };

    editor
        .chain()
        .focus()
        .insertContent({
            type: DOT_IMAGE_NODE_NAME,
            attrs: {
                src: data.asset,
                title: data.title || null,
                alt: data.title || null,
                data
            }
        })
        .run();
}

/**
 * Maps a dotCMS contentlet onto a `dotVideo` node and inserts it at the editor's current
 * selection. Used by the dotCMS browser-selector flow inside `<dot-video-dialog>` —
 * mirrors {@link insertDotImageFromContentlet} so both media nodes share the same
 * `data` shape (identifier, inode, languageId, title, asset).
 */
export function insertDotVideoFromContentlet(editor: Editor, contentlet: DotCMSContentlet): void {
    const data: DotVideoData = {
        identifier: contentlet.identifier,
        inode: contentlet.inode,
        languageId: (contentlet as { languageId?: number }).languageId ?? 1,
        title: contentlet.title ?? '',
        asset: `/dA/${contentlet.inode}`
    };

    editor
        .chain()
        .focus()
        .insertContent({
            type: DOT_VIDEO_NODE_NAME,
            attrs: {
                src: data.asset,
                title: data.title || null,
                data,
                ...videoMetaAttrsFromContentlet(contentlet)
            }
        })
        .run();
}

/**
 * Maps a dotCMS contentlet onto a `dotAudio` node and inserts it at the editor's current
 * selection. Used by the dotCMS browser-selector flow inside the audio picker — mirrors
 * {@link insertDotVideoFromContentlet} so both media nodes share the same `data` shape
 * (identifier, inode, languageId, title, asset). Audio carries only `mimeType` metadata.
 */
export function insertDotAudioFromContentlet(editor: Editor, contentlet: DotCMSContentlet): void {
    const data: DotAudioData = {
        identifier: contentlet.identifier,
        inode: contentlet.inode,
        languageId: (contentlet as { languageId?: number }).languageId ?? 1,
        title: contentlet.title ?? '',
        asset: `/dA/${contentlet.inode}`
    };

    editor
        .chain()
        .focus()
        .insertContent({
            type: DOT_AUDIO_NODE_NAME,
            attrs: {
                src: data.asset,
                title: data.title || null,
                data,
                ...audioMetaAttrsFromContentlet(contentlet)
            }
        })
        .run();
}
