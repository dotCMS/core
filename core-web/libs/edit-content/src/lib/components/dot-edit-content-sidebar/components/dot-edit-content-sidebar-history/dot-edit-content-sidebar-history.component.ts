import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, inject, output } from '@angular/core';

import { AccordionModule } from 'primeng/accordion';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TimelineModule } from 'primeng/timeline';
import { TooltipModule } from 'primeng/tooltip';

import { ComponentStatus, DotCMSContentletVersion } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

/**
 * Interface for pagination data
 */
export interface DotHistoryPagination {
    currentPage: number;
    perPage: number;
    totalEntries: number;
}

/**
 * Component that displays content version history in the sidebar.
 * Shows a timeline of all content versions with their details.
 */
@Component({
    selector: 'dot-edit-content-sidebar-history',
    standalone: true,
    imports: [
        CommonModule,
        AccordionModule,
        TimelineModule,
        AvatarModule,
        ButtonModule,
        MenuModule,
        SkeletonModule,
        TooltipModule,
        DotGravatarDirective,
        DotMessagePipe,
        DotRelativeDatePipe,
        ChipModule
    ],
    providers: [DatePipe, DotMessagePipe],
    templateUrl: './dot-edit-content-sidebar-history.component.html',
    styleUrls: ['./dot-edit-content-sidebar-history.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarHistoryComponent {
    private datePipe = inject(DatePipe);
    private dotMessagePipe = inject(DotMessagePipe);

    // Estado del accordion personalizado
    activeTab: 'versions' | 'push-publish' | null = 'versions';
    /**
     * List of history items to display
     * @readonly
     */
    $historyItems = input<DotCMSContentletVersion[]>([], { alias: 'historyItems' });

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
     * Pagination data for history items
     * @readonly
     */
    $pagination = input<DotHistoryPagination | null>(null, { alias: 'pagination' });

    /**
     * Event emitted when page changes
     */
    pageChange = output<number>();

    /**
     * Determines if the history is in a loading state
     */
    readonly $isLoading = computed(() => this.$status() === ComponentStatus.LOADING);

    /**
     * Determines if there are history items to display
     */
    readonly $hasHistoryItems = computed(() => this.$historyItems().length > 0);

    /**
     * Determines if pagination should be shown
     */
    readonly $showPagination = computed(() => {
        const pagination = this.$pagination();
        return pagination && pagination.totalEntries > pagination.perPage;
    });

    /**
     * Determines if previous page button should be enabled
     */
    readonly $canGoPrevious = computed(() => {
        const pagination = this.$pagination();
        return pagination && pagination.currentPage > 1;
    });

    /**
     * Determines if next page button should be enabled
     */
    readonly $canGoNext = computed(() => {
        const pagination = this.$pagination();
        return pagination && pagination.currentPage * pagination.perPage < pagination.totalEntries;
    });

    /**
     * Cached translations map for all labels used in the component
     */
    private readonly labels = {
        preview: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.preview'),
        restore: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.restore'),
        compare: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.compare')
    } as const;

    /**
     * Gets the timeline marker color based on the content status
     */
    getTimelineMarkerClass(item: DotCMSContentletVersion | undefined): string {
        // Safety check for undefined item
        if (!item) {
            return '';
        }

        if (item.live) {
            return 'history__marker--live';
        } else if (item.working) {
            return 'history__marker--draft';
        }

        return '';
    }

    /**
     * Gets menu items for version actions
     */
    getVersionMenuItems(item: DotCMSContentletVersion | undefined) {
        // Safety check for undefined item
        if (!item) {
            return [];
        }

        return [
            {
                label: this.labels.preview,
                command: () => this.onPreviewVersion(item)
            },
            {
                label: this.labels.restore,
                command: () => this.onRestoreVersion(item)
            },
            {
                label: this.labels.compare,
                command: () => this.onCompareVersion(item)
            }
        ];
    }

    /**
     * Handle version preview action
     */
    onPreviewVersion(_item: DotCMSContentletVersion): void {
        // TODO: Implement preview functionality
    }

    /**
     * Handle version restore action
     */
    onRestoreVersion(_item: DotCMSContentletVersion): void {
        // TODO: Implement restore functionality
    }

    /**
     * Handle version compare action
     */
    onCompareVersion(_item: DotCMSContentletVersion): void {
        // TODO: Implement compare functionality
    }

    /**
     * Handle previous page navigation
     */
    onPreviousPage(): void {
        const pagination = this.$pagination();
        if (pagination && this.$canGoPrevious()) {
            this.pageChange.emit(pagination.currentPage - 1);
        }
    }

    /**
     * Handle next page navigation
     */
    onNextPage(): void {
        const pagination = this.$pagination();
        if (pagination && this.$canGoNext()) {
            this.pageChange.emit(pagination.currentPage + 1);
        }
    }

    /**
     * Toggle accordion tab
     */
    toggleTab(tab: 'versions' | 'push-publish'): void {
        this.activeTab = this.activeTab === tab ? null : tab;
    }
}
