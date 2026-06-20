import { createApiAdapter, createExecutor } from '@dotcms/agentic-tools';

import { withAllowlist } from './allowlist';

/**
 * Typed wrappers over the four dotCMS calls the a11y loop makes — each runs as
 * an `api.request(...)` inside the agentic-tools sandbox, through the path-
 * allowlist-guarded adapter. Shapes are modelled on the S0 captures
 * (docs/plans/sessions/S0-captured-responses.json).
 *
 * The sandbox blocks direct network access; the only egress is `api.request`,
 * and the allowlist permits only the four operations below. The auth token is
 * injected by the adapter and never visible to sandbox code.
 */

// ── Response shapes (from S0 captures) ──────────────────────────────────────

/** axe check `data` — for color-contrast it carries the exact colors + ratio,
 * which makes the fix deterministic (no LLM needed to discover the value). */
export interface AxeCheckData {
    fgColor?: string;
    bgColor?: string;
    contrastRatio?: number;
    expectedContrastRatio?: string; // e.g. "4.5:1"
    fontSize?: string;
    fontWeight?: string;
    [k: string]: unknown;
}

export interface ScanFinding {
    code: string; // axe rule id, e.g. "color-contrast"
    type: string; // "error" | "warning" | "notice"
    typeCode: number;
    message: string;
    context: string; // the offending element's outerHTML
    selector: string; // CSS selector to the node
    runner: string; // "axe"
    resultType: string; // "violation" | "needsReview" | ...
    runnerExtras?: { impact?: string; help?: string; helpUrl?: string; description?: string };
    data?: AxeCheckData; // the failing check's data (fgColor/bgColor/ratio for contrast)
}

export interface ScanResult {
    ok: boolean;
    documentTitle?: string;
    totalIssues: number;
    counts: { errors: number; warnings: number; notices: number };
    findings: {
        total: number;
        violations: number;
        needsReview: number;
        items: ScanFinding[];
    };
    // Stylesheet URLs the rendered page actually loaded (post-render, from the
    // scanner's Puppeteer DOM). The authoritative "which stylesheet applies" list
    // — filter to same-origin dotCMS assets to find the compiled CSS to attribute
    // against (CSS-attribution path, S1.5). External (CDN/fonts) are included too.
    stylesheets?: string[];
}

// ── Raw axe response (what the scanner returns; agent normalizes it) ─────────
// The scanner is "pure axe" — it returns the axe result verbatim under `axe`
// (+ `stylesheets`). The agent's scan() maps it to ScanResult; the rest of the
// loop never sees raw axe.

interface AxeCheck {
    id?: string;
    data?: AxeCheckData;
}
interface AxeNode {
    target?: string[];
    html?: string;
    impact?: string;
    any?: AxeCheck[];
    all?: AxeCheck[];
    none?: AxeCheck[];
}
interface AxeRule {
    id: string;
    impact?: string;
    description?: string;
    help?: string;
    helpUrl?: string;
    nodes: AxeNode[];
}
export interface RawScanResponse {
    ok?: boolean;
    documentTitle?: string;
    stylesheets?: string[];
    axe?: {
        violations?: AxeRule[];
        incomplete?: AxeRule[];
        passes?: AxeRule[];
        inapplicable?: AxeRule[];
    };
}

export interface SourceRef {
    identifier: string;
    path: string; // host-qualified, e.g. //demo.dotcms.com/application/themes/travel/header.vtl
    extension?: string; // lowercased, no dot, e.g. "vtl", "css", "scss"; present on theme files
}

export interface ContainerSource {
    contentTypes: Array<{ contentTypeVar: string; identifier: string; path: string }>;
    source: string; // "FILE" | "DB"
}

export interface RenderSources {
    containers: Record<string, ContainerSource>;
    page: { identifier: string; languageId: number; uri: string };
    theme: {
        folderPath: string;
        id: string;
        name: string;
        // Every file under the theme folder; filter by `extension`. Open-ended set
        // (vtl, css, scss, sass, dotsass, js, ...) — the API does not whitelist types.
        files: SourceRef[];
    };
    widgets: Array<{ contentTypeVar?: string; identifier?: string; path?: string; source?: string }>;
}

export interface SavedAsset {
    fileSize: number;
    identifier: string;
    inode: string;
    lang: string;
    live: boolean;
    name: string;
    path: string;
    working: boolean;
}

/** The failing check's data on an axe node (any/all/none, first one carrying data). */
function nodeData(node: AxeNode): AxeCheckData | undefined {
    for (const bucket of [node.none, node.any, node.all]) {
        for (const chk of bucket ?? []) {
            if (chk.data && typeof chk.data === 'object') {
                return chk.data;
            }
        }
    }
    return undefined;
}

/** Flatten one axe rule's nodes into ScanFinding[] with the given resultType. */
function findingsFromRule(
    rule: AxeRule,
    resultType: string,
    type: string,
    typeCode: number
): ScanFinding[] {
    return (rule.nodes ?? []).map((node) => ({
        code: rule.id,
        type,
        typeCode,
        message: rule.help ?? rule.description ?? rule.id,
        context: node.html ?? '',
        selector: Array.isArray(node.target) ? node.target.join(' ') : String(node.target ?? ''),
        runner: 'axe',
        resultType,
        runnerExtras: {
            impact: node.impact ?? rule.impact,
            help: rule.help,
            helpUrl: rule.helpUrl,
            description: rule.description
        },
        data: nodeData(node)
    }));
}

/**
 * Normalize the scanner's raw axe response into the internal ScanResult the loop
 * uses. The scanner is "pure axe" (returns axe verbatim under `axe` + the applied
 * `stylesheets`); this adapter is the one place that knows the axe node shape.
 * Each axe violation RULE expands to one finding per flagged NODE (so a contrast
 * rule with 14 nodes = 14 findings). `incomplete` becomes needs-review notices.
 */
export function normalizeAxe(raw: RawScanResponse): ScanResult {
    const axe = raw.axe ?? {};
    const violationItems = (axe.violations ?? []).flatMap((r) =>
        findingsFromRule(r, 'violation', 'error', 1)
    );
    const needsReviewItems = (axe.incomplete ?? []).flatMap((r) =>
        findingsFromRule(r, 'needsReview', 'warning', 2)
    );
    const items = [...violationItems, ...needsReviewItems];
    return {
        ok: raw.ok ?? true,
        documentTitle: raw.documentTitle,
        totalIssues: items.length,
        counts: { errors: violationItems.length, warnings: needsReviewItems.length, notices: 0 },
        findings: {
            total: items.length,
            violations: violationItems.length,
            needsReview: needsReviewItems.length,
            items
        },
        stylesheets: raw.stylesheets
    };
}

// ── Client ──────────────────────────────────────────────────────────────────

export interface DotcmsClientConfig {
    dotcmsBaseUrl: string;
    /** The minted short-lived JWT (from the proxy, §8.2). Never logged. */
    authToken: string;
    /** Per-call sandbox timeout (ms). Scans are slow (~3s each). */
    timeoutMs?: number;
}

/**
 * Runs a single `api.request(...)` through the guarded sandbox and returns its
 * value, throwing on sandbox failure so callers see a real error.
 */
export class DotcmsClient {
    private readonly executor: ReturnType<typeof createExecutor>;
    private readonly timeoutMs: number;

    constructor(config: DotcmsClientConfig) {
        const adapter = withAllowlist(
            createApiAdapter({ dotcmsUrl: config.dotcmsBaseUrl, authToken: config.authToken })
        );
        this.executor = createExecutor({ config: { adapters: [adapter] } });
        this.timeoutMs = config.timeoutMs ?? 120000;
    }

    private async request<T>(options: Record<string, unknown>): Promise<T> {
        const code = `return await api.request(${JSON.stringify(options)});`;
        const result = await this.executor.execute<T>(code, {
            sandbox: { timeout: this.timeoutMs }
        });
        if (!result.success) {
            throw new Error(result.error?.message ?? 'sandbox request failed');
        }
        return result.value as T;
    }

    /** SCAN / RE-SCAN — POST { url } to the page-scanner proxy, normalize raw axe. */
    async scan(url: string): Promise<ScanResult> {
        const raw = await this.request<RawScanResponse>({
            method: 'POST',
            path: '/api/v1/page-scanner/a11y/check',
            body: { url }
        });
        return normalizeAxe(raw);
    }

    /** LOCATE — GET /api/v1/page/_render-sources/{uri}?host_id=. */
    async locate(uri: string, hostId: string): Promise<RenderSources> {
        const res = await this.request<{ entity: RenderSources }>({
            method: 'GET',
            path: '/api/v1/page/_render-sources' + uri,
            query: { host_id: hostId }
        });
        return res.entity;
    }

    /** READ — GET /api/v2/assets?path=. Returns the raw asset text. */
    async read(path: string): Promise<string> {
        const res = await this.request<unknown>({
            method: 'GET',
            path: '/api/v2/assets',
            query: { path }
        });
        return typeof res === 'string' ? res : JSON.stringify(res);
    }

    /**
     * FETCH-STYLESHEET — GET the COMPILED stylesheet the page loads, with the
     * inline sourcemap (`sourcemap=true`). Unlike `read` (raw asset via
     * /api/v2/assets), this hits the theme stylesheet URL directly so dotCMS's
     * SASS preprocessor returns compiled CSS + the source map (CSS-attribution
     * path, S1.5). The URL comes verbatim from `scan.stylesheets[]` (the scanner
     * is responsible for returning a resolvable URL — incl. host scope); we only
     * strip the origin to a relative path (the adapter rejects absolute paths) and
     * add the sourcemap param. Returns the compiled CSS text (map appended).
     */
    async fetchStylesheet(absoluteUrl: string): Promise<string> {
        const u = new URL(absoluteUrl);
        u.searchParams.set('sourcemap', 'true');
        const res = await this.request<unknown>({
            method: 'GET',
            path: u.pathname,
            query: Object.fromEntries(u.searchParams)
        });
        return typeof res === 'string' ? res : JSON.stringify(res);
    }

    /**
     * SAVE-WORKING — PUT /api/v2/assets/save (multipart). Working version only;
     * never /publish (the allowlist would reject /publish anyway). The host-
     * qualified `path` includes the filename.
     */
    async saveWorking(path: string, content: string, mime = 'text/plain'): Promise<SavedAsset> {
        const fileName = path.split('/').pop() ?? 'asset';
        const data = Buffer.from(content, 'utf-8').toString('base64');
        const res = await this.request<{ entity: SavedAsset }>({
            method: 'PUT',
            path: '/api/v2/assets/save',
            formData: { path, file: { name: fileName, type: mime, data } }
        });
        return res.entity;
    }
}
