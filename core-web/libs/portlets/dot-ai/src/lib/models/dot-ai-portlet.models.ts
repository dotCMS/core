/**
 * View-model-only types for the dotAI portlet. Wire shapes and their conversions live in
 * `@dotcms/data-access`; nothing here crosses the network.
 */

/**
 * The five tabs, in display order. The `id` is also the route segment, so this array is the
 * single source for both the tab bar and `lib.routes.ts` — they cannot drift apart.
 */
export const DOT_AI_TABS = [
    { id: 'search', labelKey: 'dotai.tab.search', icon: 'search' },
    { id: 'chat', labelKey: 'dotai.tab.chat', icon: 'forum' },
    { id: 'image', labelKey: 'dotai.tab.image', icon: 'imagesmode' },
    { id: 'embeddings', labelKey: 'dotai.tab.embeddings', icon: 'database' },
    { id: 'config', labelKey: 'dotai.tab.config', icon: 'tune' }
] as const;

export type DotAiTab = (typeof DOT_AI_TABS)[number];
export type DotAiTabId = DotAiTab['id'];
