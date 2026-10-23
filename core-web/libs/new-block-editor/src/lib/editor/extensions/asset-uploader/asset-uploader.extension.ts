import { Extension } from '@tiptap/core';
import { DOMParser, Slice } from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';

import {
    DATA_URL_IMAGE_RULE,
    handleMediaDrop,
    handleMediaPaste,
    swapDataUrlImages,
    uploadsPastedImages
} from './asset-handlers';
import { type MediaUploaders } from './pending-asset';

import {
    type UploadPlaceholderCopy,
    createUploadPlaceholderExtension
} from '../nodes/upload-placeholder.extension';

export { type AssetMediaType, type MediaUploaders, type PendingAsset } from './pending-asset';
export { handleMediaDrop, handleMediaPaste } from './asset-handlers';

/** Options for {@link createAssetUploaderExtension}. */
export interface AssetUploaderOptions {
    /** How each media kind reaches dotCMS. */
    uploaders: MediaUploaders;
    /** Pre-translated copy for the "Uploading…" placeholder row. */
    placeholderCopy: UploadPlaceholderCopy;
}

/**
 * Everything that turns a file into a dotAsset inside the editor, in one place — the successor
 * of the legacy block-editor's `AssetUploader` extension (#38165). This file is wiring only; the
 * work is split by responsibility:
 *
 * - `pending-asset.ts` decides WHAT is uploaded: classifies a file or base64 image, checks the
 *   field allows that kind and the host can upload it, and mints its placeholder id.
 * - `upload-asset.ts` does the upload: sends one pending asset through its uploader and swaps
 *   the placeholder for the final node, or removes the placeholder on failure.
 * - `asset-handlers.ts` holds the ProseMirror entry points: drop, paste and the base64 swap.
 *
 * The extension registers the `uploadPlaceholder` node itself and installs one plugin with
 * `handleDrop`, `handlePaste` and, when images can be uploaded here, a paste-only
 * `clipboardParser` plus `transformPasted` for base64 images. Stored content and `setContent`
 * keep the schema parser, which drops `data:` images as it always did.
 *
 * Placeholders are only ever minted as nodes, never parsed from markup, so one cannot be
 * injected or duplicated through the clipboard.
 */
export function createAssetUploaderExtension({ uploaders, placeholderCopy }: AssetUploaderOptions) {
    return Extension.create({
        name: 'assetUploader',

        addExtensions() {
            return [createUploadPlaceholderExtension(placeholderCopy)];
        },

        addProseMirrorPlugins() {
            const { editor } = this;
            const { schema } = editor;

            return [
                new Plugin({
                    key: new PluginKey('assetUploader'),
                    props: {
                        handleDrop: (view, event, _slice, moved) =>
                            handleMediaDrop(editor, view, event as DragEvent, moved, uploaders),
                        handlePaste: (view, event) =>
                            handleMediaPaste(editor, view, event, uploaders),
                        ...(uploadsPastedImages(editor, uploaders)
                            ? {
                                  clipboardParser: new DOMParser(schema, [
                                      DATA_URL_IMAGE_RULE,
                                      ...DOMParser.fromSchema(schema).rules
                                  ]),
                                  transformPasted: (slice: Slice) =>
                                      new Slice(
                                          swapDataUrlImages(editor, slice.content, uploaders),
                                          slice.openStart,
                                          slice.openEnd
                                      )
                              }
                            : {})
                    }
                })
            ];
        }
    });
}
