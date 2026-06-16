import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import {
    DotCurrentUserService,
    DotEventsSocket,
    DotSiteService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCurrentUser, DotSite } from '@dotcms/dotcms-models';

import { GlobalStore } from './store';
import { mockSiteEntity } from './store.mock';

const mockCurrentUser = {
    userId: 'user-123',
    email: 'john@example.com'
} as DotCurrentUser;

describe('GlobalStore', () => {
    let spectator: SpectatorService<InstanceType<typeof GlobalStore>>;
    let store: InstanceType<typeof GlobalStore>;
    let switchSiteSubject: Subject<DotSite>;
    // Callbacks registered via LoginService.watchUser() by the site/user features on init.
    // Invoking them simulates the LoginService notifying that a user is authenticated.
    let authCallbacks: Array<() => void>;

    const createService = createServiceFactory({
        service: GlobalStore,
        providers: [
            mockProvider(DotCurrentUserService, {
                getCurrentUser: jest.fn().mockReturnValue(of(mockCurrentUser))
            }),
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(null)),
                switchSite: jest.fn().mockReturnValue(of({} as DotSite))
            }),
            mockProvider(DotSystemConfigService),
            // watchUser captures the callback instead of firing it, so tests control exactly
            // when the "user authenticated" signal happens (no implicit load at store init).
            mockProvider(LoginService, {
                watchUser: jest.fn((fn: () => void) => authCallbacks.push(fn))
            }),
            mockProvider(DotEventsSocket, {
                connect: () => of({}),
                status$: () => new Subject(),
                on: jest.fn().mockImplementation((event: string) => {
                    if (event === 'SWITCH_SITE') return switchSiteSubject.asObservable();

                    return new Subject();
                })
            })
        ]
    });

    beforeEach(() => {
        // switchSiteSubject is assigned before createService() so the on() closure
        // captures the correct subject by the time onInit subscribes to SWITCH_SITE.
        switchSiteSubject = new Subject<DotSite>();
        authCallbacks = [];
        spectator = createService();
        store = spectator.service;
    });

    describe('Initial State', () => {
        it('should initialize with expected default values', () => {
            expect(store.siteDetails()).toBeNull();
            expect(store.currentSiteId()).toBeNull();
        });

        it('should NOT load site or user before the user is authenticated', () => {
            const siteService = spectator.inject(DotSiteService);
            const userService = spectator.inject(DotCurrentUserService);

            // watchUser() has registered callbacks but they have not fired yet.
            expect(siteService.getCurrentSite).not.toHaveBeenCalled();
            expect(userService.getCurrentUser).not.toHaveBeenCalled();
            expect(store.siteDetails()).toBeNull();
            expect(store.loggedUser()).toBeNull();
        });
    });

    describe('auth-reactive loading', () => {
        it('should load the current site and user when the auth state becomes a logged-in user', () => {
            const siteService = spectator.inject(DotSiteService);
            const userService = spectator.inject(DotCurrentUserService);
            siteService.getCurrentSite.mockReturnValue(of(mockSiteEntity));

            // Simulate LoginService notifying subscribers that a user is authenticated.
            authCallbacks.forEach((cb) => cb());

            expect(siteService.getCurrentSite).toHaveBeenCalled();
            expect(userService.getCurrentUser).toHaveBeenCalled();
            expect(store.siteDetails()).toEqual(mockSiteEntity);
            expect(store.loggedUser()).toEqual(mockCurrentUser);
        });

        it('should reload state on every auth notification (e.g. re-login)', () => {
            const siteService = spectator.inject(DotSiteService);
            // Reset call count: the getCurrentSite mock instance is shared across tests.
            siteService.getCurrentSite.mockClear();
            siteService.getCurrentSite.mockReturnValue(of(mockSiteEntity));

            authCallbacks.forEach((cb) => cb());
            authCallbacks.forEach((cb) => cb());

            // getCurrentSite fires once per notification (2 notifications here).
            expect(siteService.getCurrentSite).toHaveBeenCalledTimes(2);
        });
    });

    describe('loadCurrentSite()', () => {
        it('should call DotSiteService.getCurrentSite and update state', () => {
            const siteService = spectator.inject(DotSiteService);
            siteService.getCurrentSite.mockReturnValue(of(mockSiteEntity));

            store.loadCurrentSite();

            expect(siteService.getCurrentSite).toHaveBeenCalled();
            expect(store.siteDetails()).toEqual(mockSiteEntity);
            expect(store.currentSiteId()).toBe(mockSiteEntity.identifier);
        });
    });

    describe('switchCurrentSite()', () => {
        it('should call switchSite then getCurrentSite and update siteDetails', () => {
            const siteService = spectator.inject(DotSiteService);
            const newSite: DotSite = { ...mockSiteEntity, identifier: 'new-site' };
            siteService.switchSite.mockReturnValue(of({} as DotSite));
            siteService.getCurrentSite.mockReturnValue(of(newSite));

            store.switchCurrentSite('new-site');

            expect(siteService.switchSite).toHaveBeenCalledWith('new-site');
            expect(siteService.getCurrentSite).toHaveBeenCalled();
            expect(store.siteDetails()).toEqual(newSite);
        });
    });

    describe('SWITCH_SITE WebSocket event', () => {
        it('should update siteDetails when SWITCH_SITE event fires', () => {
            switchSiteSubject.next(mockSiteEntity);

            expect(store.siteDetails()).toEqual(mockSiteEntity);
        });
    });
});
