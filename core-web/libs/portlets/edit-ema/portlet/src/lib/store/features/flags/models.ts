import { FeaturedFlags } from '@dotcms/dotcms-models';

type UVEFlagKeys =
    | FeaturedFlags.FEATURE_FLAG_UVE_TOGGLE_LOCK
    | FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR
    | FeaturedFlags.FEATURE_FLAG_PAGE_SCANNER
    | FeaturedFlags.FEATURE_FLAG_UVE_LEGACY_SCRIPT_INJECTION;

export type UVEFlags = { [K in UVEFlagKeys]?: boolean };

export interface WithFlagsState {
    flags: UVEFlags;
}
