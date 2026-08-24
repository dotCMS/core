import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';

import { readLastAssetLocation } from './last-asset-path';
import { DotAssetPickerConfig } from './store/models';

/**
 * What the host opened the picker for.
 *
 * `file` is an Edit Content File field — any asset goes. The rest are media modes, each narrowed to
 * its own mimetype: `image` is the Image field *and* the Story Block's image node, `video` and
 * `audio` are the Story Block's media nodes.
 */
export type DotAssetPickerMode = 'file' | 'image' | 'video' | 'audio';

/** Every mode that carries a mimetype restriction — i.e. everything but `file`. */
export type DotAssetPickerMediaMode = Exclude<DotAssetPickerMode, 'file'>;

/**
 * The only two base types that carry an asset.
 *
 * Every entry point is restricted to these — none of them can hold a Widget or a piece of Content.
 * What differs is the *pre-selection*: the media modes start with both selected, `file` starts with
 * none.
 */
export const ASSET_PICKER_ASSET_BASE_TYPES: DotCMSBaseTypesContentTypes[] = [
    DotCMSBaseTypesContentTypes.DOTASSET,
    DotCMSBaseTypesContentTypes.FILEASSET
];

/**
 * Mimetype narrowing per media mode, applied silently — an Image field that could return a PDF is
 * broken, and so is a `dotVideo` node pointing at an mp3.
 *
 * `file` is absent on purpose, which is what makes it the one mode with no restriction.
 */
export const ASSET_PICKER_MIME_TYPES: Record<DotAssetPickerMediaMode, string[]> = {
    image: ['image/*'],
    video: ['video/*'],
    audio: ['audio/*']
};

/**
 * Dialog title key per entry point. The picker renders its own header, so the title travels in the
 * config instead of `DynamicDialogConfig.header`.
 */
export const ASSET_PICKER_TITLE_KEYS: Record<DotAssetPickerMode, string> = {
    file: 'dot.asset.picker.header.file',
    image: 'dot.asset.picker.header.image',
    video: 'dot.asset.picker.header.video',
    audio: 'dot.asset.picker.header.audio'
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
 * Builds the picker configuration for a host entry point.
 *
 * Kept out of the store on purpose: `DotAssetPickerStore` is a generic browse store and should not
 * know what an "Image field" or a "Story Block video node" is. This is the one place that translates
 * an entry point into filters.
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
    // Presence in the mimetype map is what makes a mode a media mode — no `mode === 'x'` chain to
    // extend the next time a media node shows up.
    const mimeTypes = ASSET_PICKER_MIME_TYPES[mode as DotAssetPickerMediaMode];

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
        // What the selector may offer — the same in every mode.
        allowedBaseTypes: [...ASSET_PICKER_ASSET_BASE_TYPES],
        ...(languageId ? { languageId } : {}),
        // What starts selected, plus the silent mimetype narrowing — media modes only.
        ...(mimeTypes
            ? {
                  baseTypes: [...ASSET_PICKER_ASSET_BASE_TYPES],
                  mimeTypes: [...mimeTypes]
              }
            : {})
    };
}
