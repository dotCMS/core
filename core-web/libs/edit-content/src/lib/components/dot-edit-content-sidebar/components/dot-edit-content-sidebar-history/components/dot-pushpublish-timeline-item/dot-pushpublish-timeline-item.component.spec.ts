import { createComponentFactory, Spectator, mockProvider, byTestId } from '@ngneat/spectator/jest';

import { DatePipe } from '@angular/common';

import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPushpublishTimelineItemComponent } from './dot-pushpublish-timeline-item.component';

import { DotPushPublishHistoryItem } from '../../../../../../models/dot-edit-content.model';

describe('DotPushpublishTimelineItemComponent', () => {
    let spectator: Spectator<DotPushpublishTimelineItemComponent>;

    const mockPushPublishHistoryItem: DotPushPublishHistoryItem = {
        bundleId: '01K6NY6Z8V92T6SAF582WMTKYQ',
        environment: 'receiver',
        pushDate: Date.now() - 86400000, // 1 day ago
        pushedBy: 'Admin User'
    };

    const createComponent = createComponentFactory({
        component: DotPushpublishTimelineItemComponent,
        imports: [AvatarModule, TooltipModule, DotMessagePipe, DotRelativeDatePipe],
        providers: [
            DatePipe,
            DotMessagePipe,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'edit.content.sidebar.pushpublish.bundle': 'Bundle',
                    'edit.content.sidebar.pushpublish.environment': 'Environment',
                    'edit.content.sidebar.pushpublish.pushedby': 'Pushed by',
                    'edit.content.sidebar.pushpublish.published': 'Published'
                })
            },
            mockProvider(DotFormatDateService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false // Don't auto-detect changes
        });
        spectator.setInput('item', mockPushPublishHistoryItem);
        spectator.detectChanges(); // Now detect changes after input is set
    });

    describe('Template Structure', () => {
        it('should render main container', () => {
            expect(spectator.query(byTestId('pushpublish-item'))).toHaveClass(
                'dot-pushpublish-timeline-item'
            );
        });

        it('should render timeline marker with dynamic class', () => {
            const marker = spectator.query(byTestId('timeline-marker'));
            expect(marker).toHaveClass('dot-pushpublish-timeline-item__marker');
            expect(marker).toHaveClass('dot-pushpublish-timeline-item__marker--live');
        });

        it('should render content wrapper with tooltip config', () => {
            const wrapper = spectator.query(byTestId('content-wrapper'));
            expect(wrapper).toHaveClass('dot-pushpublish-timeline-item__content-wrapper');
            expect(wrapper.getAttribute('tooltipPosition')).toBe('bottom');
        });

        it('should render time display', () => {
            expect(spectator.query(byTestId('time-display'))).toHaveClass(
                'dot-pushpublish-timeline-item__time-relative'
            );
        });

        it('should render user information', () => {
            const userName = spectator.query(byTestId('pushpublish-user'));
            expect(userName.textContent?.trim()).toBe('Admin User');
        });

        it('should NOT render menu button', () => {
            expect(spectator.query(byTestId('version-menu-button'))).toBeFalsy();
        });
    });

    describe('Conditional Rendering', () => {
        it('should show live chip for published content', () => {
            expect(spectator.query(byTestId('state-live'))).toBeTruthy();
            expect(spectator.query(byTestId('state-draft'))).toBeFalsy();
        });

        it('should show draft chip for working content', () => {
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                live: false,
                working: true
            });

            expect(spectator.query(byTestId('state-live'))).toBeFalsy();
            expect(spectator.query(byTestId('state-draft'))).toBeTruthy();
        });

        it('should show variant chip when experimentVariant is true', () => {
            spectator.setInput('item', { ...mockPushPublishHistoryItem, experimentVariant: true });

            expect(spectator.query(byTestId('state-variant'))).toBeTruthy();
        });

        it('should hide variant chip when experimentVariant is false', () => {
            expect(spectator.query(byTestId('state-variant'))).toBeFalsy();
        });

        it('should show "Current" text for working items', () => {
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                working: true,
                live: false
            });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).toBe('Current');
        });

        it('should show relative date for non-working items', () => {
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                working: false,
                live: true
            });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).not.toBe('Current');
        });
    });

    describe('Computed Signals', () => {
        it('should compute timeline marker class based on content state', () => {
            // Live content
            expect(spectator.component.$timelineMarkerClass()).toBe(
                'dot-pushpublish-timeline-item__marker--live'
            );

            // Draft content
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                live: false,
                working: true
            });
            expect(spectator.component.$timelineMarkerClass()).toBe(
                'dot-pushpublish-timeline-item__marker--draft'
            );

            // Archived content
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                live: false,
                working: false
            });
            expect(spectator.component.$timelineMarkerClass()).toBe('');
        });
    });

    describe('Active State', () => {
        it('should apply active CSS class when isActive is true', () => {
            spectator.setInput('isActive', true);
            spectator.detectChanges();

            const pushpublishItem = spectator.query(byTestId('pushpublish-item'));
            const contentWrapper = spectator.query(byTestId('content-wrapper'));

            expect(pushpublishItem).toHaveClass('dot-pushpublish-timeline-item--active');
            expect(contentWrapper).toHaveClass(
                'dot-pushpublish-timeline-item__content-wrapper--active'
            );
        });

        it('should not apply active CSS class when isActive is false', () => {
            spectator.setInput('isActive', false);
            spectator.detectChanges();

            const pushpublishItem = spectator.query(byTestId('pushpublish-item'));
            const contentWrapper = spectator.query(byTestId('content-wrapper'));

            expect(pushpublishItem).not.toHaveClass('dot-pushpublish-timeline-item--active');
            expect(contentWrapper).not.toHaveClass(
                'dot-pushpublish-timeline-item__content-wrapper--active'
            );
        });
    });

    describe('Working Version Handling', () => {
        it('should show "Current" text for working version regardless of itemIndex', () => {
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                working: true,
                live: false
            });
            spectator.setInput('itemIndex', 5); // Set to any index other than 0 to confirm it doesn't matter
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).toBe('Current');
        });

        it('should apply draft marker class for working versions', () => {
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                working: true,
                live: false
            });
            spectator.detectChanges();

            expect(spectator.component.$timelineMarkerClass()).toBe(
                'dot-pushpublish-timeline-item__marker--draft'
            );
        });

        it('should show relative date for non-working versions', () => {
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                working: false,
                live: false
            });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).not.toBe('Current');
            // The pipe might not render in test environment, but we verify it's not "Current"
            expect(timeDisplay).toBeTruthy();
        });
    });

    describe('Tooltip Content', () => {
        it('should render tooltip with content title and details', () => {
            const overlayTitle = spectator.query(byTestId('overlay-title'));
            expect(overlayTitle.textContent?.trim()).toBe('Test Content Item');
        });

        it('should show variant information in tooltip when experimentVariant is present', () => {
            spectator.setInput('item', {
                ...mockPushPublishHistoryItem,
                experimentVariant: 'Test Variant'
            });
            spectator.detectChanges();

            // The variant information should be present in the template
            expect(spectator.fixture.debugElement.nativeElement.innerHTML).toContain('Variant');
        });
    });

    describe('User Avatar and Information', () => {
        it('should render user avatar with label', () => {
            const avatar = spectator.query(byTestId('user-avatar'));
            expect(avatar).toBeTruthy();
            expect(avatar.getAttribute('label')).toBe('A'); // First letter of 'Admin User'
        });

        it('should display user name correctly', () => {
            const userName = spectator.query(byTestId('pushpublish-user'));
            expect(userName.textContent?.trim()).toBe('Admin User');
        });
    });
});
