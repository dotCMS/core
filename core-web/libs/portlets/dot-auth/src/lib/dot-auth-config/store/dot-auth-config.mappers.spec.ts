import { DotAuthConfig, DotAuthConfigView, DotAuthDiscoveryView } from '@dotcms/dotcms-models';

import {
    DEFAULT_CONFIG,
    applyOidcDiscovery,
    applyTrustedDiscovery,
    clone,
    equal,
    fromView,
    setPath,
    toHeadlessPayload,
    toPayload,
    toWellKnownUrl,
    validate,
    validateHeadless
} from './dot-auth-config.mappers';

describe('dot-auth-config.mappers', () => {
    describe('clone / equal', () => {
        it('produces a deep copy', () => {
            const original = clone(DEFAULT_CONFIG);
            const copy = clone(original);
            copy.oidc.clientId = 'changed';
            expect(original.oidc.clientId).toBe('');
        });

        it('equal returns true for identical structures', () => {
            expect(equal({ a: 1 }, { a: 1 })).toBe(true);
        });

        it('equal returns false when values differ', () => {
            expect(equal({ a: 1 }, { a: 2 })).toBe(false);
        });
    });

    describe('toWellKnownUrl', () => {
        it('appends .well-known path when missing', () => {
            expect(toWellKnownUrl('https://idp.example')).toBe(
                'https://idp.example/.well-known/openid-configuration'
            );
        });

        it('strips trailing slashes before appending', () => {
            expect(toWellKnownUrl('https://idp.example/')).toBe(
                'https://idp.example/.well-known/openid-configuration'
            );
        });

        it('returns url unchanged when it already ends with the well-known path', () => {
            const url = 'https://idp.example/.well-known/openid-configuration';
            expect(toWellKnownUrl(url)).toBe(url);
        });
    });

    describe('setPath', () => {
        it('sets a top-level property', () => {
            const config = clone(DEFAULT_CONFIG);
            const result = setPath(config, 'ssoEnabled', true);
            expect(result.ssoEnabled).toBe(true);
            expect(config.ssoEnabled).toBe(false);
        });

        it('sets a nested property', () => {
            const result = setPath(clone(DEFAULT_CONFIG), 'oidc.clientId', 'my-client');
            expect(result.oidc.clientId).toBe('my-client');
        });

        it('sets an array element by index', () => {
            const config = clone(DEFAULT_CONFIG);
            config.headless.allowedOrigins = ['https://a.example', 'https://b.example'];
            const result = setPath(config, 'headless.allowedOrigins.1', 'https://c.example');
            expect(result.headless.allowedOrigins[1]).toBe('https://c.example');
        });
    });

    describe('validate', () => {
        it('returns empty when protocol is none', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'none';
            expect(validate(config)).toEqual({});
        });

        it('returns empty when ssoEnabled is false', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'oidc';
            config.ssoEnabled = false;
            expect(validate(config)).toEqual({});
        });

        it('flags missing OIDC required fields', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'oidc';
            config.ssoEnabled = true;
            config.oidc.issuer = '';
            config.oidc.clientId = '';
            config.oidc.clientSecret = 'secret';
            const errors = validate(config);
            expect(errors['oidc.issuer']).toBeDefined();
            expect(errors['oidc.clientId']).toBeDefined();
            expect(errors['oidc.clientSecret']).toBeUndefined();
        });

        it('flags missing SAML required fields', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';
            config.ssoEnabled = true;
            config.saml.entityId = '';
            config.saml.metadataUrl = 'https://idp.example/metadata';
            config.saml.x509cert = '';
            const errors = validate(config);
            expect(errors['saml.entityId']).toBeDefined();
            expect(errors['saml.metadataUrl']).toBeUndefined();
            expect(errors['saml.x509cert']).toBeUndefined();
        });
    });

    describe('validateHeadless', () => {
        it('returns empty when headless is disabled', () => {
            const config = clone(DEFAULT_CONFIG);
            config.headless.enabled = false;
            expect(validateHeadless(config)).toEqual({});
        });

        it('flags missing trusted IdPs when enabled', () => {
            const config = clone(DEFAULT_CONFIG);
            config.headless.enabled = true;
            config.headless.trustedIdps = [];
            const errors = validateHeadless(config);
            expect(errors['headless.trustedIdps']).toBeDefined();
        });

        it('returns empty when headless is enabled with at least one IdP', () => {
            const config = clone(DEFAULT_CONFIG);
            config.headless.enabled = true;
            config.headless.trustedIdps = [
                {
                    id: '1',
                    name: 'Test',
                    enabled: true,
                    discoveryUrl: '',
                    discoveryStatus: 'idle',
                    issuer: 'https://idp.example',
                    jwksUrl: 'https://idp.example/jwks',
                    audience: '',
                    algs: ['RS256'],
                    claimEmail: 'email',
                    claimFirstName: 'given_name',
                    claimLastName: 'family_name',
                    claimGroups: 'groups',
                    autoProvision: true,
                    syncOnExchange: true,
                    defaultRoles: [],
                    roleBehavior: 'sync-all',
                    groupMappings: []
                }
            ];
            expect(validateHeadless(config)).toEqual({});
        });
    });

    describe('fromView — OIDC', () => {
        const OIDC_VIEW: DotAuthConfigView = {
            hostId: 'SYSTEM_HOST',
            protocol: 'OAUTH',
            configured: true,
            inherited: false,
            values: {
                enabled: true,
                enableBackend: true,
                enableFrontend: false,
                hashUserId: true,
                callbackUrl: 'https://cms.example/callback',
                issuerUrl: 'https://idp.example',
                clientId: 'my-client',
                clientSecret: '****',
                scopes: 'openid email profile groups',
                authorizationUrl: 'https://idp.example/auth',
                tokenUrl: 'https://idp.example/token',
                userinfoUrl: 'https://idp.example/userinfo',
                logoutUrl: 'https://idp.example/logout',
                groupsClaim: 'roles',
                emailClaim: 'email',
                firstNameClaim: 'first',
                lastNameClaim: 'last',
                autoProvision: false,
                extraRoles: 'Editor,Reviewer',
                buildRolesStrategy: 'staticadd',
                groupMappings: JSON.stringify([{ idpGroup: 'admins', dotcmsRole: 'Admin' }])
            },
            headlessValues: {}
        };

        it('maps protocol to oidc', () => {
            const config = fromView(OIDC_VIEW);
            expect(config.protocol).toBe('oidc');
            expect(config.ssoEnabled).toBe(true);
        });

        it('maps OIDC connection fields', () => {
            const config = fromView(OIDC_VIEW);
            expect(config.oidc.issuer).toBe('https://idp.example');
            expect(config.oidc.clientId).toBe('my-client');
            expect(config.oidc.clientSecret).toBe('****');
            expect(config.oidc.authUrl).toBe('https://idp.example/auth');
            expect(config.oidc.tokenUrl).toBe('https://idp.example/token');
        });

        it('maps OIDC claim fields', () => {
            const config = fromView(OIDC_VIEW);
            expect(config.oidc.claimGroups).toBe('roles');
            expect(config.oidc.claimEmail).toBe('email');
            expect(config.oidc.claimFirstName).toBe('first');
            expect(config.oidc.claimLastName).toBe('last');
        });

        it('maps OIDC provisioning fields', () => {
            const config = fromView(OIDC_VIEW);
            expect(config.oidc.autoProvision).toBe(false);
            expect(config.oidc.defaultRoles).toEqual(['Editor', 'Reviewer']);
            expect(config.oidc.roleBehavior).toBe('additive');
            expect(config.oidc.groupMappings).toEqual([
                { idpGroup: 'admins', dotcmsRole: 'Admin' }
            ]);
        });

        it('maps top-level login behavior fields', () => {
            const config = fromView(OIDC_VIEW);
            expect(config.enableBackend).toBe(true);
            expect(config.enableFrontend).toBe(false);
            expect(config.callbackUrl).toBe('https://cms.example/callback');
        });

        it('sets protocol none when not configured and not inherited', () => {
            const view: DotAuthConfigView = {
                ...OIDC_VIEW,
                configured: false,
                inherited: false
            };
            expect(fromView(view).protocol).toBe('none');
        });
    });

    describe('fromView — SAML', () => {
        const SAML_VIEW: DotAuthConfigView = {
            hostId: 'SYSTEM_HOST',
            protocol: 'SAML',
            configured: true,
            inherited: false,
            values: {
                enable: true,
                idpName: 'Okta',
                sPIssuerURL: 'https://cms.example',
                sPEndpointHostname: '',
                signatureValidationType: 'responseandassertion',
                idPMetadataFile: 'https://idp.example/metadata.xml',
                publicCert: 'MIIC...',
                privateKey: '****',
                'identity.provider.destinationsso.url': 'https://idp.example/sso',
                'identity.provider.destinationslo.url': 'https://idp.example/slo',
                'attribute.email.name': 'mail',
                'attribute.firstname.name': 'givenName',
                'attribute.lastname.name': 'sn',
                'attribute.roles.name': 'memberOf',
                'allow.user.synchronization': 'true',
                'login.email.update': 'false',
                'role.extra': 'Reviewer,Author',
                'build.roles': 'idp',
                groupMappings: JSON.stringify([]),
                enableBackend: 'true',
                enableFrontend: 'true',
                customSamlProp: 'customValue'
            },
            headlessValues: {}
        };

        it('maps protocol to saml', () => {
            const config = fromView(SAML_VIEW);
            expect(config.protocol).toBe('saml');
            expect(config.ssoEnabled).toBe(true);
        });

        it('maps SAML service provider fields', () => {
            const config = fromView(SAML_VIEW);
            expect(config.saml.entityId).toBe('https://cms.example');
            expect(config.saml.metadataUrl).toBe('https://idp.example/metadata.xml');
            expect(config.saml.x509cert).toBe('MIIC...');
            expect(config.saml.privateKey).toBe('****');
        });

        it('maps SAML IdP endpoint fields', () => {
            const config = fromView(SAML_VIEW);
            expect(config.saml.ssoUrl).toBe('https://idp.example/sso');
            expect(config.saml.sloUrl).toBe('https://idp.example/slo');
        });

        it('maps signature validation to individual booleans', () => {
            const config = fromView(SAML_VIEW);
            expect(config.saml.wantAssertionsSigned).toBe(true);
            expect(config.saml.wantResponseSigned).toBe(true);
        });

        it('maps SAML claim fields', () => {
            const config = fromView(SAML_VIEW);
            expect(config.saml.claimEmail).toBe('mail');
            expect(config.saml.claimFirstName).toBe('givenName');
            expect(config.saml.claimLastName).toBe('sn');
            expect(config.saml.claimGroups).toBe('memberOf');
        });

        it('maps SAML provisioning fields', () => {
            const config = fromView(SAML_VIEW);
            expect(config.saml.autoProvision).toBe(true);
            expect(config.saml.syncOnLogin).toBe(false);
            expect(config.saml.defaultRoles).toEqual(['Reviewer', 'Author']);
            expect(config.saml.roleBehavior).toBe('idp-only');
        });

        it('extracts non-declared keys as extraProperties', () => {
            const config = fromView(SAML_VIEW);
            expect(config.saml.extraProperties).toEqual([
                { key: 'customSamlProp', value: 'customValue' }
            ]);
        });
    });

    describe('fromView — headless values', () => {
        it('parses headless config from headlessValues', () => {
            const view: DotAuthConfigView = {
                hostId: 'SYSTEM_HOST',
                protocol: 'OAUTH',
                configured: true,
                inherited: false,
                values: { enabled: true, clientId: 'c', clientSecret: 's', issuerUrl: 'i' },
                headlessValues: {
                    enabled: true,
                    sessionRefTtlMinutes: '120',
                    clampToIdpExp: false,
                    allowedOrigins: JSON.stringify(['https://a.example']),
                    trustedIdps: JSON.stringify([
                        {
                            id: '1',
                            name: 'T',
                            enabled: true,
                            issuer: 'i',
                            jwksUrl: 'j',
                            algs: ['RS256']
                        }
                    ])
                }
            };
            const config = fromView(view);
            expect(config.headless.enabled).toBe(true);
            expect(config.headless.sessionRefTtlMinutes).toBe(120);
            expect(config.headless.clampToIdpExp).toBe(false);
            expect(config.headless.allowedOrigins).toEqual(['https://a.example']);
            expect(config.headless.trustedIdps).toHaveLength(1);
        });

        it('uses defaults when headlessValues is empty', () => {
            const view: DotAuthConfigView = {
                hostId: 'h',
                protocol: 'OAUTH',
                configured: false,
                inherited: false,
                values: {},
                headlessValues: {}
            };
            const config = fromView(view);
            expect(config.headless.enabled).toBe(false);
            expect(config.headless.sessionRefTtlMinutes).toBe(60);
            expect(config.headless.clampToIdpExp).toBe(true);
        });
    });

    describe('toPayload — OIDC', () => {
        it('produces an OAUTH payload with correct field mapping', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'oidc';
            config.ssoEnabled = true;
            config.oidc.issuer = 'https://idp.example';
            config.oidc.clientId = 'client';
            config.oidc.clientSecret = 'secret';
            config.oidc.scopes = 'openid';
            config.oidc.defaultRoles = ['Editor'];
            config.oidc.roleBehavior = 'idp-only';

            const payload = toPayload(config, 'test-site-id');
            expect(payload.protocol).toBe('OAUTH');
            expect(payload.values.issuerUrl).toBe('https://idp.example');
            expect(payload.values.clientId).toBe('client');
            expect(payload.values.clientSecret).toBe('secret');
            expect(payload.values.extraRoles).toBe('Editor');
            expect(payload.values.buildRolesStrategy).toBe('idp');
        });

        it('round-trips revocationUrl and groupsUrl so a save never deletes the stored secrets', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'oidc';
            config.oidc.revocationUrl = 'https://idp.example/revoke';
            config.oidc.groupsUrl = 'https://idp.example/groups';

            const payload = toPayload(config, 'test-site-id');
            expect(payload.values.revocationUrl).toBe('https://idp.example/revoke');
            expect(payload.values.groupsUrl).toBe('https://idp.example/groups');
        });

        it('omits revocationUrl and groupsUrl when unset', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'oidc';

            const payload = toPayload(config, 'test-site-id');
            expect(payload.values.revocationUrl).toBeUndefined();
            expect(payload.values.groupsUrl).toBeUndefined();
        });
    });

    describe('toPayload — SAML', () => {
        it('produces a SAML payload with correct field mapping', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';
            config.ssoEnabled = true;
            config.saml.entityId = 'https://cms.example';
            config.saml.metadataUrl = 'https://idp/meta';
            config.saml.wantAssertionsSigned = true;
            config.saml.wantResponseSigned = true;
            config.saml.defaultRoles = ['A', 'B'];
            config.saml.roleBehavior = 'static-only';
            config.saml.extraProperties = [{ key: 'extra', value: 'val' }];

            const payload = toPayload(config, 'test-site-id');
            expect(payload.protocol).toBe('SAML');

            const vals = payload.values as Record<string, unknown>;
            expect(vals['sPIssuerURL']).toBe('https://cms.example');
            expect(vals['signatureValidationType']).toBe('responseandassertion');
            expect(vals['role.extra']).toBe('A,B');
            expect(vals['build.roles']).toBe('staticonly');
            expect(vals['extra']).toBe('val');
        });

        it('maps signature validation combinations correctly', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';
            config.ssoEnabled = true;

            config.saml.wantAssertionsSigned = true;
            config.saml.wantResponseSigned = false;
            expect(
                (toPayload(config, 'test-site-id').values as Record<string, unknown>)['signatureValidationType']
            ).toBe('assertion');

            config.saml.wantAssertionsSigned = false;
            config.saml.wantResponseSigned = true;
            expect(
                (toPayload(config, 'test-site-id').values as Record<string, unknown>)['signatureValidationType']
            ).toBe('response');

            config.saml.wantAssertionsSigned = false;
            config.saml.wantResponseSigned = false;
            expect(
                (toPayload(config, 'test-site-id').values as Record<string, unknown>)['signatureValidationType']
            ).toBe('none');
        });

        it('round-trips idpName and sPEndpointHostname so a save never wipes a working SP hostname', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';
            config.saml.idpName = 'Okta';
            config.saml.spEndpointHostname = 'auth.customer.com';

            const vals = toPayload(config, 'test-site-id').values as Record<string, unknown>;
            expect(vals['idpName']).toBe('Okta');
            expect(vals['sPEndpointHostname']).toBe('auth.customer.com');
        });

        it('omits sPEndpointHostname when unset so the backend host-name fallback stays intact', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';
            config.saml.spEndpointHostname = '';

            const vals = toPayload(config, 'test-site-id').values as Record<string, unknown>;
            expect(vals['sPEndpointHostname']).toBeUndefined();
        });

        it('serializes signRequests so the toggle persists', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';
            config.saml.signRequests = false;

            const vals = toPayload(config, 'test-site-id').values as Record<string, unknown>;
            expect(vals['signRequests']).toBe('false');
        });

        it('persists the Hash user ID toggle under the SAML runtime key hash.userid', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';
            config.hashUserId = false;

            const vals = toPayload(config, 'test-site-id').values as Record<string, unknown>;
            expect(vals['hash.userid']).toBe('false');
        });

        it('substitutes the real site id into buttonParam instead of the $siteId template', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'saml';

            const vals = toPayload(config, 'abc-123').values as Record<string, unknown>;
            expect(vals['buttonParam']).toBe('/api/v1/dotsaml/metadata/abc-123');
        });
    });

    describe('fromView/toPayload SAML round-trip', () => {
        it('preserves loaded idpName and sPEndpointHostname across an unrelated edit', () => {
            const view: DotAuthConfigView = {
                hostId: 'SYSTEM_HOST',
                protocol: 'SAML',
                configured: true,
                inherited: false,
                values: {
                    enable: true,
                    idpName: 'Corp IdP',
                    sPEndpointHostname: 'auth.customer.com',
                    sPIssuerURL: 'https://cms.example',
                    signRequests: 'false'
                },
                headlessValues: {}
            };
            const config = fromView(view);
            const vals = toPayload(config, 'test-site-id').values as Record<string, unknown>;
            expect(vals['idpName']).toBe('Corp IdP');
            expect(vals['sPEndpointHostname']).toBe('auth.customer.com');
            expect(vals['signRequests']).toBe('false');
            // and signRequests must not leak into extraProperties as a custom attribute
            expect(config.saml.extraProperties).toEqual([]);
        });

        it('round-trips a stored hash.userid=false without leaking it into extraProperties', () => {
            const view: DotAuthConfigView = {
                hostId: 'SYSTEM_HOST',
                protocol: 'SAML',
                configured: true,
                inherited: false,
                values: {
                    enable: true,
                    idpName: 'Corp IdP',
                    'hash.userid': 'false'
                } as never,
                headlessValues: {}
            };
            const config = fromView(view);
            expect(config.hashUserId).toBe(false);
            expect(config.saml.extraProperties).toEqual([]);

            const vals = toPayload(config, 'test-site-id').values as Record<string, unknown>;
            expect(vals['hash.userid']).toBe('false');
        });
    });

    describe('toHeadlessPayload', () => {
        it('serializes headless config with JSON-stringified arrays', () => {
            const config = clone(DEFAULT_CONFIG);
            config.headless.enabled = true;
            config.headless.sessionRefTtlMinutes = 120;
            config.headless.allowedOrigins = ['https://a.example'];
            config.headless.trustedIdps = [];

            const payload = toHeadlessPayload(config);
            expect(payload.enabled).toBe(true);
            expect(payload.sessionRefTtlMinutes).toBe('120');
            expect(payload.allowedOrigins).toBe('["https://a.example"]');
            expect(payload.trustedIdps).toBe('[]');
        });
    });

    describe('applyOidcDiscovery', () => {
        it('populates OIDC fields from discovery response', () => {
            const config = clone(DEFAULT_CONFIG);
            config.protocol = 'oidc';
            const discovery: DotAuthDiscoveryView = {
                issuer: 'https://discovered.example',
                authorizationEndpoint: 'https://discovered.example/auth',
                tokenEndpoint: 'https://discovered.example/token',
                jwksUri: 'https://discovered.example/jwks',
                userinfoEndpoint: 'https://discovered.example/userinfo',
                endSessionEndpoint: 'https://discovered.example/logout'
            };
            const result = applyOidcDiscovery(config, discovery);
            expect(result.oidc.discoveryStatus).toBe('ok');
            expect(result.oidc.issuer).toBe('https://discovered.example');
            expect(result.oidc.authUrl).toBe('https://discovered.example/auth');
            expect(result.oidc.tokenUrl).toBe('https://discovered.example/token');
            expect(result.oidc.jwksUrl).toBe('https://discovered.example/jwks');
            expect(result.oidc.userinfoUrl).toBe('https://discovered.example/userinfo');
            expect(result.oidc.logoutUrl).toBe('https://discovered.example/logout');
            expect(config.oidc.issuer).toBe('');
        });

        it('preserves existing values for missing discovery fields', () => {
            const config = clone(DEFAULT_CONFIG);
            config.oidc.issuer = 'https://original.example';
            const result = applyOidcDiscovery(config, {});
            expect(result.oidc.issuer).toBe('https://original.example');
        });
    });

    describe('applyTrustedDiscovery', () => {
        it('populates a trusted IdP from discovery', () => {
            const config = clone(DEFAULT_CONFIG);
            config.headless.trustedIdps = [
                {
                    id: '1',
                    name: 'Test',
                    enabled: true,
                    discoveryUrl: '',
                    discoveryStatus: 'loading',
                    issuer: '',
                    jwksUrl: '',
                    audience: '',
                    algs: [],
                    claimEmail: 'email',
                    claimFirstName: 'given_name',
                    claimLastName: 'family_name',
                    claimGroups: 'groups',
                    autoProvision: true,
                    syncOnExchange: true,
                    defaultRoles: [],
                    roleBehavior: 'sync-all',
                    groupMappings: []
                }
            ];
            const discovery: DotAuthDiscoveryView = {
                issuer: 'https://trusted.example',
                jwksUri: 'https://trusted.example/jwks',
                signingAlgs: ['RS256', 'ES256']
            };
            const result = applyTrustedDiscovery(config, 'headless.trustedIdps.0', discovery);
            expect(result.headless.trustedIdps[0].discoveryStatus).toBe('ok');
            expect(result.headless.trustedIdps[0].issuer).toBe('https://trusted.example');
            expect(result.headless.trustedIdps[0].jwksUrl).toBe('https://trusted.example/jwks');
            expect(result.headless.trustedIdps[0].algs).toEqual(['RS256', 'ES256']);
        });
    });

    describe('roleBehavior round-trip', () => {
        const BEHAVIORS: Array<[string, DotAuthConfig['oidc']['roleBehavior']]> = [
            ['all', 'sync-all'],
            ['idp', 'idp-only'],
            ['staticadd', 'additive'],
            ['staticonly', 'static-only'],
            ['none', 'none']
        ];

        it.each(BEHAVIORS)(
            'OIDC: backend "%s" round-trips through UI "%s"',
            (backendValue, uiValue) => {
                const view: DotAuthConfigView = {
                    hostId: 'h',
                    protocol: 'OAUTH',
                    configured: true,
                    inherited: false,
                    values: { buildRolesStrategy: backendValue },
                    headlessValues: {}
                };
                const config = fromView(view);
                expect(config.oidc.roleBehavior).toBe(uiValue);

                config.protocol = 'oidc';
                config.ssoEnabled = true;
                const payload = toPayload(config, 'test-site-id');
                expect(payload.values.buildRolesStrategy).toBe(backendValue);
            }
        );

        it.each(BEHAVIORS)(
            'SAML: backend "%s" round-trips through UI "%s"',
            (backendValue, uiValue) => {
                const view: DotAuthConfigView = {
                    hostId: 'h',
                    protocol: 'SAML',
                    configured: true,
                    inherited: false,
                    values: { 'build.roles': backendValue },
                    headlessValues: {}
                };
                const config = fromView(view);
                expect(config.saml.roleBehavior).toBe(uiValue);

                config.protocol = 'saml';
                config.ssoEnabled = true;
                const payload = toPayload(config, 'test-site-id');
                expect((payload.values as Record<string, unknown>)['build.roles']).toBe(
                    backendValue
                );
            }
        );
    });
});
