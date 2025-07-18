import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, ViewChild } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';
import { SkeletonModule } from 'primeng/skeleton';
import { TimelineModule } from 'primeng/timeline';

import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

/**
 * Component that displays content version history in the sidebar.
 * Shows a timeline of all content versions with their details.
 */
@Component({
    selector: 'dot-edit-content-sidebar-history',
    standalone: true,
    imports: [
        CommonModule,
        TimelineModule,
        AvatarModule,
        ButtonModule,
        MenuModule,
        OverlayPanelModule,
        SkeletonModule,
        DotGravatarDirective,
        DotMessagePipe,
        DotRelativeDatePipe
    ],
    templateUrl: './dot-edit-content-sidebar-history.component.html',
    styleUrls: ['./dot-edit-content-sidebar-history.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarHistoryComponent {
    /**
     * List of history items to display
     * @readonly
     */
    $historyItems = input<DotCMSContentlet[]>([], { alias: 'historyItems' });

    /**
     * Current status of the history component
     * Used to control loading states
     * @readonly
     */
    $status = input<ComponentStatus>(ComponentStatus.LOADING, { alias: 'status' });

    /**
     * Current content identifier
     * @readonly
     */
    $contentIdentifier = input<string>('', { alias: 'contentIdentifier' });

    /**
     * Determines if the history is in a loading state
     */
    readonly $isLoading = computed(() => this.$status() === ComponentStatus.LOADING);

    /**
     * Determines if there are history items to display
     */
    readonly $hasHistoryItems = computed(() => this.$historyItems().length > 0);

    /**
     * ViewChild reference to the overlay panel
     */
    @ViewChild('overlayPanel') overlayPanel!: OverlayPanel;

    /**
     * Currently selected item for the overlay
     */
    selectedItem: DotCMSContentlet | null = null;

    /**
     * Gets the status label for a history item
     */
    getStatusLabel(item: DotCMSContentlet): string {
        if (item.live && item.working) {
            return 'Published';
        } else if (item.working) {
            return 'Working';
        } else if (item.live) {
            return 'Live';
        }

        return 'Draft';
    }

    /**
     * Gets the CSS class for the status badge
     */
    getStatusClass(item: DotCMSContentlet): string {
        if (item.live && item.working) {
            return 'history__status--published';
        } else if (item.working) {
            return 'history__status--working';
        } else if (item.live) {
            return 'history__status--live';
        }

        return 'history__status--draft';
    }

    /**
     * Gets the timeline marker color based on the content status
     */
    getTimelineMarkerClass(item: DotCMSContentlet, index: number): string {
        if (index === 0) {
            return 'history__marker--current';
        } else if (item.live && item.working) {
            return 'history__marker--published';
        } else if (item.live) {
            return 'history__marker--live';
        }

        return 'history__marker--draft';
    }

    /**
     * Gets the relative time display for a version
     */
    getTimeDisplay(item: DotCMSContentlet, index: number): string {
        if (index === 0) {
            return 'Now';
        }

        // The dotRelativeDate pipe will be used in the template for other items
        return '';
    }

    /**
     * Shows the overlay panel with item details on hover
     */
    showOverlay(event: MouseEvent, item: DotCMSContentlet): void {
        this.selectedItem = item;
        this.overlayPanel.show(event);
    }

    /**
     * Hides the overlay panel when mouse leaves
     */
    hideOverlay(): void {
        this.overlayPanel.hide();
        this.selectedItem = null;
    }

    /**
     * Gets menu items for version actions
     */
    getVersionMenuItems(item: DotCMSContentlet) {
        return [
            {
                label: 'Preview',
                icon: 'pi pi-eye',
                command: () => this.onPreviewVersion(item)
            },
            {
                label: 'Restore',
                icon: 'pi pi-refresh',
                command: () => this.onRestoreVersion(item)
            },
            {
                label: 'Compare',
                icon: 'pi pi-clone',
                command: () => this.onCompareVersion(item)
            }
        ];
    }

    /**
     * Handle version preview action
     */
    onPreviewVersion(_item: DotCMSContentlet): void {
        // TODO: Implement preview functionality
    }

    /**
     * Handle version restore action
     */
    onRestoreVersion(_item: DotCMSContentlet): void {
        // TODO: Implement restore functionality
    }

    /**
     * Handle version compare action
     */
    onCompareVersion(_item: DotCMSContentlet): void {
        // TODO: Implement compare functionality
    }
}
