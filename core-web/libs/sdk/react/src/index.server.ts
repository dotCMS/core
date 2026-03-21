// Server-safe entry point — excludes client-only modules that import
// browser APIs or class components (e.g. TinyMCE) which break SSR.
export { DotCMSLayoutBody } from './lib/next/components/DotCMSLayoutBody/DotCMSLayoutBody';

export { DotCMSShow } from './lib/next/components/DotCMSShow/DotCMSShow';

export { useDotCMSShowWhen } from './lib/next/hooks/useDotCMSShowWhen';

export { useEditableDotCMSPage } from './lib/next/hooks/useEditableDotCMSPage';

export {
    DotCMSBlockEditorRenderer,
    CustomRenderer
} from './lib/next/components/DotCMSBlockEditorRenderer/DotCMSBlockEditorRenderer';

export type {
    BlockEditorRendererProps,
    CustomRendererProps
} from './lib/next/components/DotCMSBlockEditorRenderer/DotCMSBlockEditorRenderer';

export type { DotCMSLayoutBodyProps } from './lib/next/components/DotCMSLayoutBody/DotCMSLayoutBody';

export { useAISearch } from './lib/next/hooks/useAISearch';

export { useStyleEditorSchemas } from './lib/next/hooks/useStyleEditorSchemas';

//Export AI types from shared types
export type { DotCMSAISearchValue, DotCMSAISearchProps } from './lib/next/shared/types';

export { buildSlots } from './lib/next/utils/buildSlots';
