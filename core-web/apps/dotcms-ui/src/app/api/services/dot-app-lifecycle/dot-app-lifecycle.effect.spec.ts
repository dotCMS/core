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

import { DotAppLifecycleEffect } from './dot-app-lifecycle.effect';

describe('DotAppLifecycleEffect', () => {
    let spectator: SpectatorService<DotAppLifecycleEffect>;
    let dotRouterService: SpyObject<DotRouterService>;
    let switchSiteSubject: Subject<DotSite>;

    const createService = createServiceFactory({
        service: DotAppLifecycleEffect,
        providers: [{ provide: DotRouterService, useClass: MockDotRouterService }]
    });

    beforeEach(() => {
        // switchSiteSubject must be assigned before createService() so the effect
        // constructor receives the correct observable when it subscribes on instantiation.
        switchSiteSubject = new Subject<DotSite>();

        spectator = createService({
            providers: [
                mockProvider(GlobalStore, {
                    switchSiteEvent$: jest.fn().mockReturnValue(switchSiteSubject.asObservable())
                })
            ]
        });

        dotRouterService = spectator.inject(DotRouterService);
    });

    it('should navigate to site browser when SWITCH_SITE fires on edit page', () => {
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(true);
        switchSiteSubject.next(mockSites[0] as unknown as DotSite);
        expect(dotRouterService.goToSiteBrowser).toHaveBeenCalled();
    });

    it('should NOT navigate when SWITCH_SITE fires on a non-edit page', () => {
        jest.spyOn(dotRouterService, 'isEditPage').mockReturnValue(false);
        switchSiteSubject.next(mockSites[0] as unknown as DotSite);
        expect(dotRouterService.goToSiteBrowser).not.toHaveBeenCalled();
    });
});
