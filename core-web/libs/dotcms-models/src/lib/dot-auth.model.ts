/**
 * dotAuth portlet models — mirror the REST surface at /api/v1/dotauth.
 */

/** Sentinel host id representing the SYSTEM_HOST / global default row. */
export const DOT_AUTH_SYSTEM_HOST = 'SYSTEM_HOST';

/** Value returned for hidden secrets. Posting it back preserves the stored secret. */
export const DOT_AUTH_HIDDEN_SECRET_MASK = '****';

export type DotAuthStatus = 'SITE_OVERRIDE' | 'INHERITED' | 'NOT_CONFIGURED';

export interface DotAuthSystemView {
    configured: boolean;
}

export interface DotAuthSiteRow {
    hostId: string;
    hostName: string;
    status: DotAuthStatus;
}

export interface DotAuthSitesView {
    system: DotAuthSystemView;
    sites: DotAuthSiteRow[];
}

export interface DotAuthConfigView {
    hostId: string;
    configured: boolean;
    inherited: boolean;
    values: DotAuthConfigValues;
}

/**
 * Field values persisted in AppSecrets. Booleans are sent as booleans;
 * everything else as strings. clientSecret is rendered as "****" in GET
 * responses when a stored value exists.
 */
export interface DotAuthConfigValues {
    enabled?: boolean;
    enableBackend?: boolean;
    enableFrontend?: boolean;
    providerType?: 'OIDC' | 'OAuth2';
    issuerUrl?: string;
    clientId?: string;
    clientSecret?: string;
    scopes?: string;
    authorizationUrl?: string;
    tokenUrl?: string;
    userinfoUrl?: string;
    revocationUrl?: string;
    logoutUrl?: string;
    groupsClaim?: string;
    groupsUrl?: string;
    extraRoles?: string;
    callbackUrl?: string;
}

export interface DotAuthConfigPayload {
    values: DotAuthConfigValues;
}
