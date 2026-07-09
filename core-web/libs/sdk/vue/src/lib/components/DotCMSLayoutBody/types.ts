import type { DotCMSPageAsset, DotCMSPageRendererMode } from '@dotcms/types';

import type { Component } from 'vue';

/**
 * Prefix for the per-contentlet named slots of {@link DotCMSLayoutBody}. A slot
 * named `contentlet-<identifier>` overrides the mapped component for the
 * contentlet with that identifier.
 *
 * @example `<template #contentlet-abc123>…</template>` targets identifier `abc123`.
 */
export const CONTENTLET_SLOT_PREFIX = 'contentlet-';

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
