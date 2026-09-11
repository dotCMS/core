import { byText, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { Mock, MockInstance, Mocked, vi } from 'vitest';

import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AUTH_SYSTEM_HOST, DotAuthConfig } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAuthConfigComponent } from './dot-auth-config.component';
import { DotAuthConfigStore } from './store/dot-auth-config.store';

const DRAFT: DotAuthConfig = {
    ssoEnabled: true,
    protocol: 'oidc',
    enableBackend: true,
    enableFrontend: false,
    hashUserId: true,
    callbackUrl: 'http://localhost:8080',
    oidc: {
        discoveryUrl: 'https://idp.example/.well-known/openid-configuration',
        discoveryStatus: 'idle',
        issuer: 'https://idp.example',
        authUrl: 'https://idp.example/auth',
        tokenUrl: 'https://idp.example/token',
        jwksUrl: 'https://idp.example/jwks',
        userinfoUrl: 'https://idp.example/userinfo',
        logoutUrl: '',
        clientId: 'dotcms',
        clientSecret: '****',
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
        defaultRoles: ['Frontend Editor'],
        roleBehavior: 'sync-all',
        groupMappings: [{ idpGroup: 'editors', dotcmsRole: 'Frontend Editor' }],
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
        roleBehavior: 'sync-all',
        groupMappings: [],
        sessionTtlMinutes: 60
    },
    headless: {
        enabled: true,
        sessionRefTtlMinutes: 60,
        clampToIdpExp: true,
        allowedOrigins: ['https://app.example'],
        trustedIdps: [
            {
                id: 'idp-1',
                name: 'Marketing IdP',
                enabled: true,
                discoveryUrl: '',
                discoveryStatus: 'idle',
                issuer: 'https://idp.example',
                jwksUrl: 'https://idp.example/jwks',
                audience: 'dotcms',
                algs: ['RS256'],
                claimEmail: 'email',
                claimFirstName: 'given_name',
                claimLastName: 'family_name',
                claimGroups: 'groups',
                autoProvision: true,
                syncOnExchange: true,
                defaultRoles: ['Frontend Reader'],
                roleBehavior: 'sync-all',
                groupMappings: []
            }
        ]
    }
};

describe('DotAuthConfigComponent', () => {
    let spectator: Spectator<DotAuthConfigComponent>;

    const createComponent = createComponentFactory({
        component: DotAuthConfigComponent,
        componentProviders: [
            mockProvider(DotAuthConfigStore, {
                load: vi.fn(),
                saveSso: vi.fn(),
                reset: vi.fn(),
                clearOverride: vi.fn(),
                update: vi.fn(),
                setProtocol: vi.fn(),
                runOidcDiscovery: vi.fn(),
                applyGoogleGroupsPreset: vi.fn(),
                dismissGooglePrefill: vi.fn(),
                revokeAllSessionRefs: vi.fn(),
                addAllowedOrigin: vi.fn(),
                removeAllowedOrigin: vi.fn(),
                addTrustedIdp: vi.fn(),
                removeTrustedIdp: vi.fn(),
                siteId: vi.fn().mockReturnValue(DOT_AUTH_SYSTEM_HOST),
                draft: vi.fn().mockReturnValue(DRAFT),
                original: vi.fn().mockReturnValue(DRAFT),
                configured: vi.fn().mockReturnValue(true),
                inherited: vi.fn().mockReturnValue(false),
                status: vi.fn().mockReturnValue('loaded'),
                errors: vi.fn().mockReturnValue({}),
                errorCount: vi.fn().mockReturnValue(0),
                dirty: vi.fn().mockReturnValue(false),
                ssoDirty: vi.fn().mockReturnValue(false),
                isSystem: vi.fn().mockReturnValue(true),
                googlePrefillPending: vi.fn().mockReturnValue(false)
            })
        ],
        providers: [
            ConfirmationService,
            MessageService,
            mockProvider(Router),
            {
                provide: ActivatedRoute,
                useValue: { snapshot: { paramMap: { get: () => DOT_AUTH_SYSTEM_HOST } } }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dotauth.config.sso.title': 'Single sign-on',
                    'dotauth.config.headless.title': 'Headless token exchange',
                    'dotauth.config.trusted-idps.title': 'Trusted IdPs',
                    'dotauth.confirm.google-groups.header': 'Google Workspace detected'
                })
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
    });

    it('loads the route host and renders the SSO track by default', () => {
        spectator.detectChanges();
        expect(spectator.component.store.load).toHaveBeenCalledWith(DOT_AUTH_SYSTEM_HOST);
        expect(spectator.query(byText('Single sign-on'))).toExist();
    });

    it('renders SSO content without tab selection (tabs removed)', () => {
        spectator.detectChanges();
        expect(spectator.query(byText('Single sign-on'))).toExist();
    });

    describe('Google Workspace groups pre-fill offer', () => {
        let store: Mocked<InstanceType<typeof DotAuthConfigStore>>;
        let confirmSpy: MockInstance;

        beforeEach(() => {
            store = spectator.inject(DotAuthConfigStore, true) as unknown as Mocked<
                InstanceType<typeof DotAuthConfigStore>
            >;
            confirmSpy = vi.spyOn(spectator.inject(ConfirmationService), 'confirm');
        });

        it('does not open the dialog when no Google discovery is pending', () => {
            spectator.detectChanges();
            expect(confirmSpy).not.toHaveBeenCalled();
        });

        it('opens the dialog once and clears the pending flag when Google is detected', () => {
            (store.googlePrefillPending as unknown as Mock).mockReturnValue(true);
            spectator.detectChanges();
            expect(store.dismissGooglePrefill).toHaveBeenCalledTimes(1);
            expect(confirmSpy).toHaveBeenCalledTimes(1);
            expect(confirmSpy.mock.calls[0][0].header).toBe('Google Workspace detected');
        });

        it('applies the preset on accept and nothing on reject', () => {
            (store.googlePrefillPending as unknown as Mock).mockReturnValue(true);
            spectator.detectChanges();
            const options = confirmSpy.mock.calls[0][0];
            options.reject?.();
            expect(store.applyGoogleGroupsPreset).not.toHaveBeenCalled();
            options.accept();
            expect(store.applyGoogleGroupsPreset).toHaveBeenCalledTimes(1);
        });
    });
});
