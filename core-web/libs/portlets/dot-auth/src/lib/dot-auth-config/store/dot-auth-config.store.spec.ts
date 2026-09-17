import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mocked, vi } from 'vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotAuthService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DOT_AUTH_SYSTEM_HOST, DotAuthConfigView } from '@dotcms/dotcms-models';

import { DotAuthConfigStore } from './dot-auth-config.store';

const SAML_VIEW: DotAuthConfigView = {
    hostId: DOT_AUTH_SYSTEM_HOST,
    protocol: 'SAML',
    configured: true,
    inherited: false,
    values: {
        enable: true,
        idpName: 'Okta',
        sPIssuerURL: 'https://cms.example.com',
        publicCert: 'STORED-CERT',
        privateKey: '****'
    },
    headlessValues: {}
};

describe('DotAuthConfigStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAuthConfigStore>>;
    let store: InstanceType<typeof DotAuthConfigStore>;
    let service: Mocked<DotAuthService>;
    let errorManager: Mocked<DotHttpErrorManagerService>;

    const createService = createServiceFactory({
        service: DotAuthConfigStore,
        providers: [
            mockProvider(DotAuthService, {
                getConfig: vi.fn().mockReturnValue(of(SAML_VIEW)),
                saveConfig: vi.fn().mockReturnValue(of(undefined))
            }),
            mockProvider(DotHttpErrorManagerService, {
                handle: vi.fn().mockReturnValue(of({ redirected: false, status: 400 }))
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotAuthService) as Mocked<DotAuthService>;
        errorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as Mocked<DotHttpErrorManagerService>;
        store.load(DOT_AUTH_SYSTEM_HOST);
        // mockProvider creates the vi.fn() instances once per factory, so clear per test.
        service.saveConfig.mockClear();
        errorManager.handle.mockClear();
    });

    const sentValues = () => service.saveConfig.mock.calls[0][1].values as Record<string, unknown>;

    describe('saveSso', () => {
        it('does not send the regenerate flag on an ordinary save', () => {
            store.saveSso();

            expect(sentValues().regenerateKeypair).toBeUndefined();
        });

        it('sends regenerateKeypair when the caller confirmed regeneration', () => {
            store.update('saml.x509cert', '');
            store.update('saml.privateKey', '');

            store.saveSso({ regenerateKeypair: true });

            expect(sentValues().regenerateKeypair).toBe(true);
            expect(sentValues().publicCert).toBeUndefined();
            expect(sentValues().privateKey).toBeUndefined();
        });

        it('hands a rejected save to the HTTP error manager so the server message is shown', () => {
            const rejected = new HttpErrorResponse({
                status: 400,
                error: { message: 'An SP keypair is already stored.' }
            });
            service.saveConfig.mockReturnValueOnce(throwError(() => rejected));

            store.saveSso();

            expect(errorManager.handle).toHaveBeenCalledWith(rejected);
            expect(errorManager.handle.mock.calls[0][0].error.message).toBe(
                'An SP keypair is already stored.'
            );
            expect(store.status()).toBe('error');
        });
    });
});
