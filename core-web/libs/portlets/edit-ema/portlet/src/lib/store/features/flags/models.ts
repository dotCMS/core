import { FeaturedFlags } from '@dotcms/dotcms-models';

type UVEFlagKeys =
    | FeaturedFlags.FEATURE_FLAG_UVE_TOGGLE_LOCK
    | FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR
    | FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR_FOR_TRADITIONAL_PAGES
    | FeaturedFlags.FEATURE_FLAG_PAGE_SCANNER;

export type UVEFlags = { [K in UVEFlagKeys]?: boolean };

export interface WithFlagsState {
    flags: UVEFlags;
}
