import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotAuthService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAuthSitesView } from '@dotcms/dotcms-models';

import { DotAuthListStore } from './dot-auth-list.store';

const FIXTURE: DotAuthSitesView = {
    system: { configured: true, protocol: 'SAML', headlessConfigured: false },
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
        // Force the withHooks onInit effect to fire deterministically; otherwise
        // the first assertions race the scheduler.
        spectator.flushEffects();
    });

    describe('initial state', () => {
        it('populates the system row from the REST view after onInit', () => {
            expect(store.system()).toEqual({
                configured: true,
                protocol: 'SAML',
                headlessConfigured: false
            });
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

        it('resets status to loaded on clearConfig error', () => {
            service.clearConfig.mockReturnValue(throwError(() => new Error('clear fail')));
            store.clearSite('1');

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
            expect(store.status()).toBe('loaded');
        });
    });

    describe('loadSites error path', () => {
        // Restore the happy-path mock in afterEach so the next describe block
        // starts from a clean state — otherwise the throwError() below poisons
        // the shared jest.fn() and later tests see errors during onInit.
        afterEach(() => {
            service.listSites.mockReturnValue(of(FIXTURE));
        });

        it('resets status to loaded so the list stays usable', () => {
            service.listSites.mockReturnValue(throwError(() => new Error('fail')));
            store.loadSites();

            expect(store.status()).toBe('loaded');
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalled();
        });
    });
});
