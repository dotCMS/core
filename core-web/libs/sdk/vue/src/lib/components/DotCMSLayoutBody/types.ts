import type { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

import type { Component } from 'vue';


/**
 * Props for {@link DotCMSLayoutBody}.
 */
export interface DotCMSLayoutBodyProps {
    /** The dotCMS page asset containing the layout information. */
    page: DotCMSPageAsset;
    /**
     * Map of content-type variable name to the Vue component that renders it.
     * The special `CustomNoComponent` key is used as the fallback for unmatched
     * content types.
     */
    components: Record<string, Component>;
    /** Renderer mode; defaults to `production`. */
    mode?: DotCMSPageRendererMode;
}
