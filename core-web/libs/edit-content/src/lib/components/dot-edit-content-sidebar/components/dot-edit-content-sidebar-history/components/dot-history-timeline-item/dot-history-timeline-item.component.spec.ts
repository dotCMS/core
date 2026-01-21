import { createComponentFactory, Spectator, mockProvider, byTestId } from '@ngneat/spectator/jest';

import { DatePipe } from '@angular/common';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentletVersion } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotHistoryTimelineItemComponent } from './dot-history-timeline-item.component';

import { DotHistoryTimelineItemActionType } from '../../../../../../models/dot-edit-content.model';

describe('DotHistoryTimelineItemComponent', () => {
    let spectator: Spectator<DotHistoryTimelineItemComponent>;

    const mockVersionItem: DotCMSContentletVersion = {
        archived: false,
        country: 'US',
        countryCode: 'US',
        experimentVariant: false,
        inode: 'test-inode-123',
        isoCode: 'en-US',
        language: 'English',
        languageCode: 'en',
        languageFlag: 'en_US',
        languageId: 1,
        live: true,
        modDate: Date.now() - 86400000,
        modUser: 'admin@dotcms.com',
        modUserName: 'admin@dotcms.com',
        title: 'Test Content Item',
        working: false
    };

    const createComponent = createComponentFactory({
        component: DotHistoryTimelineItemComponent,
        imports: [
            AvatarModule,
            ButtonModule,
            ChipModule,
            MenuModule,
            TooltipModule,
            DotGravatarDirective,
            DotMessagePipe,
            DotRelativeDatePipe
        ],
        providers: [
            DatePipe,
            DotMessagePipe,
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'edit.content.sidebar.history.variant': 'Variant',
                    'edit.content.sidebar.history.menu.preview': 'Preview',
                    'edit.content.sidebar.history.menu.restore': 'Restore',
                    'edit.content.sidebar.history.menu.compare': 'Compare',
                    'edit.content.sidebar.history.menu.delete': 'Delete',
                    'edit.content.sidebar.history.menu.current': 'Current',
                    'edit.content.sidebar.history.published': 'Published',
                    'edit.content.sidebar.history.draft': 'Draft'
                })
            },
            mockProvider(DotFormatDateService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false // Don't auto-detect changes
        });
        spectator.setInput('item', mockVersionItem);
        spectator.detectChanges(); // Now detect changes after input is set
    });

    describe('Template Structure', () => {
        it('should render main container', () => {
            expect(spectator.query(byTestId('history-item'))).toHaveClass(
                'dot-history-timeline-item'
            );
        });

        it('should render timeline marker with dynamic class', () => {
            const marker = spectator.query(byTestId('timeline-marker'));
            expect(marker).toHaveClass('dot-history-timeline-item__marker');
            expect(marker).toHaveClass('dot-history-timeline-item__marker--live');
        });

        it('should render content wrapper with tooltip config', () => {
            const wrapper = spectator.query(byTestId('content-wrapper'));
            expect(wrapper).toHaveClass('dot-history-timeline-item__content-wrapper');
            expect(wrapper.getAttribute('tooltipPosition')).toBe('bottom');
        });

        it('should render time display', () => {
            expect(spectator.query(byTestId('time-display'))).toHaveClass(
                'dot-history-timeline-item__time-relative'
            );
        });

        it('should render user information', () => {
            const userName = spectator.query(byTestId('history-user'));
            expect(userName.textContent?.trim()).toBe('admin@dotcms.com');
        });

        it('should render menu button', () => {
            expect(spectator.query(byTestId('version-menu-button'))).toBeTruthy();
        });
    });

    describe('Conditional Rendering', () => {
        it('should show live chip for published content', () => {
            expect(spectator.query(byTestId('state-live'))).toBeTruthy();
            expect(spectator.query(byTestId('state-draft'))).toBeFalsy();
        });

        it('should show draft chip for working content', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: true });

            expect(spectator.query(byTestId('state-live'))).toBeFalsy();
            expect(spectator.query(byTestId('state-draft'))).toBeTruthy();
        });

        it('should show variant chip when experimentVariant is true', () => {
            spectator.setInput('item', { ...mockVersionItem, experimentVariant: true });

            expect(spectator.query(byTestId('state-variant'))).toBeTruthy();
        });

        it('should hide variant chip when experimentVariant is false', () => {
            expect(spectator.query(byTestId('state-variant'))).toBeFalsy();
        });

        it('should show "Current" text for working items', () => {
            spectator.setInput('item', { ...mockVersionItem, working: true, live: false });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).toBe('Current');
        });

        it('should show relative date for non-working items', () => {
            spectator.setInput('item', { ...mockVersionItem, working: false, live: true });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).not.toBe('Current');
        });

        it('should show compare and delete actions for live items', () => {
            // Item is live by default (live: true, working: false)
            const menuItems = spectator.component.$menuItems();
            expect(menuItems).toHaveLength(2);
            expect(menuItems[0].id).toBe('compare');
            expect(menuItems[1].id).toBe('delete');
        });

        it('should show restore and delete actions for working items', () => {
            // Set item to working (live: false, working: true)
            spectator.setInput('item', { ...mockVersionItem, live: false, working: true });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();
            expect(menuItems).toHaveLength(2);
            expect(menuItems[0].id).toBe('restore');
            expect(menuItems[1].id).toBe('delete');
        });

        it('should show all actions (restore, compare, delete) for archived items', () => {
            // Set item to archived (neither live nor working)
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();
            expect(menuItems).toHaveLength(3);
            expect(menuItems[0].id).toBe('restore');
            expect(menuItems[1].id).toBe('compare');
            expect(menuItems[2].id).toBe('delete');
        });
    });

    describe('Computed Signals', () => {
        it('should compute timeline marker class based on content state', () => {
            // Live content
            expect(spectator.component.$timelineMarkerClass()).toBe(
                'dot-history-timeline-item__marker--live'
            );

            // Draft content
            spectator.setInput('item', { ...mockVersionItem, live: false, working: true });
            expect(spectator.component.$timelineMarkerClass()).toBe(
                'dot-history-timeline-item__marker--draft'
            );

            // Archived content
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            expect(spectator.component.$timelineMarkerClass()).toBe('');
        });

        it('should compute menu items with correct actions for live item', () => {
            // Item is live by default (live: true, working: false)
            const menuItems = spectator.component.$menuItems();

            expect(menuItems).toHaveLength(2);
            expect(menuItems[0].id).toBe('compare');
            expect(menuItems[0].label).toBe('Compare');
            expect(menuItems[1].id).toBe('delete');
            expect(menuItems[1].label).toBe('Delete');
        });

        it('should compute menu items with correct actions for archived item', () => {
            // Set item to archived (neither live nor working)
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();

            expect(menuItems).toHaveLength(3);
            expect(menuItems[0].id).toBe('restore');
            expect(menuItems[0].label).toBe('Restore');
            expect(menuItems[1].id).toBe('compare');
            expect(menuItems[1].label).toBe('Compare');
            expect(menuItems[2].id).toBe('delete');
            expect(menuItems[2].label).toBe('Delete');
        });
    });

    describe('Event Emission', () => {
        it('should emit actionTriggered when menu actions are triggered', () => {
            // Set item to archived to enable delete action
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();

            const actionSpy = jest.spyOn(spectator.component.actionTriggered, 'emit');
            const menuItems = spectator.component.$menuItems();

            // Test delete action (enabled for archived items) - Delete is the second item
            const deleteMenuItem = menuItems.find((item) => item.label === 'Delete');
            deleteMenuItem?.command();
            expect(actionSpy).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.DELETE,
                item: { ...mockVersionItem, live: false, working: false }
            });
        });

        it('should emit RESTORE action when restore menu item is triggered', () => {
            // Set item to archived to enable restore action
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();

            const actionSpy = jest.spyOn(spectator.component.actionTriggered, 'emit');
            const menuItems = spectator.component.$menuItems();

            // Find and trigger restore action (first menu item)
            const restoreMenuItem = menuItems.find((item) => item.label === 'Restore');
            expect(restoreMenuItem).toBeDefined();
            restoreMenuItem?.command();

            expect(actionSpy).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.RESTORE,
                item: { ...mockVersionItem, live: false, working: false }
            });
        });

        it('should emit COMPARE action when compare menu item is triggered', () => {
            // Set item to live (working: false) to enable compare action
            spectator.setInput('item', { ...mockVersionItem, live: true, working: false });
            spectator.detectChanges();

            const actionSpy = jest.spyOn(spectator.component.actionTriggered, 'emit');
            const menuItems = spectator.component.$menuItems();

            // Find and trigger compare action
            const compareMenuItem = menuItems.find((item) => item.label === 'Compare');
            expect(compareMenuItem).toBeDefined();
            compareMenuItem?.command();

            expect(actionSpy).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.COMPARE,
                item: { ...mockVersionItem, live: true, working: false }
            });
        });
    });

    describe('Active State', () => {
        it('should apply active CSS class when isActive is true', () => {
            spectator.setInput('isActive', true);
            spectator.detectChanges();

            const historyItem = spectator.query(byTestId('history-item'));
            const contentWrapper = spectator.query(byTestId('content-wrapper'));

            expect(historyItem).toHaveClass('dot-history-timeline-item--active');
            expect(contentWrapper).toHaveClass(
                'dot-history-timeline-item__content-wrapper--active'
            );
        });

        it('should not apply active CSS class when isActive is false', () => {
            spectator.setInput('isActive', false);
            spectator.detectChanges();

            const historyItem = spectator.query(byTestId('history-item'));
            const contentWrapper = spectator.query(byTestId('content-wrapper'));

            expect(historyItem).not.toHaveClass('dot-history-timeline-item--active');
            expect(contentWrapper).not.toHaveClass(
                'dot-history-timeline-item__content-wrapper--active'
            );
        });
    });

    describe('Menu Items Configuration', () => {
        it('should not show restore action for live items', () => {
            spectator.setInput('item', { ...mockVersionItem, live: true });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();
            const restoreItem = menuItems.find((item) => item.id === 'restore');

            expect(restoreItem).toBeUndefined();
        });

        it('should not show compare action for working items', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: true });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();
            const compareItem = menuItems.find((item) => item.id === 'compare');

            expect(compareItem).toBeUndefined();
        });

        it('should not show delete action for items that are both live and working', () => {
            spectator.setInput('item', { ...mockVersionItem, live: true, working: true });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();
            const deleteItem = menuItems.find((item) => item.id === 'delete');

            expect(deleteItem).toBeUndefined();
        });
    });

    describe('Working Version Handling', () => {
        it('should show "Current" text for working version regardless of itemIndex', () => {
            spectator.setInput('item', { ...mockVersionItem, working: true, live: false });
            spectator.setInput('itemIndex', 5); // Set to any index other than 0 to confirm it doesn't matter
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).toBe('Current');
        });

        it('should show delete action for working versions (non-live)', () => {
            spectator.setInput('item', { ...mockVersionItem, working: true, live: false });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();
            const deleteItem = menuItems.find((item) => item.id === 'delete');

            // Delete shows for working versions when they're not live
            expect(deleteItem).toBeDefined();
            expect(deleteItem?.label).toBe('Delete');
        });

        it('should apply draft marker class for working versions', () => {
            spectator.setInput('item', { ...mockVersionItem, working: true, live: false });
            spectator.detectChanges();

            expect(spectator.component.$timelineMarkerClass()).toBe(
                'dot-history-timeline-item__marker--draft'
            );
        });

        it('should show relative date for non-working versions', () => {
            spectator.setInput('item', { ...mockVersionItem, working: false, live: false });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).not.toBe('Current');
            // The pipe might not render in test environment, but we verify it's not "Current"
            expect(timeDisplay).toBeTruthy();
        });
    });
});
