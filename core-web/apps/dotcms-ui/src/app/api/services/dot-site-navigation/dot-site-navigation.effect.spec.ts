import {
    createServiceFactory,
    mockProvider,
    SpectatorService,
    SpyObject
} from '@ngneat/spectator/jest';
import { Subject } from 'rxjs';

import { DotRouterService } from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { MockDotRouterService, mockSites } from '@dotcms/utils-testing';

import { DotSiteNavigationEffect } from './dot-site-navigation.effect';

describe('DotSiteNavigationEffect', () => {
    let spectator: SpectatorService<DotSiteNavigationEffect>;
    let dotRouterService: SpyObject<DotRouterService>;
    let switchSiteSubject: Subject<DotSite>;

    const createService = createServiceFactory({
        service: DotSiteNavigationEffect,
        providers: [
            { provide: DotRouterService, useClass: MockDotRouterService },
            mockProvider(GlobalStore, {
                switchSiteEvent$: jest.fn()
            })
        ]
    });

    beforeEach(() => {
        switchSiteSubject = new Subject<DotSite>();

        spectator = createService();
        dotRouterService = spectator.inject(DotRouterService);

        const globalStore = spectator.inject(GlobalStore);
        (globalStore.switchSiteEvent$ as jest.Mock).mockReturnValue(
            switchSiteSubject.asObservable()
        );
    });

    it('should navigate to site browser when SWITCH_SITE fires on edit page', () => {
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(true);
        switchSiteSubject.next(mockSites[0]);
        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
    });

    it('should NOT navigate when SWITCH_SITE fires on a non-edit page', () => {
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(false);
        switchSiteSubject.next(mockSites[0]);
        expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
    });
});
