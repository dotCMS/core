import type { Adapter } from './types';

export interface ContentTypeSummary {
    id: string;
    name: string;
    variable: string;
    baseType: string;
    host?: string;
    folder?: string;
}

export interface SiteSummary {
    identifier: string;
    hostname: string;
    isDefault: boolean;
    archived: boolean;
}

export interface LanguageSummary {
    id: number;
    languageCode: string;
    countryCode: string;
    language: string;
    country: string;
    isoCode: string;
}

export interface CurrentUserSummary {
    userId: string;
    email: string;
    givenName?: string;
    surname?: string;
    admin: boolean;
    roles?: string[];
}

export interface DotCMSContext {
    contentTypes: ContentTypeSummary[];
    sites: SiteSummary[];
    languages: LanguageSummary[];
    currentUser: CurrentUserSummary | null;
}

type RequestFn = (...args: unknown[]) => unknown | Promise<unknown>;

function getRequestFn(adapter: Adapter): RequestFn {
    const method = adapter.methods.get('request');
    if (!method) {
        throw new Error('api adapter is missing the "request" method');
    }
    return method.execute.bind(method);
}

function unwrapEntity(payload: unknown): unknown {
    if (
        payload &&
        typeof payload === 'object' &&
        'entity' in (payload as Record<string, unknown>)
    ) {
        return (payload as Record<string, unknown>).entity;
    }
    return payload;
}

function asArray(value: unknown): unknown[] {
    return Array.isArray(value) ? value : [];
}

function asString(value: unknown): string {
    return typeof value === 'string' ? value : '';
}

function asBool(value: unknown): boolean {
    return value === true;
}

async function loadContentTypes(request: RequestFn): Promise<ContentTypeSummary[]> {
    const raw = await request({
        method: 'GET',
        path: '/api/v1/contenttype',
        query: { per_page: 200, orderby: 'name' }
    });
    const list = asArray(unwrapEntity(raw));
    return list.map((item) => {
        const t = item as Record<string, unknown>;
        return {
            id: asString(t.id),
            name: asString(t.name),
            variable: asString(t.variable),
            baseType: asString(t.baseType),
            host: typeof t.host === 'string' ? t.host : undefined,
            folder: typeof t.folder === 'string' ? t.folder : undefined
        };
    });
}

async function loadSites(request: RequestFn): Promise<SiteSummary[]> {
    const raw = await request({
        method: 'GET',
        path: '/api/v1/site',
        query: { per_page: 200 }
    });
    const list = asArray(unwrapEntity(raw));
    return list.map((item) => {
        const s = item as Record<string, unknown>;
        return {
            identifier: asString(s.identifier),
            hostname: asString(s.hostname ?? s.hostName),
            isDefault: asBool(s.default ?? s.isDefault),
            archived: asBool(s.archived)
        };
    });
}

async function loadLanguages(request: RequestFn): Promise<LanguageSummary[]> {
    const raw = await request({
        method: 'GET',
        path: '/api/v2/languages'
    });
    const list = asArray(unwrapEntity(raw));
    return list.map((item) => {
        const l = item as Record<string, unknown>;
        return {
            id: typeof l.id === 'number' ? l.id : Number(l.id) || 0,
            languageCode: asString(l.languageCode),
            countryCode: asString(l.countryCode),
            language: asString(l.language),
            country: asString(l.country),
            isoCode: asString(l.isoCode)
        };
    });
}

async function loadCurrentUser(request: RequestFn): Promise<CurrentUserSummary | null> {
    const raw = await request({
        method: 'GET',
        path: '/api/v1/users/current'
    });
    const entity = unwrapEntity(raw);
    if (!entity || typeof entity !== 'object') return null;
    const u = entity as Record<string, unknown>;
    const roles = Array.isArray(u.roles)
        ? u.roles.map((r) =>
              typeof r === 'string' ? r : asString((r as Record<string, unknown>)?.id)
          )
        : undefined;
    return {
        userId: asString(u.userId ?? u.id),
        email: asString(u.email ?? u.emailAddress),
        givenName: typeof u.givenName === 'string' ? u.givenName : undefined,
        surname: typeof u.surname === 'string' ? u.surname : undefined,
        admin: asBool(u.admin ?? u.hasAdminRole),
        roles
    };
}

/**
 * Load a minimal snapshot of dotCMS instance context for sandbox injection.
 * Each loader is independent — a failure in one does not poison the others.
 */
export async function loadDotCMSContext(
    apiAdapter: Adapter,
    onError?: (label: string, error: unknown) => void
): Promise<DotCMSContext> {
    const request = getRequestFn(apiAdapter);

    const [contentTypes, sites, languages, currentUser] = await Promise.all([
        loadContentTypes(request).catch((err) => {
            onError?.('contentTypes', err);
            return [] as ContentTypeSummary[];
        }),
        loadSites(request).catch((err) => {
            onError?.('sites', err);
            return [] as SiteSummary[];
        }),
        loadLanguages(request).catch((err) => {
            onError?.('languages', err);
            return [] as LanguageSummary[];
        }),
        loadCurrentUser(request).catch((err) => {
            onError?.('currentUser', err);
            return null as CurrentUserSummary | null;
        })
    ]);

    return { contentTypes, sites, languages, currentUser };
}
