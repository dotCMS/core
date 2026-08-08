import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';

import { readLastAssetPath } from './last-asset-path';
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

export interface DotAssetPickerEntryOptions {
    mode: DotAssetPickerMode;

    /** Site to browse. */
    site: DotSite;

    /** Language of the contentlet being edited, pre-selected as the locale filter. */
    languageId?: string;

    /**
     * Explicit starting folder. When omitted the picker falls back to the globally remembered
     * last-used path, so it reopens where the editor last picked something.
     */
    initialAssetPath?: string;
}

/**
 * Builds the picker configuration for an Edit Content entry point.
 *
 * Kept out of the store on purpose: `DotAssetPickerStore` is a generic browse store and should not
 * know what an "Image field" is. This is the one place that translates a field type into filters.
 *
 * Not pure — it reads the remembered path from storage when no explicit one is given, which is what
 * makes "reopen where I left off" work without every caller remembering to do it.
 */
export function buildAssetPickerConfig({
    mode,
    site,
    languageId,
    initialAssetPath
}: DotAssetPickerEntryOptions): DotAssetPickerConfig {
    const isImage = mode === 'image';

    return {
        site,
        path: initialAssetPath ?? readLastAssetPath(),
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
