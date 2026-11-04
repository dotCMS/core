import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { EMPTY } from 'rxjs';
import { Mock, Mocked, vi } from 'vitest';

import { Injector, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRouteSnapshot, Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';
import { DotSidePanelRef } from '@dotcms/ui';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { EditContentHost } from './edit-content-host.model';
import { RouterEditContentHost } from './router-edit-content-host';

import { DotOpenContentEvent } from '../../models/dot-open-content-event.model';
import {
    DotEditContentOpenerService,
    DotEditContentOpenOptions
} from '../../opener/dot-edit-content-opener.service';
import {
    DotRelatedContentCrumb,
    DotRelatedContentNavigationStore
} from '../../store/dot-related-content-navigation.store';

/**
 * Asks a host to open content, as a field would (#38303). Fails with a clear message while the
 * host has no `openContent`.
 *
 * @param host The host under test.
 * @param event What the field hands it.
 */
const openContent = (host: EditContentHost, event: DotOpenContentEvent): void => {
    const withOpenContent = host as EditContentHost & {
        openContent?: (event: DotOpenContentEvent) => void;
    };

    expect(typeof withOpenContent.openContent, 'the host can open content').toBe('function');
    withOpenContent.openContent?.(event);
};

// Builds a fake ActivatedRouteSnapshot tree (root → ...leaf) so resolveIdentity's
// firstChild walk can be exercised.
const routeTree = (
    levels: { params?: Record<string, string>; queryParams?: Record<string, string> }[]
): ActivatedRouteSnapshot => {
    const nodes = levels.map(
        (l) =>
            ({
                params: l.params ?? {},
                queryParams: l.queryParams ?? {},
                firstChild: null
            }) as unknown as ActivatedRouteSnapshot
    );
    nodes.forEach((n, i) => {
        (n as { firstChild: ActivatedRouteSnapshot | null }).firstChild = nodes[i + 1] ?? null;
    });

    return nodes[0];
};

describe('RouterEditContentHost', () => {
    let spectator: SpectatorService<RouterEditContentHost>;
    let host: RouterEditContentHost;
    let router: Mocked<Pick<Router, 'navigate'>>;
    let title: Mocked<Pick<Title, 'setTitle'>>;
    let globalStore: { addNewBreadcrumb: Mock };
    let relatedNav: {
        registerTitle: Mock;
        buildTrailForSavedInode: Mock;
        appendToTrail: Mock;
        trailInodes: Mock;
        trail: ReturnType<typeof signal<DotRelatedContentCrumb[]>>;
    };

    const trailSignal = signal<DotRelatedContentCrumb[]>([]);

    const createHost = createServiceFactory({
        service: RouterEditContentHost,
        providers: [
            mockProvider(Router, {
                navigate: vi.fn(),
                // identityChanges$ subscribes to router.events at construction.
                events: EMPTY,
                routerState: {
                    snapshot: { root: routeTree([{}]) }
                }
            }),
            mockProvider(Title, { setTitle: vi.fn() }),
            mockProvider(GlobalStore, { addNewBreadcrumb: vi.fn() }),
            mockProvider(DotRelatedContentNavigationStore, {
                registerTitle: vi.fn(),
                buildTrailForSavedInode: vi.fn().mockReturnValue('a,b'),
                appendToTrail: vi.fn().mockReturnValue(['inode-a', 'inode-b']),
                trailInodes: vi.fn().mockReturnValue(['inode-a']),
                trail: trailSignal
            }),
            mockProvider(DotMessageService, {
                get: vi.fn().mockReturnValue('dotCMS')
            }),
            mockProvider(DotEditContentOpenerService, {
                open: vi.fn(() => new DotSidePanelRef(() => undefined))
            })
        ]
    });

    beforeEach(() => {
        trailSignal.set([]);
        spectator = createHost();
        host = spectator.service;
        router = spectator.inject(Router) as unknown as typeof router;
        title = spectator.inject(Title) as unknown as typeof title;
        globalStore = spectator.inject(GlobalStore) as unknown as typeof globalStore;
        relatedNav = spectator.inject(
            DotRelatedContentNavigationStore
        ) as unknown as typeof relatedNav;

        // The provider vi.fns are shared across tests (providers are evaluated
        // once); clear call history so per-test assertions are isolated.
        router.navigate.mockClear();
        relatedNav.registerTitle.mockClear();
        relatedNav.buildTrailForSavedInode.mockClear();
        relatedNav.appendToTrail.mockClear();
        (spectator.inject(DotEditContentOpenerService).open as Mock).mockClear();
    });

    it('reports that it does not navigate in place', () => {
        expect(host.inPlaceNavigation).toBe(false);
        expect(host.inPlaceNavigation$).toBeUndefined();
    });

    it('exposes the shared nav-store trail', () => {
        trailSignal.set([{ inode: 'x', title: 'X' }]);
        expect(host.trail()).toEqual([{ inode: 'x', title: 'X' }]);
    });

    it('setTrail is a no-op (the URL owns the full-screen trail)', () => {
        expect(() => host.setTrail(['a', 'b'])).not.toThrow();
    });

    describe('resolveIdentity', () => {
        it('reads id/contentType/folderPath from the leaf route', () => {
            (
                router as unknown as { routerState: { snapshot: { root: ActivatedRouteSnapshot } } }
            ).routerState.snapshot.root = routeTree([
                { params: { id: 'inode-1' }, queryParams: { folderPath: 'a/b/' } }
            ]);

            expect(host.resolveIdentity()).toEqual({
                inode: 'inode-1',
                contentTypeId: undefined,
                folderPath: 'a/b/'
            });
        });

        // The full-page editor keeps today's behavior: a new content starts in the default
        // language, whatever the URL carries (#37759, FR-003 applies to Content Drive's panel only).
        it('never carries a starting language', () => {
            (
                router as unknown as { routerState: { snapshot: { root: ActivatedRouteSnapshot } } }
            ).routerState.snapshot.root = routeTree([
                { params: { contentType: 'Blog' }, queryParams: { languageId: '2' } }
            ]);

            expect(host.resolveIdentity().languageId).toBeUndefined();
        });

        it('walks firstChild down to the leaf route (nested routes)', () => {
            (
                router as unknown as { routerState: { snapshot: { root: ActivatedRouteSnapshot } } }
            ).routerState.snapshot.root = routeTree([{}, {}, { params: { contentType: 'Blog' } }]);

            expect(host.resolveIdentity()).toEqual({
                inode: undefined,
                contentTypeId: 'Blog',
                folderPath: undefined
            });
        });
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

    describe('leaveDeletedContent', () => {
        it('should navigate to the content listing filtered by the content type', () => {
            host.leaveDeletedContent('SimpleWidget');

            expect(router.navigate).toHaveBeenCalledWith(['/c/content'], {
                replaceUrl: true,
                queryParams: { filter: 'SimpleWidget' }
            });
        });
    });

    describe('goToRestoredVersion', () => {
        it('should not navigate when the inode did not change', () => {
            host.goToRestoredVersion('123', '123');

            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('repoints the trail current crumb to the restored inode when it changed', () => {
            host.goToRestoredVersion('789', '123');

            expect(relatedNav.buildTrailForSavedInode).toHaveBeenCalledWith('789');
            expect(router.navigate).toHaveBeenCalledWith(['/content', '789'], {
                replaceUrl: true,
                queryParams: { rc: 'a,b' },
                queryParamsHandling: 'merge'
            });
        });
    });

    describe('reloadContent (locale switch)', () => {
        it('repoints the trail current crumb to the new-locale inode', () => {
            host.reloadContent('789');

            expect(relatedNav.buildTrailForSavedInode).toHaveBeenCalledWith('789');
            expect(router.navigate).toHaveBeenCalledWith(['/content', '789'], {
                replaceUrl: true,
                queryParams: { rc: 'a,b' },
                queryParamsHandling: 'merge'
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
                ['inode-a'],
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

    /**
     * A field asks the full-screen editor's host to open other content (#38303, R8). The editor
     * is a route, not a panel, so whatever the field asks for opens in a panel over it: the editor
     * stays mounted with its unsaved edits, and nothing navigates.
     */
    describe('openContent', () => {
        /** The opener's `open`, as the mock records it. */
        const opened = () => spectator.inject(DotEditContentOpenerService).open as Mock;

        it.each<[string, DotOpenContentEvent['request'], DotOpenContentEvent['source']]>([
            [
                'an existing item from a Relationship field',
                { mode: 'edit', inode: 'inode-b', title: 'B' },
                'relationship'
            ],
            [
                'a create from a Relationship field',
                {
                    mode: 'new',
                    contentType: 'Blog',
                    title: 'New Blog',
                    relationship: {
                        parentContentletId: 'parent-inode',
                        relationshipName: 'blogs',
                        isParent: true
                    }
                },
                'relationship'
            ],
            [
                'content a block editor names',
                { mode: 'edit', inode: 'inode-c', contentType: 'Blog' },
                'block-editor'
            ],
            ['an edit that names no source', { mode: 'edit', inode: 'inode-d' }, undefined]
        ])(
            'should open %s in a panel through the opener, with the host injector',
            (_label, request, source) => {
                const onSaved = vi.fn();
                const contentlet = createFakeContentlet({ identifier: 'saved-id' });

                openContent(host, { request, source, onSaved });

                expect(opened().mock.calls.length, 'the host opens a panel').toBe(1);
                const [openedRequest, options] = opened().mock.calls[0] as [
                    DotOpenContentEvent['request'],
                    DotEditContentOpenOptions
                ];
                expect(openedRequest).toEqual(request);
                // Compared by identity: printing an injector runs the worker out of memory.
                expect(options.injector === spectator.inject(Injector)).toBe(true);

                options.saved?.(contentlet);
                expect(onSaved).toHaveBeenCalledWith(contentlet);
            }
        );

        it('should never navigate or extend the related-content trail', () => {
            openContent(host, {
                request: { mode: 'edit', inode: 'inode-b', title: 'B' },
                source: 'relationship'
            });

            expect(router.navigate).not.toHaveBeenCalled();
            expect(relatedNav.appendToTrail).not.toHaveBeenCalled();
        });
    });
});
