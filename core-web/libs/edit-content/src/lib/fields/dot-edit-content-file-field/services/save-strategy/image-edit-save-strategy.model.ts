import { DotCMSTempFile } from '@dotcms/dotcms-models';

/**
 * Strategy for persisting the result of an image-editor session back to a file
 * field.
 *
 * {@link DotFileFieldComponent} renders Binary, Image and File fields with a
 * single component, but each stores its binary differently, so the "apply edited
 * image" step differs per input type:
 *
 * - **Binary** keeps the binary inline on the contentlet, so the edited temp file
 *   becomes the field value directly (resolved to a binary on the contentlet
 *   check-in).
 * - **Image/File** reference a separate `dotAsset` contentlet by identifier (the
 *   binary lives in that asset's `asset` field), so the edit must be checked in as
 *   a new version of the referenced asset without changing the field value.
 *
 * Implementations are selected by {@link ImageEditSaveStrategyResolver}.
 */
export interface ImageEditSaveStrategy {
    /**
     * Applies the temp file returned by the image editor to the field.
     *
     * @param tempFile - The edited image staged by the editor as a temp file.
     */
    apply(tempFile: DotCMSTempFile): void;
}
