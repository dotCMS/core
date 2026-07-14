import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { defer, Subject, of, throwError } from 'rxjs';

import {
    DotCurrentUserService,
    DotEventsSocket,
    DotSiteService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { Auth, LoginService } from '@dotcms/dotcms-js';
import { DotCurrentUser, DotSite } from '@dotcms/dotcms-models';

import { GlobalStore } from './store';
import { mockSiteEntity } from './store.mock';

const mockCurrentUser = {
    userId: 'user-123',
    email: 'john@example.com'
} as DotCurrentUser;

// Minimal Auth payload; the features only read `auth.user.userId` to key the reactive load.
const authForUser = (userId: string): Auth => ({ user: { userId } }) as unknown as Auth;

describe('GlobalStore', () => {
    let spectator: SpectatorService<InstanceType<typeof GlobalStore>>;
    let store: InstanceType<typeof GlobalStore>;
    let switchSiteSubject: Subject<DotSite>;
    // Auth stream the site/user features subscribe to. Emitting on it simulates the
    // LoginService notifying that the authenticated user changed (login / re-login).
    let authSubject: Subject<Auth>;

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
            // No session at init (`auth` null); the deferred `auth$` resolves to the current
            // per-test subject so tests control exactly when an authenticated user is signalled.
            mockProvider(LoginService, {
                auth: null as unknown as Auth,
                auth$: defer(() => authSubject)
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
        authSubject = new Subject<Auth>();
        // Reset shared mock call counts each test so assertions don't depend on test order.
        // clearAllMocks() clears recorded calls but preserves the mockReturnValue / jest.fn
        // implementations declared in createServiceFactory.
        jest.clearAllMocks();
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

            // The store subscribed to auth$ but no authenticated user has been emitted yet.
            expect(siteService.getCurrentSite).not.toHaveBeenCalled();
            expect(userService.getCurrentUser).not.toHaveBeenCalled();
            expect(store.siteDetails()).toBeNull();
            expect(store.loggedUser()).toBeNull();
        });
    });

    describe('auth-reactive loading', () => {
        it('should load the current site and user when a user authenticates', () => {
            const siteService = spectator.inject(DotSiteService);
            const userService = spectator.inject(DotCurrentUserService);
            siteService.getCurrentSite.mockReturnValue(of(mockSiteEntity));

            authSubject.next(authForUser('user-123'));

            expect(siteService.getCurrentSite).toHaveBeenCalled();
            expect(userService.getCurrentUser).toHaveBeenCalled();
            expect(store.siteDetails()).toEqual(mockSiteEntity);
            expect(store.loggedUser()).toEqual(mockCurrentUser);
        });

        it('should NOT reload when the same user is re-emitted (e.g. login-as)', () => {
            const siteService = spectator.inject(DotSiteService);
            siteService.getCurrentSite.mockReturnValue(of(mockSiteEntity));

            authSubject.next(authForUser('user-123'));
            authSubject.next(authForUser('user-123'));

            // distinctUntilChanged on userId collapses the duplicate emission.
            expect(siteService.getCurrentSite).toHaveBeenCalledTimes(1);
        });

        it('should reload when a different user authenticates (re-login)', () => {
            const siteService = spectator.inject(DotSiteService);
            siteService.getCurrentSite.mockReturnValue(of(mockSiteEntity));

            authSubject.next(authForUser('user-123'));
            authSubject.next(authForUser('user-456'));

            expect(siteService.getCurrentSite).toHaveBeenCalledTimes(2);
        });

        it('should keep loggedUser null when getCurrentUser fails after auth fires', () => {
            const userService = spectator.inject(DotCurrentUserService);
            userService.getCurrentUser.mockReturnValue(throwError(() => new Error('401')));

            authSubject.next(authForUser('user-123'));

            // The pre-auth/failed-load scenario must degrade gracefully, not crash the store.
            expect(store.loggedUser()).toBeNull();
        });

        it('should keep siteDetails null when getCurrentSite fails after auth fires', () => {
            const siteService = spectator.inject(DotSiteService);
            siteService.getCurrentSite.mockReturnValue(throwError(() => new Error('500')));

            authSubject.next(authForUser('user-123'));

            expect(store.siteDetails()).toBeNull();
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
