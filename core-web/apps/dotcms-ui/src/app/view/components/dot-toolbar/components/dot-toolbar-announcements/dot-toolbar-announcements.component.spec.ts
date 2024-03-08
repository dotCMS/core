import { Spectator, byTestId, createComponentFactory, mockProvider } from '@ngneat/spectator';
import { of } from 'rxjs';

import { NgClass, NgForOf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotMessageService } from '@dotcms/data-access';
import { SiteService, SiteServiceMock } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolbarAnnouncementsComponent } from './dot-toolbar-announcements.component';

describe('DotToolbarAnnouncementsComponent', () => {
    let spectator: Spectator<DotToolbarAnnouncementsComponent>;

    const messageServiceMock = new MockDotMessageService({
        announcements: 'Announcements',
        'announcements.show.all': 'Show All',
        'announcements.knowledge.center': 'Knowledge Center',
        'announcements.knowledge.contact.us': 'Contact Us',
        'announcements.contact.customer.support': 'Customer Support',
        'announcements.contact.professional.services': 'Professional Services',
        'announcements.contact.request.feature': 'Request a Feature',
        'announcements.contact.report.bug': 'Report a Bug',
        'announcements.knowledge.center.documentation': 'Documentation',
        'announcements.knowledge.center.blog': 'Blog',
        'announcements.knowledge.center.github': 'GitHub Repository',
        'announcements.knowledge.center.training': 'Online Training',
        'announcements.knowledge.center.forum': 'Forum'
    });
    const siteServiceMock = new SiteServiceMock();
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
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: SiteService,
                useValue: siteServiceMock
            }
        ],
        imports: [NgForOf, NgClass, DotMessagePipe, HttpClientTestingModule, OverlayPanelModule]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.component.toggleDialog(new MouseEvent('click'));
    });

    it('should display announcements', () => {
        spectator.detectChanges();
        const announcements = spectator.queryAll('.announcements__list-item');
        expect(announcements.length).toBe(spectator.component.announcements().length);
    });

    it('should have a "Show All" link', () => {
        spectator.detectChanges();
        const showAllLink = spectator.query(byTestId('announcement_link_all'));
        expect(showAllLink).toBeTruthy();
        expect(showAllLink.getAttribute('target')).toBe('_blank');
    });

    it('should have a target blank on the announcements link', () => {
        spectator.detectChanges();
        const announcementLink = spectator.query(byTestId('announcement_link'));
        expect(announcementLink.getAttribute('target')).toBe('_blank');
    });

    it('should call markAnnouncementsAsRead when showUnreadAnnouncement is false', () => {
        const markAnnouncementsAsReadSpy = spyOn(
            spectator.component.announcementsStore,
            'markAnnouncementsAsRead'
        );
        spectator.setInput('showUnreadAnnouncement', false);
        expect(markAnnouncementsAsReadSpy).toHaveBeenCalled();
    });

    it('should show the mask when the dialog is opened', () => {
        spectator.detectChanges();
        const mask = spectator.query(byTestId('dot-mask'));
        expect(mask).toBeTruthy();
    });

    it('should hide the mask when the dialog is closed', () => {
        spectator.component.toggleDialog(new MouseEvent('click'));
        const mask = spectator.query(byTestId('dot-mask'));
        expect(mask).toBeFalsy();
    });

    it('should have about links', () => {
        spectator.detectChanges();
        const announcementLink = spectator.query(byTestId('announcements__about-link'));
        expect(announcementLink.getAttribute('target')).toBe('_blank');
    });

    it('should call markAnnouncementsAsRead on ngOnInit', () => {
        const markAnnouncementsAsReadSpy = spyOn(
            spectator.component.announcementsStore,
            'markAnnouncementsAsRead'
        );

        spectator.component.ngOnChanges({ showUnreadAnnouncement: { currentValue: false } });
        expect(markAnnouncementsAsReadSpy).toHaveBeenCalled();
    });

    it('should close the overlaypanel when clicking on the links', () => {
        const announcementPanel = spectator.query(byTestId('announcement_link'));
        const links = spectator.queryAll(byTestId('announcement_link'));
        links.forEach((link) => link.dispatchEvent(new MouseEvent('click')));
        spectator.detectChanges();
        expect(announcementPanel).toBeFalsy();
    });
});
