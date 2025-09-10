import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotSiteService, DotSystemConfigService } from '@dotcms/data-access';
import { SiteEntity } from '@dotcms/dotcms-models';

import { GlobalStore } from './store';
import { mockSiteEntity } from './store.mock';

describe('GlobalStore', () => {
    let spectator: SpectatorService<InstanceType<typeof GlobalStore>>;
    let store: InstanceType<typeof GlobalStore>;

    const createService = createServiceFactory({
        service: GlobalStore,
        providers: [mockProvider(DotSiteService), mockProvider(DotSystemConfigService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Initial State', () => {
        it('should initialize with expected default values', () => {
            expect(store.siteDetails()).toBeNull();
            expect(store.currentSiteId()).toBeNull();
        });
    });

    describe('Site Management', () => {
        it('should call DotSiteService.getCurrentSite when loadCurrentSite is invoked', () => {
            const mockService = spectator.inject(DotSiteService);
            mockService.getCurrentSite.mockReturnValue(of(mockSiteEntity as SiteEntity));

            store.loadCurrentSite();

            expect(mockService.getCurrentSite).toHaveBeenCalled();
        });

        it('should properly update store state through setCurrentSite (verifies rxMethod target behavior)', () => {
            // Initially store should be empty
            expect(store.siteDetails()).toBeNull();
            expect(store.currentSiteId()).toBeNull();

            store.setCurrentSite(mockSiteEntity);

            expect(store.siteDetails()).toEqual(mockSiteEntity);
            expect(store.currentSiteId()).toBe(mockSiteEntity.identifier);
        });
    });
});
