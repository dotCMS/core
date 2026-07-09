import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DialogEditContentHost } from './dialog-edit-content-host';

import { DotRelatedContentNavigationStore } from '../../store/dot-related-content-navigation.store';

describe('DialogEditContentHost', () => {
    let spectator: SpectatorService<DialogEditContentHost>;
    let host: DialogEditContentHost;
    let relatedNav: { appendToTrail: jest.Mock; titleCache: jest.Mock };
    let config: DynamicDialogConfig;

    const createHost = createServiceFactory({
        service: DialogEditContentHost,
        providers: [
            mockProvider(DotRelatedContentNavigationStore, {
                appendToTrail: jest.fn().mockReturnValue(['inode-a', 'inode-b']),
                titleCache: jest.fn().mockReturnValue({ 'inode-a': 'A', 'inode-b': 'B' })
            }),
            mockProvider(DynamicDialogConfig, { data: undefined })
        ]
    });

    beforeEach(() => {
        spectator = createHost();
        host = spectator.service;
        relatedNav = spectator.inject(
            DotRelatedContentNavigationStore
        ) as unknown as typeof relatedNav;
        config = spectator.inject(DynamicDialogConfig);
    });

    it('reports that it navigates in place', () => {
        expect(host.inPlaceNavigation).toBe(true);
        expect(host.inPlaceNavigation$).toBeDefined();
    });

    it('starts with an empty per-instance trail (does not touch the shared store)', () => {
        expect(host.trail()).toEqual([]);
    });

    describe('resolveIdentity', () => {
        it('returns empty when the dialog config has no data', () => {
            config.data = undefined;
            expect(host.resolveIdentity()).toEqual({
                inode: undefined,
                contentTypeId: undefined
            });
        });

        it('maps contentletInode and contentTypeId from the dialog config', () => {
            config.data = { contentletInode: 'inode-9', contentTypeId: 'Blog' };
            expect(host.resolveIdentity()).toEqual({
                inode: 'inode-9',
                contentTypeId: 'Blog'
            });
        });
    });

    describe('saved$', () => {
        it('emits the contentlet reported via reportSaved', (done) => {
            const contentlet = { inode: 'x', title: 't' } as DotCMSContentlet;
            host.saved$.subscribe((c) => {
                expect(c).toBe(contentlet);
                done();
            });

            host.reportSaved(contentlet);
        });
    });

    // Chrome/route intents overlay another context, so they are safe no-ops.
    it('treats title/breadcrumb/save/restore as no-ops', () => {
        expect(() => host.setContentTitle('x')).not.toThrow();
        expect(() => host.addBreadcrumb({ label: 'x', url: '/y' })).not.toThrow();
        expect(() => host.goToSavedContent()).not.toThrow();
        expect(() => host.goToRestoredVersion()).not.toThrow();
    });

    describe('goToRelatedContent', () => {
        it('emits a reload request carrying the DEFERRED trail (not committed yet)', (done) => {
            host.inPlaceNavigation$.subscribe((request) => {
                expect(request).toEqual({ inode: 'inode-b', trail: ['inode-a', 'inode-b'] });
                // Trail is NOT committed until setTrail is called by the layout.
                expect(host.trail()).toEqual([]);
                done();
            });

            host.goToRelatedContent(
                { inode: 'inode-a', title: 'A' },
                { inode: 'inode-b', title: 'B' }
            );

            expect(relatedNav.appendToTrail).toHaveBeenCalledWith(
                [],
                { inode: 'inode-a', title: 'A' },
                { inode: 'inode-b', title: 'B' }
            );
        });
    });

    describe('goToCrumb', () => {
        it('emits a reload request with the trimmed trail (deferred)', (done) => {
            host.inPlaceNavigation$.subscribe((request) => {
                expect(request).toEqual({ inode: 'inode-a', trail: ['inode-a'] });
                done();
            });

            host.goToCrumb('inode-a', ['inode-a']);
        });
    });

    describe('reloadContent (locale switch)', () => {
        it('emits a reload request WITHOUT a trail (keeps the current trail)', (done) => {
            host.inPlaceNavigation$.subscribe((request) => {
                expect(request).toEqual({ inode: 'inode-5' });
                expect(request.trail).toBeUndefined();
                done();
            });

            host.reloadContent('inode-5');
        });
    });

    describe('setTrail', () => {
        it('commits the trail as this host instance current trail', () => {
            host.setTrail(['inode-a', 'inode-b']);
            expect(host.trail()).toEqual([
                { inode: 'inode-a', title: 'A' },
                { inode: 'inode-b', title: 'B' }
            ]);
        });
    });
});
