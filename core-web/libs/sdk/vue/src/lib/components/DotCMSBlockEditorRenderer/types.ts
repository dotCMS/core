import type { BlockEditorNode } from '@dotcms/types';

import type { Component, CSSProperties } from 'vue';

/**
 * Props that every custom block renderer receives.
 *
 * A custom renderer is a Vue component that gets the full {@link BlockEditorNode}
 * as its `node` prop and the already-rendered children in its default slot.
 *
 * @template TData the type of data stored in `node.attrs.data` (contentlet blocks)
 */
export interface CustomRendererProps<TData = unknown> {
    /** The full BlockEditorNode with attrs, marks, content, etc. */
    node: BlockEditorNode & {
        attrs?: {
            data?: TData;
            [key: string]: unknown;
        };
    };
}

/**
 * A custom renderer component — a Vue component whose props include the `node`
 * (typed via {@link CustomRendererProps}) plus a default slot with the rendered
 * children.
 */
export type CustomRendererComponent = Component<CustomRendererProps>;

/**
 * Map of block type name to the custom renderer component that should render it.
 */
export type CustomRenderer = Record<string, CustomRendererComponent>;

/**
 * Props for {@link DotCMSBlockEditorRenderer}.
 */
export interface BlockEditorRendererProps {
    /** The block editor field value (a root node with `content`). */
    blocks: BlockEditorNode;
    /** Inline styles for the container element. */
    style?: CSSProperties;
    /** CSS class for the container element. */
    className?: string;
    /** Custom renderers keyed by block type (or content type for contentlet blocks). */
    customRenderers?: CustomRenderer;
    /** When true, renders helpful dev-only messages for invalid/unknown blocks. */
    isDevMode?: boolean;
}
