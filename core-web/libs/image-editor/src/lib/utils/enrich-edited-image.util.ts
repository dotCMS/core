import { DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';

import { ImageEditorAssetContext, NormalizedPoint } from '../models/image-editor.models';

/**
 * Enriches the temp file returned by the Save servlet so downstream consumers
 * keep recognizing it as an image.
 *
 * The editor only ever edits images, but the Save endpoint can return a temp
 * file without metadata (`metadata: null`, `image: false`, `mimeType: "unknown"`).
 * Left untouched, the thumbnail, the "edit image" gate and the file-info dialog
 * all stop treating it as an image (blank thumb / hidden pencil / crash on
 * `metadata.title`). Synthesizing minimal metadata here — the one place that
 * knows the result is an edited image — keeps every consumer and host consistent,
 * and always folds in the current focal point.
 *
 * @param tempFile - The temp file returned by the Save servlet.
 * @param ctx - The asset context the editor opened with (source of name/mime/size).
 * @param focalPoint - The focal point at save time, seeded into the metadata.
 * @returns A temp file flagged as an image with complete, focal-seeded metadata.
 */
export function enrichEditedImage(
    tempFile: DotCMSTempFile,
    ctx: ImageEditorAssetContext,
    focalPoint: NormalizedPoint
): DotCMSTempFile {
    const name = tempFile.fileName || ctx.fileName || '';
    const metadata: DotFileMetadata = {
        contentType: ctx.mimeType || tempFile.mimeType || 'image/*',
        fileSize: tempFile.length ?? 0,
        length: tempFile.length ?? 0,
        modDate: 0,
        name,
        sha256: '',
        title: name,
        version: 0,
        ...(ctx.naturalWidth ? { width: ctx.naturalWidth } : {}),
        ...(ctx.naturalHeight ? { height: ctx.naturalHeight } : {}),
        // Preserve real server metadata when present; then force the invariants:
        // the result is an image, seeded with the current focal point.
        ...tempFile.metadata,
        isImage: true,
        focalPoint: `${focalPoint.x},${focalPoint.y}`
    };

    return { ...tempFile, image: true, metadata };
}
