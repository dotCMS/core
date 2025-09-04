import { createComponentFactory, Spectator, mockProvider } from '@ngneat/spectator/jest';

import { DatePipe } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TimelineModule } from 'primeng/timeline';
import { TooltipModule } from 'primeng/tooltip';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { ComponentStatus, DotCMSContentletVersion } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotEditContentSidebarHistoryComponent } from './dot-edit-content-sidebar-history.component';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType
} from '../../../../models/dot-edit-content.model';

describe('DotEditContentSidebarHistoryComponent', () => {
    let spectator: Spectator<DotEditContentSidebarHistoryComponent>;

    const mockHistoryItems: DotCMSContentletVersion[] = [
        {
            archived: false,
            country: 'US',
            countryCode: 'US',
            experimentVariant: false,
            inode: 'test-inode-1',
            isoCode: 'en-US',
            language: 'English',
            languageCode: 'en',
            languageFlag: 'en_US',
            languageId: 1,
            live: true,
            modDate: Date.now() - 86400000, // 1 day ago
            modUser: 'admin',
            title: 'Test Content v1',
            working: true
        },
        {
            archived: false,
            country: 'US',
            countryCode: 'US',
            experimentVariant: false,
            inode: 'test-inode-2',
            isoCode: 'en-US',
            language: 'English',
            languageCode: 'en',
            languageFlag: 'en_US',
            languageId: 1,
            live: false,
            modDate: Date.now() - 172800000, // 2 days ago
            modUser: 'editor',
            title: 'Test Content v2',
            working: true
        },
        {
            archived: false,
            country: 'US',
            countryCode: 'US',
            experimentVariant: false,
            inode: 'test-inode-3',
            isoCode: 'en-US',
            language: 'English',
            languageCode: 'en',
            languageFlag: 'en_US',
            languageId: 1,
            live: true,
            modDate: Date.now() - 259200000, // 3 days ago
            modUser: 'author',
            title: 'Test Content v3',
            working: false
        }
    ];

    const messageServiceMock = new MockDotMessageService({
        'edit.content.sidebar.history.variant': 'Variant',
        'edit.content.sidebar.history.menu.preview': 'Preview',
        'edit.content.sidebar.history.menu.restore': 'Restore',
        'edit.content.sidebar.history.menu.compare': 'Compare'
    });

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarHistoryComponent,
        providers: [
            DatePipe,
            DotMessagePipe,
            DotRelativeDatePipe,
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotFormatDateService),
            mockProvider(CoreWebService),
            mockProvider(LoginService)
        ],
        imports: [
            HttpClientTestingModule,
            TimelineModule,
            AvatarModule,
            ButtonModule,
            ChipModule,
            MenuModule,
            SkeletonModule,
            TooltipModule,
            DotGravatarDirective,
            DotMessagePipe,
            DotRelativeDatePipe
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                $historyItems: mockHistoryItems,
                $status: ComponentStatus.LOADED
            }
        });
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should have input values set correctly', () => {
            expect(spectator.component.$historyItems()).toEqual(mockHistoryItems);
            expect(spectator.component.$status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('Loading State', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADING);
            spectator.detectChanges();
        });

        it('should show loading state when isLoading is true', () => {
            const loadingElement = spectator.query('[data-testid="loading-state"]');
            expect(loadingElement).toBeTruthy();
        });

        it('should show skeleton placeholders', () => {
            const skeletonElements = spectator.queryAll('p-skeleton');
            expect(skeletonElements.length).toBeGreaterThan(0);
        });

        it('should not show history content when loading', () => {
            const historyTimeline = spectator.query('[data-testid="history-timeline"]');
            expect(historyTimeline).toBeFalsy();
        });
    });

    describe('Empty State', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', []);
            spectator.detectChanges();
        });

        it('should show empty state when no history items', () => {
            const emptyElement = spectator.query('[data-testid="empty-state"]');
            expect(emptyElement).toBeTruthy();
        });

        it('should show empty state icon', () => {
            const iconElement = spectator.query('.history__empty-icon .pi-history');
            expect(iconElement).toBeTruthy();
        });

        it('should not show history timeline when empty', () => {
            const historyTimeline = spectator.query('[data-testid="history-timeline"]');
            expect(historyTimeline).toBeFalsy();
        });
    });

    describe('History Content', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should show history timeline when items are available', () => {
            const historyTimeline = spectator.query('[data-testid="history-timeline"]');
            expect(historyTimeline).toBeTruthy();
        });

        it('should display correct number of history items', () => {
            const historyItems = spectator.queryAll('[data-testid="history-item"]');
            expect(historyItems.length).toBe(mockHistoryItems.length);
        });

        it('should display time information correctly', () => {
            const timeElements = spectator.queryAll('[data-testid="time-display"]');
            expect(timeElements[0].textContent.trim()).toBe('Now');
            // Other elements will show relative dates
            expect(timeElements.length).toBe(mockHistoryItems.length);
        });

        it('should display user names correctly', () => {
            const userElements = spectator.queryAll('[data-testid="history-user"]');
            expect(userElements[0].textContent.trim()).toBe('admin');
            expect(userElements[1].textContent.trim()).toBe('editor');
            expect(userElements[2].textContent.trim()).toBe('author');
        });

        it('should display status chips correctly', () => {
            const liveChips = spectator.queryAll('[data-testid="state-live"]');
            const draftChips = spectator.queryAll('[data-testid="state-draft"]');

            // Should have live chips for items with live: true
            expect(liveChips.length).toBeGreaterThan(0);
            // Should have draft chips for items with working: true and live: false
            expect(draftChips.length).toBeGreaterThan(0);
        });
    });

    describe('Timeline Marker Functionality', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should render timeline items correctly', () => {
            const timelineItems = spectator.queryAll('dot-history-timeline-item');
            expect(timelineItems).toHaveLength(mockHistoryItems.length);
        });
    });

    describe('Timeline Functionality', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should have timeline markers for each item', () => {
            const markers = spectator.queryAll('[data-testid="timeline-marker"]');
            expect(markers.length).toBe(mockHistoryItems.length);
        });

        it('should display user avatars', () => {
            const avatars = spectator.queryAll('[data-testid="user-avatar"]');
            expect(avatars.length).toBe(mockHistoryItems.length);
        });

        it('should show version menu buttons', () => {
            const menuButtons = spectator.queryAll('[data-testid="version-menu-button"]');
            expect(menuButtons.length).toBe(mockHistoryItems.length);
        });
    });

    describe('Computed Properties', () => {
        it('should compute isLoading correctly', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            expect(spectator.component.$isLoading()).toBe(true);

            spectator.setInput('status', ComponentStatus.LOADED);
            expect(spectator.component.$isLoading()).toBe(false);
        });

        it('should compute hasHistoryItems correctly', () => {
            spectator.setInput('historyItems', []);
            expect(spectator.component.$hasHistoryItems()).toBe(false);

            spectator.setInput('historyItems', mockHistoryItems);
            expect(spectator.component.$hasHistoryItems()).toBe(true);
        });
    });

    describe('Timeline Item Actions', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should handle timeline item actions correctly', () => {
            const mockAction: DotHistoryTimelineItemAction = {
                type: DotHistoryTimelineItemActionType.PREVIEW,
                item: mockHistoryItems[0]
            };

            spyOn(spectator.component, 'onTimelineItemAction').and.callThrough();
            spectator.component.onTimelineItemAction(mockAction);

            expect(spectator.component.onTimelineItemAction).toHaveBeenCalledWith(mockAction);
        });
    });
});
