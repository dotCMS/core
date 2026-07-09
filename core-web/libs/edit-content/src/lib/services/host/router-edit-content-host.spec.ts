import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';

import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { RouterEditContentHost } from './router-edit-content-host';

import { DotRelatedContentNavigationStore } from '../../store/dot-related-content-navigation.store';

describe('RouterEditContentHost', () => {
    let spectator: SpectatorService<RouterEditContentHost>;
    let host: RouterEditContentHost;
    let router: jest.Mocked<Pick<Router, 'navigate'>>;
    let title: jest.Mocked<Pick<Title, 'setTitle'>>;
    let globalStore: { addNewBreadcrumb: jest.Mock };
    let relatedNav: {
        registerTitle: jest.Mock;
        buildTrailForSavedInode: jest.Mock;
        appendToTrail: jest.Mock;
    };

    const createHost = createServiceFactory({
        service: RouterEditContentHost,
        providers: [
            mockProvider(Router, { navigate: jest.fn() }),
            mockProvider(Title, { setTitle: jest.fn() }),
            mockProvider(GlobalStore, { addNewBreadcrumb: jest.fn() }),
            mockProvider(DotRelatedContentNavigationStore, {
                registerTitle: jest.fn(),
                buildTrailForSavedInode: jest.fn().mockReturnValue('a,b'),
                appendToTrail: jest.fn().mockReturnValue(['inode-a', 'inode-b'])
            }),
            mockProvider(DotMessageService, {
                get: jest.fn().mockReturnValue('dotCMS')
            })
        ]
    });

    beforeEach(() => {
        spectator = createHost();
        host = spectator.service;
        router = spectator.inject(Router) as unknown as typeof router;
        title = spectator.inject(Title) as unknown as typeof title;
        globalStore = spectator.inject(GlobalStore) as unknown as typeof globalStore;
        relatedNav = spectator.inject(
            DotRelatedContentNavigationStore
        ) as unknown as typeof relatedNav;

        // The provider jest.fns are shared across tests (providers are evaluated
        // once); clear call history so per-test assertions are isolated.
        router.navigate.mockClear();
        relatedNav.registerTitle.mockClear();
        relatedNav.buildTrailForSavedInode.mockClear();
        relatedNav.appendToTrail.mockClear();
    });

    it('reports that it does not navigate in place', () => {
        expect(host.inPlaceNavigation).toBe(false);
        expect(host.inPlaceNavigation$).toBeUndefined();
    });

    describe('setContentTitle', () => {
        it('should append the platform suffix', () => {
            host.setContentTitle('My Content');

            expect(title.setTitle).toHaveBeenCalledWith('My Content - dotCMS');
        });
    });

    describe('addBreadcrumb', () => {
        it('should contribute a self-target breadcrumb to the global store', () => {
            host.addBreadcrumb({ label: 'My Content', url: '/dotAdmin/#/content/123' });

            expect(globalStore.addNewBreadcrumb).toHaveBeenCalledWith({
                label: 'My Content',
                target: '_self',
                url: '/dotAdmin/#/content/123'
            });
        });
    });

    describe('goToSavedContent', () => {
        it('should not navigate when the inode did not change', () => {
            host.goToSavedContent({ inode: '123', title: 'X' }, '123');

            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should reconcile the trail and navigate when the inode changed', () => {
            host.goToSavedContent({ inode: '456', title: 'New Version' }, '123');

            expect(relatedNav.registerTitle).toHaveBeenCalledWith('456', 'New Version');
            expect(relatedNav.buildTrailForSavedInode).toHaveBeenCalledWith('456');
            expect(router.navigate).toHaveBeenCalledWith(['/content', '456'], {
                replaceUrl: true,
                queryParams: { rc: 'a,b' },
                queryParamsHandling: 'merge'
            });
        });

        it('should navigate when there was no previous inode', () => {
            host.goToSavedContent({ inode: '456', title: 'New Version' }, undefined);

            expect(router.navigate).toHaveBeenCalled();
        });
    });

    describe('goToRestoredVersion', () => {
        it('should not navigate when the inode did not change', () => {
            host.goToRestoredVersion('123', '123');

            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('should navigate preserving query params when the inode changed', () => {
            host.goToRestoredVersion('789', '123');

            expect(router.navigate).toHaveBeenCalledWith(['/content', '789'], {
                replaceUrl: true,
                queryParamsHandling: 'preserve'
            });
        });
    });

    describe('goToRelatedContent', () => {
        it('should append to the trail and navigate with the rc query param', () => {
            host.goToRelatedContent(
                { inode: 'inode-a', title: 'A' },
                { inode: 'inode-b', title: 'B' }
            );

            expect(relatedNav.appendToTrail).toHaveBeenCalledWith(
                { inode: 'inode-a', title: 'A' },
                { inode: 'inode-b', title: 'B' }
            );
            expect(router.navigate).toHaveBeenCalledWith(['/content', 'inode-b'], {
                queryParams: { rc: 'inode-a,inode-b' },
                queryParamsHandling: 'merge'
            });
        });
    });

    describe('goToCrumb', () => {
        it('should navigate with the trimmed trail as rc', () => {
            host.goToCrumb('inode-a', ['inode-a', 'inode-b']);

            expect(router.navigate).toHaveBeenCalledWith(['/content', 'inode-a'], {
                queryParams: { rc: 'inode-a,inode-b' },
                queryParamsHandling: 'merge'
            });
        });

        it('should clear rc when the trail collapses to a single crumb', () => {
            host.goToCrumb('inode-a', ['inode-a']);

            expect(router.navigate).toHaveBeenCalledWith(['/content', 'inode-a'], {
                queryParams: { rc: null },
                queryParamsHandling: 'merge'
            });
        });
    });
});
