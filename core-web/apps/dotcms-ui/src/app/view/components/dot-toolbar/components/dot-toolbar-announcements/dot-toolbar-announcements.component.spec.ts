import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClient, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DotMessageService } from '@dotcms/data-access';
import { SiteService, SiteServiceMock } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotToolbarAnnouncementsComponent } from './dot-toolbar-announcements.component';
import { AnnouncementsStore, TypesIcons } from './store/dot-announcements.store';

import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';

describe('DotToolbarAnnouncementsComponent', () => {
    let spectator: Spectator<DotToolbarAnnouncementsComponent>;
    let siteService: SpyObject<SiteServiceMock>;
    let store: SpyObject<AnnouncementsStore>;

    const mockAnnouncementsData = {
        entity: [
            {
                title: 'Test Announcement 1',
                type: 'announcement',
                announcementDateAsISO8601: '2024-01-31T17:51',
                identifier: 'test-announcement-1',
                url: 'https://www.example1.com',
                hasBeenRead: false
            },
            {
                title: 'Test Release',
                type: 'release',
                announcementDateAsISO8601: '2024-01-30T10:30',
                identifier: 'test-release-1',
                url: 'https://www.example2.com',
                hasBeenRead: true
            },
            {
                title: 'Important Notice',
                type: 'important',
                announcementDateAsISO8601: '2024-01-29T14:15',
                identifier: 'test-important-1',
                url: 'https://www.example3.com',
                hasBeenRead: false
            }
        ]
    };

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

    const createComponent = createComponentFactory({
        detectChanges: false,
        component: DotToolbarAnnouncementsComponent,
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            DotMessagePipe,
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: SiteService, useClass: SiteServiceMock },
            mockProvider(HttpClient, {
                get: jasmine.createSpy('get').and.returnValue(of(mockAnnouncementsData))
            })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        siteService = spectator.inject(SiteService) as unknown as SpyObject<SiteServiceMock>;
        store = spectator.inject(AnnouncementsStore, true);
    });

    describe('Component Initialization', () => {
        it('should create the component', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should initialize with proper dependencies', () => {
            expect(spectator.component.announcementsStore).toBeTruthy();
            expect(spectator.component.siteService).toBeTruthy();
        });

        it('should have proper typesIcons mapping', () => {
            expect(spectator.component.typesIcons).toEqual({
                tip: TypesIcons.Tip,
                release: TypesIcons.Release,
                announcement: TypesIcons.Announcement,
                article: TypesIcons.Article,
                important: TypesIcons.Important
            });
        });
    });

    describe('ngOnInit', () => {
        it('should load announcements and set about links on init', () => {
            const loadSpy = spyOn(store, 'load');
            const getAboutLinksSpy = spyOn(spectator.component, 'getAboutLinks').and.returnValue(
                []
            );

            spectator.component.ngOnInit();

            expect(loadSpy).toHaveBeenCalled();
            expect(getAboutLinksSpy).toHaveBeenCalled();
        });

        it('should set about links signal with returned data', () => {
            const mockAboutLinks = [
                { title: 'Knowledge Center', items: [] },
                { title: 'Contact Us', items: [] }
            ];
            spyOn(spectator.component, 'getAboutLinks').and.returnValue(mockAboutLinks);

            spectator.component.ngOnInit();

            expect(spectator.component.$aboutLinks()).toEqual(mockAboutLinks);
        });
    });

    describe('Site Switching Subscription', () => {
        it('should reload announcements when site changes', () => {
            const loadSpy = spyOn(spectator.component.announcementsStore, 'load');
            const getAboutLinksSpy = spyOn(spectator.component, 'getAboutLinks').and.returnValue(
                []
            );

            spectator.detectChanges();
            siteService.setFakeCurrentSite({});

            expect(loadSpy).toHaveBeenCalledTimes(2); // Once on init, once on site switch
            expect(getAboutLinksSpy).toHaveBeenCalledTimes(2);
        });

        it('should update about links when site changes', () => {
            const mockAboutLinks = [{ title: 'Updated Links', items: [] }];
            spyOn(spectator.component, 'getAboutLinks').and.returnValue(mockAboutLinks);

            spectator.detectChanges();
            siteService.setFakeCurrentSite({});

            expect(spectator.component.$aboutLinks()).toEqual(mockAboutLinks);
        });
    });

    describe('getAboutLinks', () => {
        it('should return properly structured about links', () => {
            const knowledgeLinks = store.selectKnowledgeCenterLinks();
            const contactLinks = store.selectContactLinks();

            const result = spectator.component.getAboutLinks();

            expect(result).toEqual([
                {
                    title: 'announcements.knowledge.center',
                    items: knowledgeLinks
                },
                {
                    title: 'announcements.knowledge.contact.us',
                    items: contactLinks
                }
            ]);
        });
    });

    describe('hideOverlayPanel', () => {
        it('should call hide method on overlay panel', () => {
            const overlayPanel = spectator.component.$overlayPanel();
            const hideSpy = spyOn(overlayPanel, 'hide');

            spectator.component.hideOverlayPanel();

            expect(hideSpy).toHaveBeenCalled();
        });
    });

    describe('markAnnouncementsAsRead', () => {
        it('should call markAnnouncementsAsRead on store', () => {
            const markAsReadSpy = spyOn(store, 'markAnnouncementsAsRead');

            spectator.component.markAnnouncementsAsRead();

            expect(markAsReadSpy).toHaveBeenCalled();
        });
    });

    describe('Template Rendering', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should display the announcements title', () => {
            spectator.click(byTestId('btn-overlay'));
            const title = spectator.query('.announcements__title');
            expect(title).toBeTruthy();
            expect(title).toHaveText('Announcements');
        });

        it('should display announcements list', () => {
            spectator.click(byTestId('btn-overlay'));
            const announcements = spectator.queryAll('.announcements__list-item');
            const announcementsStore = spectator.component.$announcements();
            expect(announcements.length).toBe(announcementsStore.length);
        });

        it('should display unread badge for unread announcements', () => {
            spectator.click(byTestId('btn-overlay'));
            const unreadItems = spectator.queryAll('.announcements__list-item--active');
            const badges = spectator.queryAll('.announcements__badge');

            expect(unreadItems.length).toBeGreaterThan(0);
            expect(badges.length).toBeGreaterThan(0);
        });

        it('should display correct icons for different announcement types', () => {
            spectator.click(byTestId('btn-overlay'));
            const icons = spectator.queryAll('.announcements__image');

            expect(icons.length).toBeGreaterThan(0);
            icons.forEach((icon) => {
                expect(icon).toHaveClass('pi');
            });
        });

        it('should display announcement dates', () => {
            spectator.click(byTestId('btn-overlay'));
            const dates = spectator.queryAll('.announcements__date');

            expect(dates.length).toBeGreaterThan(0);
            dates.forEach((date) => {
                expect(date.textContent.trim()).toBeTruthy();
            });
        });

        it('should have proper attributes on announcement links', () => {
            spectator.click(byTestId('btn-overlay'));
            const announcementLinks = spectator.queryAll(byTestId('announcement_link'));

            announcementLinks.forEach((link) => {
                expect(link.getAttribute('target')).toBe('_blank');
                expect(link.getAttribute('rel')).toBe('noopener noreferrer');
                expect(link.getAttribute('href')).toBeTruthy();
            });
        });

        it('should display "Show All" link with proper attributes', () => {
            spectator.click(byTestId('btn-overlay'));
            const showAllLink = spectator.query(byTestId('announcement_link_all'));

            expect(showAllLink).toBeTruthy();
            expect(showAllLink.getAttribute('target')).toBe('_blank');
            expect(showAllLink.getAttribute('rel')).toBe('noopener');
            expect(showAllLink).toHaveText('Show All');
        });

        it('should display about sections with proper links', () => {
            spectator.click(byTestId('btn-overlay'));
            const aboutLinks = spectator.queryAll(byTestId('announcements__about-link'));

            aboutLinks.forEach((link) => {
                expect(link.getAttribute('target')).toBe('_blank');
                expect(link.getAttribute('rel')).toBe('noopener');
                expect(link.getAttribute('href')).toBeTruthy();
            });
        });
    });

    describe('User Interactions', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should hide overlay panel when clicking on announcement links', () => {
            spectator.click(byTestId('btn-overlay'));
            const hideOverlayPanelSpy = spyOn(spectator.component, 'hideOverlayPanel');

            const links = spectator.queryAll(byTestId('announcement_link'));
            if (links.length > 0) {
                spectator.click(links[0]);
                expect(hideOverlayPanelSpy).toHaveBeenCalled();
            }
        });

        it('should hide overlay panel when clicking on "Show All" link', () => {
            spectator.click(byTestId('btn-overlay'));
            const hideOverlayPanelSpy = spyOn(spectator.component, 'hideOverlayPanel');

            const showAllLink = spectator.query(byTestId('announcement_link_all'));
            spectator.click(showAllLink);

            expect(hideOverlayPanelSpy).toHaveBeenCalled();
        });

        it('should hide overlay panel when clicking on about links', () => {
            spectator.click(byTestId('btn-overlay'));
            const hideOverlayPanelSpy = spyOn(spectator.component, 'hideOverlayPanel');

            const aboutLinks = spectator.queryAll(byTestId('announcements__about-link'));
            if (aboutLinks.length > 0) {
                spectator.click(aboutLinks[0]);
                expect(hideOverlayPanelSpy).toHaveBeenCalled();
            }
        });

        it('should mark announcements as read when overlay is hidden', () => {
            const markAnnouncementsAsReadSpy = spyOn(store, 'markAnnouncementsAsRead');

            spectator.click(byTestId('btn-overlay'));
            spectator.triggerEventHandler(DotToolbarBtnOverlayComponent, 'onHide', void 0);

            expect(markAnnouncementsAsReadSpy).toHaveBeenCalled();
        });
    });

    describe('Badge Display', () => {
        it('should show badge when there are unread announcements', () => {
            spectator.detectChanges();
            expect(spectator.component.$showUnreadAnnouncement()).toBe(true);
        });
    });
});
