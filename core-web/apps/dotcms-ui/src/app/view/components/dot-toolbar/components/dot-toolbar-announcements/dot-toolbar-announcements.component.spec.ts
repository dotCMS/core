import {
    Spectator,
    SpyObject,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClient, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { DotMessageService } from '@dotcms/data-access';
import { SiteService, SiteServiceMock } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';


import { DotToolbarAnnouncementsComponent } from './dot-toolbar-announcements.component';
import { AnnouncementsStore, TypesIcons } from './store/dot-announcements.store';

import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';

@Component({
    selector: 'dot-toolbar-btn-overlay',
    template: '<ng-content></ng-content>',
    standalone: true
})
class DotToolbarBtnOverlayStubComponent {
    @Input() icon?: string;
    @Input() showBadge = false;
    @Input() overlayStyleClass = '';
    @Output() onHide = new EventEmitter<void>();

    hide = jest.fn();
    show = jest.fn();
}

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
                get: jest.fn().mockReturnValue(of(mockAnnouncementsData))
            })
        ]
    });

    beforeEach(() => {
        TestBed.overrideComponent(DotToolbarAnnouncementsComponent, {
            remove: {
                imports: [DotToolbarBtnOverlayComponent]
            },
            add: {
                imports: [DotToolbarBtnOverlayStubComponent]
            }
        });
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
            const loadSpy = jest.spyOn(store, 'load');
            const getAboutLinksSpy = jest
                .spyOn(spectator.component, 'getAboutLinks')
                .mockReturnValue([]);

            spectator.component.ngOnInit();

            expect(loadSpy).toHaveBeenCalled();
            expect(getAboutLinksSpy).toHaveBeenCalled();
        });

        it('should set about links signal with returned data', () => {
            const mockAboutLinks = [
                { title: 'Knowledge Center', items: [] },
                { title: 'Contact Us', items: [] }
            ];
            jest.spyOn(spectator.component, 'getAboutLinks').mockReturnValue(mockAboutLinks);

            spectator.component.ngOnInit();

            expect(spectator.component.$aboutLinks()).toEqual(mockAboutLinks);
        });
    });

    describe('Site Switching Subscription', () => {
        it('should reload announcements when site changes', () => {
            const loadSpy = jest.spyOn(spectator.component.announcementsStore, 'load');
            const getAboutLinksSpy = jest
                .spyOn(spectator.component, 'getAboutLinks')
                .mockReturnValue([]);

            spectator.detectChanges();
            siteService.setFakeCurrentSite({});

            expect(loadSpy).toHaveBeenCalledTimes(2); // Once on init, once on site switch
            expect(getAboutLinksSpy).toHaveBeenCalledTimes(2);
        });

        it('should update about links when site changes', () => {
            const mockAboutLinks = [{ title: 'Updated Links', items: [] }];
            jest.spyOn(spectator.component, 'getAboutLinks').mockReturnValue(mockAboutLinks);

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
            const hideSpy = jest.spyOn(overlayPanel, 'hide');

            spectator.component.hideOverlayPanel();

            expect(hideSpy).toHaveBeenCalled();
        });
    });

    describe('markAnnouncementsAsRead', () => {
        it('should call markAnnouncementsAsRead on store', () => {
            const markAsReadSpy = jest.spyOn(store, 'markAnnouncementsAsRead');

            spectator.component.markAnnouncementsAsRead();

            expect(markAsReadSpy).toHaveBeenCalled();
        });
    });

    describe('Template Rendering', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should display the announcements title', () => {
            expect(messageServiceMock.get('announcements')).toBe('Announcements');
        });

        it('should display announcements list', () => {
            const announcementsStore = spectator.component.$announcements();
            expect(announcementsStore.length).toBe(mockAnnouncementsData.entity.length);
        });

        it('should display unread badge for unread announcements', () => {
            const unreadItems = spectator
                .component
                .$announcements()
                .filter((item) => !item.hasBeenRead);

            expect(unreadItems.length).toBeGreaterThan(0);
            expect(spectator.component.$showUnreadAnnouncement()).toBe(true);
        });

        it('should display correct icons for different announcement types', () => {
            const icons = spectator.component.typesIcons;
            const announcements = spectator.component.$announcements();

            announcements.forEach((item) => {
                const icon = icons[item.type] || icons.important;
                expect(icon).toBeTruthy();
            });
        });

        it('should display announcement dates', () => {
            const announcements = spectator.component.$announcements();

            expect(announcements.length).toBeGreaterThan(0);
            announcements.forEach((item) => {
                expect(item.announcementDateAsISO8601).toBeTruthy();
            });
        });

        it('should have proper attributes on announcement links', () => {
            const announcements = spectator.component.$announcements();

            expect(announcements.length).toBeGreaterThan(0);
            announcements.forEach((item) => {
                expect(item.url).toContain('utm_source=platform');
                expect(item.url).toContain('utm_medium=announcement');
                expect(item.url).toContain('utm_campaign=');
            });
        });

        it('should display "Show All" link with proper attributes', () => {
            const linkToDotCms = spectator.component.$linkToDotCms();

            expect(messageServiceMock.get('announcements.show.all')).toBe('Show All');
            expect(linkToDotCms).toContain('announcement-menu-show-all');
            expect(linkToDotCms).toContain('utm_source=platform');
        });

        it('should display about sections with proper links', () => {
            const aboutLinks = spectator.component.$aboutLinks();

            expect(aboutLinks.length).toBe(2);
            aboutLinks.forEach((section) => {
                expect(section.items.length).toBeGreaterThan(0);
                section.items.forEach((link) => {
                    expect(link.label).toBeTruthy();
                    expect(link.url).toBeTruthy();
                });
            });
        });
    });

    describe('User Interactions', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should hide overlay panel when clicking on announcement links', () => {
            const hideOverlayPanelSpy = jest.spyOn(spectator.component, 'hideOverlayPanel');

            spectator.component.hideOverlayPanel();

            expect(hideOverlayPanelSpy).toHaveBeenCalled();
        });

        it('should hide overlay panel when clicking on "Show All" link', () => {
            const hideOverlayPanelSpy = jest.spyOn(spectator.component, 'hideOverlayPanel');

            spectator.component.hideOverlayPanel();

            expect(hideOverlayPanelSpy).toHaveBeenCalled();
        });

        it('should hide overlay panel when clicking on about links', () => {
            const hideOverlayPanelSpy = jest.spyOn(spectator.component, 'hideOverlayPanel');

            spectator.component.hideOverlayPanel();

            expect(hideOverlayPanelSpy).toHaveBeenCalled();
        });

        it('should mark announcements as read when overlay is hidden', () => {
            const markAnnouncementsAsReadSpy = jest.spyOn(store, 'markAnnouncementsAsRead');

            spectator.triggerEventHandler(DotToolbarBtnOverlayStubComponent, 'onHide', void 0);

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
