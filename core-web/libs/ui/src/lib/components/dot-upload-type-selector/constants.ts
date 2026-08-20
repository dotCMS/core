import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

/**
 * The two ways an upload can be stored. Order is the display order; `recommended` flags the one the
 * product steers users toward.
 */
export const UPLOAD_SELECTOR_OPTIONS = [
    {
        baseType: DotCMSBaseTypesContentTypes.DOTASSET,
        icon: 'image',
        labelKey: 'content-drive.dialog.upload-selector.asset',
        descriptionKey: 'content-drive.dialog.upload-selector.asset.description',
        recommended: true
    },
    {
        baseType: DotCMSBaseTypesContentTypes.FILEASSET,
        icon: 'code_blocks',
        labelKey: 'content-drive.dialog.upload-selector.file',
        descriptionKey: 'content-drive.dialog.upload-selector.file.description',
        recommended: false
    }
] as const;
