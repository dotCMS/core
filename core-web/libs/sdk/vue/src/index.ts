// Public entry for @dotcms/vue.

// Client plugin
export { createDotCMSVue, useDotCMSClient, DOTCMS_CLIENT } from './lib/client/dotcms-client.plugin';
export type { DotCMSClient, DotCMSVuePlugin } from './lib/client/dotcms-client.plugin';

// Composables (UVE integration)
export { useEditableDotCMSPage } from './lib/composables/useEditableDotCMSPage';
export { useDotCMSShowWhen } from './lib/composables/useDotCMSShowWhen';

// Utilities
export { toPlain } from './lib/utils/toPlain';

// Components
export { default as DotCMSShow } from './lib/components/DotCMSShow/DotCMSShow.vue';
export { default as DotCMSLayoutBody } from './lib/components/DotCMSLayoutBody/DotCMSLayoutBody.vue';
export { default as DotCMSEditableText } from './lib/components/DotCMSEditableText/DotCMSEditableText.vue';
export { default as DotCMSBlockEditorRenderer } from './lib/components/DotCMSBlockEditorRenderer/DotCMSBlockEditorRenderer.vue';

// Types
export type { DotCMSLayoutBodyProps } from './lib/components/DotCMSLayoutBody/types';
export type {
    BlockEditorRendererProps,
    CustomRenderer,
    CustomRendererComponent,
    CustomRendererProps
} from './lib/components/DotCMSBlockEditorRenderer/types';
export type {
    DotCMSEditableTextProps,
    DOT_EDITABLE_TEXT_MODE,
    DOT_EDITABLE_TEXT_FORMAT
} from './lib/components/DotCMSEditableText/utils';

// Page context (advanced usage / custom renderers)
export {
    provideDotCMSPageContext,
    useDotCMSPageContext,
    DOTCMS_PAGE_CONTEXT
} from './lib/contexts/dotcms-page.context';
export type { DotCMSPageContextValue } from './lib/contexts/dotcms-page.context';
