/**
 * dotAuth portlet models — mirror the REST surface at /api/v1/dotauth.
 */

/** Sentinel host id representing the SYSTEM_HOST / global default row. */
export const DOT_AUTH_SYSTEM_HOST = 'SYSTEM_HOST';

/** Value returned for hidden secrets. Posting it back preserves the stored secret. */
export const DOT_AUTH_HIDDEN_SECRET_MASK = '****';

export type DotAuthStatus = 'SITE_OVERRIDE' | 'INHERITED' | 'NOT_CONFIGURED';

export type DotAuthProtocol = 'OAUTH' | 'SAML';

export interface DotAuthSystemView {
    configured: boolean;
    protocol: DotAuthProtocol | null;
    headlessConfigured: boolean;
}

export interface DotAuthSiteRow {
    hostId: string;
    hostName: string;
    status: DotAuthStatus;
    protocol: DotAuthProtocol | null;
}

export interface DotAuthSitesView {
    system: DotAuthSystemView;
    sites: DotAuthSiteRow[];
}

/**
 * OAuth / OIDC field values — mirrors OAuthAppConfig.KEY_*. Booleans are sent
 * as booleans; everything else as strings. clientSecret is rendered as "****"
 * in GET responses when a stored value exists.
 */
export interface DotAuthConfigValues {
    enabled?: boolean;
    enableBackend?: boolean;
    enableFrontend?: boolean;
    hashUserId?: boolean;
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
    emailClaim?: string;
    firstNameClaim?: string;
    lastNameClaim?: string;
    groupMappings?: string;
    extraRoles?: string;
    buildRolesStrategy?: string;
    callbackUrl?: string;
    autoProvision?: boolean;
}

export type DotAuthSignatureValidation = 'none' | 'response' | 'assertion' | 'responseandassertion';

/**
 * SAML field values — mirrors SamlProtocolHandler.SAML_SECRET_KEYS /
 * dotsaml-config.yml. `privateKey` is hidden: a returned "****" means a stored
 * value exists, and posting "****" back preserves it.
 *
 * The index signature allows arbitrary additional attributes — the Apps
 * descriptor has `allowExtraParameters: true`, so admins can store extra
 * SAML-handler settings (e.g. `emailAttribute`, `rolesAttribute`,
 * `autoCreateUsers`) alongside the declared keys. They round-trip as strings.
 */
export interface DotAuthSamlConfigValues {
    enable?: boolean;
    idpName?: string;
    sPIssuerURL?: string;
    sPEndpointHostname?: string;
    signatureValidationType?: DotAuthSignatureValidation;
    idPMetadataFile?: string;
    publicCert?: string;
    privateKey?: string;
    buttonParam?: string;
    [extraKey: string]: unknown;
}

/**
 * Ordered list of keys declared on the SAML config. Used by the edit dialog
 * to split {@link DotAuthSamlConfigValues} between the declared form fields
 * and the custom-attribute key/value editor.
 */
export const DOT_AUTH_SAML_DECLARED_KEYS = [
    'enable',
    'idpName',
    'sPIssuerURL',
    'sPEndpointHostname',
    'signatureValidationType',
    'idPMetadataFile',
    'publicCert',
    'privateKey',
    'buttonParam'
] as const;

export type DotAuthSamlDeclaredKey = (typeof DOT_AUTH_SAML_DECLARED_KEYS)[number];

/**
 * Discriminated union on `protocol`. `values` shape is determined by the
 * protocol discriminator.
 */
/**
 * Headless config values — returned under {@code headlessValues} in the config
 * response, using clean key names (no {@code exchange}/{@code headless} prefix).
 */
export interface DotAuthHeadlessValues {
    enabled?: boolean;
    providerType?: 'OIDC' | 'OAuth2';
    issuerUrl?: string;
    clientId?: string;
    scopes?: string;
    authorizationUrl?: string;
    tokenUrl?: string;
    userinfoUrl?: string;
    revocationUrl?: string;
    logoutUrl?: string;
    groupsClaim?: string;
    groupsUrl?: string;
    extraRoles?: string;
    buildRolesStrategy?: string;
    callbackUrl?: string;
    hashUserId?: boolean;
    sessionRefTtlMinutes?: string;
    clampToIdpExp?: boolean;
    allowedOrigins?: string;
    trustedIdps?: string;
}

export type DotAuthConfigView =
    | {
          hostId: string;
          protocol: 'OAUTH';
          configured: boolean;
          inherited: boolean;
          values: DotAuthConfigValues;
          headlessValues: DotAuthHeadlessValues;
      }
    | {
          hostId: string;
          protocol: 'SAML';
          configured: boolean;
          inherited: boolean;
          values: DotAuthSamlConfigValues;
          headlessValues: DotAuthHeadlessValues;
      };

/** PUT body for SSO config. Protocol determines which value shape is sent. */
export type DotAuthConfigPayload =
    | { protocol: 'OAUTH'; values: DotAuthConfigValues }
    | { protocol: 'SAML'; values: DotAuthSamlConfigValues };

/** PUT body for headless config (sent to /sites/{hostId}/headless). */
export type DotAuthHeadlessPayload = DotAuthHeadlessValues;

export type DotAuthUiProtocol = 'none' | 'oidc' | 'saml';

export type DotAuthDiscoveryStatus = 'idle' | 'loading' | 'ok' | 'error';

export type DotAuthRoleBehavior = 'sync-all' | 'idp-only' | 'static-only' | 'additive' | 'none';

export interface DotAuthGroupMapping {
    idpGroup: string;
    dotcmsRole: string;
}

export interface DotAuthProvisioningConfig {
    autoProvision: boolean;
    defaultRoles: string[];
    roleBehavior: DotAuthRoleBehavior;
    groupMappings: DotAuthGroupMapping[];
}

export interface DotAuthOidcConfig extends DotAuthProvisioningConfig {
    discoveryUrl: string;
    discoveryStatus: DotAuthDiscoveryStatus;
    issuer: string;
    authUrl: string;
    tokenUrl: string;
    jwksUrl: string;
    userinfoUrl: string;
    logoutUrl: string;
    /** Token-revocation endpoint — not edited in the UI, but must round-trip so a save never deletes it. */
    revocationUrl: string;
    /** Groups endpoint — not edited in the UI, but must round-trip so a save never deletes it. */
    groupsUrl: string;
    clientId: string;
    clientSecret: string;
    scopes: string;
    responseType: 'code' | 'id_token' | 'code id_token';
    pkce: boolean;
    audience: string;
    claimEmail: string;
    claimFirstName: string;
    claimLastName: string;
    claimGroups: string;
    syncOnLogin: boolean;
    sessionTtlMinutes: number;
    idleTimeoutMinutes: number;
    postLogoutRedirect: string;
}

export interface DotAuthSamlExtraProperty {
    key: string;
    value: string;
}

export interface DotAuthSamlUiConfig extends DotAuthProvisioningConfig {
    /** Display name for the IdP — not edited in the UI, but must round-trip so a save never resets it. */
    idpName: string;
    /** SP endpoint hostname (drives SP entity/ACS URLs) — not edited in the UI, but must round-trip so a save never blanks it. */
    spEndpointHostname: string;
    metadataUrl: string;
    entityId: string;
    ssoUrl: string;
    sloUrl: string;
    x509cert: string;
    privateKey: string;
    signRequests: boolean;
    wantAssertionsSigned: boolean;
    wantResponseSigned: boolean;
    claimEmail: string;
    claimFirstName: string;
    claimLastName: string;
    claimGroups: string;
    syncOnLogin: boolean;
    sessionTtlMinutes: number;
    extraProperties: DotAuthSamlExtraProperty[];
}

export interface DotAuthTrustedIdp extends DotAuthProvisioningConfig {
    id: string;
    name: string;
    enabled: boolean;
    discoveryUrl: string;
    discoveryStatus: DotAuthDiscoveryStatus;
    issuer: string;
    jwksUrl: string;
    audience: string;
    algs: string[];
    claimEmail: string;
    claimFirstName: string;
    claimLastName: string;
    claimGroups: string;
    syncOnExchange: boolean;
}

export interface DotAuthHeadlessConfig {
    enabled: boolean;
    sessionRefTtlMinutes: number;
    clampToIdpExp: boolean;
    trustedIdps: DotAuthTrustedIdp[];
    allowedOrigins: string[];
}

export interface DotAuthConfig {
    ssoEnabled: boolean;
    protocol: DotAuthUiProtocol;
    enableBackend: boolean;
    enableFrontend: boolean;
    hashUserId: boolean;
    callbackUrl: string;
    oidc: DotAuthOidcConfig;
    saml: DotAuthSamlUiConfig;
    headless: DotAuthHeadlessConfig;
}

export type DotAuthListFilter = 'all' | 'overrides' | 'sso-on' | 'headless-on' | 'disabled';

export type DotAuthCapabilityStatus = 'enabled' | 'override' | 'inherits' | 'disabled';

export interface DotAuthDiscoveryRequest {
    url: string;
}

export interface DotAuthDiscoveryView {
    issuer?: string;
    authorizationEndpoint?: string;
    tokenEndpoint?: string;
    jwksUri?: string;
    userinfoEndpoint?: string;
    endSessionEndpoint?: string;
    signingAlgs?: string[];
}
