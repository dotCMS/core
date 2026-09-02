/**
 * `@dotcms/ai/adapter` — the dotCMS-specific building blocks: the dotCMS door out of the
 * sandbox (`dotcmsAdapter` / `createApiAdapter`), the shared request core, instance-context
 * loading, and the context cache. dotCMS-wired by design (the generic engine is `/sandbox`).
 */

export { createApiAdapter, dotcmsAdapter, isBinaryResponseEnvelope } from './http-client';
export type {
    ApiAdapterConfig,
    DotCMSAdapter,
    BinaryResponseEnvelope,
    RequestOptions,
    RequestPolicy,
    RequestCallEvent
} from './http-client';

export { requestCore } from './request-core';
export type { RequestCoreContext } from './request-core';

export { loadDotCMSContext } from './context';
export type {
    DotCMSContext,
    ContentTypeSummary,
    SiteSummary,
    LanguageSummary,
    CurrentUserSummary
} from './context';

export { ContextCache, getSharedContextCache } from './context-cache';
