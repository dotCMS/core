import { createRuntime } from '@dotcms/ai/runtime';

type DotCMSRuntime = ReturnType<typeof createRuntime>;

/** The default page content type when the caller does not name one. */
export const DEFAULT_PAGE_CONTENT_TYPE = 'htmlpageasset';

/** Base type string the REST API reports for any page content type. */
const PAGE_BASE_TYPE = 'HTMLPAGE';

/**
 * System field variables every page content type carries — mirrored by hand from
 * dotCMS/.../contenttype/model/type/PageContentType.java (requiredFields() + the field-var
 * constants). These are either set explicitly from typed args below or filled by the platform, so
 * they are NOT treated as "unsatisfied user-required fields" during validation. Re-sync this set
 * if those field variables change backend-side. (The fetched field's `fixed` flag also excludes
 * platform fields at line ~`assertRequiredFieldsSatisfied`; this list is the belt-and-suspenders
 * fallback for instances/versions where `fixed` isn't reliably serialized.)
 */
const SYSTEM_PAGE_FIELD_VARS = new Set([
    'title',
    'url',
    'hostFolder',
    'template',
    'showOnMenu',
    'sortOrder',
    'cachettl',
    'friendlyName',
    'pageTitle',
    'redirecturl',
    'httpsreq',
    'seodescription',
    'seokeywords',
    'pagemetadata'
]);

export interface CreatePageOptions {
    dotcms: DotCMSRuntime;
    /** Site identifier (UUID) or hostname the page lives on. */
    site: string;
    /** Page-relative URL path, e.g. "/books/index" or "/books". The leaf becomes the page url. */
    urlPath: string;
    /** Page title. */
    title: string;
    /** Template identifier the page renders with. */
    template: string;
    /**
     * Content type for the page — variable or id. Defaults to `htmlpageasset`. Any content type
     * works as long as its base type is HTMLPAGE (a custom page type may add its own fields).
     */
    contentType?: string;
    /**
     * Values for content-type fields beyond the common page fields — keyed by field variable.
     * Required for any user-added required field on a custom page type that has no default value.
     */
    extraFields?: Record<string, unknown>;
    /** Optional friendly name; defaults to `title`. */
    friendlyName?: string;
    /** Optional page title (browser <title>); defaults to `title`. */
    pageTitle?: string;
    /** Language id. Default 1. */
    languageId?: number;
    /** Cache TTL seconds (as dotCMS expects: a string). Default "0". */
    cacheTtl?: string;
    /** Sort order. Default 0. */
    sortOrder?: number;
}

export interface CreatePageManifest {
    /** Identifier of the created page contentlet. */
    identifier?: string;
    /** Inode of the created version. */
    inode?: string;
    /** The resolved content type variable the page was created as. */
    contentType: string;
    /** The folder path the page was placed under (e.g. "/books"). */
    folder: string;
    /** The leaf url stored on the page (e.g. "index"). */
    url: string;
    /** Full live URL on the site (e.g. "/books/index"). */
    fullPath: string;
    /** Site the page lives on. */
    site: string;
    /** Whether the page is live after the publish fire. */
    live: boolean;
    /**
     * Set when the page was created but NOT verified live. The page exists; it just may render
     * blank with no content placed (the two-step trap). Not a hard failure.
     */
    warnings: string[];
}

/**
 * Create and publish a dotCMS page in one safe call.
 *
 * A "page" is a contentlet whose content type's base type is HTMLPAGE, fired through the generic
 * workflow endpoint — there is no dedicated create-page endpoint. The content type defaults to
 * `htmlpageasset` but can be any page type; custom page types may add their own (possibly required)
 * fields, so we resolve the type and validate against its actual field set before firing.
 *
 * This wrapper absorbs the URL-collapse trap: dotCMS silently collapses a page `url` whose parent
 * folder does not exist down to `/index`, which then 400s against the home page. We split `urlPath`
 * into folder + leaf, create the folder first, then fire the page with `url: "<leaf>"` and
 * `hostFolder: <created folder>` so the URL lands where the caller meant.
 *
 * This is the THIN tier: it does NOT place content. The page comes up live but blank — content
 * placement and the re-publish that follows are a separate, explicit step for the caller. The
 * manifest flags this so a successful create is never mistaken for a fully-populated page.
 */
export async function createPage(options: CreatePageOptions): Promise<CreatePageManifest> {
    const { folder, url, fullPath } = splitUrlPath(options.urlPath);
    const warnings: string[] = [];
    const extraFields = options.extraFields ?? {};

    // Resolve the site to its identifier UUID up front. `contentHost` on the fire body MUST be a
    // site UUID — a bare hostname makes the fire NPE ("Host.getIdentifier() because host is null"),
    // which is exactly the root-page (`/`) trap where there is no folder to anchor the page on.
    const siteId = await resolveSiteId(options.dotcms, options.site);

    // Resolve the page content type and validate it BEFORE creating anything. A wrong type (not a
    // page) or a missing user-required field would otherwise 400 the fire opaquely — and only after
    // we'd already created the folder. Fail early with a precise message instead.
    const contentType = await resolvePageContentType(options.dotcms, options.contentType);
    assertRequiredFieldsSatisfied(contentType, extraFields);

    // Trap #1: the parent folder must exist before the page is fired, or dotCMS collapses the
    // page url to /index. createfolders is idempotent — re-creating an existing folder is a no-op.
    const folderId = await ensureFolder(options.dotcms, siteId, folder);

    const title = options.title;
    const fired = (await options.dotcms.request({
        method: 'PUT',
        path: '/api/v1/workflow/actions/default/fire/PUBLISH',
        query: { indexPolicy: 'WAIT_FOR' },
        body: {
            contentlet: {
                // User-added fields first, so the typed page fields below always win on the keys
                // they own (a caller can't accidentally override `url`/`template` via extraFields).
                ...extraFields,
                contentType: contentType.variable,
                contentHost: siteId,
                hostFolder: folderId,
                languageId: options.languageId ?? 1,
                title,
                url,
                template: options.template,
                cachettl: options.cacheTtl ?? '0',
                sortOrder: options.sortOrder ?? 0,
                friendlyName: options.friendlyName ?? title,
                pageTitle: options.pageTitle ?? title
            }
        }
    })) as { entity?: PageEntity };

    const entity = extractPageEntity(fired);
    const identifier = entity?.identifier;
    const inode = entity?.inode;

    const live = await isLive(options.dotcms, identifier);
    if (!live) {
        warnings.push(
            `Page created but not confirmed live. It may render blank until content is placed and the page is re-published.`
        );
    }

    return {
        identifier,
        inode,
        contentType: contentType.variable,
        folder,
        url,
        fullPath,
        site: options.site,
        live,
        warnings
    };
}

interface PageEntity {
    identifier?: string;
    inode?: string;
    live?: boolean;
    contentlets?: PageEntity[];
}

interface ContentTypeField {
    variable?: string;
    required?: boolean;
    fixed?: boolean;
    defaultValue?: unknown;
}

interface ContentTypeDefinition {
    id: string;
    variable: string;
    baseType: string;
    fields: ContentTypeField[];
}

/**
 * Resolve a site (given as a hostname OR an identifier UUID) to its identifier UUID.
 *
 * The fire body's `contentHost` must be a site UUID. Passing a bare hostname works for pages under
 * a folder (the folder anchors the host) but NPEs for a root page, where there is no folder — dotCMS
 * then can't resolve the host and throws "Host.getIdentifier() because host is null". Resolving to
 * the UUID here makes every page (root included) fire cleanly. Uses the runtime's cached site
 * context (already loaded), mirroring how resolvePageContentType uses cached content types.
 */
async function resolveSiteId(dotcms: DotCMSRuntime, site: string): Promise<string> {
    const wanted = site.trim();
    const { sites } = await dotcms.loadContext();

    const match = sites.find((s) => s.identifier === wanted || s.hostname === wanted);
    if (match) {
        return match.identifier;
    }

    const available = sites.map((s) => s.hostname).join(', ') || '(none found)';
    throw new Error(
        `Site "${site}" was not found (neither a known hostname nor a site identifier). ` +
            `Available sites: ${available}.`
    );
}

/**
 * Resolve the page content type by variable or id and confirm it is actually a page type.
 *
 * Defaults to `htmlpageasset`. We first match against the runtime's cached content-type summaries
 * (cheap, already loaded) to give a precise "not found / not a page type" error, then fetch the
 * full definition (with fields) so required-field validation can run. Firing a non-page type as a
 * page produces a broken contentlet, so a wrong base type is a hard error, not a warning.
 */
async function resolvePageContentType(
    dotcms: DotCMSRuntime,
    requested?: string
): Promise<ContentTypeDefinition> {
    const wanted = (requested ?? DEFAULT_PAGE_CONTENT_TYPE).trim();

    const context = await dotcms.loadContext();
    const summary = context.contentTypes.find(
        (ct) => ct.variable === wanted || ct.id === wanted
    );

    // Computed lazily — the happy path never needs the list of page types for an error message.
    const availablePageTypes = () =>
        context.contentTypes
            .filter((ct) => ct.baseType === PAGE_BASE_TYPE)
            .map((ct) => ct.variable)
            .join(', ') || '(none found)';

    if (!summary) {
        throw new Error(
            `Content type "${wanted}" was not found. Available page content types: ${availablePageTypes()}.`
        );
    }

    if (summary.baseType !== PAGE_BASE_TYPE) {
        throw new Error(
            `Content type "${summary.variable}" is base type ${summary.baseType}, not a page (HTMLPAGE). ` +
                `page_create only creates pages. Available page content types: ${availablePageTypes()}.`
        );
    }

    return fetchContentTypeDefinition(dotcms, summary.id || summary.variable);
}

/** Fetch the full content-type definition (including fields) by id or variable. */
async function fetchContentTypeDefinition(
    dotcms: DotCMSRuntime,
    idOrVar: string
): Promise<ContentTypeDefinition> {
    const response = (await dotcms.request({
        path: `/api/v1/contenttype/id/${encodeURIComponent(idOrVar)}`
    })) as { entity?: { id?: string; variable?: string; baseType?: string; fields?: unknown } };

    const entity = response.entity ?? {};
    const fields = Array.isArray(entity.fields) ? (entity.fields as ContentTypeField[]) : [];

    return {
        id: entity.id ?? idOrVar,
        variable: entity.variable ?? idOrVar,
        baseType: entity.baseType ?? PAGE_BASE_TYPE,
        fields
    };
}

/**
 * Fail before firing if the type has a user-added required field we have no value for. dotCMS
 * would reject the fire with a 400 anyway — but only after we've created the folder, and with a
 * less actionable message. We skip system page fields (filled from typed args / the platform),
 * fixed fields, and fields that carry a default value.
 */
function assertRequiredFieldsSatisfied(
    contentType: ContentTypeDefinition,
    extraFields: Record<string, unknown>
): void {
    const missing = contentType.fields
        .filter((field) => {
            const variable = field.variable;
            if (!variable || !field.required || field.fixed) return false;
            if (SYSTEM_PAGE_FIELD_VARS.has(variable)) return false;
            if (hasDefault(field.defaultValue)) return false;
            return !hasValue(extraFields[variable]);
        })
        .map((field) => field.variable as string);

    if (missing.length > 0) {
        throw new Error(
            `Content type "${contentType.variable}" has required field(s) with no value: ` +
                `${missing.join(', ')}. Pass them via extraFields, e.g. ` +
                `{ "${missing[0]}": <value> }.`
        );
    }
}

/** A server-side default counts if non-null and (for strings) non-empty. */
function hasDefault(defaultValue: unknown): boolean {
    return typeof defaultValue === 'string' ? defaultValue.length > 0 : defaultValue != null;
}

/** A caller-supplied value counts if non-null and (for strings) non-blank after trimming. */
function hasValue(value: unknown): boolean {
    if (value == null) return false;
    if (typeof value === 'string') return value.trim().length > 0;
    return true;
}

/**
 * Split a page-relative URL into the parent folder and the leaf url stored on the page.
 *
 *   "/books/index" → { folder: "/books", url: "index", fullPath: "/books/index" }
 *   "/books"       → { folder: "/books", url: "index", fullPath: "/books/index" }
 *   "/about-us/"   → { folder: "/about-us", url: "index", fullPath: "/about-us/index" }
 *   "/"            → { folder: "/", url: "index", fullPath: "/index" }
 *
 * The input is first normalized through the URL API (against a throwaway base): this collapses
 * "."/".." segments and drops any query string or fragment. URL.pathname keeps percent-encoding
 * intact (notably so an encoded "%2F" can't smuggle a path separator), so we decode each segment
 * individually AFTER splitting — "/my%20books" → folder "my books" — which is safe because a
 * decoded slash then lives inside one segment name instead of creating a new path boundary.
 * The folder-vs-leaf decision below stays ours: the URL API preserves a trailing slash but does
 * not know that "/about-us/" means a folder index while "/about-us" means a leaf url.
 *
 * dotCMS pages always have a leaf url (commonly "index"). If the caller gives a path with no
 * explicit leaf, we default the leaf to "index" under that folder, which matches how the admin UI
 * and the rest of the platform address a folder's default page.
 */
export function splitUrlPath(urlPath: string): { folder: string; url: string; fullPath: string } {
    const trimmed = urlPath.trim();
    if (!trimmed.startsWith('/')) {
        throw new Error(`urlPath must start with "/": "${urlPath}"`);
    }

    // Normalize via URL: decode %xx, collapse ./.., strip ?query/#fragment. The base is a
    // throwaway — only `pathname` is read back out, so the host never leaks into the result.
    let pathname: string;
    try {
        pathname = new URL(trimmed, 'http://_').pathname;
    } catch {
        throw new Error(`urlPath is not a valid path: "${urlPath}"`);
    }

    const segments = pathname
        .split('/')
        .filter(Boolean)
        .map((segment) => decodeURIComponent(segment));

    // No segments → the site root; the page is the root index.
    if (segments.length === 0) {
        return { folder: '/', url: 'index', fullPath: '/index' };
    }

    // A trailing slash means the whole path IS the folder and the page is its index. Otherwise the
    // last segment is the leaf url and everything before it is the folder. (segments is non-empty
    // here — the length===0 case returned above.)
    if (!pathname.endsWith('/')) {
        const url = segments[segments.length - 1];
        const folderSegments = segments.slice(0, -1);
        const folder = folderSegments.length ? `/${folderSegments.join('/')}` : '/';
        const fullPath = `${folder === '/' ? '' : folder}/${url}`;
        return { folder, url, fullPath };
    }

    const folder = `/${segments.join('/')}`;
    return { folder, url: 'index', fullPath: `${folder}/index` };
}

/**
 * Ensure the folder path exists on the site and return the id of the deepest (target) folder.
 * createfolders creates the full path and is idempotent on existing folders.
 */
async function ensureFolder(
    dotcms: DotCMSRuntime,
    site: string,
    folder: string
): Promise<string | undefined> {
    if (folder === '/' || folder === '') {
        return undefined; // root — pages can hang directly off the site with no hostFolder.
    }

    const response = (await dotcms.request({
        method: 'POST',
        path: `/api/v1/folder/createfolders/${encodeURIComponent(site)}`,
        body: [folder]
    })) as { entity?: FolderEntity[] | FolderEntity };

    return extractFolderId(response, folder);
}

interface FolderEntity {
    identifier?: string;
    inode?: string;
    path?: string;
}

/**
 * createfolders returns the created/found folders. We want the id of the folder matching our
 * target path. Shapes vary across versions (array vs object, identifier vs inode), so probe
 * defensively and fall back to the deepest entry.
 */
function extractFolderId(
    response: { entity?: FolderEntity[] | FolderEntity },
    folder: string
): string | undefined {
    const entity = response.entity;
    const list: FolderEntity[] = Array.isArray(entity) ? entity : entity ? [entity] : [];
    if (list.length === 0) {
        return undefined;
    }

    const normalized = folder.replace(/\/+$/, '');
    const match = list.find((f) => f.path?.replace(/\/+$/, '') === normalized);
    const chosen = match ?? list[list.length - 1];

    return chosen.identifier ?? chosen.inode;
}

function extractPageEntity(fired: { entity?: PageEntity }): PageEntity | undefined {
    const entity = fired.entity;
    if (!entity) {
        return undefined;
    }
    // Fire responses sometimes wrap the contentlet under `contentlets[0]`.
    if (entity.contentlets?.length) {
        return entity.contentlets[0];
    }
    return entity;
}

async function isLive(dotcms: DotCMSRuntime, identifier?: string): Promise<boolean> {
    if (!identifier) {
        return false;
    }

    try {
        const response = (await dotcms.request({
            path: `/api/v1/content/${encodeURIComponent(identifier)}`,
            query: { depth: 0 }
        })) as { entity?: PageEntity };
        return extractPageEntity(response)?.live === true;
    } catch {
        // A failed liveness check is not a failed create — surface it as a non-live result.
        return false;
    }
}
