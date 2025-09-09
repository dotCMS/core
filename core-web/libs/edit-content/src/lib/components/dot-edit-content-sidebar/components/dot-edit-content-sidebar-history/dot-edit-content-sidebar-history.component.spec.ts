import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';

import { DatePipe } from '@angular/common';
import { fakeAsync, tick } from '@angular/core/testing';

import { ScrollerLazyLoadEvent } from 'primeng/scroller';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentletVersion, DotPagination } from '@dotcms/dotcms-models';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotRelativeDatePipe,
    DotSidebarAccordionComponent,
    DotSidebarAccordionTabComponent
} from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotHistoryTimelineItemComponent } from './components/dot-history-timeline-item/dot-history-timeline-item.component';
import { DotEditContentSidebarHistoryComponent } from './dot-edit-content-sidebar-history.component';

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
            modDate: Date.now() - 86400000,
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
            modDate: Date.now() - 172800000,
            modUser: 'editor',
            title: 'Test Content v2',
            working: true
        }
    ];

    const mockPagination: DotPagination = {
        currentPage: 1,
        perPage: 10,
        totalEntries: 25
    };

    const messageServiceMock = new MockDotMessageService({
        'edit.content.sidebar.history.versions': 'Versions',
        'edit.content.sidebar.history.push.publish': 'Push Publish',
        'edit.content.sidebar.history.empty.message':
            "This content doesn't have any version history yet."
    });

    const createComponent = createComponentFactory({
        component: DotEditContentSidebarHistoryComponent,
        providers: [
            DatePipe,
            DotMessagePipe,
            DotRelativeDatePipe,
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        imports: [
            DotEmptyContainerComponent,
            DotMessagePipe,
            DotRelativeDatePipe,
            DotSidebarAccordionComponent,
            DotSidebarAccordionTabComponent,
            DotHistoryTimelineItemComponent
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false // Don't auto-detect changes
        });
        spectator.setInput('historyItems', mockHistoryItems);
        spectator.setInput('status', ComponentStatus.LOADED);

        spectator.detectChanges();
    });

    describe('Component Initialization', () => {
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
            const loadingElement = spectator.query(byTestId('loading-state'));
            expect(loadingElement).toBeTruthy();
        });

        it('should show skeleton placeholders', () => {
            const skeletonElements = spectator.queryAll('p-skeleton');
            expect(skeletonElements.length).toBeGreaterThan(0);
        });

        it('should not show history content when loading', () => {
            const historyTimeline = spectator.query(byTestId('history-timeline'));
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
            const emptyComponent = spectator.query('dot-empty-container');
            expect(emptyComponent).toBeTruthy();
        });

        it('should not show history timeline when empty', () => {
            const historyTimeline = spectator.query(byTestId('history-timeline'));
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
            const historyTimeline = spectator.query(byTestId('history-timeline'));
            expect(historyTimeline).toBeTruthy();
        });

        it('should configure p-scroller with correct properties', () => {
            const scroller = spectator.query('p-scroller');
            expect(scroller).toBeTruthy();
            expect(scroller.getAttribute('ng-reflect-item-size')).toBe('85');
            expect(scroller.getAttribute('ng-reflect-lazy')).toBe('true');
            expect(scroller.getAttribute('scrollHeight')).toBe('100%');
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

        it('should compute hasMoreItems correctly', () => {
            spectator.setInput('pagination', mockPagination);
            expect(spectator.component.$hasMoreItems()).toBe(true);

            const completePagination: DotPagination = {
                currentPage: 3,
                perPage: 10,
                totalEntries: 25
            };
            spectator.setInput('pagination', completePagination);
            expect(spectator.component.$hasMoreItems()).toBe(false);
        });
    });

    describe('Scroll Events', () => {
        beforeEach(() => {
            spectator.setInput('pagination', mockPagination);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should emit pageChange when loading next page', fakeAsync(() => {
            const spy = jest.spyOn(spectator.component.pageChange, 'emit');

            const scrollEvent: ScrollerLazyLoadEvent = {
                first: 0,
                last: mockHistoryItems.length - 3
            };

            spectator.component.onScrollIndexChange(scrollEvent);
            tick();

            expect(spy).toHaveBeenCalledWith(2);
        }));

        it('should not emit pageChange when already loading', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            const spy = jest.spyOn(spectator.component.pageChange, 'emit');

            const scrollEvent: ScrollerLazyLoadEvent = {
                first: 0,
                last: mockHistoryItems.length - 3
            };

            spectator.component.onScrollIndexChange(scrollEvent);

            expect(spy).not.toHaveBeenCalled();
        });
    });
});
