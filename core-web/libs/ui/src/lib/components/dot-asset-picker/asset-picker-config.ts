import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';

import { readLastAssetLocation } from './last-asset-path';
import { DotAssetPickerBrowseOptions, DotAssetPickerConfig } from './store/models';

/**
 * What the host opened the picker for.
 *
 * `file` is an Edit Content File field — any asset goes. `image`, `video` and `audio` are media
 * modes, each narrowed to its own mimetype: `image` is the Image field *and* the Story Block's
 * image node, `video` and `audio` are the Story Block's media nodes.
 *
 * `browse` is `DotCustomFieldApi.openBrowserModal` — the only mode that can ask for folders, menu
 * links or pages, and the only one that carries {@link DotAssetPickerBrowseOptions}. Like `file`
 * it applies no mimetype narrowing of its own.
 */
export type DotAssetPickerMode = 'file' | 'image' | 'video' | 'audio' | 'browse';

/**
 * Every mode that carries a mimetype restriction.
 *
 * `file` and `browse` are both excluded: they are the two modes that impose no narrowing, so
 * neither has an entry in {@link ASSET_PICKER_MIME_TYPES}.
 */
export type DotAssetPickerMediaMode = Exclude<DotAssetPickerMode, 'file' | 'browse'>;

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
 * `file` and `browse` are absent on purpose: they are the two modes with no restriction.
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
    browse: 'dot.asset.picker.header.browse',
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

    /**
     * Base types the selector may offer, overriding the asset-only default.
     *
     * `browse` mode only — it is the one entry point that can ask for pages. Ignored elsewhere, so
     * a change to a File-field call site cannot widen what that field offers.
     */
    allowedBaseTypes?: string[];

    /**
     * Folders, menu links, version state and sort direction.
     *
     * `browse` mode only, for the same reason as {@link allowedBaseTypes}.
     */
    browse?: DotAssetPickerBrowseOptions;
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
    initialAssetPath,
    allowedBaseTypes,
    browse
}: DotAssetPickerEntryOptions): DotAssetPickerConfig {
    // Presence in the mimetype map is what makes a mode a media mode — no `mode === 'x'` chain to
    // extend the next time a media node shows up.
    const mimeTypes = ASSET_PICKER_MIME_TYPES[mode as DotAssetPickerMediaMode];

    // An explicit path is always about the entry site; a remembered one carries its own.
    const remembered = initialAssetPath ? undefined : readLastAssetLocation();

    // Gated on the mode rather than on "was it passed?", so the browse capabilities are structurally
    // unreachable from the File / Image / video / audio entry points. Those four must keep offering
    // assets only, and a guard that depends on every call site remembering not to pass a field is
    // not a guard.
    const isBrowse = mode === 'browse';

    return {
        site,
        // Only when the remembered site is a real, identified one — a legacy bare-path payload has
        // no site, so its path is applied to the entry site instead.
        ...(remembered?.siteId
            ? { browseSite: { identifier: remembered.siteId, hostname: remembered.hostname } }
            : {}),
        ...(title ? { title } : {}),
        path: initialAssetPath ?? remembered?.path,
        // What the selector may offer. Asset-only everywhere except `browse`, which is the only
        // entry point that can ask for pages.
        allowedBaseTypes:
            isBrowse && allowedBaseTypes?.length
                ? [...allowedBaseTypes]
                : [...ASSET_PICKER_ASSET_BASE_TYPES],
        ...(isBrowse && browse ? { browse } : {}),
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
