import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';

import { DialogEditContentHost } from './dialog-edit-content-host';
import { EditContentHost } from './edit-content-host.model';

import { DotRelatedContentNavigationStore } from '../../store/dot-related-content-navigation.store';

describe('DialogEditContentHost', () => {
    let spectator: SpectatorService<DialogEditContentHost>;
    let host: DialogEditContentHost;
    let relatedNav: {
        appendToTrail: jest.Mock;
        setInMemoryTrail: jest.Mock;
    };

    const createHost = createServiceFactory({
        service: DialogEditContentHost,
        providers: [
            mockProvider(DotRelatedContentNavigationStore, {
                appendToTrail: jest.fn().mockReturnValue(['inode-a', 'inode-b']),
                setInMemoryTrail: jest.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createHost();
        host = spectator.service;
        relatedNav = spectator.inject(
            DotRelatedContentNavigationStore
        ) as unknown as typeof relatedNav;
    });

    it('seeds an empty in-memory trail on creation', () => {
        expect(relatedNav.setInMemoryTrail).toHaveBeenCalledWith([]);
    });

    it('reports that it navigates in place', () => {
        expect(host.inPlaceNavigation).toBe(true);
        expect(host.inPlaceNavigation$).toBeDefined();
    });

    // Chrome/route intents overlay another context, so they are safe no-ops.
    it('treats title/breadcrumb/save/restore as no-ops', () => {
        const asHost = host as EditContentHost;
        expect(() => asHost.setContentTitle('x')).not.toThrow();
        expect(() => asHost.addBreadcrumb({ label: 'x', url: '/y' })).not.toThrow();
        expect(() => asHost.goToSavedContent({ inode: '1', title: 't' }, undefined)).not.toThrow();
        expect(() => asHost.goToRestoredVersion('1', undefined)).not.toThrow();
    });

    describe('goToRelatedContent', () => {
        it('appends to the trail, stores it in memory and requests an in-place reload', (done) => {
            host.inPlaceNavigation$.subscribe((inode) => {
                expect(inode).toBe('inode-b');
                done();
            });

            host.goToRelatedContent(
                { inode: 'inode-a', title: 'A' },
                { inode: 'inode-b', title: 'B' }
            );

            expect(relatedNav.appendToTrail).toHaveBeenCalled();
            expect(relatedNav.setInMemoryTrail).toHaveBeenCalledWith(['inode-a', 'inode-b']);
        });
    });

    describe('goToCrumb', () => {
        it('stores the trimmed trail in memory and requests an in-place reload', (done) => {
            host.inPlaceNavigation$.subscribe((inode) => {
                expect(inode).toBe('inode-a');
                done();
            });

            host.goToCrumb('inode-a', ['inode-a']);

            expect(relatedNav.setInMemoryTrail).toHaveBeenCalledWith(['inode-a']);
        });
    });

    it('clears the in-memory trail on destroy', () => {
        relatedNav.setInMemoryTrail.mockClear();
        spectator.service.ngOnDestroy();

        expect(relatedNav.setInMemoryTrail).toHaveBeenCalledWith(null);
    });
});
