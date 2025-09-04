import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, inject, output } from '@angular/core';

import { ScrollerModule, ScrollerLazyLoadEvent } from 'primeng/scroller';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { ComponentStatus, DotCMSContentletVersion } from '@dotcms/dotcms-models';
import {
    DotMessagePipe,
    DotSidebarAccordionComponent,
    DotSidebarAccordionTabComponent
} from '@dotcms/ui';

import { DotHistoryTimelineItemComponent } from './components/dot-history-timeline-item/dot-history-timeline-item.component';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType,
    DotHistoryPagination
} from '../../../../models/dot-edit-content.model';

/**
 * Component that displays content version history in the sidebar.
 * Shows a timeline of all content versions with their details.
 */
@Component({
    selector: 'dot-edit-content-sidebar-history',
    standalone: true,
    imports: [
        CommonModule,
        ScrollerModule,
        SkeletonModule,
        TooltipModule,
        DotMessagePipe,
        DotSidebarAccordionComponent,
        DotSidebarAccordionTabComponent,
        DotHistoryTimelineItemComponent
    ],
    providers: [DatePipe, DotMessagePipe],
    templateUrl: './dot-edit-content-sidebar-history.component.html',
    styleUrls: ['./dot-edit-content-sidebar-history.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarHistoryComponent {
    private readonly dotMessagePipe = inject(DotMessagePipe);

    /**
     * List of history items to display (accumulated items from store)
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
     * Determines if there are more items to load for infinite scroll
     */
    readonly $hasMoreItems = computed(() => {
        const pagination = this.$pagination();
        return pagination && pagination.currentPage * pagination.perPage < pagination.totalEntries;
    });

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
     * Handle timeline item actions
     */
    onTimelineItemAction(action: DotHistoryTimelineItemAction): void {
        switch (action.type) {
            case DotHistoryTimelineItemActionType.PREVIEW:
                this.onPreviewVersion(action.item);
                break;
            case DotHistoryTimelineItemActionType.RESTORE:
                this.onRestoreVersion(action.item);
                break;
            case DotHistoryTimelineItemActionType.COMPARE:
                this.onCompareVersion(action.item);
                break;
            default:
                console.warn('Unknown timeline item action type:', action.type);
        }
    }

    /**
     * Handle version preview action
     */
    private onPreviewVersion(_item: DotCMSContentletVersion): void {
        // TODO: Implement preview functionality
    }

    /**
     * Handle version restore action
     */
    private onRestoreVersion(_item: DotCMSContentletVersion): void {
        // TODO: Implement restore functionality
    }

    /**
     * Handle version compare action
     */
    private onCompareVersion(_item: DotCMSContentletVersion): void {
        // TODO: Implement compare functionality
    }

    /**
     * Handle infinite scroll when user scrolls near the end
     */
    onScrollIndexChange(event: ScrollerLazyLoadEvent): void {
        if (this.shouldLoadMore(event) && !this.$isLoading()) {
            this.loadNextPage();
        }
    }

    /**
     * Determine if we should load more items based on scroll position
     */
    private shouldLoadMore(event: ScrollerLazyLoadEvent): boolean {
        const { last } = event;
        const totalItems = this.$historyItems().length;
        const threshold = 5; // Load when 5 items remaining

        return totalItems - last <= threshold && this.$hasMoreItems();
    }

    /**
     * Load the next page of history items
     */
    private loadNextPage(): void {
        const pagination = this.$pagination();
        if (pagination && this.$hasMoreItems()) {
            this.pageChange.emit(pagination.currentPage + 1);
        }
    }

    /**
     * Handle accordion tab change
     */
    onAccordionTabChange(_activeTab: string | null): void {
        // Aquí puedes agregar lógica adicional si es necesaria
        // Por ejemplo, tracking de analytics, etc.
        // console.log('Active tab changed to:', activeTab);
    }
}
