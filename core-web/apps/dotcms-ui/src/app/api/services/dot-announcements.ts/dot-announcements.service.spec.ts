import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { AnnouncementsService, Announcement } from './dot-announcements.service';

describe('AnnouncementsService', () => {
    let spectator: SpectatorService<AnnouncementsService>;

    const createService = createServiceFactory({
        service: AnnouncementsService,
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

    it('should fetch announcements', () => {
        const mockAnnouncements: Announcement[] = [
            {
                title: 'Test Announcement',
                type: 'announcement',
                announcementDateAsISO8601: '2024-01-31T17:51',
                identifier: 'test-announcement-id',
                url: 'https://www.example.com'
            }
        ];

        expect(spectator.service.announcements()).toEqual(mockAnnouncements);
    });

    it('should mark announcements as unread', () => {
        const storedAnnouncements: Announcement[] = [
            {
                title: 'Stored Announcement',
                type: 'announcement',
                announcementDateAsISO8601: '2024-01-30T14:30',
                identifier: 'stored-announcement-id',
                url: 'https://www.example.com'
            }
        ];

        localStorage.setItem('announcementsData', JSON.stringify(storedAnnouncements));

        const isNewAnnouncement = spectator.service.unreadAnnouncements();

        expect(isNewAnnouncement).toBe(true);
    });

    it('should not mark announcements as unread when there are no new announcements', () => {
        const storedAnnouncements: Announcement[] = [
            {
                title: 'Stored Announcement',
                type: 'announcement',
                announcementDateAsISO8601: '2024-01-31T14:30',
                identifier: 'test-announcement-id',
                url: 'https://www.example.com'
            }
        ];

        localStorage.setItem('announcementsData', JSON.stringify(storedAnnouncements));

        const isNewAnnouncement = spectator.service.unreadAnnouncements();

        expect(isNewAnnouncement).toBe(false);
    });
});
