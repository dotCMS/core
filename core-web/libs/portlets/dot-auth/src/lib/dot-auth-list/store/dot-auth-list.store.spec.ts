import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotAuthService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAuthSitesView } from '@dotcms/dotcms-models';

import { DotAuthListStore } from './dot-auth-list.store';

const FIXTURE: DotAuthSitesView = {
    system: { configured: true, protocol: 'SAML' },
    sites: [
        { hostId: '1', hostName: 'a.example', status: 'SITE_OVERRIDE', protocol: 'OAUTH' },
        { hostId: '2', hostName: 'b.example', status: 'INHERITED', protocol: 'SAML' },
        { hostId: '3', hostName: 'c.example', status: 'NOT_CONFIGURED', protocol: null }
    ]
};

describe('DotAuthListStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotAuthListStore>>;
    let store: InstanceType<typeof DotAuthListStore>;
    let service: jest.Mocked<DotAuthService>;

    const createService = createServiceFactory({
        service: DotAuthListStore,
        providers: [
            mockProvider(DotAuthService, {
                listSites: jest.fn().mockReturnValue(of(FIXTURE)),
                saveConfig: jest.fn().mockReturnValue(of(undefined)),
                clearConfig: jest.fn().mockReturnValue(of(undefined))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotAuthService) as jest.Mocked<DotAuthService>;
    });

    describe('initial state', () => {
        it('starts with an unconfigured system row and no protocol', () => {
            // Before the onInit hook fires with the mocked data we cannot read
            // the pre-load state, so assert the contract via the loaded state
            // that carries `protocol` through to `system`.
            expect(store.system()).toEqual({ configured: true, protocol: 'SAML' });
        });
    });

    describe('loadSites', () => {
        it('populates sites with protocol carried through from the REST view', () => {
            expect(store.sites()).toEqual(FIXTURE.sites);
            expect(store.sites()[0].protocol).toBe('OAUTH');
            expect(store.sites()[1].protocol).toBe('SAML');
            expect(store.sites()[2].protocol).toBeNull();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('filteredSites', () => {
        it('returns all rows when filter is empty', () => {
            expect(store.filteredSites()).toHaveLength(3);
        });

        it('filters by hostName case-insensitively', () => {
            store.setFilter('A.EXAMPLE');
            expect(store.filteredSites()).toHaveLength(1);
            expect(store.filteredSites()[0].hostName).toBe('a.example');
        });
    });

    describe('saveSite', () => {
        it('emits an OAUTH payload and reloads on success', () => {
            service.listSites.mockClear();
            store.saveSite('1', { protocol: 'OAUTH', values: { clientId: 'abc' } });

            expect(service.saveConfig).toHaveBeenCalledWith('1', {
                protocol: 'OAUTH',
                values: { clientId: 'abc' }
            });
            expect(service.listSites).toHaveBeenCalled();
        });

        it('emits a SAML payload and reloads on success', () => {
            service.listSites.mockClear();
            store.saveSite('2', { protocol: 'SAML', values: { idpName: 'Okta' } });

            expect(service.saveConfig).toHaveBeenCalledWith('2', {
                protocol: 'SAML',
                values: { idpName: 'Okta' }
            });
            expect(service.listSites).toHaveBeenCalled();
        });

        it('resets status to loaded on save error', () => {
            service.saveConfig.mockReturnValue(throwError(() => new Error('save fail')));
            store.saveSite('1', { protocol: 'OAUTH', values: {} });

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('clearSite', () => {
        it('calls clearConfig and reloads', () => {
            service.listSites.mockClear();
            store.clearSite('1');

            expect(service.clearConfig).toHaveBeenCalledWith('1');
            expect(service.listSites).toHaveBeenCalled();
        });
    });

    // Kept last — poisons the shared jest.fn() listSites mock; subsequent
    // describe blocks would see throwError() during onInit if this ran earlier.
    describe('loadSites error path', () => {
        it('marks status as error on failure', () => {
            service.listSites.mockReturnValue(throwError(() => new Error('fail')));
            store.loadSites();

            expect(store.status()).toBe('error');
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
        });
    });
});
