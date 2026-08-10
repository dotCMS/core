import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';

import { readLastAssetLocation } from './last-asset-path';
import { DotAssetPickerConfig } from './store/models';

/** Which Edit Content field opened the picker. */
export type DotAssetPickerMode = 'file' | 'image';

/**
 * The only two base types that carry an asset.
 *
 * Both entry points are restricted to these — neither a File nor an Image field can hold a Widget
 * or a piece of Content. What differs is the *pre-selection*: Image starts with both selected,
 * File starts with none.
 */
export const ASSET_PICKER_ASSET_BASE_TYPES: DotCMSBaseTypesContentTypes[] = [
    DotCMSBaseTypesContentTypes.DOTASSET,
    DotCMSBaseTypesContentTypes.FILEASSET
];

/** Applied silently — an Image field that could return a PDF is broken. */
export const ASSET_PICKER_IMAGE_MIME_TYPES = ['image/*'];

/**
 * Dialog title key per entry point. The picker renders its own header, so the title travels in the
 * config instead of `DynamicDialogConfig.header`.
 */
export const ASSET_PICKER_TITLE_KEYS: Record<DotAssetPickerMode, string> = {
    file: 'dot.asset.picker.header.file',
    image: 'dot.asset.picker.header.image'
};

export interface DotAssetPickerEntryOptions {
    mode: DotAssetPickerMode;

    /** Site to browse. */
    site: DotSite;

    /**
     * Dialog title, already translated. Callers resolve {@link ASSET_PICKER_TITLE_KEYS} — this
     * module has no `DotMessageService` and stays a pure config builder.
     */
    title?: string;

    /** Language of the contentlet being edited, pre-selected as the locale filter. */
    languageId?: string;

    /**
     * Explicit starting folder. When omitted the picker falls back to the globally remembered
     * last-used location, so it reopens where the editor last picked something.
     */
    initialAssetPath?: string;
}

/**
 * Builds the picker configuration for an Edit Content entry point.
 *
 * Kept out of the store on purpose: `DotAssetPickerStore` is a generic browse store and should not
 * know what an "Image field" is. This is the one place that translates a field type into filters.
 *
 * Not pure — it reads the remembered location from storage when no explicit path is given, which is
 * what makes "reopen where I left off" work without every caller remembering to do it.
 */
export function buildAssetPickerConfig({
    mode,
    site,
    title,
    languageId,
    initialAssetPath
}: DotAssetPickerEntryOptions): DotAssetPickerConfig {
    const isImage = mode === 'image';

    // An explicit path is always about the entry site; a remembered one carries its own.
    const remembered = initialAssetPath ? undefined : readLastAssetLocation();

    return {
        site,
        // Only when the remembered site is a real, identified one — a legacy bare-path payload has
        // no site, so its path is applied to the entry site instead.
        ...(remembered?.siteId
            ? { browseSite: { identifier: remembered.siteId, hostname: remembered.hostname } }
            : {}),
        ...(title ? { title } : {}),
        path: initialAssetPath ?? remembered?.path,
        // What the selector may offer — the same in both modes.
        allowedBaseTypes: [...ASSET_PICKER_ASSET_BASE_TYPES],
        ...(languageId ? { languageId } : {}),
        // What starts selected, plus the silent mimetype narrowing — Image only.
        ...(isImage
            ? {
                  baseTypes: [...ASSET_PICKER_ASSET_BASE_TYPES],
                  mimeTypes: [...ASSET_PICKER_IMAGE_MIME_TYPES]
              }
            : {})
    };
}
