import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { Announcement, AnnouncementsStore } from './dot-announcements.store';

describe('AnnouncementsStore', () => {
    let spectator: SpectatorService<AnnouncementsStore>;

    const createService = createServiceFactory({
        service: AnnouncementsStore,
        imports: [HttpClientTestingModule],
        providers: [
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
                url: 'https://www.example.com?utm_source=dotcms&utm_medium=application&utm_campaign=announcement_menu'
            }
        ];
        spectator.service.loadAnnouncements();
        spectator.service.state$.subscribe((state) => {
            expect(state.announcements).toEqual(mockAnnouncements);
            done();
        });
    });

    it('should not mark announcements as unread when there are no new announcements', (done) => {
        localStorage.removeItem('announcementsData');

        spectator.service.markAnnouncementsAsRead();
        spectator.service.loadAnnouncements();

        spectator.service.state$.subscribe((state) => {
            expect(state.showUnreadAnnouncement).toBe(false);
            done();
        });
    });

    it('should mark announcements as unread', (done) => {
        localStorage.removeItem('announcementsData');

        spectator.service.loadAnnouncements();

        spectator.service.state$.subscribe((state) => {
            expect(state.showUnreadAnnouncement).toBe(true);
            done();
        });
    });
});
