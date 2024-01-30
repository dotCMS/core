import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { SiteService, SiteServiceMock } from '@dotcms/dotcms-js';

import { Announcement, AnnouncementsStore } from './dot-announcements.store';

describe('AnnouncementsStore', () => {
    let spectator: SpectatorService<AnnouncementsStore>;
    const siteServiceMock = new SiteServiceMock();

    const createService = createServiceFactory({
        service: AnnouncementsStore,
        imports: [HttpClientTestingModule],
        providers: [
            {
                provide: SiteService,
                useValue: siteServiceMock
            },
            mockProvider(HttpClient, {
                get: jasmine.createSpy('get').and.returnValue(
                    of({
                        entity: [
                            {
                                title: 'Test Announcement',
                                type: 'announcement',
                                announcementDateAsISO8601: '2024-01-31T17:51',
                                identifier: 'test-announcement-id',
                                inode: '123',
                                url: 'https://www.example.com'
                            }
                        ]
                    })
                )
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('should fetch announcements', (done) => {
        const mockAnnouncements: Announcement[] = [
            {
                title: 'Test Announcement',
                type: 'announcement',
                announcementDateAsISO8601: '2024-01-31T17:51',
                identifier: 'test-announcement-id',
                inode: '123',
                url: 'https://www.example.com?utm_source=platform&utm_medium=demo.dotcms.com&utm_campaign=announcement'
            }
        ];
        spectator.service.load();
        spectator.service.state$.subscribe((state) => {
            expect(state.announcements).toEqual(mockAnnouncements);
            done();
        });
    });

    it('should not mark announcements as unread when there are no new announcements', (done) => {
        localStorage.removeItem('dotAnnouncementsData');
        spectator.service.load();
        spectator.service.markAnnouncementsAsRead();

        spectator.service.state$.subscribe((state) => {
            expect(state.showUnreadAnnouncement).toBe(false);
            done();
        });
    });

    it('should mark announcements as unread', (done) => {
        localStorage.removeItem('dotAnnouncementsData');

        spectator.service.load();
        spectator.service.state$.subscribe((state) => {
            expect(state.showUnreadAnnouncement).toBe(true);
            done();
        });
    });

    it('should update the url when the site changes', (done) => {
        spectator.service.load();
        spectator.service.state$.subscribe((state) => {
            expect(state.announcements[0].url).toBe(
                'https://www.example.com?utm_source=platform&utm_medium=demo.dotcms.com&utm_campaign=announcement'
            );
            done();
        });
    });
});
