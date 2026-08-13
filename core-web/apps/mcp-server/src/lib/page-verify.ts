import { HttpError, type DotCMSRuntime } from '@dotcms/ai/runtime';

import { normalizePagePath } from './page-path';
import { resolveSite } from './resolve';

/** Render modes the verify tool supports. LIVE = published; WORKING = latest saved (pre-publish). */
export type VerifyMode = 'LIVE' | 'WORKING';

/** The default render mode when the caller does not name one. */
export const DEFAULT_MODE: VerifyMode = 'LIVE';
/** The default language id when the caller does not name one. */
export const DEFAULT_LANGUAGE_ID = 1;

/**
 * Per-slot verdict:
 * - ok               — the slot produced rendered HTML.
 * - empty-vtl-error  — content IS placed in the slot, but it rendered empty → the container/widget
 *                      VTL failed (a #dotParse error swallowed to nothing). Fix lives in Layer 1:
 *                      re-run that VTL through POST /api/vtl/dynamic to get the parse/eval error.
 * - empty-no-content — the slot resolved but has no contentlets placed. A placement gap, not a code
 *                      bug — use page_place_content to add content.
 * - cache-stale      — the slot rendered HTML, but the full page.rendered came back empty → a
 *                      page-level cache problem. Set cachettl "0" and re-publish.
 * - not-assembled    — the slot rendered HTML in isolation, but distinctive evidence from that
 *                      HTML is absent from the assembled page → the template/theme omitted it.
 */
export type SlotVerdict =
    | 'ok'
    | 'empty-vtl-error'
    | 'empty-no-content'
    | 'cache-stale'
    | 'not-assembled';

export interface VerifySlotResult {
    /** Container key as the layout addresses it (path for file containers, id for db containers). */
    container: string;
    /** Slot instance uuid. */
    uuid: string;
    /** Whether this slot produced non-empty rendered HTML. */
    rendered: boolean;
    /** Byte length of this slot's rendered HTML (0 when empty). */
    bytes: number;
    /** How many contentlets are placed in the slot (from the render response's contentlets map). */
    contentCount: number;
    /** Classification of the slot's render outcome. */
    verdict: SlotVerdict;
}

/** The $URLMapContent resolution outcome — only present for URL-mapped (detail) pages. */
export interface UrlMapResult {
    /** True when the path resolved to a concrete detail contentlet (vs. the no-match branch). */
    resolved: boolean;
    /** The resolved contentlet identifier, when resolved. */
    contentletId?: string;
}

export interface VerifyPageOptions {
    dotcms: DotCMSRuntime;
    /** Page URL path (e.g. "/about-us" or a URL-mapped detail slug like "/blog/my-post"). */
    path: string;
    /**
     * Site to render against — hostname or identifier (UUID). Optional: when omitted the instance's
     * current/default site is used (no host_id sent). Only NON-default sites need this.
     */
    site?: string;
    /** Language id. Default 1. */
    languageId?: number;
    /** Render mode. Default "LIVE" (published). "WORKING" checks the latest saved, pre-publish. */
    mode?: VerifyMode;
    /** Include a bounded prefix of the assembled page HTML in the manifest. Default false. */
    includeHtml?: boolean;
}

export interface VerifyHtmlResult {
    /** Bounded prefix of the assembled page HTML. */
    content: string;
    /** Total JavaScript character count before truncation. */
    totalChars: number;
    /** Total UTF-8 byte count before truncation. */
    totalBytes: number;
    /** True when content is only a prefix of the assembled HTML. */
    truncated: boolean;
    /** Maximum number of characters returned in content. */
    limit: number;
}

export interface VerifyPageManifest {
    /** The path that was verified. */
    path: string;
    /** The page's resolved url. */
    url: string;
    /** The site the page rendered against (hostname when resolved, else "(default)"). */
    site: string;
    /** The render mode used. */
    mode: VerifyMode;
    /** Language id used. */
    languageId: number;
    /** HTTP status of the render call (200 even when the body is empty — see pageRendered). */
    httpStatus: number;
    /** True when page.rendered came back non-empty. A 200 with pageRendered=false is a swallowed error. */
    pageRendered: boolean;
    /** Byte length of the full page.rendered HTML. */
    pageBytes: number;
    /** Per-slot verdicts, in layout order. */
    slots: VerifySlotResult[];
    /** URL-map resolution for detail pages; null for a regular page. */
    urlMap: UrlMapResult | null;
    /** Actionable notices derived from the verdicts. */
    warnings: string[];
    /** One-line summary plus the next action to take. */
    diagnosis: string;
    /** Bounded assembled HTML, present only when includeHtml=true. */
    html?: VerifyHtmlResult;
}

export const MAX_INCLUDED_HTML_CHARS = 20_000;

/**
 * Verify that a dotCMS page actually renders — the layer that catches a blank slot, a swallowed
 * #dotParse error, a cache-stale page, or an unpublished edit, none of which a VTL-only check
 * (/api/vtl/dynamic) can see because that runs in a request context, not a render context.
 *
 * Wraps GET /api/v1/page/render/{uri} and absorbs its sharp edges:
 *   - Host: the caller passes a hostname (or nothing); the tool resolves it to a host_id. host_id is
 *     required only for a NON-default site — omit `site` and the default site is used.
 *   - 200 != rendered: #dotParse swallows a VTL error into an empty HTTP 200, so a 200 with an empty
 *     body is a failure, surfaced as pageRendered=false, not success.
 *   - Two rendered layers that can disagree: each slot's rendered HTML (containers[].rendered[uuid])
 *     vs. the assembled page.rendered. The disagreement IS the diagnosis (cache-stale) as opposed to
 *     an empty slot (VTL error or no content).
 *
 * Returns a structured verdict — per-slot classification plus a one-line diagnosis with the next
 * action — instead of two JSON blobs the caller has to compare by hand.
 */
export async function verifyPage(options: VerifyPageOptions): Promise<VerifyPageManifest> {
    const mode: VerifyMode = options.mode ?? DEFAULT_MODE;
    const languageId = options.languageId ?? DEFAULT_LANGUAGE_ID;

    // Resolve the site to a host_id ONLY when one was given. Absent → the backend uses the default
    // site, and we send no host_id (the render endpoint's documented default behavior).
    const resolvedSite = options.site ? await resolveSite(options.dotcms, options.site) : undefined;

    // Normalized BEFORE it reaches the request URL: `/a/../b` used to render `/b` while the
    // manifest reported `/a/../b`, and a `#` silently truncated the path.
    const uri = normalizePagePath(options.path);

    const query: Record<string, string | number> = {
        language_id: languageId,
        mode
    };
    if (resolvedSite) {
        query.host_id = resolvedSite.identifier;
    }

    const { status, body } = await renderPage(options.dotcms, uri, query);

    return buildManifest({
        path: options.path,
        uri,
        siteLabel: resolvedSite?.hostname ?? '(default)',
        mode,
        languageId,
        status,
        body,
        includeHtml: options.includeHtml ?? false
    });
}

interface RenderResponse {
    entity?: {
        page?: { rendered?: string; pageURI?: string; pageUrl?: string; url?: string };
        containers?: Record<string, RenderedContainer>;
        layout?: { body?: { rows?: LayoutRow[] } };
        urlContentMap?: { identifier?: string; inode?: string } | null;
    };
}

interface RenderedContainer {
    /** Per-slot rendered HTML, keyed by uuid (ContainerRendered.getRendered()). */
    rendered?: Record<string, string>;
    /** Per-slot placed contentlets, keyed by uuid (ContainerRaw.getContentletsMap()). */
    contentlets?: Record<string, Array<{ identifier?: string }>>;
}

interface LayoutRow {
    columns?: Array<{ containers?: Array<{ identifier?: string; uuid?: string }> }>;
}

/** Assemble the verdict manifest from a render response. Pure — no I/O, so it is unit-testable. */
export function buildManifest(input: {
    path: string;
    uri: string;
    siteLabel: string;
    mode: VerifyMode;
    languageId: number;
    status: number;
    body: RenderResponse;
    includeHtml?: boolean;
}): VerifyPageManifest {
    const entity = input.body.entity ?? {};
    const page = entity.page ?? {};
    const containers = entity.containers ?? {};
    const rows = entity.layout?.body?.rows ?? [];

    const pageHtml = page.rendered ?? '';
    const pageBytes = byteLength(pageHtml);
    const pageRendered = !isBlank(pageHtml);
    const url = page.pageURI ?? page.pageUrl ?? page.url ?? input.uri;

    const slots: VerifySlotResult[] = [];
    for (const row of rows) {
        for (const column of row.columns ?? []) {
            for (const layoutContainer of column.containers ?? []) {
                const container = layoutContainer.identifier;
                const uuid = layoutContainer.uuid;
                if (!container || !uuid) {
                    continue;
                }
                slots.push(classifySlot(containers, container, uuid, pageHtml));
            }
        }
    }

    const urlMap = resolveUrlMap(entity.urlContentMap);
    const warnings = collectWarnings(slots, pageRendered, input.status, urlMap, input.mode);
    const diagnosis = diagnose(slots, pageRendered, input.status, urlMap, input.mode);

    const manifest: VerifyPageManifest = {
        path: input.path,
        url,
        site: input.siteLabel,
        mode: input.mode,
        languageId: input.languageId,
        httpStatus: input.status,
        pageRendered,
        pageBytes,
        slots,
        urlMap,
        warnings,
        diagnosis
    };

    if (input.includeHtml) {
        manifest.html = {
            content: pageHtml.slice(0, MAX_INCLUDED_HTML_CHARS),
            totalChars: pageHtml.length,
            totalBytes: pageBytes,
            truncated: pageHtml.length > MAX_INCLUDED_HTML_CHARS,
            limit: MAX_INCLUDED_HTML_CHARS
        };
    }

    return manifest;
}

/** Classify one slot from the render response. */
function classifySlot(
    containers: Record<string, RenderedContainer>,
    container: string,
    uuid: string,
    pageHtml: string
): VerifySlotResult {
    const raw = findContainer(containers, container);
    const html = lookupByUuid(raw?.rendered, uuid) ?? '';
    const contentCount = (lookupByUuid(raw?.contentlets, uuid) ?? []).length;
    const rendered = !isBlank(html);
    const pageRendered = !isBlank(pageHtml);
    const bytes = byteLength(html);

    let verdict: SlotVerdict;
    if (rendered) {
        // The slot produced HTML. If the assembled page did NOT, that is a page-level cache problem.
        if (!pageRendered) {
            verdict = 'cache-stale';
        } else if (!slotEvidenceAppearsInPage(html, pageHtml)) {
            verdict = 'not-assembled';
        } else {
            verdict = 'ok';
        }
    } else if (contentCount > 0) {
        // Content is placed but rendered to nothing → the container/widget VTL failed.
        verdict = 'empty-vtl-error';
    } else {
        // Nothing placed → a placement gap, not a code bug.
        verdict = 'empty-no-content';
    }

    return { container, uuid, rendered, bytes, contentCount, verdict };
}

function resolveUrlMap(
    urlContentMap: { identifier?: string; inode?: string } | null | undefined
): UrlMapResult | null {
    // The field is present (as null/absent) on every page; a non-null value with an identifier means
    // the path resolved to a concrete detail contentlet via $URLMapContent.
    if (urlContentMap === undefined) {
        return null;
    }
    if (urlContentMap === null) {
        // Present-but-null happens on URL-map-capable responses that did not resolve. We can't tell
        // that apart from a plain page here, so treat absence of a match as "no url map".
        return null;
    }
    const contentletId = urlContentMap.identifier;
    return contentletId ? { resolved: true, contentletId } : { resolved: false };
}

function collectWarnings(
    slots: VerifySlotResult[],
    pageRendered: boolean,
    status: number,
    urlMap: UrlMapResult | null,
    mode: VerifyMode
): string[] {
    const warnings: string[] = [];

    if (status === 200 && !pageRendered) {
        warnings.push(
            'HTTP 200 but page.rendered is empty — a #dotParse VTL error was likely swallowed into ' +
                'an empty body. 200 does NOT mean the page rendered.'
        );
    }

    for (const slot of slots) {
        if (slot.verdict === 'empty-vtl-error') {
            warnings.push(
                `Slot ${slot.container} [uuid ${slot.uuid}] has ${slot.contentCount} contentlet(s) ` +
                    'placed but rendered empty — the container/widget VTL failed. Re-run that VTL ' +
                    'through POST /api/vtl/dynamic to get the parse/eval error with line/column.'
            );
        } else if (slot.verdict === 'empty-no-content') {
            warnings.push(
                `Slot ${slot.container} [uuid ${slot.uuid}] is empty — no content placed. Use ` +
                    'page_place_content to add contentlets.'
            );
        } else if (slot.verdict === 'cache-stale') {
            warnings.push(
                `Slot ${slot.container} [uuid ${slot.uuid}] rendered content but the page did not — ` +
                    'page-level cache is stale. Set cachettl "0" and re-publish the page.'
            );
        } else if (slot.verdict === 'not-assembled') {
            warnings.push(
                `Slot ${slot.container} [uuid ${slot.uuid}] rendered content, but its HTML is ` +
                    'absent from the assembled page — the template/theme layout loop is not ' +
                    "emitting this container. Inspect the theme's row/column loop and use " +
                    '$render.eval($column.draw()).'
            );
        }
    }

    if (urlMap && !urlMap.resolved) {
        warnings.push(
            'This looks like a URL-mapped page but the path did not resolve to a detail contentlet ' +
                '($URLMapContent hit the no-match branch). Check the detail-page slug.'
        );
    }

    if (mode === 'WORKING' && pageRendered) {
        warnings.push(
            'Rendered in WORKING mode — this reflects unpublished edits. Re-verify with mode "LIVE" ' +
                'to confirm what the public actually sees.'
        );
    }

    return warnings;
}

/** Derive the one-line diagnosis + next action from the worst thing found. */
function diagnose(
    slots: VerifySlotResult[],
    pageRendered: boolean,
    status: number,
    urlMap: UrlMapResult | null,
    mode: VerifyMode
): string {
    if (status !== 200) {
        return `Render returned HTTP ${status}. The page did not render — check the path, site, and that the page exists in ${mode} mode.`;
    }

    if (urlMap && !urlMap.resolved) {
        return 'URL-mapped page did not resolve to a detail contentlet — the slug hit the no-match branch. Verify the detail-page slug exists and is published.';
    }

    const vtlError = slots.find((s) => s.verdict === 'empty-vtl-error');
    if (vtlError) {
        return `Slot ${vtlError.container} [uuid ${vtlError.uuid}] has content but rendered empty (VTL error). Next: re-run that container's VTL through POST /api/vtl/dynamic to get the parse/eval error.`;
    }

    const stale = slots.find((s) => s.verdict === 'cache-stale');
    if (stale || (!pageRendered && slots.some((s) => s.rendered))) {
        return 'Slots rendered but the assembled page.rendered is empty — page-level cache is stale. Next: set cachettl "0" and re-publish the page.';
    }

    const notAssembled = slots.find((s) => s.verdict === 'not-assembled');
    if (notAssembled) {
        return `Slot ${notAssembled.container} [uuid ${notAssembled.uuid}] rendered in isolation but is absent from the assembled page. Next: inspect the theme's row/column loop and ensure it emits $render.eval($column.draw()).`;
    }

    if (!pageRendered) {
        return `HTTP 200 but the page rendered empty (a swallowed #dotParse error, or nothing is placed). Next: check per-slot verdicts${mode === 'LIVE' ? ' and confirm the page is published' : ''}.`;
    }

    const noContent = slots.filter((s) => s.verdict === 'empty-no-content');
    if (noContent.length > 0) {
        return `Page rendered, but ${noContent.length} slot(s) are empty (no content placed). Next: use page_place_content to fill them if intended.`;
    }

    // Zero slots is NOT a clean bill of health. `collectWarnings` iterates `slots`, so an
    // empty list yields no warnings, and the sentence below would interpolate to "all 0
    // slot(s) produced content" — the tool built to catch blank slots declaring success over
    // a page whose layout it could not read. Legacy/advanced templates, and any response with
    // `entity.containers` populated but no `entity.layout.body.rows`, land exactly here.
    if (slots.length === 0) {
        return (
            `The page rendered in ${mode} mode, but NO slots could be parsed from its layout, ` +
            `so per-slot verification did not run and this is not a clean result. The template ` +
            `may be legacy/advanced (no layout.body.rows), or the response shape may be ` +
            `unexpected. Next: inspect the page's template directly — do not read this as "the ` +
            `page is fine".`
        );
    }

    return `Page rendered successfully in ${mode} mode — all ${slots.length} slot(s) produced content.`;
}

/** GET the render endpoint, capturing the HTTP status even on a non-2xx so verdicts can use it. */
async function renderPage(
    dotcms: DotCMSRuntime,
    uri: string,
    query: Record<string, string | number>
): Promise<{ status: number; body: RenderResponse }> {
    try {
        const body = (await dotcms.request({
            path: `/api/v1/page/render${uri}`,
            query
        })) as RenderResponse;
        return { status: 200, body };
    } catch (error) {
        // A 404/403/etc. is a legitimate verify outcome, not a tool failure — surface it in the
        // manifest with an empty body so the diagnosis explains the HTTP result.
        //
        // ONLY a real HttpError qualifies. The previous version fell back to scraping the
        // first three-digit run out of the message, which turned any error containing a
        // number into a fabricated verdict: `connect ETIMEDOUT 10.0.0.5:443` became status
        // 443, and `buildManifest` then stated flatly that the page did not render and to
        // check the path, site and existence — about a page that is very likely fine. The
        // model's next move is to "fix" something that was never broken. A transport failure
        // must surface AS a transport failure.
        if (error instanceof HttpError) {
            return { status: error.status, body: {} };
        }
        throw error;
    }
}

/**
 * The layout `identifier` may not be byte-identical to the containers-map key (shorty vs full id,
 * host-relative vs host-qualified path). Match tolerantly, same as page_place_content.
 */
function findContainer(
    containers: Record<string, RenderedContainer>,
    identifier: string
): RenderedContainer | undefined {
    if (containers[identifier]) {
        return containers[identifier];
    }
    const key = Object.keys(containers).find((k) => containerMatches(k, identifier));
    return key ? containers[key] : undefined;
}

function containerMatches(a: string, b: string): boolean {
    if (a === b) return true;
    const la = a.toLowerCase();
    const lb = b.toLowerCase();
    return la === lb || la.includes(lb) || lb.includes(la);
}

/** The per-slot maps are historically keyed as "1" or "uuid-1"; look up tolerantly. */
function lookupByUuid<T>(map: Record<string, T> | undefined, uuid: string): T | undefined {
    if (!map) {
        return undefined;
    }
    const direct = map[uuid] ?? map[`uuid-${uuid}`] ?? map[stripUuidPrefix(uuid)];
    if (direct !== undefined) {
        return direct;
    }
    const key = Object.keys(map).find((k) => stripUuidPrefix(k) === stripUuidPrefix(uuid));
    return key ? map[key] : undefined;
}

function stripUuidPrefix(uuid: string): string {
    return uuid.startsWith('uuid-') ? uuid.slice('uuid-'.length) : uuid;
}

/**
 * Confirm assembly using several short, normalized pieces of evidence rather than an exact HTML
 * substring. Theme assembly may rewrite whitespace and entity encoding, so exact matching would
 * label healthy pages as broken. If no reliable evidence can be extracted, return true (unknown)
 * instead of issuing a false failure.
 */
function slotEvidenceAppearsInPage(slotHtml: string, pageHtml: string): boolean {
    const candidates = assemblyEvidence(slotHtml);
    if (candidates.length === 0) {
        return true;
    }
    const normalizedPage = normalizeEvidence(pageHtml);
    return candidates.some((candidate) => normalizedPage.includes(candidate));
}

function assemblyEvidence(html: string): string[] {
    const candidates: string[] = [];
    const attributePattern = /\b(?:id|data-[\w:-]+)\s*=\s*["']([^"']+)["']/gi;
    for (const match of html.matchAll(attributePattern)) {
        addEvidence(candidates, match[1], 6);
    }

    const classPattern = /\bclass\s*=\s*["']([^"']+)["']/gi;
    for (const match of html.matchAll(classPattern)) {
        for (const className of match[1].split(/\s+/)) {
            addEvidence(candidates, className, 8);
        }
    }

    const visibleText = decodeEntities(
        html
            .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, ' ')
            .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, ' ')
            .replace(/<[^>]+>/g, ' ')
    );
    for (const textRun of visibleText.split(/\s{2,}|[.!?]\s+/)) {
        addEvidence(candidates, textRun, 12);
    }

    return [...new Set(candidates)].slice(0, 12);
}

function addEvidence(target: string[], raw: string, minimumLength: number): void {
    const normalized = normalizeEvidence(raw);
    if (normalized.length >= minimumLength) {
        target.push(normalized);
    }
}

function normalizeEvidence(value: string): string {
    return decodeEntities(value).replace(/\s+/g, ' ').trim().toLowerCase();
}

function decodeEntities(value: string): string {
    const named: Record<string, string> = {
        amp: '&',
        apos: "'",
        gt: '>',
        lt: '<',
        nbsp: ' ',
        quot: '"'
    };
    return value.replace(/&(#x[0-9a-f]+|#\d+|[a-z]+);/gi, (entity, token: string) => {
        if (token[0] !== '#') {
            return named[token.toLowerCase()] ?? entity;
        }
        const hex = token[1]?.toLowerCase() === 'x';
        const codePoint = Number.parseInt(token.slice(hex ? 2 : 1), hex ? 16 : 10);
        return Number.isFinite(codePoint) ? String.fromCodePoint(codePoint) : entity;
    });
}

/** Empty or whitespace-only (comments/blank lines count as "not rendered" for verdict purposes). */
function isBlank(html: string): boolean {
    return html.trim().length === 0;
}

function byteLength(html: string): number {
    // Byte length, not char length — multibyte content should report its real size.
    return typeof TextEncoder !== 'undefined'
        ? new TextEncoder().encode(html).length
        : Buffer.byteLength(html, 'utf8');
}
