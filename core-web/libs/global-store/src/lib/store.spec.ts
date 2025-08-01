import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotSiteService } from '@dotcms/data-access';
import { SiteEntity } from '@dotcms/dotcms-models';

import { GlobalStore } from './store';
import { mockSiteEntity, mockUserData, mockUserDataAlt } from './store.mock';

describe('GlobalStore', () => {
    let spectator: SpectatorService<InstanceType<typeof GlobalStore>>;
    let store: InstanceType<typeof GlobalStore>;

    const createService = createServiceFactory({
        service: GlobalStore,
        providers: [mockProvider(DotSiteService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    describe('Initial State', () => {
        it('should initialize with expected default values', () => {
            expect(store.user()).toBeNull();
            expect(store.siteDetails()).toBeNull();
            expect(store.isLoggedIn()).toBe(false);
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

    // TODO: Implement user management tests when user functionality is fully implemented
    describe('User Management', () => {
        describe('login method', () => {
            it('should update user state when login is called', () => {
                store.login(mockUserData);

                expect(store.user()).toEqual(mockUserData);
                expect(store.isLoggedIn()).toBe(true);
            });
        });

        describe('isLoggedIn computed', () => {
            it('should return false when user is null', () => {
                expect(store.isLoggedIn()).toBe(false);
            });

            it('should return true when user is set', () => {
                store.login(mockUserDataAlt);

                expect(store.isLoggedIn()).toBe(true);
            });
        });
    });
});
