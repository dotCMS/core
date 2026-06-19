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

    /** SCAN / RE-SCAN — POST { url } to the page-scanner proxy. */
    scan(url: string): Promise<ScanResult> {
        return this.request<ScanResult>({
            method: 'POST',
            path: '/api/v1/page-scanner/a11y/check',
            body: { url }
        });
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

    /**
     * READ — GET /api/v2/assets?path=. Returns the raw asset text.
     * `mode: 'EDIT_MODE'` reads the working version (S0: reflects unpublished edits).
     */
    async read(path: string, mode?: 'EDIT_MODE'): Promise<string> {
        const query: Record<string, string> = { path };
        if (mode) {
            query.mode = mode;
        }
        const res = await this.request<unknown>({ method: 'GET', path: '/api/v2/assets', query });
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
