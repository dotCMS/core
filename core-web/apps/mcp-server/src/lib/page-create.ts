import { HttpError, type DotCMSRuntime, type RequestOptions } from '@dotcms/ai/runtime';

import { splitUrlPath } from './page-path';
import { resolveLanguageId, resolveSite } from './resolve';
import { errorMessage } from './runtime';

/** The default page content type when the caller does not name one. */
export const DEFAULT_PAGE_CONTENT_TYPE = 'htmlpageasset';

/** Cap on an interpolated response entity inside an error message (see describeEntity). */
const MAX_ENTITY_CHARS = 1_000;

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

    // Validate the remaining caller inputs BEFORE anything is written. `site` and
    // `contentType` are already resolved above against cached context; `template` and
    // `languageId` were the two that were not, which made them the only inputs whose
    // rejection arrived AFTER the folder had been created (see ensureFolder below).
    const languageId = await resolveLanguageId(options.dotcms, options.languageId);
    await assertTemplateExists(options.dotcms, options.template);

    // Trap #1: the parent folder must exist before the page is fired, or dotCMS collapses the
    // page url to /index. createfolders is idempotent — re-creating an existing folder is a no-op.
    const folderId = await ensureFolder(options.dotcms, siteId, folder);

    // Trap #3 (root/leaf page on a NON-default site): the page's HOST_OR_FOLDER field must carry a
    // concrete location id, or the fire cannot resolve the host and 500s with
    // "Host.getIdentifier() ... host is null". A nested page passes the folder id (which carries its
    // host); a root page has no folder, so `folderId` is undefined and, left alone, `hostFolder`
    // would be dropped from the JSON body — leaving only `contentHost`, which is not enough to
    // anchor a root page. Fall the location back to the SITE id: HOST_OR_FOLDER accepts a host id
    // and resolves it to that host's system folder. (This mirrors the working manual recovery:
    // fire with hostFolder = site id.)
    const hostFolder = folderId ?? siteId;

    const title = options.title;
    // Guarded: `ensureFolder` above has ALREADY COMMITTED a folder by this point, so a bare
    // rethrow here leaves a folder nothing mentions. `page_create({urlPath:"/books/index"})`
    // with a bad input would create `/books`, fail, and report only the HTTP error — and a
    // corrected retry then operates on folder state the caller does not know exists.
    const fired = await fireCreate(options.dotcms, folder, folderId, {
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
                hostFolder,
                languageId,
                title,
                url,
                template: options.template,
                cachettl: options.cacheTtl ?? '0',
                sortOrder: options.sortOrder ?? 0,
                friendlyName: options.friendlyName ?? title,
                pageTitle: options.pageTitle ?? title
            }
        }
    });

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
    return (await resolveSite(dotcms, site)).identifier;
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

    // The cache is consulted for the id and for a candidate list, but it does NOT decide
    // whether the type exists: the definition fetch below is a live call that answers the
    // same question authoritatively. Gating on the cache meant that when the one-time
    // context load failed — leaving `contentTypes` empty for the whole session — even the
    // default `htmlpageasset` was rejected as "not found".
    const context = await safeLoadContext(dotcms);
    const summary = context.contentTypes.find((ct) => ct.variable === wanted || ct.id === wanted);

    // Computed lazily — the happy path never needs the list of page types for an error message.
    const availablePageTypes = () =>
        context.contentTypes
            .filter((ct) => ct.baseType === PAGE_BASE_TYPE)
            .map((ct) => ct.variable)
            .join(', ') || '(unknown — the session content-type list did not load)';

    let definition: ContentTypeDefinition | undefined;
    try {
        definition = await fetchContentTypeDefinition(dotcms, summary?.id || wanted);
    } catch (error) {
        if (!(error instanceof HttpError) || error.status !== 404) {
            throw error;
        }
        // 404 → not found, handled below alongside the empty-entity case.
    }

    if (!definition) {
        throw new Error(
            `Content type "${wanted}" was not found on this instance. ` +
                `Available page content types: ${availablePageTypes()}.`
        );
    }

    // Checked against the FETCHED definition, not the cached summary — the fetch is the
    // authority and is present on every path.
    if (definition.baseType !== PAGE_BASE_TYPE) {
        throw new Error(
            `Content type "${definition.variable}" is base type ${definition.baseType}, not a page ` +
                `(HTMLPAGE). page_create only creates pages. Available page content types: ` +
                `${availablePageTypes()}.`
        );
    }

    return definition;
}

/**
 * `loadContext()` with its failure absorbed — the context is an accelerator here, and every
 * caller below has a live path that does not need it. See lib/resolve.ts for the full
 * rationale on why an empty cache must never be read as "this does not exist".
 */
async function safeLoadContext(dotcms: DotCMSRuntime) {
    try {
        return await dotcms.loadContext();
    } catch (error) {
        console.error(
            `[context] load failed during content-type resolution: ${errorMessage(error)}`
        );

        return { contentTypes: [], sites: [], languages: [], currentUser: null };
    }
}

/** Fetch the full content-type definition (including fields) by id or variable. */
async function fetchContentTypeDefinition(
    dotcms: DotCMSRuntime,
    idOrVar: string
): Promise<ContentTypeDefinition | undefined> {
    const response = await dotcms.request({
        path: `/api/v1/contenttype/id/${encodeURIComponent(idOrVar)}`
    });

    const entity = asRecord(responseEntity(response));
    const rawFields = entity?.['fields'];
    const fields: ContentTypeField[] = Array.isArray(rawFields)
        ? rawFields
              .map(asRecord)
              .filter((f): f is Record<string, unknown> => f !== undefined)
              .map((f) => ({
                  variable: optionalString(f, 'variable'),
                  required: f['required'] === true,
                  fixed: f['fixed'] === true,
                  defaultValue: f['defaultValue']
              }))
        : [];

    const variable = optionalString(entity, 'variable');
    const baseType = optionalString(entity, 'baseType');

    // An empty/unrecognised entity means the type was not resolved. Returning a shape that
    // defaults `baseType` to HTMLPAGE would assert the very thing the caller is being
    // checked for, so the base-type guard would pass on a type that does not exist.
    if (!variable || !baseType) {
        return undefined;
    }

    return {
        id: optionalString(entity, 'id') ?? idOrVar,
        variable,
        baseType,
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
    // flatMap (not filter + map) so `variable` stays narrowed to string without a cast.
    const missing = contentType.fields.flatMap((field) => {
        const variable = field.variable;
        if (!variable || !field.required || field.fixed) return [];
        if (SYSTEM_PAGE_FIELD_VARS.has(variable)) return [];
        if (hasDefault(field.defaultValue)) return [];

        return hasValue(extraFields[variable]) ? [] : [variable];
    });

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
 * Ensure the folder path exists on the site and return the id of the deepest (target) folder.
 * createfolders creates the full path and is idempotent on existing folders.
 */
async function ensureFolder(
    dotcms: DotCMSRuntime,
    site: string,
    folder: string
): Promise<string | undefined> {
    if (folder === '/' || folder === '') {
        // Root page: no folder to create. The caller falls hostFolder back to the site id (a root
        // page fired with an undefined hostFolder 500s on a non-default site — see Trap #3).
        return undefined;
    }

    const response = await dotcms.request({
        method: 'POST',
        path: `/api/v1/folder/createfolders/${encodeURIComponent(site)}`,
        body: [folder]
    });

    const folderId = extractFolderId(response, folder);

    // A nested page MUST be anchored on its real folder id. Falling back to the site id here
    // (the root-page path below) would silently anchor the page at the site root and
    // reintroduce the very /index URL-collapse trap this function exists to prevent, while the
    // manifest still reported the intended folder/fullPath. Fail loudly instead.
    if (!folderId) {
        throw new Error(
            `Folder "${folder}" was requested on site "${site}" but createfolders returned no ` +
                `resolvable folder id, so the page cannot be anchored to it. Refusing to fall ` +
                `back to the site root (that would collapse the page url to /index). ` +
                `Response entity: ${describeEntity(response)}`
        );
    }

    return folderId;
}

/**
 * `dotcms.request()` is typed `unknown` — it speaks to a live instance whose payload we cannot
 * prove at compile time. Rather than assert a shape with `as` (which type-checks a lie and then
 * lets `undefined` surface deep in the caller), narrow the ONE thing every dotCMS REST envelope
 * guarantees: an object with an optional `entity`. Callers keep their own per-endpoint guards for
 * what lives inside it, so a shape change becomes a handled `undefined` rather than a crash.
 */
function responseEntity(response: unknown): unknown {
    if (typeof response !== 'object' || response === null) {
        return undefined;
    }

    return (response as { entity?: unknown }).entity;
}

/** Narrow a value to an indexable record, or undefined when it isn't one. */
function asRecord(value: unknown): Record<string, unknown> | undefined {
    return typeof value === 'object' && value !== null && !Array.isArray(value)
        ? (value as Record<string, unknown>)
        : undefined;
}

/** Read an optional string property, ignoring non-string values. */
function optionalString(
    record: Record<string, unknown> | undefined,
    key: string
): string | undefined {
    const value = record?.[key];

    return typeof value === 'string' ? value : undefined;
}

/**
 * createfolders returns the created/found folders. We want the id of the folder matching our
 * target path. Shapes vary across versions (array vs object, identifier vs inode), so probe
 * defensively and fall back to the deepest entry.
 */
function extractFolderId(response: unknown, folder: string): string | undefined {
    const entity = responseEntity(response);
    const raw: unknown[] = Array.isArray(entity) ? entity : entity ? [entity] : [];
    const list = raw.map(asRecord).filter((f): f is Record<string, unknown> => f !== undefined);
    if (list.length === 0) {
        return undefined;
    }

    const normalized = folder.replace(/\/+$/, '');
    const match = list.find((f) => optionalString(f, 'path')?.replace(/\/+$/, '') === normalized);
    const chosen = match ?? list[list.length - 1];

    return optionalString(chosen, 'identifier') ?? optionalString(chosen, 'inode');
}

/**
 * Read the page fields out of a fire/read response.
 *
 * Every field is read through a checking accessor rather than asserted with `as`. The two
 * casts that used to live here claimed `identifier`, `inode` and `live` existed with the
 * right types off a `Record<string, unknown>` that nothing had checked — precisely the
 * "type-check a lie and let `undefined` surface deep in the caller" failure that
 * `responseEntity`/`asRecord`/`optionalString` were introduced to close.
 */
function extractPageEntity(response: unknown): PageEntity | undefined {
    const entity = asRecord(responseEntity(response));
    if (!entity) {
        return undefined;
    }
    // Fire responses sometimes wrap the contentlet under `contentlets[0]`.
    const contentlets = entity['contentlets'];
    if (Array.isArray(contentlets) && contentlets.length) {
        const first = asRecord(contentlets[0]);

        return first && toPageEntity(first);
    }

    return toPageEntity(entity);
}

/** Project a checked record onto {@link PageEntity} — unknown/mistyped fields stay undefined. */
function toPageEntity(record: Record<string, unknown>): PageEntity {
    return {
        identifier: optionalString(record, 'identifier'),
        inode: optionalString(record, 'inode'),
        live: optionalBoolean(record, 'live')
    };
}

/** Read an optional boolean property, ignoring non-boolean values. */
function optionalBoolean(
    record: Record<string, unknown> | undefined,
    key: string
): boolean | undefined {
    const value = record?.[key];

    return typeof value === 'boolean' ? value : undefined;
}

async function isLive(dotcms: DotCMSRuntime, identifier?: string): Promise<boolean> {
    if (!identifier) {
        return false;
    }

    try {
        const response = await dotcms.request({
            path: `/api/v1/content/${encodeURIComponent(identifier)}`,
            query: { depth: 0 }
        });

        return extractPageEntity(response)?.live === true;
    } catch {
        // A failed liveness check is not a failed create — surface it as a non-live result.
        return false;
    }
}

/**
 * Confirm the template exists before anything is written.
 *
 * `template` is the one input a caller most often gets wrong — the schema says "the template
 * UUID, not its name", which is exactly the mistake worth catching — and it was the only one
 * whose rejection arrived from the fire, i.e. after the folder had already been created.
 *
 * A non-404 failure here is deliberately NOT fatal: the check is a courtesy, and refusing to
 * create a page because the template lookup was briefly unavailable would be worse than
 * letting the fire decide.
 */
async function assertTemplateExists(dotcms: DotCMSRuntime, template: string): Promise<void> {
    try {
        await dotcms.request({
            path: `/api/v1/templates/${encodeURIComponent(template)}/working`
        });
    } catch (error) {
        if (error instanceof HttpError && error.status === 404) {
            throw new Error(
                `Template "${template}" was not found. This must be the template's IDENTIFIER ` +
                    `(a UUID), not its name — passing a human-readable title here is the most ` +
                    `common cause. Nothing has been created; fix the template and re-run.`
            );
        }
        // Anything else (403, 5xx, timeout): fall through and let the fire be the judge.
    }
}

/**
 * Fire the create, and on failure say what has ALREADY been committed.
 *
 * The folder write happens before this point and cannot be rolled back, so the recoverable
 * outcome depends on the caller knowing three things: which inputs were used, that folder
 * `<x>` now exists, and that re-running is safe because `createfolders` is idempotent. That
 * last sentence is what turns an orphaned folder into a retryable operation.
 */
async function fireCreate(
    dotcms: DotCMSRuntime,
    folder: string,
    folderId: string | undefined,
    request: RequestOptions
): Promise<unknown> {
    try {
        return await dotcms.request(request);
    } catch (error) {
        const created = folderId
            ? `Folder "${folder}" (${folderId}) WAS created before this failure and still exists. `
            : '';
        throw new Error(
            `${errorMessage(error)}\n\n${created}Re-running this call after fixing the input is ` +
                `SAFE: folder creation is idempotent, so no duplicate folder is made.`
        );
    }
}

/**
 * Render a response's entity for an error message.
 *
 * `JSON.stringify(undefined)` returns `undefined` (the value, not a string), so interpolating
 * it printed the literal text "Response entity: undefined" — and an ABSENT entity is exactly
 * the condition that triggers the branch using this, so that was the common case rather than
 * the edge one. When an entity IS present it is capped: the raw blob can be arbitrarily large
 * and this is going straight into an error the model has to read.
 */
function describeEntity(response: unknown): string {
    const entity = responseEntity(response);
    if (entity === undefined || entity === null) {
        return '(no entity in the response — the endpoint returned a body this tool could not read)';
    }

    const json = JSON.stringify(entity);

    return json.length <= MAX_ENTITY_CHARS
        ? json
        : `${json.slice(0, MAX_ENTITY_CHARS)}… [truncated]`;
}
