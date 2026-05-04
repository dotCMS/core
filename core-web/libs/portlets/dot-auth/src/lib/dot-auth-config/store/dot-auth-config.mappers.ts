import {
    DOT_AUTH_HIDDEN_SECRET_MASK,
    DotAuthConfig,
    DotAuthConfigPayload,
    DotAuthConfigView,
    DotAuthDiscoveryView,
    DotAuthHeadlessPayload,
    DotAuthRoleBehavior
} from '@dotcms/dotcms-models';

export const DEFAULT_CONFIG: DotAuthConfig = {
    ssoEnabled: false,
    protocol: 'none',
    enableBackend: true,
    enableFrontend: false,
    hashUserId: true,
    callbackUrl: '',
    oidc: {
        discoveryUrl: '',
        discoveryStatus: 'idle',
        issuer: '',
        authUrl: '',
        tokenUrl: '',
        jwksUrl: '',
        userinfoUrl: '',
        logoutUrl: '',
        clientId: '',
        clientSecret: '',
        scopes: 'openid email profile',
        responseType: 'code',
        pkce: false,
        audience: '',
        claimEmail: 'email',
        claimFirstName: 'given_name',
        claimLastName: 'family_name',
        claimGroups: 'groups',
        autoProvision: true,
        syncOnLogin: true,
        defaultRoles: [],
        roleBehavior: 'sync-all',
        groupMappings: [],
        sessionTtlMinutes: 60,
        idleTimeoutMinutes: 30,
        postLogoutRedirect: ''
    },
    saml: {
        metadataUrl: '',
        entityId: '',
        ssoUrl: '',
        sloUrl: '',
        x509cert: '',
        signRequests: true,
        wantAssertionsSigned: true,
        wantResponseSigned: false,
        claimEmail: 'mail',
        claimFirstName: 'givenName',
        claimLastName: 'sn',
        claimGroups: 'authorisations',
        autoProvision: true,
        syncOnLogin: true,
        defaultRoles: [],
        roleBehavior: 'sync-all',
        groupMappings: [],
        sessionTtlMinutes: 60,
        extraProperties: []
    },
    headless: {
        enabled: false,
        sessionRefTtlMinutes: 60,
        clampToIdpExp: true,
        trustedIdps: [],
        allowedOrigins: []
    }
};

export function fromView(view: DotAuthConfigView): DotAuthConfig {
    const config = clone(DEFAULT_CONFIG);
    config.protocol =
        view.configured || view.inherited ? (view.protocol === 'SAML' ? 'saml' : 'oidc') : 'none';
    config.ssoEnabled =
        config.protocol !== 'none' &&
        (view.protocol === 'SAML'
            ? (view.values as { enable?: boolean }).enable !== false
            : (view.values as { enabled?: boolean }).enabled !== false);

    const vals = view.values as Record<string, unknown>;
    config.enableBackend = booleanValue(vals['enableBackend'], config.enableBackend);
    config.enableFrontend = booleanValue(vals['enableFrontend'], config.enableFrontend);
    config.hashUserId = booleanValue(vals['hashUserId'], config.hashUserId);
    config.callbackUrl = String(vals['callbackUrl'] ?? config.callbackUrl);

    if (view.protocol === 'SAML') {
        const values = view.values;
        config.saml = {
            ...config.saml,
            metadataUrl: String(values.idPMetadataFile ?? ''),
            entityId: String(values.sPIssuerURL ?? ''),
            ssoUrl: String(values['identity.provider.destinationsso.url'] ?? ''),
            sloUrl: String(values['identity.provider.destinationslo.url'] ?? ''),
            x509cert: String(values.publicCert ?? ''),
            signRequests: String(values['signRequests'] ?? 'true') === 'true',
            wantAssertionsSigned:
                values.signatureValidationType === 'assertion' ||
                values.signatureValidationType === 'responseandassertion',
            wantResponseSigned:
                values.signatureValidationType === 'response' ||
                values.signatureValidationType === 'responseandassertion',
            claimEmail: String(values['attribute.email.name'] ?? config.saml.claimEmail),
            claimFirstName: String(
                values['attribute.firstname.name'] ?? config.saml.claimFirstName
            ),
            claimLastName: String(values['attribute.lastname.name'] ?? config.saml.claimLastName),
            claimGroups: String(values['attribute.roles.name'] ?? config.saml.claimGroups),
            autoProvision: String(values['allow.user.synchronization'] ?? 'true') === 'true',
            syncOnLogin: String(values['login.email.update'] ?? 'true') === 'true',
            defaultRoles: splitList(values['role.extra']),
            roleBehavior: fromBuildRolesStrategy(values['build.roles'] as string),
            groupMappings: parseJson(values['groupMappings'], []),
            extraProperties: extractSamlExtraProperties(values)
        };
        config.headless = fromHeadlessValues(view, config.headless);
        return config;
    }

    const values = view.values;
    config.oidc = {
        ...config.oidc,
        issuer: String(values.issuerUrl ?? ''),
        authUrl: String(values.authorizationUrl ?? ''),
        tokenUrl: String(values.tokenUrl ?? ''),
        userinfoUrl: String(values.userinfoUrl ?? ''),
        logoutUrl: String(values.logoutUrl ?? ''),
        clientId: String(values.clientId ?? ''),
        clientSecret: String(values.clientSecret ?? ''),
        scopes: String(values.scopes ?? config.oidc.scopes),
        claimGroups: String(values.groupsClaim ?? config.oidc.claimGroups),
        claimEmail: String(values.emailClaim ?? config.oidc.claimEmail),
        claimFirstName: String(values.firstNameClaim ?? config.oidc.claimFirstName),
        claimLastName: String(values.lastNameClaim ?? config.oidc.claimLastName),
        autoProvision: values.enabled ?? config.oidc.autoProvision,
        syncOnLogin: true,
        defaultRoles: splitList(values.extraRoles),
        roleBehavior: fromBuildRolesStrategy(values.buildRolesStrategy),
        groupMappings: parseJson(values['groupMappings'], config.oidc.groupMappings)
    };
    config.headless = fromHeadlessValues(view, config.headless);
    return config;
}

export function toPayload(config: DotAuthConfig): DotAuthConfigPayload {
    if (config.protocol === 'saml') {
        return {
            protocol: 'SAML',
            values: {
                enable: config.ssoEnabled,
                idpName: 'SAML Identity Provider',
                sPIssuerURL: config.saml.entityId,
                sPEndpointHostname: '',
                signatureValidationType: config.saml.wantResponseSigned
                    ? config.saml.wantAssertionsSigned
                        ? 'responseandassertion'
                        : 'response'
                    : config.saml.wantAssertionsSigned
                      ? 'assertion'
                      : 'none',
                idPMetadataFile: config.saml.metadataUrl,
                publicCert: config.saml.x509cert,
                privateKey: DOT_AUTH_HIDDEN_SECRET_MASK,
                buttonParam: '/api/v1/dotsaml/metadata/$siteId',
                'identity.provider.destinationsso.url': config.saml.ssoUrl || undefined,
                'identity.provider.destinationslo.url': config.saml.sloUrl || undefined,
                'attribute.email.name': config.saml.claimEmail || undefined,
                'attribute.firstname.name': config.saml.claimFirstName || undefined,
                'attribute.lastname.name': config.saml.claimLastName || undefined,
                'attribute.roles.name': config.saml.claimGroups || undefined,
                'allow.user.synchronization': String(config.saml.autoProvision),
                'login.email.update': String(config.saml.syncOnLogin),
                'role.extra': config.saml.defaultRoles.join(',') || undefined,
                'build.roles': toBuildRoles(config.saml.roleBehavior),
                groupMappings: JSON.stringify(config.saml.groupMappings),
                enableBackend: String(config.enableBackend),
                enableFrontend: String(config.enableFrontend),
                ...Object.fromEntries(
                    (config.saml.extraProperties ?? [])
                        .filter((p) => p.key && p.value)
                        .map((p) => [p.key, p.value])
                )
            }
        };
    }

    return {
        protocol: 'OAUTH',
        values: {
            enabled: config.ssoEnabled,
            enableBackend: config.enableBackend,
            enableFrontend: config.enableFrontend,
            hashUserId: config.hashUserId,
            callbackUrl: config.callbackUrl || undefined,
            providerType: 'OIDC',
            issuerUrl: config.oidc.issuer,
            clientId: config.oidc.clientId,
            clientSecret: config.oidc.clientSecret,
            scopes: config.oidc.scopes,
            authorizationUrl: config.oidc.authUrl,
            tokenUrl: config.oidc.tokenUrl,
            userinfoUrl: config.oidc.userinfoUrl,
            logoutUrl: config.oidc.logoutUrl,
            groupsClaim: config.oidc.claimGroups,
            emailClaim: config.oidc.claimEmail || undefined,
            firstNameClaim: config.oidc.claimFirstName || undefined,
            lastNameClaim: config.oidc.claimLastName || undefined,
            groupMappings: JSON.stringify(config.oidc.groupMappings),
            extraRoles: config.oidc.defaultRoles.join(','),
            buildRolesStrategy: toBuildRoles(config.oidc.roleBehavior)
        }
    };
}

export function validate(config: DotAuthConfig): Record<string, string> {
    if (!config.ssoEnabled || config.protocol === 'none') return {};
    if (config.protocol === 'oidc') {
        return required({
            'oidc.issuer': config.oidc.issuer,
            'oidc.clientId': config.oidc.clientId,
            'oidc.clientSecret': config.oidc.clientSecret
        });
    }
    return required({
        'saml.entityId': config.saml.entityId,
        'saml.metadataUrl': config.saml.metadataUrl,
        'saml.x509cert': config.saml.x509cert
    });
}

export function validateHeadless(config: DotAuthConfig): Record<string, string> {
    if (!config.headless.enabled) return {};
    const errors: Record<string, string> = {};
    if (config.headless.trustedIdps.length === 0) {
        errors['headless.trustedIdps'] = 'dotauth.validation.headless.no.trusted.idps';
    }
    return errors;
}

export function toHeadlessPayload(config: DotAuthConfig): DotAuthHeadlessPayload {
    return {
        enabled: config.headless.enabled,
        sessionRefTtlMinutes: String(config.headless.sessionRefTtlMinutes),
        clampToIdpExp: config.headless.clampToIdpExp,
        allowedOrigins: JSON.stringify(config.headless.allowedOrigins),
        trustedIdps: JSON.stringify(config.headless.trustedIdps),
        hashUserId: true,
        providerType: 'OIDC'
    };
}

function fromHeadlessValues(
    view: DotAuthConfigView,
    defaults: DotAuthConfig['headless']
): DotAuthConfig['headless'] {
    const hv = view.headlessValues ?? {};
    return {
        ...defaults,
        enabled: Boolean(hv.enabled ?? false),
        sessionRefTtlMinutes: numberValue(hv.sessionRefTtlMinutes, 60),
        clampToIdpExp: booleanValue(hv.clampToIdpExp, true),
        allowedOrigins: parseJson(hv.allowedOrigins, []),
        trustedIdps: parseJson(hv.trustedIdps, [])
    };
}

export function applyOidcDiscovery(
    config: DotAuthConfig,
    view: DotAuthDiscoveryView
): DotAuthConfig {
    const draft = clone(config);
    draft.oidc.discoveryStatus = 'ok';
    draft.oidc.issuer = view.issuer ?? draft.oidc.issuer;
    draft.oidc.authUrl = view.authorizationEndpoint ?? draft.oidc.authUrl;
    draft.oidc.tokenUrl = view.tokenEndpoint ?? draft.oidc.tokenUrl;
    draft.oidc.jwksUrl = view.jwksUri ?? draft.oidc.jwksUrl;
    draft.oidc.userinfoUrl = view.userinfoEndpoint ?? draft.oidc.userinfoUrl;
    draft.oidc.logoutUrl = view.endSessionEndpoint ?? draft.oidc.logoutUrl;
    return draft;
}

export function applyTrustedDiscovery(
    config: DotAuthConfig,
    path: `headless.trustedIdps.${number}`,
    view: DotAuthDiscoveryView
): DotAuthConfig {
    const draft = clone(config);
    const parts = path.split('.');
    const index = Number(parts[parts.length - 1]);
    const idp = draft.headless.trustedIdps[index];
    idp.discoveryStatus = 'ok';
    idp.issuer = view.issuer ?? idp.issuer;
    idp.jwksUrl = view.jwksUri ?? idp.jwksUrl;
    idp.algs = view.signingAlgs?.length ? view.signingAlgs : idp.algs;
    return draft;
}

export function setPath(config: DotAuthConfig, path: string, value: unknown): DotAuthConfig {
    const draft = clone(config);
    const parts = path.split('.');
    let cursor: Record<string, unknown> | unknown[] = draft as unknown as Record<string, unknown>;
    for (const part of parts.slice(0, -1)) {
        cursor = Array.isArray(cursor)
            ? (cursor[Number(part)] as Record<string, unknown>)
            : (cursor[part] as Record<string, unknown>);
    }
    const last = parts[parts.length - 1];
    if (Array.isArray(cursor)) {
        cursor[Number(last)] = value;
    } else {
        cursor[last] = value;
    }
    return draft;
}

export function clone<T>(value: T): T {
    return structuredClone(value);
}

export function equal(a: unknown, b: unknown): boolean {
    return JSON.stringify(a) === JSON.stringify(b);
}

export function toWellKnownUrl(url: string): string {
    const trimmed = url.replace(/\/+$/, '');
    if (trimmed.endsWith('.well-known/openid-configuration')) return trimmed;
    return `${trimmed}/.well-known/openid-configuration`;
}

function required(values: Record<string, string>): Record<string, string> {
    return Object.fromEntries(
        Object.entries(values)
            .filter(([, value]) => !String(value ?? '').trim())
            .map(([key]) => [key, 'dotauth.validation.required'])
    );
}

function splitList(value: unknown): string[] {
    return String(value ?? '')
        .split(',')
        .map((item) => item.trim())
        .filter(Boolean);
}

function parseJson<T>(value: unknown, fallback: T): T {
    if (!value) return fallback;
    try {
        return JSON.parse(String(value)) as T;
    } catch {
        return fallback;
    }
}

function numberValue(value: unknown, fallback: number): number {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
}

function booleanValue(value: unknown, fallback: boolean): boolean {
    if (value === undefined || value === null || value === '') return fallback;
    return value === true || String(value) === 'true';
}

function fromBuildRolesStrategy(value: unknown): DotAuthRoleBehavior {
    switch (String(value ?? '').toUpperCase()) {
        case 'IDP':
            return 'idp-only';
        case 'STATICADD':
            return 'additive';
        case 'STATICONLY':
            return 'static-only';
        case 'NONE':
            return 'none';
        default:
            return 'sync-all';
    }
}

const SAML_ELEVATED_KEYS = new Set([
    'enable',
    'idpName',
    'sPIssuerURL',
    'sPEndpointHostname',
    'signatureValidationType',
    'idPMetadataFile',
    'publicCert',
    'privateKey',
    'buttonParam',
    'identity.provider.destinationsso.url',
    'identity.provider.destinationslo.url',
    'attribute.email.name',
    'attribute.firstname.name',
    'attribute.lastname.name',
    'attribute.roles.name',
    'allow.user.synchronization',
    'login.email.update',
    'role.extra',
    'build.roles',
    'groupMappings',
    'enableBackend',
    'enableFrontend'
]);

function extractSamlExtraProperties(
    values: Record<string, unknown>
): { key: string; value: string }[] {
    return Object.entries(values)
        .filter(([key]) => !SAML_ELEVATED_KEYS.has(key))
        .filter(([, value]) => value !== undefined && value !== null && value !== '')
        .map(([key, value]) => ({ key, value: String(value) }));
}

function toBuildRoles(value: string): string {
    switch (value) {
        case 'idp-only':
            return 'idp';
        case 'additive':
            return 'staticadd';
        case 'static-only':
            return 'staticonly';
        case 'none':
            return 'none';
        default:
            return 'all';
    }
}
