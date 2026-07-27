import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentletVersion } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe } from '@dotcms/ui';
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
            TagModule,
            MenuModule,
            DotGravatarDirective,
            DotMessagePipe
        ],
        providers: [
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
            }
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
            expect(spectator.query(byTestId('history-item'))).toBeTruthy();
        });

        it('should not render a tooltip on the content wrapper', () => {
            const wrapper = spectator.query(byTestId('content-wrapper'));
            expect(wrapper).toBeTruthy();
            expect(wrapper.getAttribute('tooltipPosition')).toBeNull();
            expect(spectator.query(byTestId('overlay-title'))).toBeFalsy();
        });

        it('should render time display', () => {
            expect(spectator.query(byTestId('time-display'))).toBeTruthy();
        });

        it('should render user information', () => {
            const userName = spectator.query(byTestId('history-user'));
            expect(userName.textContent?.trim()).toBe('admin@dotcms.com');
        });

        it('should render menu button when item has actions', () => {
            expect(spectator.query(byTestId('version-menu-button'))).toBeTruthy();
        });

        it('should hide menu button for draft items (no actions)', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: true });
            spectator.detectChanges();

            expect(spectator.query(byTestId('version-menu-button'))).toBeFalsy();
        });
    });

    describe('Conditional Rendering', () => {
        it('should show a success live tag with the published label for published content', () => {
            const liveTag = spectator.query(byTestId('state-live'));
            expect(liveTag).toBeTruthy();
            expect(liveTag?.textContent?.trim()).toBe('Published');
            expect(liveTag?.classList.contains('p-tag-success')).toBe(true);
            expect(spectator.query(byTestId('state-draft'))).toBeFalsy();
        });

        it('should show a warn draft tag with the draft label for working content', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: true });
            spectator.detectChanges();

            const draftTag = spectator.query(byTestId('state-draft'));
            expect(spectator.query(byTestId('state-live'))).toBeFalsy();
            expect(draftTag).toBeTruthy();
            expect(draftTag?.textContent?.trim()).toBe('Draft');
            expect(draftTag?.classList.contains('p-tag-warn')).toBe(true);
        });

        it('should show an info variant tag when experimentVariant is true', () => {
            spectator.setInput('item', { ...mockVersionItem, experimentVariant: true });
            spectator.detectChanges();

            const variantTag = spectator.query(byTestId('state-variant'));
            expect(variantTag).toBeTruthy();
            expect(variantTag?.classList.contains('p-tag-info')).toBe(true);
        });

        it('should hide variant tag when experimentVariant is false', () => {
            expect(spectator.query(byTestId('state-variant'))).toBeFalsy();
        });

        it('should show a spinner while the version is being fetched', () => {
            spectator.setInput('isLoadingVersion', true);
            spectator.detectChanges();

            expect(spectator.query(byTestId('version-loading-spinner'))).toBeTruthy();
        });

        it('should not show a spinner when the version is not being fetched', () => {
            expect(spectator.query(byTestId('version-loading-spinner'))).toBeFalsy();
        });

        it('should show the exact date/time — not "Current" — for the working (most recent) version', () => {
            spectator.setInput('item', {
                ...mockVersionItem,
                working: true,
                live: false,
                modDate: new Date(2026, 4, 16, 13, 10).getTime()
            });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).toBe('May 16, 2026 - 1:10 PM');
        });

        it('should show the exact date/time for non-working versions', () => {
            spectator.setInput('item', {
                ...mockVersionItem,
                working: false,
                live: true,
                modDate: new Date(2026, 4, 16, 13, 10).getTime()
            });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).toBe('May 16, 2026 - 1:10 PM');
        });

        it.each([
            [new Date(2026, 4, 16, 0, 0).getTime(), 'May 16, 2026 - 12:00 AM'],
            [new Date(2026, 4, 16, 12, 0).getTime(), 'May 16, 2026 - 12:00 PM'],
            [new Date(2026, 4, 16, 23, 59).getTime(), 'May 16, 2026 - 11:59 PM']
        ])('should format %s as "%s" (AM/PM boundaries)', (modDate, expected) => {
            spectator.setInput('item', { ...mockVersionItem, modDate });
            spectator.detectChanges();

            const timeDisplay = spectator.query(byTestId('time-display'));
            expect(timeDisplay.textContent?.trim()).toBe(expected);
        });

        it('should never render a relative-time string (e.g. "now", "ago") for any version', () => {
            for (const overrides of [
                { working: true, live: false },
                { working: false, live: true },
                { working: false, live: false }
            ]) {
                spectator.setInput('item', { ...mockVersionItem, ...overrides });
                spectator.detectChanges();

                const timeDisplay = spectator.query(byTestId('time-display'));
                expect(timeDisplay.textContent?.trim()).not.toMatch(/now|ago|current/i);
            }
        });
    });

    describe('Menu Items by Status', () => {
        it('should expose no actions for working items (working && !live)', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: true });
            spectator.detectChanges();

            expect(spectator.component.$menuItems()).toHaveLength(0);
        });

        it('should expose no actions for the current published version (working && live)', () => {
            spectator.setInput('item', { ...mockVersionItem, live: true, working: true });
            spectator.detectChanges();

            expect(spectator.component.$menuItems()).toHaveLength(0);
        });

        it('should expose restore, separator and compare for published items (live)', () => {
            // Default item is live: true, working: false
            const menuItems = spectator.component.$menuItems();
            expect(menuItems).toHaveLength(3);
            expect(menuItems[0].id).toBe('restore');
            expect(menuItems[0].label).toBe('Restore');
            expect(menuItems[1].separator).toBe(true);
            expect(menuItems[2].id).toBe('compare');
            expect(menuItems[2].label).toBe('Compare');
        });

        it('should expose restore, compare, separator and delete for historical items (!working && !live)', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();
            expect(menuItems).toHaveLength(4);
            expect(menuItems[0].id).toBe('restore');
            expect(menuItems[1].id).toBe('compare');
            expect(menuItems[2].separator).toBe(true);
            expect(menuItems[3].id).toBe('delete');
        });

        it('should always place separator before the last non-separator item', () => {
            // Published: [restore, separator, compare] — separator before last (compare)
            const publishedItems = spectator.component.$menuItems();
            const lastPublished = publishedItems[publishedItems.length - 1];
            const secondToLastPublished = publishedItems[publishedItems.length - 2];
            expect(lastPublished.separator).toBeFalsy();
            expect(secondToLastPublished.separator).toBe(true);

            // Historical: [restore, compare, separator, delete] — separator before last (delete)
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();
            const historicalItems = spectator.component.$menuItems();
            const lastHistorical = historicalItems[historicalItems.length - 1];
            const secondToLastHistorical = historicalItems[historicalItems.length - 2];
            expect(lastHistorical.separator).toBeFalsy();
            expect(secondToLastHistorical.separator).toBe(true);
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

            // Historical content
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            expect(spectator.component.$timelineMarkerClass()).toBe('');
        });
    });

    describe('Event Emission', () => {
        it('should emit RESTORE action when restore menu item is triggered', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();

            const actionSpy = jest.spyOn(spectator.component.actionTriggered, 'emit');
            const restoreMenuItem = spectator.component
                .$menuItems()
                .find((item) => item.id === 'restore');

            expect(restoreMenuItem).toBeDefined();
            restoreMenuItem?.command?.({} as never);

            expect(actionSpy).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.RESTORE,
                item: { ...mockVersionItem, live: false, working: false }
            });
        });

        it('should emit COMPARE action when compare menu item is triggered', () => {
            spectator.setInput('item', { ...mockVersionItem, live: true, working: false });
            spectator.detectChanges();

            const actionSpy = jest.spyOn(spectator.component.actionTriggered, 'emit');
            const compareMenuItem = spectator.component
                .$menuItems()
                .find((item) => item.id === 'compare');

            expect(compareMenuItem).toBeDefined();
            compareMenuItem?.command?.({} as never);

            expect(actionSpy).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.COMPARE,
                item: { ...mockVersionItem, live: true, working: false }
            });
        });

        it('should emit DELETE action when delete menu item is triggered', () => {
            spectator.setInput('item', { ...mockVersionItem, live: false, working: false });
            spectator.detectChanges();

            const actionSpy = jest.spyOn(spectator.component.actionTriggered, 'emit');
            const deleteMenuItem = spectator.component
                .$menuItems()
                .find((item) => item.id === 'delete');

            expect(deleteMenuItem).toBeDefined();
            deleteMenuItem?.command?.({} as never);

            expect(actionSpy).toHaveBeenCalledWith({
                type: DotHistoryTimelineItemActionType.DELETE,
                item: { ...mockVersionItem, live: false, working: false }
            });
        });
    });

    describe('Menu Open State ($isMenuOpen)', () => {
        it('should initialize $isMenuOpen as false', () => {
            expect(spectator.component.$isMenuOpen()).toBe(false);
        });

        it('should show the menu button wrapper as fully visible when $isMenuOpen is true', () => {
            spectator.component.$isMenuOpen.set(true);
            spectator.detectChanges();

            const menuWrapper = spectator
                .query('[data-testid="version-menu-button"]')
                ?.closest('div');
            expect(menuWrapper?.classList.contains('opacity-100')).toBe(true);
        });

        it('should show the menu button wrapper as hover-only when $isMenuOpen is false', () => {
            spectator.component.$isMenuOpen.set(false);
            spectator.detectChanges();

            const menuWrapper = spectator
                .query('[data-testid="version-menu-button"]')
                ?.closest('div');
            expect(menuWrapper?.classList.contains('opacity-0')).toBe(true);
            expect(menuWrapper?.classList.contains('group-hover:opacity-100')).toBe(true);
        });
    });

    describe('Active State', () => {
        it('should render when isActive is true', () => {
            spectator.setInput('isActive', true);
            spectator.detectChanges();

            const historyItem = spectator.query(byTestId('history-item'));
            const contentWrapper = spectator.query(byTestId('content-wrapper'));

            expect(historyItem).toBeTruthy();
            expect(contentWrapper).toBeTruthy();
        });

        it('should apply active highlight class when isActive is true', () => {
            spectator.setInput('isActive', true);
            spectator.detectChanges();

            const wrapper = spectator.query(byTestId('content-wrapper'));
            expect(wrapper?.classList.contains('bg-primary-50')).toBe(true);
        });

        it('should not apply active highlight class when isActive is false', () => {
            spectator.setInput('isActive', false);
            spectator.detectChanges();

            const wrapper = spectator.query(byTestId('content-wrapper'));
            expect(wrapper?.classList.contains('bg-primary-50')).toBe(false);
        });
    });
});
