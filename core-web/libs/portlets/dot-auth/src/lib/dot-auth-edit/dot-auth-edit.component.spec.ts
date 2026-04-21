import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotAuthService, DotMessageService } from '@dotcms/data-access';
import {
    DotAuthConfigView,
    DotAuthConfigValues,
    DotAuthSamlConfigValues
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAuthEditComponent } from './dot-auth-edit.component';

const OAUTH_VALUES: DotAuthConfigValues = {
    enabled: true,
    enableBackend: true,
    enableFrontend: false,
    providerType: 'OIDC',
    issuerUrl: 'https://okta.example/oauth2/default',
    clientId: 'my-client',
    clientSecret: '****',
    scopes: 'openid email profile'
};

const SAML_VALUES: DotAuthSamlConfigValues = {
    enable: true,
    idpName: 'Okta',
    sPIssuerURL: 'https://sp.example',
    sPEndpointHostname: 'https://sp.example/sso',
    signatureValidationType: 'none',
    idPMetadataFile: '<xml/>',
    publicCert: 'PEM-public',
    privateKey: '****',
    buttonParam: '/api/v1/dotsaml/metadata/$siteId'
};

const MESSAGES = {
    Cancel: 'Cancel',
    Save: 'Save',
    Continue: 'Continue',
    'dotauth.protocol.oauth': 'OAuth 2.0 / OIDC',
    'dotauth.protocol.saml': 'SAML 2.0',
    'dotauth.confirm.switch-protocol.header': 'Switch protocol?',
    'dotauth.confirm.switch-protocol.message':
        'Saving as {0} will delete the existing {1} configuration for this site. Continue?',
    'dotauth.banner.system': 'System banner',
    'dotauth.banner.inheriting': 'Inheriting banner',
    'dotauth.fieldset.provider': 'Provider',
    'dotauth.fieldset.endpoints': 'Endpoints',
    'dotauth.fieldset.roles': 'Roles',
    'dotauth.fieldset.saml.idp': 'IDP Details',
    'dotauth.fieldset.saml.sp': 'SP Details',
    'dotauth.fieldset.saml.credentials': 'Credentials',
    'dotauth.field.enabled': 'Enabled',
    'dotauth.field.enableBackend': 'Backend',
    'dotauth.field.enableFrontend': 'Frontend',
    'dotauth.field.providerType': 'Provider',
    'dotauth.field.issuerUrl': 'Issuer',
    'dotauth.field.clientId': 'Client ID',
    'dotauth.field.clientSecret': 'Client Secret',
    'dotauth.field.scopes': 'Scopes',
    'dotauth.field.authorizationUrl': 'Authz',
    'dotauth.field.tokenUrl': 'Token',
    'dotauth.field.userinfoUrl': 'Userinfo',
    'dotauth.field.revocationUrl': 'Revocation',
    'dotauth.field.logoutUrl': 'Logout',
    'dotauth.field.groupsClaim': 'Groups Claim',
    'dotauth.field.groupsUrl': 'Groups URL',
    'dotauth.field.extraRoles': 'Extra Roles',
    'dotauth.field.callbackUrl': 'Callback',
    'dotauth.field.enable': 'Enabled',
    'dotauth.field.idpName': 'IDP Name',
    'dotauth.field.sPIssuerURL': 'SP Issuer',
    'dotauth.field.sPEndpointHostname': 'SP Endpoint',
    'dotauth.field.signatureValidationType': 'Sig Validation',
    'dotauth.field.idPMetadataFile': 'IDP Metadata',
    'dotauth.field.publicCert': 'Public Cert',
    'dotauth.field.privateKey': 'Private Key',
    'dotauth.field.buttonParam': 'Metadata URL'
};

function oauthView(extras: Partial<DotAuthConfigView> = {}): DotAuthConfigView {
    return {
        hostId: '1',
        protocol: 'OAUTH',
        configured: true,
        inherited: false,
        values: OAUTH_VALUES,
        ...extras
    } as DotAuthConfigView;
}

function samlView(extras: Partial<DotAuthConfigView> = {}): DotAuthConfigView {
    return {
        hostId: '1',
        protocol: 'SAML',
        configured: true,
        inherited: false,
        values: SAML_VALUES,
        ...extras
    } as DotAuthConfigView;
}

describe('DotAuthEditComponent', () => {
    let spectator: Spectator<DotAuthEditComponent>;
    let service: jest.Mocked<DotAuthService>;
    const dialogRef = { close: jest.fn() };

    const createComponent = createComponentFactory({
        component: DotAuthEditComponent,
        providers: [
            { provide: DynamicDialogRef, useValue: dialogRef },
            { provide: DynamicDialogConfig, useValue: { data: { hostId: '1' } } },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotAuthService, {
                getConfig: jest.fn().mockReturnValue(of(oauthView()))
            })
        ]
    });

    function build(view: DotAuthConfigView): Spectator<DotAuthEditComponent> {
        dialogRef.close.mockClear();
        const s = createComponent({ providers: [] });
        service = s.inject(DotAuthService) as jest.Mocked<DotAuthService>;
        service.getConfig.mockReturnValue(of(view));
        s.component.ngOnInit();
        s.detectChanges();
        return s;
    }

    describe('when protocol = OAUTH', () => {
        beforeEach(() => {
            spectator = build(oauthView());
        });

        it('selects OAUTH as the active protocol', () => {
            expect(spectator.component.selectedProtocol()).toBe('OAUTH');
        });

        it('pre-populates the OAuth form from view.values', () => {
            expect(spectator.component.oauthForm.getRawValue()).toMatchObject({
                clientId: 'my-client',
                issuerUrl: 'https://okta.example/oauth2/default',
                clientSecret: '****'
            });
        });

        it('emits an OAUTH payload on save', () => {
            spectator.component.save();
            expect(dialogRef.close).toHaveBeenCalledWith({
                protocol: 'OAUTH',
                values: expect.objectContaining({
                    clientId: 'my-client',
                    clientSecret: '****'
                })
            });
        });
    });

    describe('when protocol = SAML', () => {
        beforeEach(() => {
            spectator = build(samlView());
        });

        it('selects SAML as the active protocol', () => {
            expect(spectator.component.selectedProtocol()).toBe('SAML');
        });

        it('pre-populates the SAML fieldset from view.values', () => {
            expect(spectator.component.samlForm.getRawValue()).toMatchObject({
                idpName: 'Okta',
                signatureValidationType: 'none',
                privateKey: '****'
            });
        });

        it('emits a SAML payload on save', () => {
            spectator.component.save();
            expect(dialogRef.close).toHaveBeenCalledWith({
                protocol: 'SAML',
                values: expect.objectContaining({
                    idpName: 'Okta',
                    // Mask round-trips untouched so the backend preserves the stored value.
                    privateKey: '****'
                })
            });
        });

        it('posts the **** mask back untouched when the user never edits privateKey', () => {
            spectator.component.save();
            const payload = dialogRef.close.mock.calls[0][0];
            expect(payload.values.privateKey).toBe('****');
        });
    });

    describe('SAML custom attributes', () => {
        it('loads undeclared keys from view.values into the FormArray', () => {
            spectator = build(
                samlView({
                    values: {
                        ...SAML_VALUES,
                        emailAttribute: 'emailAddress',
                        rolesAttribute: 'groups'
                    }
                })
            );

            const rows = spectator.component.customAttributes.controls;
            const flat = rows.map((row) => row.getRawValue());
            expect(flat).toEqual(
                expect.arrayContaining([
                    { key: 'emailAttribute', value: 'emailAddress' },
                    { key: 'rolesAttribute', value: 'groups' }
                ])
            );
        });

        it('does not duplicate declared keys into the FormArray', () => {
            spectator = build(samlView());
            const keys = spectator.component.customAttributes.controls.map(
                (row) => row.get('key')?.value
            );
            expect(keys).not.toContain('idpName');
            expect(keys).not.toContain('privateKey');
        });

        it('flattens custom attributes into values on save', () => {
            spectator = build(samlView());
            spectator.component.addCustomAttribute('emailAttribute', 'emailAddress');
            spectator.component.addCustomAttribute('autoCreateUsers', 'true');

            spectator.component.save();

            const payload = dialogRef.close.mock.calls[0][0];
            expect(payload.values.emailAttribute).toBe('emailAddress');
            expect(payload.values.autoCreateUsers).toBe('true');
            expect(payload.values.idpName).toBe('Okta');
        });

        it('drops rows with empty keys on save', () => {
            spectator = build(samlView());
            spectator.component.addCustomAttribute('', 'orphan-value');

            spectator.component.save();

            // form.invalid from the empty-key row prevents close
            expect(dialogRef.close).not.toHaveBeenCalled();
        });

        it('removes a custom attribute row', () => {
            spectator = build(samlView());
            spectator.component.addCustomAttribute('k1', 'v1');
            spectator.component.addCustomAttribute('k2', 'v2');
            expect(spectator.component.customAttributes.length).toBe(2);

            spectator.component.removeCustomAttribute(0);

            expect(spectator.component.customAttributes.length).toBe(1);
            expect(
                spectator.component.customAttributes.at(0).get('key')?.value
            ).toBe('k2');
        });

        it('flags a duplicate key with duplicateKey error', () => {
            spectator = build(samlView());
            spectator.component.addCustomAttribute('emailAttribute', 'a');
            spectator.component.addCustomAttribute('emailAttribute', 'b');

            const second = spectator.component.customAttributes.at(1).get('key');
            expect(second?.errors).toEqual({ duplicateKey: true });
        });

        it('flags a reserved key with reservedKey error', () => {
            spectator = build(samlView());
            spectator.component.addCustomAttribute('idpName', 'would-override');

            const key = spectator.component.customAttributes.at(0).get('key');
            expect(key?.errors).toEqual({ reservedKey: true });
        });
    });

    describe('SAML metadata download', () => {
        it('substitutes $siteId into buttonParam for the metadata URL', () => {
            spectator = build(
                samlView({
                    values: { ...SAML_VALUES, buttonParam: '/api/v1/dotsaml/metadata/$siteId' }
                })
            );
            expect(spectator.component.metadataUrl()).toBe('/api/v1/dotsaml/metadata/1');
        });

        it('triggers a browser download on click', () => {
            spectator = build(samlView());
            const clickSpy = jest
                .spyOn(HTMLAnchorElement.prototype, 'click')
                .mockImplementation(() => {});

            spectator.component.downloadMetadata();

            expect(clickSpy).toHaveBeenCalled();
            clickSpy.mockRestore();
        });
    });

    describe('protocol switch confirmation', () => {
        it('switches without confirmation when nothing is stored and the form is clean', () => {
            // Not-configured host returns OAUTH shape with configured: false
            spectator = build(oauthView({ configured: false }));
            const confirm = spectator.inject(
                ConfirmationService,
                true
            ) as jest.Mocked<ConfirmationService>;
            confirm.confirm = jest.fn();

            spectator.component.onProtocolChange('SAML');

            expect(spectator.component.selectedProtocol()).toBe('SAML');
            expect(confirm.confirm).not.toHaveBeenCalled();
        });

        it('confirms before switching when the active form is dirty', () => {
            spectator = build(oauthView({ configured: false }));
            const confirm = spectator.inject(
                ConfirmationService,
                true
            ) as jest.Mocked<ConfirmationService>;
            confirm.confirm = jest.fn();

            spectator.component.oauthForm.markAsDirty();
            spectator.component.onProtocolChange('SAML');

            expect(confirm.confirm).toHaveBeenCalled();
            // Protocol doesn't change until the accept callback runs
            expect(spectator.component.selectedProtocol()).toBe('OAUTH');
        });

        it('confirms before switching when the stored protocol differs from the target', () => {
            spectator = build(samlView());
            const confirm = spectator.inject(
                ConfirmationService,
                true
            ) as jest.Mocked<ConfirmationService>;
            confirm.confirm = jest.fn();

            spectator.component.onProtocolChange('OAUTH');

            expect(confirm.confirm).toHaveBeenCalled();
            expect(spectator.component.selectedProtocol()).toBe('SAML');
        });

        it('applies the switch when the confirmation is accepted', () => {
            spectator = build(samlView());
            const confirm = spectator.inject(
                ConfirmationService,
                true
            ) as jest.Mocked<ConfirmationService>;
            confirm.confirm = jest.fn().mockImplementation((opts) => opts.accept?.());

            spectator.component.onProtocolChange('OAUTH');

            expect(spectator.component.selectedProtocol()).toBe('OAUTH');
        });
    });

    describe('template rendering', () => {
        it('renders the OAuth form when OAUTH is selected', () => {
            spectator = build(oauthView());
            expect(spectator.query(byTestId('dotauth-oauth-form'))).toBeTruthy();
            expect(spectator.query(byTestId('dotauth-saml-form'))).toBeFalsy();
        });

        it('renders the SAML form when SAML is selected', () => {
            spectator = build(samlView());
            expect(spectator.query(byTestId('dotauth-saml-form'))).toBeTruthy();
            expect(spectator.query(byTestId('dotauth-oauth-form'))).toBeFalsy();
        });
    });
});
