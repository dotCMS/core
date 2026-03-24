// Server-safe entry point — excludes client-only modules that import
// browser APIs or class components (e.g. TinyMCE) which break SSR.
// Hooks (useEditableDotCMSPage, useDotCMSShowWhen, useAISearch, useStyleEditorSchemas)
// are intentionally excluded: they use useState/useEffect and have no 'use client'
// directive, so including them here would pull React client APIs into the RSC module
// graph and cause a build error in Next.js App Router.
export { DotCMSLayoutBody } from './lib/next/components/DotCMSLayoutBody/DotCMSLayoutBody';

export type { DotCMSLayoutBodyProps } from './lib/next/components/DotCMSLayoutBody/DotCMSLayoutBody';

export { buildSlots } from './lib/next/utils/buildSlots';

export {
    DotCMSBlockEditorRenderer,
    CustomRenderer
} from './lib/next/components/DotCMSBlockEditorRenderer/DotCMSBlockEditorRenderer';

export type {
    BlockEditorRendererProps,
    CustomRendererProps
} from './lib/next/components/DotCMSBlockEditorRenderer/DotCMSBlockEditorRenderer';

//Export AI types from shared types
export type { DotCMSAISearchValue, DotCMSAISearchProps } from './lib/next/shared/types';
