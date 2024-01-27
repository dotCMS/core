import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator';
import { of } from 'rxjs';

import { NgClass, NgForOf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolbarAnnouncementsComponent } from './dot-toolbar-announcements.component';

describe('DotToolbarAnnouncementsComponent', () => {
    let spectator: Spectator<DotToolbarAnnouncementsComponent>;

    const messageServiceMock = new MockDotMessageService({
        announcements: 'Announcements',
        announcements_show_all: 'Show All',
        announcements_knowledge_center: 'Knowledge Center',
        announcements_knowledge_contact_us: 'Contact Us'
    });

    const createComponent = createComponentFactory({
        component: DotToolbarAnnouncementsComponent,
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
            }),
            DotMessagePipe,
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        imports: [NgForOf, NgClass, DotMessagePipe, HttpClientTestingModule]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should display announcements', () => {
        spectator.detectChanges();
        const announcements = spectator.queryAll('.announcements__list-item');
        expect(announcements.length).toBe(spectator.component.announcements().length);
    });

    it('should have a "Show All" link', () => {
        spectator.detectChanges();
        const showAllLink = spectator.query(byTestId('announcement_link'));
        expect(showAllLink).toBeTruthy();
    });
});
