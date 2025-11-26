import { FeaturedFlags } from '@dotcms/dotcms-models';

export type UVEFlags = { [key in FeaturedFlags]?: boolean };

export interface WithFlagsState {
    flags: UVEFlags;
}
