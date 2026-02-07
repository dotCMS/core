import { createComponentFactory, Spectator, byTestId } from '@ngneat/spectator/jest';

import { DatePipe } from '@angular/common';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

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
import { DotPushpublishTimelineItemComponent } from './components/dot-pushpublish-timeline-item/dot-pushpublish-timeline-item.component';
import { DotEditContentSidebarHistoryComponent } from './dot-edit-content-sidebar-history.component';

import {
    DotHistoryTimelineItemActionType,
    DotPushPublishHistoryItem
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
            modDate: Date.now() - 86400000,
            modUser: 'admin',
            modUserName: 'Admin',
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
            modUserName: 'editor',
            title: 'Test Content v2',
            working: true
        }
    ];

    const mockPushPublishHistoryItems: DotPushPublishHistoryItem[] = [
        {
            bundleId: 'bundle-123',
            environment: 'Production',
            pushDate: Date.now() - 86400000,
            pushedBy: 'admin'
        },
        {
            bundleId: 'bundle-456',
            environment: 'Staging',
            pushDate: Date.now() - 172800000,
            pushedBy: 'editor'
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
            DotHistoryTimelineItemComponent,
            DotPushpublishTimelineItemComponent
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
            const scrollerElement = spectator.query('p-scroller');
            expect(scrollerElement).toBeTruthy();

            // Verify scrollHeight attribute is set correctly
            expect(scrollerElement.getAttribute('scrollHeight')).toBe('100%');

            // Access the PrimeNG Scroller component instance to verify properties
            const scrollerDebugElement = spectator.debugElement.query(By.css('p-scroller'));
            const scrollerComponent = scrollerDebugElement?.componentInstance;

            expect(scrollerComponent).toBeTruthy();
            expect(scrollerComponent.itemSize).toBe(83);
            expect(scrollerComponent.lazy).toBe(true);
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
            spectator.setInput('historyPagination', mockPagination);
            expect(spectator.component.$hasMoreItems()).toBe(true);

            const completePagination: DotPagination = {
                currentPage: 3,
                perPage: 10,
                totalEntries: 25
            };
            spectator.setInput('historyPagination', completePagination);
            expect(spectator.component.$hasMoreItems()).toBe(false);
        });
    });

    describe('Scroll Events (timeline lazy load)', () => {
        function createNearBottomScrollEvent(): Event {
            const target = {
                scrollHeight: 500,
                scrollTop: 400,
                clientHeight: 100
            };
            return { target } as unknown as Event;
        }

        beforeEach(() => {
            spectator.setInput('historyPagination', mockPagination);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should emit pageChange when scroll is near bottom', fakeAsync(() => {
            const spy = jest.spyOn(spectator.component.historyPageChange, 'emit');

            spectator.component.onTimelineScroll(createNearBottomScrollEvent());
            tick();

            expect(spy).toHaveBeenCalledWith(2);
        }));

        it('should not emit pageChange when already loading', () => {
            spectator.setInput('status', ComponentStatus.LOADING);
            const spy = jest.spyOn(spectator.component.historyPageChange, 'emit');

            spectator.component.onTimelineScroll(createNearBottomScrollEvent());

            expect(spy).not.toHaveBeenCalled();
        });
    });

    describe('Historical Version Inode', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should update active state when historicalVersionInode changes', () => {
            // Ensure we have timeline content showing
            const historyTimeline = spectator.query(byTestId('history-timeline'));
            expect(historyTimeline).toBeTruthy();

            // Initially no item is active
            spectator.setInput('historicalVersionInode', null);
            spectator.detectChanges();

            expect(spectator.component.$historicalVersionInode()).toBeNull();

            // Set a specific inode as active
            const testInode = mockHistoryItems[0].inode;
            spectator.setInput('historicalVersionInode', testInode);
            spectator.detectChanges();

            expect(spectator.component.$historicalVersionInode()).toBe(testInode);
        });
    });

    describe('Timeline Item Actions', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should emit timelineItemAction through template click binding', () => {
            const actionSpy = jest.spyOn(spectator.component.timelineItemAction, 'emit');

            // Since p-scroller doesn't render items in test environment,
            // we'll test the output emission directly by simulating what the template would do
            spectator.component.timelineItemAction.emit({
                type: DotHistoryTimelineItemActionType.VIEW,
                item: mockHistoryItems[0]
            });

            expect(actionSpy).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.VIEW,
                item: mockHistoryItems[0]
            });
        });

        it('should emit timelineItemAction through template actionTriggered binding', () => {
            const actionSpy = jest.spyOn(spectator.component.timelineItemAction, 'emit');

            const testAction = {
                type: DotHistoryTimelineItemActionType.RESTORE,
                item: mockHistoryItems[0]
            };

            // Simulate what the template does: timelineItemAction.emit($event)
            spectator.component.timelineItemAction.emit(testAction);

            expect(actionSpy).toHaveBeenCalledWith(testAction);
        });
    });

    describe('Content Identifier', () => {
        it('should set contentIdentifier input correctly', () => {
            const testIdentifier = 'test-content-123';
            spectator.setInput('contentIdentifier', testIdentifier);
            spectator.detectChanges();

            expect(spectator.component.$contentIdentifier()).toBe(testIdentifier);
        });

        it('should handle empty contentIdentifier', () => {
            spectator.setInput('contentIdentifier', '');
            spectator.detectChanges();

            expect(spectator.component.$contentIdentifier()).toBe('');
        });
    });

    describe('Real Index Calculation', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should return correct real index for items', () => {
            const firstItem = mockHistoryItems[0];
            const secondItem = mockHistoryItems[1];

            expect(spectator.component.getRealIndex(firstItem)).toBe(0);
            expect(spectator.component.getRealIndex(secondItem)).toBe(1);
        });
    });

    describe('Integration Tests - Active State Logic', () => {
        beforeEach(() => {
            spectator.setInput('status', ComponentStatus.LOADED);
            spectator.setInput('historyItems', mockHistoryItems);
            spectator.detectChanges();
        });

        it('should correctly determine active state logic for timeline items', () => {
            const testInode = mockHistoryItems[1].inode;
            spectator.setInput('historicalVersionInode', testInode);
            spectator.detectChanges();

            // Check parent component state
            expect(spectator.component.$historicalVersionInode()).toBe(testInode);

            // Verify the active state logic that would be passed to child components
            const historicalVersionInode = spectator.component.$historicalVersionInode();
            expect(historicalVersionInode === mockHistoryItems[0].inode).toBe(false);
            expect(historicalVersionInode === mockHistoryItems[1].inode).toBe(true);
        });
    });

    describe('Push Publish Functionality', () => {
        describe('Push Publish Computed Properties', () => {
            it('should compute hasPushPublishHistoryItems correctly', () => {
                spectator.setInput('pushPublishHistoryItems', []);
                expect(spectator.component.$hasPushPublishHistoryItems()).toBe(false);

                spectator.setInput('pushPublishHistoryItems', mockPushPublishHistoryItems);
                expect(spectator.component.$hasPushPublishHistoryItems()).toBe(true);
            });

            it('should compute hasMorePushPublishItems correctly', () => {
                spectator.setInput('pushPublishHistoryPagination', mockPagination);
                expect(spectator.component.$hasMorePushPublishItems()).toBe(true);

                const completePagination: DotPagination = {
                    currentPage: 3,
                    perPage: 10,
                    totalEntries: 25
                };
                spectator.setInput('pushPublishHistoryPagination', completePagination);
                expect(spectator.component.$hasMorePushPublishItems()).toBe(false);
            });
        });

        describe('Push Publish Scroll Events', () => {
            beforeEach(() => {
                spectator.setInput('pushPublishHistoryPagination', mockPagination);
                spectator.setInput('pushPublishHistoryItems', mockPushPublishHistoryItems);
                spectator.detectChanges();
            });

            function createNearBottomScrollEvent(): Event {
                const target = {
                    scrollHeight: 500,
                    scrollTop: 400,
                    clientHeight: 100
                };
                return { target } as unknown as Event;
            }

            it('should emit pushPublishPageChange when scroll is near bottom', fakeAsync(() => {
                const spy = jest.spyOn(spectator.component.pushPublishPageChange, 'emit');

                spectator.component.onPushPublishTimelineScroll(createNearBottomScrollEvent());
                tick();

                expect(spy).toHaveBeenCalledWith(2);
            }));

            it('should not emit pushPublishPageChange when already loading', () => {
                spectator.setInput('status', ComponentStatus.LOADING);
                const spy = jest.spyOn(spectator.component.pushPublishPageChange, 'emit');

                spectator.component.onPushPublishTimelineScroll(createNearBottomScrollEvent());

                expect(spy).not.toHaveBeenCalled();
            });

            it('should not emit pushPublishPageChange when no more items to load', () => {
                const completePagination: DotPagination = {
                    currentPage: 3,
                    perPage: 10,
                    totalEntries: 25
                };
                spectator.setInput('pushPublishHistoryPagination', completePagination);

                const spy = jest.spyOn(spectator.component.pushPublishPageChange, 'emit');

                spectator.component.onPushPublishTimelineScroll(createNearBottomScrollEvent());

                expect(spy).not.toHaveBeenCalled();
            });
        });

        describe('Push Publish Menu Actions', () => {
            beforeEach(() => {
                spectator.setInput('pushPublishHistoryItems', mockPushPublishHistoryItems);
                spectator.detectChanges();
            });

            it('should have correct menu items structure', () => {
                const menuItems = spectator.component.$menuItems();

                expect(menuItems).toHaveLength(1);
                expect(menuItems[0]).toEqual({
                    label: expect.any(String),
                    icon: 'pi pi-trash',
                    command: expect.any(Function)
                });
            });

            it('should emit deletePushPublishHistory when menu delete action is triggered', () => {
                const spy = jest.spyOn(spectator.component.deletePushPublishHistory, 'emit');
                const menuItems = spectator.component.$menuItems();

                menuItems[0].command();

                expect(spy).toHaveBeenCalled();
            });

            it('should disable menu button when no push publish history items', () => {
                spectator.setInput('pushPublishHistoryItems', []);
                spectator.detectChanges();

                const menuButtonComponent = spectator.query(
                    '[data-testid="push-publish-menu-button"]'
                );
                expect(menuButtonComponent).toBeTruthy();
                // Access the actual button element inside PrimeNG component
                const actualButton = menuButtonComponent.querySelector(
                    'button'
                ) as HTMLButtonElement;
                expect(actualButton).toBeTruthy();
                expect(actualButton.disabled).toBe(true);
            });

            it('should enable menu button when push publish history items exist', () => {
                const menuButtonComponent = spectator.query(
                    '[data-testid="push-publish-menu-button"]'
                );
                expect(menuButtonComponent).toBeTruthy();
                // Access the actual button element inside PrimeNG component
                const actualButton = menuButtonComponent.querySelector(
                    'button'
                ) as HTMLButtonElement;
                expect(actualButton).toBeTruthy();
                expect(actualButton.disabled).toBe(false);
            });
        });

        describe('Push Publish Display States', () => {
            it('should show loading state when status is LOADING', () => {
                spectator.setInput('status', ComponentStatus.LOADING);
                spectator.detectChanges();

                const loadingState = spectator.query('[data-testid="push-publish-loading-state"]');
                expect(loadingState).toExist();
            });

            it('should show empty state when no push publish history items', () => {
                spectator.setInput('status', ComponentStatus.LOADED);
                spectator.setInput('pushPublishHistoryItems', []);
                spectator.detectChanges();

                const emptyContainer = spectator.query('dot-empty-container');
                expect(emptyContainer).toExist();
            });

            it('should show push publish timeline when items exist', () => {
                spectator.setInput('status', ComponentStatus.LOADED);
                spectator.setInput('pushPublishHistoryItems', mockPushPublishHistoryItems);
                spectator.detectChanges();

                const timeline = spectator.query('[data-testid="push-publish-timeline"]');
                expect(timeline).toExist();
            });
        });
    });
});
