import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotAuthService, DotHttpErrorManagerService } from '@dotcms/data-access';
import {
    DOT_AUTH_HIDDEN_SECRET_MASK,
    DOT_AUTH_SYSTEM_HOST,
    DotAuthConfig,
    DotAuthConfigPayload,
    DotAuthConfigView,
    DotAuthDiscoveryView,
    DotAuthUiProtocol
} from '@dotcms/dotcms-models';

type DotAuthConfigStatus = 'init' | 'loading' | 'loaded' | 'saving';

interface DotAuthConfigState {
    siteId: string;
    original: DotAuthConfig;
    draft: DotAuthConfig;
    configured: boolean;
    inherited: boolean;
    status: DotAuthConfigStatus;
    errors: Record<string, string>;
}

const DEFAULT_CONFIG: DotAuthConfig = {
    ssoEnabled: false,
    protocol: 'none',
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
        roleBehavior: 'merge',
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
        claimEmail: 'email',
        claimFirstName: 'firstName',
        claimLastName: 'lastName',
        claimGroups: 'groups',
        autoProvision: true,
        syncOnLogin: true,
        defaultRoles: [],
        roleBehavior: 'merge',
        groupMappings: [],
        sessionTtlMinutes: 60
    },
    headless: {
        enabled: false,
        sessionRefTtlMinutes: 60,
        refreshTtlHours: 8,
        rotateOnUse: true,
        clampToIdpExp: true,
        trustedIdps: [],
        allowedOrigins: []
    }
};

const initialState: DotAuthConfigState = {
    siteId: DOT_AUTH_SYSTEM_HOST,
    original: clone(DEFAULT_CONFIG),
    draft: clone(DEFAULT_CONFIG),
    configured: false,
    inherited: false,
    status: 'init',
    errors: {}
};

export const DotAuthConfigStore = signalStore(
    withState<DotAuthConfigState>(initialState),
    withComputed((store) => ({
        isSystem: computed(() => store.siteId() === DOT_AUTH_SYSTEM_HOST),
        dirty: computed(() => !equal(store.original(), store.draft())),
        errorCount: computed(() => Object.keys(store.errors()).length)
    })),
    withMethods((store) => {
        const service = inject(DotAuthService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        function load(siteId: string): void {
            patchState(store, { siteId, status: 'loading', errors: {} });
            service
                .getConfig(siteId)
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe((view) => {
                    const config = fromView(view);
                    patchState(store, {
                        original: config,
                        draft: clone(config),
                        configured: view.configured,
                        inherited: view.inherited,
                        status: 'loaded'
                    });
                });
        }

        function save(): void {
            const validation = validate(store.draft());
            if (Object.keys(validation).length) {
                patchState(store, { errors: validation });
                return;
            }

            if (store.draft().protocol === 'none') {
                patchState(store, { status: 'saving' });
                service
                    .clearConfig(store.siteId())
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                            return EMPTY;
                        })
                    )
                    .subscribe(() => load(store.siteId()));
                return;
            }

            patchState(store, { status: 'saving', errors: {} });
            service
                .saveConfig(store.siteId(), toPayload(store.draft()))
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'loaded' });
                        return EMPTY;
                    })
                )
                .subscribe(() => load(store.siteId()));
        }

        return {
            load,
            save,

            reset(): void {
                patchState(store, { draft: clone(store.original()), errors: {} });
            },

            clearOverride(): void {
                patchState(store, { status: 'saving' });
                service
                    .clearConfig(store.siteId())
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                            return EMPTY;
                        })
                    )
                    .subscribe(() => load(store.siteId()));
            },

            setProtocol(protocol: DotAuthUiProtocol): void {
                patchState(store, {
                    draft: { ...store.draft(), protocol, ssoEnabled: protocol !== 'none' }
                });
            },

            update(path: string, value: unknown): void {
                patchState(store, { draft: setPath(store.draft(), path, value) });
            },

            addAllowedOrigin(): void {
                const draft = clone(store.draft());
                draft.headless.allowedOrigins.push('');
                patchState(store, { draft });
            },

            removeAllowedOrigin(index: number): void {
                const draft = clone(store.draft());
                draft.headless.allowedOrigins.splice(index, 1);
                patchState(store, { draft });
            },

            addTrustedIdp(): void {
                const draft = clone(store.draft());
                draft.headless.trustedIdps.push({
                    id: crypto.randomUUID?.() ?? String(Date.now()),
                    name: '',
                    enabled: true,
                    discoveryUrl: '',
                    discoveryStatus: 'idle',
                    issuer: '',
                    jwksUrl: '',
                    audience: '',
                    algs: ['RS256'],
                    claimEmail: 'email',
                    claimFirstName: 'given_name',
                    claimLastName: 'family_name',
                    claimGroups: 'groups',
                    autoProvision: true,
                    syncOnExchange: true,
                    defaultRoles: [],
                    roleBehavior: 'merge',
                    groupMappings: []
                });
                patchState(store, { draft });
            },

            removeTrustedIdp(index: number): void {
                const draft = clone(store.draft());
                draft.headless.trustedIdps.splice(index, 1);
                patchState(store, { draft });
            },

            runOidcDiscovery(path: 'oidc' | `headless.trustedIdps.${number}`): void {
                const draft = clone(store.draft());
                const rawUrl =
                    path === 'oidc'
                        ? draft.oidc.discoveryUrl
                        : draft.headless.trustedIdps[Number(path.split('.').at(-1))].discoveryUrl;
                if (!rawUrl) return;
                const discoveryUrl = toWellKnownUrl(rawUrl);
                patchState(store, {
                    draft: setPath(draft, `${path}.discoveryStatus`, 'loading')
                });
                service
                    .discoverOidc(discoveryUrl)
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, {
                                draft: setPath(store.draft(), `${path}.discoveryStatus`, 'error')
                            });
                            return EMPTY;
                        })
                    )
                    .subscribe((view) => {
                        patchState(store, {
                            draft:
                                path === 'oidc'
                                    ? applyOidcDiscovery(store.draft(), view)
                                    : applyTrustedDiscovery(store.draft(), path, view)
                        });
                    });
            },

            revokeAllSessionRefs(): void {
                service
                    .revokeAllSessionRefs()
                    .pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            return EMPTY;
                        })
                    )
                    .subscribe();
            }
        };
    })
);

function fromView(view: DotAuthConfigView): DotAuthConfig {
    const config = clone(DEFAULT_CONFIG);
    config.protocol =
        view.configured || view.inherited ? (view.protocol === 'SAML' ? 'saml' : 'oidc') : 'none';
    config.ssoEnabled =
        config.protocol !== 'none' &&
        (view.protocol === 'SAML'
            ? (view.values as { enable?: boolean }).enable !== false
            : (view.values as { enabled?: boolean }).enabled !== false);

    if (view.protocol === 'SAML') {
        const values = view.values;
        config.saml = {
            ...config.saml,
            metadataUrl: String(values.idPMetadataFile ?? ''),
            entityId: String(values.sPIssuerURL ?? ''),
            ssoUrl: String(values.idpSsoUrl ?? ''),
            sloUrl: String(values.idpSloUrl ?? ''),
            x509cert: String(values.publicCert ?? ''),
            signRequests: String(values.signRequests ?? 'true') === 'true',
            wantAssertionsSigned:
                values.signatureValidationType === 'assertion' ||
                values.signatureValidationType === 'responseandassertion',
            wantResponseSigned:
                values.signatureValidationType === 'response' ||
                values.signatureValidationType === 'responseandassertion',
            claimEmail: String(values.emailAttribute ?? config.saml.claimEmail),
            claimFirstName: String(values.firstNameAttribute ?? config.saml.claimFirstName),
            claimLastName: String(values.lastNameAttribute ?? config.saml.claimLastName),
            claimGroups: String(values.rolesAttribute ?? config.saml.claimGroups),
            autoProvision: String(values.autoCreateUsers ?? 'true') === 'true',
            syncOnLogin: String(values.syncOnLogin ?? 'true') === 'true',
            defaultRoles: splitList(values.extraRoles),
            roleBehavior: 'merge',
            groupMappings: parseJson(values.groupMappings, [])
        };
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
        autoProvision: values.enabled ?? config.oidc.autoProvision,
        syncOnLogin: true,
        defaultRoles: splitList(values.extraRoles),
        roleBehavior: fromBuildRolesStrategy(values.buildRolesStrategy)
    };
    config.headless = {
        ...config.headless,
        enabled: Boolean(values.exchangeEnabled ?? false),
        sessionRefTtlMinutes: numberValue(values.headlessSessionRefTtlMinutes, 60),
        refreshTtlHours: numberValue(values.headlessRefreshTtlHours, 8),
        rotateOnUse: booleanValue(values.headlessRotateOnUse, true),
        clampToIdpExp: booleanValue(values.headlessClampToIdpExp, true),
        allowedOrigins: parseJson(values.headlessAllowedOrigins, []),
        trustedIdps: parseJson(values.headlessTrustedIdps, [])
    };
    return config;
}

function toPayload(config: DotAuthConfig): DotAuthConfigPayload {
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
                emailAttribute: config.saml.claimEmail,
                firstNameAttribute: config.saml.claimFirstName,
                lastNameAttribute: config.saml.claimLastName,
                rolesAttribute: config.saml.claimGroups,
                autoCreateUsers: String(config.saml.autoProvision),
                syncOnLogin: String(config.saml.syncOnLogin),
                extraRoles: config.saml.defaultRoles.join(','),
                groupMappings: JSON.stringify(config.saml.groupMappings)
            }
        };
    }

    return {
        protocol: 'OAUTH',
        values: {
            enabled: config.ssoEnabled,
            enableBackend: config.ssoEnabled,
            enableFrontend: false,
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
            extraRoles: config.oidc.defaultRoles.join(','),
            buildRolesStrategy: toBuildRolesStrategy(config.oidc.roleBehavior),
            hashUserId: true,
            exchangeEnabled: config.headless.enabled,
            exchangeProviderType: 'OIDC',
            exchangeIssuerUrl: config.oidc.issuer,
            exchangeClientId: config.oidc.clientId,
            exchangeClientSecret: config.oidc.clientSecret,
            exchangeScopes: config.oidc.scopes,
            exchangeAuthorizationUrl: config.oidc.authUrl,
            exchangeTokenUrl: config.oidc.tokenUrl,
            exchangeUserinfoUrl: config.oidc.userinfoUrl,
            exchangeGroupsClaim: config.oidc.claimGroups,
            exchangeBuildRolesStrategy: toBuildRolesStrategy(config.oidc.roleBehavior),
            exchangeHashUserId: true,
            headlessSessionRefTtlMinutes: String(config.headless.sessionRefTtlMinutes),
            headlessRefreshTtlHours: String(config.headless.refreshTtlHours),
            headlessRotateOnUse: config.headless.rotateOnUse,
            headlessClampToIdpExp: config.headless.clampToIdpExp,
            headlessAllowedOrigins: JSON.stringify(config.headless.allowedOrigins),
            headlessTrustedIdps: JSON.stringify(config.headless.trustedIdps)
        }
    };
}

function validate(config: DotAuthConfig): Record<string, string> {
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

function required(values: Record<string, string>): Record<string, string> {
    return Object.fromEntries(
        Object.entries(values)
            .filter(([, value]) => !String(value ?? '').trim())
            .map(([key]) => [key, 'dotauth.validation.required'])
    );
}

function applyOidcDiscovery(config: DotAuthConfig, view: DotAuthDiscoveryView): DotAuthConfig {
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

function applyTrustedDiscovery(
    config: DotAuthConfig,
    path: `headless.trustedIdps.${number}`,
    view: DotAuthDiscoveryView
): DotAuthConfig {
    const draft = clone(config);
    const index = Number(path.split('.').at(-1));
    const idp = draft.headless.trustedIdps[index];
    idp.discoveryStatus = 'ok';
    idp.issuer = view.issuer ?? idp.issuer;
    idp.jwksUrl = view.jwksUri ?? idp.jwksUrl;
    idp.algs = view.signingAlgs?.length ? view.signingAlgs : idp.algs;
    return draft;
}

function setPath(config: DotAuthConfig, path: string, value: unknown): DotAuthConfig {
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

function fromBuildRolesStrategy(value: unknown) {
    switch (value) {
        case 'IDP':
            return 'replace';
        case 'STATICADD':
            return 'add-only';
        case 'STATICONLY':
            return 'first-only';
        default:
            return 'merge';
    }
}

function toBuildRolesStrategy(value: string) {
    switch (value) {
        case 'replace':
            return 'IDP';
        case 'add-only':
            return 'STATICADD';
        case 'first-only':
            return 'STATICONLY';
        default:
            return 'ALL';
    }
}

function clone<T>(value: T): T {
    return JSON.parse(JSON.stringify(value)) as T;
}

function equal(a: unknown, b: unknown): boolean {
    return JSON.stringify(a) === JSON.stringify(b);
}

function toWellKnownUrl(url: string): string {
    const trimmed = url.replace(/\/+$/, '');
    if (trimmed.endsWith('.well-known/openid-configuration')) return trimmed;
    return `${trimmed}/.well-known/openid-configuration`;
}
