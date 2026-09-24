import { byText, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { Mock, MockInstance, Mocked, vi } from 'vitest';

import { signal, WritableSignal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DOT_AUTH_SYSTEM_HOST, DotAuthConfig, DotAuthSamlUiConfig } from '@dotcms/dotcms-models';
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
                saveSso: vi.fn().mockReturnValue(true),
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
                hostName: vi.fn().mockReturnValue(''),
                draft: signal<DotAuthConfig>(DRAFT),
                original: signal<DotAuthConfig>(DRAFT),
                configured: vi.fn().mockReturnValue(true),
                inherited: vi.fn().mockReturnValue(false),
                status: signal('loaded'),
                reloadFailed: signal(false),
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
                    'dotauth.confirm.google-groups.header': 'Google Workspace detected',
                    'dotauth.confirm.regenerate-keypair.header': 'Generate a new SP keypair?',
                    'dotauth.toast.saved': 'dotAuth changes saved',
                    'dotauth.toast.saved.keypair-regenerated':
                        'dotAuth changes saved. A new SP keypair was generated.'
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

    describe('SAML keypair regeneration', () => {
        let store: Mocked<InstanceType<typeof DotAuthConfigStore>>;
        let confirmSpy: MockInstance;
        let toastSpy: MockInstance;

        const withSaml = (saml: Partial<DotAuthSamlUiConfig>): DotAuthConfig => ({
            ...DRAFT,
            protocol: 'saml',
            saml: { ...DRAFT.saml, entityId: 'https://cms.example.com', ...saml }
        });
        const STORED = { x509cert: 'STORED-CERT', privateKey: '****' };
        const CLEARED = { x509cert: '', privateKey: '' };

        beforeEach(() => {
            store = spectator.inject(DotAuthConfigStore, true) as unknown as Mocked<
                InstanceType<typeof DotAuthConfigStore>
            >;
            confirmSpy = vi.spyOn(spectator.inject(ConfirmationService), 'confirm');
            toastSpy = vi.spyOn(spectator.inject(MessageService, true), 'add');
            // mockProvider creates the vi.fn() instances once per factory, so clear per test.
            (store.saveSso as unknown as Mock).mockClear();
            (store.original as unknown as WritableSignal<DotAuthConfig>).set(withSaml(STORED));
            spectator.detectChanges();
        });

        it('asks before regenerating when both key fields are cleared on a stored keypair', () => {
            (store.draft as unknown as WritableSignal<DotAuthConfig>).set(withSaml(CLEARED));

            spectator.component.save();

            expect(store.saveSso).not.toHaveBeenCalled();
            expect(confirmSpy).toHaveBeenCalledTimes(1);
            expect(confirmSpy.mock.calls[0][0].header).toBe('Generate a new SP keypair?');
            confirmSpy.mock.calls[0][0].accept();
            expect(store.saveSso).toHaveBeenCalledWith({ regenerateKeypair: true });
        });

        it('saves without asking when the stored keypair is kept', () => {
            (store.draft as unknown as WritableSignal<DotAuthConfig>).set(withSaml(STORED));

            spectator.component.save();

            expect(confirmSpy).not.toHaveBeenCalled();
            expect(store.saveSso).toHaveBeenCalledTimes(1);
            expect(store.saveSso.mock.calls[0][0]).toBeUndefined();
        });

        it('saves without asking when nothing is stored yet', () => {
            (store.original as unknown as WritableSignal<DotAuthConfig>).set(withSaml(CLEARED));
            (store.draft as unknown as WritableSignal<DotAuthConfig>).set(withSaml(CLEARED));

            spectator.component.save();

            expect(confirmSpy).not.toHaveBeenCalled();
            expect(store.saveSso).toHaveBeenCalledTimes(1);
            expect(store.saveSso.mock.calls[0][0]).toBeUndefined();
        });

        it('says a new keypair was generated in the success toast', () => {
            (store.draft as unknown as WritableSignal<DotAuthConfig>).set(withSaml(CLEARED));
            spectator.component.save();
            confirmSpy.mock.calls[0][0].accept();

            (store.status as unknown as WritableSignal<string>).set('saving');
            spectator.detectChanges();
            (store.status as unknown as WritableSignal<string>).set('loaded');
            spectator.detectChanges();

            expect(toastSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    severity: 'success',
                    summary: 'dotAuth changes saved. A new SP keypair was generated.'
                })
            );
        });

        it('keeps the plain saved toast for an ordinary save', () => {
            (store.draft as unknown as WritableSignal<DotAuthConfig>).set(withSaml(STORED));
            spectator.component.save();

            (store.status as unknown as WritableSignal<string>).set('saving');
            spectator.detectChanges();
            (store.status as unknown as WritableSignal<string>).set('loaded');
            spectator.detectChanges();

            expect(toastSpy).toHaveBeenCalledWith(
                expect.objectContaining({ summary: 'dotAuth changes saved' })
            );
        });
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
