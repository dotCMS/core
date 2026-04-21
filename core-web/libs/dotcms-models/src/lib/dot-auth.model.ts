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

export type DotAuthSignatureValidation = 'none' | 'response' | 'assertion' | 'responseandassertion';

/**
 * SAML field values — mirrors SamlProtocolHandler.SAML_SECRET_KEYS /
 * dotsaml-config.yml. `privateKey` is hidden: a returned "****" means a stored
 * value exists, and posting "****" back preserves it.
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
}

/**
 * Discriminated union on `protocol`. `values` shape is determined by the
 * protocol discriminator.
 */
export type DotAuthConfigView =
    | {
          hostId: string;
          protocol: 'OAUTH';
          configured: boolean;
          inherited: boolean;
          values: DotAuthConfigValues;
      }
    | {
          hostId: string;
          protocol: 'SAML';
          configured: boolean;
          inherited: boolean;
          values: DotAuthSamlConfigValues;
      };

/** PUT body. Protocol determines which value shape is sent. */
export type DotAuthConfigPayload =
    | { protocol: 'OAUTH'; values: DotAuthConfigValues }
    | { protocol: 'SAML'; values: DotAuthSamlConfigValues };
