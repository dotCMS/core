import { type InferSchema, type ToolExtraArguments, type ToolMetadata } from 'xmcp';
import { z } from 'zod';

import { verifyPage, type VerifyPageOptions } from '../lib/page-verify';
import { runtimeFromEnv, toolFailure } from '../lib/runtime';

export const schema = {
    path: z
        .string()
        .min(1)
        .describe(
            'Page URL path, e.g. "/about-us". For a URL-mapped detail page, pass a concrete slug ' +
                '(e.g. "/blog/my-post") — the tool reports whether $URLMapContent resolved.'
        ),
    site: z
        .string()
        .optional()
        .describe(
            'Site to render against — hostname or identifier (UUID). Omit for the default site. ' +
                'Only a NON-default site needs this; the tool resolves it to the host_id the ' +
                'endpoint requires.'
        ),
    languageId: z.number().int().positive().optional().describe('Language id. Default 1.'),
    mode: z
        .enum(['LIVE', 'WORKING'])
        .optional()
        .describe(
            'Render mode. "LIVE" (default) = what the public sees (published). "WORKING" = latest ' +
                'saved, for a pre-publish check. An unpublished edit renders stale in LIVE.'
        )
};

export const metadata: ToolMetadata = {
    name: 'page_verify',
    description: `Verify that a dotCMS page actually RENDERS — catch a blank slot, a swallowed VTL error, a cache-stale page, or an unpublished edit.

This is the render-verification layer, distinct from VTL validation (POST /api/vtl/dynamic). VTL
validation runs in a REQUEST context and is structurally blind to $URLMapContent, $CONTENTLETS,
$dotContentMap, $dotTheme, and per-container vars. Only a real render exercises the full
page-assembly pipeline — so this is the only layer that catches a slot that came out empty, a
#dotParse error that got swallowed, or a page serving stale from cache.

It wraps GET /api/v1/page/render/{uri} and absorbs its sharp edges:
  - Host: pass a hostname in \`site\` (or nothing). The tool resolves it to the host_id the endpoint
    needs. host_id is required only for a NON-default site; omit \`site\` for the default site.
  - 200 != rendered: #dotParse swallows a VTL error into an empty HTTP 200. A 200 with an empty body
    is a FAILURE here (pageRendered=false), not success.
  - Two rendered layers that can disagree: each slot's HTML (containers[].rendered[uuid]) vs. the
    assembled page.rendered. Their disagreement IS the diagnosis.

Per-slot \`verdict\`:
  - ok               — the slot produced rendered HTML.
  - empty-vtl-error  — content is placed but the slot rendered empty → the container/widget VTL
                       failed. The fix lives in the OTHER layer: re-run that container's VTL through
                       POST /api/vtl/dynamic to get the parse/eval error with line/column.
  - empty-no-content — the slot resolved but has no content placed (a placement gap, not a code bug).
                       Use page_place_content to fill it.
  - cache-stale      — the slot rendered HTML but page.rendered is empty → page-level cache. Set
                       cachettl "0" and re-publish.

LIVE vs WORKING: an unpublished edit renders stale in LIVE. Use mode "WORKING" for a pre-publish
check; the result flags that it reflects unpublished edits.

URL-mapped pages: for a detail slug, \`urlMap\` reports whether $URLMapContent resolved to a concrete
contentlet (vs. the no-match/404 branch).

Returns a manifest: { path, url, site, mode, languageId, httpStatus, pageRendered, pageBytes,
slots: [{ container, uuid, rendered, bytes, contentCount, verdict }], urlMap, warnings, diagnosis }.
The \`diagnosis\` is a one-line summary plus the next action to take — read it first.

Out of scope: visual/screenshot checks, accessibility, performance, multi-page crawl (one page per
call). This verifies DEFAULT-variant rendering — the render endpoint does not take a variant.`,
    annotations: {
        title: 'Verify a dotCMS Page Renders',
        readOnlyHint: true,
        destructiveHint: false,
        idempotentHint: true,
        openWorldHint: true
    }
};

export default async function handler(
    args: InferSchema<typeof schema>,
    extra?: ToolExtraArguments
) {
    try {
        const options: VerifyPageOptions = {
            dotcms: runtimeFromEnv(extra?.sessionId),
            path: args.path,
            site: args.site,
            languageId: args.languageId,
            mode: args.mode
        };

        const manifest = await verifyPage(options);

        return JSON.stringify(manifest, null, 2);
    } catch (error) {
        return toolFailure('page_verify', error);
    }
}
