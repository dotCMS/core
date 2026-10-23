import { Extension } from '@tiptap/core';
import {
    DOMParser,
    Fragment,
    type Node as PMNode,
    Slice,
    type TagParseRule
} from '@tiptap/pm/model';
import { Plugin, PluginKey } from '@tiptap/pm/state';

import { DOT_IMAGE_NODE_NAME } from './nodes/image.extension';
import {
    UPLOAD_PLACEHOLDER_NODE_NAME,
    createUploadPlaceholderId
} from './nodes/upload-placeholder.extension';

import { uploadImageIntoPlaceholder } from '../editor.utils';
import { type UploadedImage } from '../services/dot-upload.service';
import { dataUrlToFile, isDataUrl } from '../utils/data-url.utils';

/**
 * Paste-only parse rule: lets `<img src="data:image/…">` become a `dotImage` long enough for
 * `transformPasted` to swap it out. `DotImage`'s own rule excludes `data:` sources on purpose,
 * and that stays true for stored content and `setContent`, which never use this parser.
 */
const DATA_URL_IMAGE_RULE: TagParseRule = {
    tag: 'img[src^="data:image/"]',
    node: DOT_IMAGE_NODE_NAME,
    getAttrs: (element) => ({
        src: element.getAttribute('src'),
        alt: element.getAttribute('alt')
    })
};

/**
 * Uploads base64 images that arrive through the paste pipeline as dotAssets (#38165).
 *
 * Pasted HTML, and Markdown routed through `view.pasteHTML`, can embed images as `data:` URLs.
 * The schema's parser drops those, so the images silently vanished. This extension gives the
 * clipboard its own parser that keeps them as `dotImage` nodes, then `transformPasted` walks the
 * parsed slice and replaces each one with the same `uploadPlaceholder` node drag-and-drop uses,
 * starting the upload. Once the upload resolves the placeholder becomes the uploaded `dotImage`;
 * on failure it is removed.
 *
 * Working on parsed nodes rather than the HTML string means ProseMirror reads `src` and `alt`,
 * there is one placeholder insertion path, and a placeholder can never be minted from markup.
 *
 * Registers nothing when the field disallows images (no `dotImage` in the schema): the normal
 * parser then drops `data:` images as it always did.
 *
 * @param uploadImage Publishes a file as a dotAsset; omitted in hosts that cannot upload.
 */
export function createPastedImageUploadExtension(
    uploadImage?: (file: File) => Promise<UploadedImage>
) {
    return Extension.create({
        name: 'pastedImageUpload',

        addProseMirrorPlugins() {
            const { editor } = this;
            const { schema } = editor;
            const placeholderType = schema.nodes[UPLOAD_PLACEHOLDER_NODE_NAME];

            if (!uploadImage || !schema.nodes[DOT_IMAGE_NODE_NAME] || !placeholderType) {
                return [];
            }

            /** Replaces every `data:` `dotImage` in the fragment with a placeholder, recursively. */
            const swapDataUrlImages = (fragment: Fragment): Fragment => {
                const nodes: PMNode[] = [];
                fragment.forEach((node) => {
                    const src: string = node.attrs['src'] ?? '';
                    if (node.type.name !== DOT_IMAGE_NODE_NAME || !isDataUrl(src)) {
                        nodes.push(node.isLeaf ? node : node.copy(swapDataUrlImages(node.content)));

                        return;
                    }

                    const file = dataUrlToFile(src, 'pasted-image');
                    // Undecodable: dropped, as the schema parser always did.
                    if (!file) return;

                    const id = createUploadPlaceholderId('image');
                    uploadImageIntoPlaceholder(
                        editor,
                        id,
                        file,
                        node.attrs['alt'] ?? file.name,
                        uploadImage
                    );
                    nodes.push(placeholderType.create({ id, mediaType: 'image' }));
                });

                return Fragment.from(nodes);
            };

            return [
                new Plugin({
                    key: new PluginKey('pastedImageUpload'),
                    props: {
                        clipboardParser: new DOMParser(schema, [
                            DATA_URL_IMAGE_RULE,
                            ...DOMParser.fromSchema(schema).rules
                        ]),
                        transformPasted: (slice) =>
                            new Slice(
                                swapDataUrlImages(slice.content),
                                slice.openStart,
                                slice.openEnd
                            )
                    }
                })
            ];
        }
    });
}
