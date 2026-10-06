import type { DotCMSRuntime } from '@dotcms/ai/runtime';
import type { DotCMSColumnContainer } from '@dotcms/types';

/**
 * Response shapes and probes shared by more than one page tool.
 *
 * Small on purpose: these are the pieces that were previously written out twice and had
 * already started to drift, not a general dumping ground for page types.
 */

/**
 * One row of a page layout, as `/api/v1/page/json` and `/api/v1/page/render` return it.
 *
 * Hoisted here because `page-verify` and `page-place-content` declared byte-identical
 * copies, which meant two tools parsing the SAME response through two shapes nobody kept in
 * step.
 *
 * The innermost element is DERIVED from `DotCMSColumnContainer` — the SDK's own page-asset
 * contract — so a change to the container shape upstream fails here at compile time instead
 * of drifting silently. `import type` erases at build, so this costs nothing at runtime.
 *
 * The surrounding optionality stays hand-written, and that is deliberate: the canonical
 * `DotPageAssetLayoutRow` declares `columns` (and every field below it) as REQUIRED, which
 * would be a lie about a response this code has not validated. Everything here is optional
 * because the payload is unproven at compile time. `historyUUIDs` is picked off for the same
 * reason — these tools never read it, and requiring it would assert a field that may be
 * absent. `@dotcms/dotcms-models` also ships a `DotLayoutRow`, but it is the editor-side
 * model (fully required, and its barrel pulls Angular into the transitive graph), so it is
 * the wrong contract for an MCP server parsing a REST response.
 */
export interface LayoutRow {
    columns?: Array<{ containers?: Array<Pick<DotCMSColumnContainer, 'identifier' | 'uuid'>> }>;
}

/** The `/api/v1/content/{id}` envelope, which nests the contentlet one of two ways. */
interface ContentLiveResponse {
    entity?: { live?: boolean; contentlets?: Array<{ live?: boolean }> };
}

/**
 * Whether a contentlet is published.
 *
 * THROWS on a failed read, and that is the contract callers depend on rather than an
 * oversight. The two previous copies disagreed here: one swallowed every error into `false`,
 * which conflates "this is not live" with "we could not find out" — and for the transfer
 * manifest those are opposite conclusions, since a read failure reported as not-live sends
 * the caller off to re-publish assets that were already fine. `assets-transfer` needs the
 * distinction, so the shared primitive is the honest one and the caller that wants a
 * best-effort answer catches for itself.
 */
export async function isContentLive(dotcms: DotCMSRuntime, identifier: string): Promise<boolean> {
    const response = (await dotcms.request({
        path: `/api/v1/content/${encodeURIComponent(identifier)}`,
        query: { depth: 0 }
    })) as ContentLiveResponse;

    const entity = response.entity;
    // A fire response wraps the contentlet under `contentlets[0]`; a plain read does not.
    const contentlet = entity?.contentlets?.[0] || entity;

    return contentlet?.live === true;
}
