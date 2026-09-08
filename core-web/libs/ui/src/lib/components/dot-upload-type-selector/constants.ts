import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

/**
 * The two ways an upload can be stored. Order is the display order; `recommended` flags the one the
 * product steers users toward.
 *
 * Each option carries two descriptions. `descriptionKey` is the general one; `scopedDescriptionKey`
 * takes the host's restriction as `{0}`, so a picker opened for video does not offer "images,
 * documents, and media". Hosts with no restriction never reach the scoped key.
 */
export const UPLOAD_SELECTOR_OPTIONS = [
    {
        baseType: DotCMSBaseTypesContentTypes.DOTASSET,
        icon: 'image',
        labelKey: 'content-drive.dialog.upload-selector.asset',
        descriptionKey: 'content-drive.dialog.upload-selector.asset.description',
        scopedDescriptionKey: 'content-drive.dialog.upload-selector.asset.description.scoped',
        recommended: true
    },
    {
        baseType: DotCMSBaseTypesContentTypes.FILEASSET,
        icon: 'code_blocks',
        labelKey: 'content-drive.dialog.upload-selector.file',
        descriptionKey: 'content-drive.dialog.upload-selector.file.description',
        scopedDescriptionKey: 'content-drive.dialog.upload-selector.file.description.scoped',
        recommended: false
    }
] as const;
