import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { Subject, of } from 'rxjs';

import {
    DotCurrentUserService,
    DotEventsSocket,
    DotSiteService,
    DotSystemConfigService
} from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';

import { GlobalStore } from './store';
import { mockSiteEntity } from './store.mock';

describe('GlobalStore', () => {
    let spectator: SpectatorService<InstanceType<typeof GlobalStore>>;
    let store: InstanceType<typeof GlobalStore>;
    let switchSiteSubject: Subject<DotSite>;

    const createService = createServiceFactory({
        service: GlobalStore,
        providers: [
            mockProvider(DotCurrentUserService),
            mockProvider(DotSiteService, {
                getCurrentSite: jest.fn().mockReturnValue(of(null)),
                switchSite: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotSystemConfigService),
            mockProvider(DotEventsSocket, {
                connect: () => of({}),
                status$: () => new Subject(),
                on: jest.fn().mockReturnValue(new Subject())
            })
        ]
    });

    beforeEach(() => {
        switchSiteSubject = new Subject<DotSite>();
        spectator = createService();
        store = spectator.service;

        // Wire up switchSiteSubject for SWITCH_SITE tests
        const eventsSocket = spectator.inject(DotEventsSocket);
        (eventsSocket.on as jest.Mock).mockImplementation((event: string) => {
            if (event === 'SWITCH_SITE') return switchSiteSubject.asObservable();

            return new Subject();
        });
    });

    describe('Initial State', () => {
        it('should initialize with expected default values', () => {
            expect(store.siteDetails()).toBeNull();
            expect(store.currentSiteId()).toBeNull();
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
            siteService.switchSite.mockReturnValue(of({}));
            siteService.getCurrentSite.mockReturnValue(of(newSite));

            store.switchCurrentSite('new-site');

            expect(siteService.switchSite).toHaveBeenCalledWith('new-site');
            expect(siteService.getCurrentSite).toHaveBeenCalled();
            expect(store.siteDetails()).toEqual(newSite);
        });
    });

    describe('SWITCH_SITE WebSocket event', () => {
        it('should update siteDetails when SWITCH_SITE event fires', () => {
            // Re-create service after wiring up the mock so onInit picks up the subject
            spectator = createService();
            store = spectator.service;
            const eventsSocket = spectator.inject(DotEventsSocket);
            (eventsSocket.on as jest.Mock).mockImplementation((event: string) => {
                if (event === 'SWITCH_SITE') return switchSiteSubject.asObservable();

                return new Subject();
            });

            // Trigger the event
            switchSiteSubject.next(mockSiteEntity);

            expect(store.siteDetails()).toEqual(mockSiteEntity);
        });
    });
});
