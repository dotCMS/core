import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, inject, output } from '@angular/core';

import { ScrollerModule, ScrollerLazyLoadEvent } from 'primeng/scroller';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { ComponentStatus, DotCMSContentletVersion, DotPagination } from '@dotcms/dotcms-models';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotSidebarAccordionComponent,
    DotSidebarAccordionTabComponent
} from '@dotcms/ui';

import { DotHistoryTimelineItemComponent } from './components/dot-history-timeline-item/dot-history-timeline-item.component';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType
} from '../../../../models/dot-edit-content.model';

/**
 * Component that displays content version history in the sidebar.
 * Shows a timeline of all content versions with their details.
 */
@Component({
    selector: 'dot-edit-content-sidebar-history',
    imports: [
        CommonModule,
        ScrollerModule,
        SkeletonModule,
        TooltipModule,
        DotEmptyContainerComponent,
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
     * Expose DotHistoryTimelineItemActionType to template
     */
    readonly DotHistoryTimelineItemActionType = DotHistoryTimelineItemActionType;

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
    $pagination = input<DotPagination | null>(null, { alias: 'pagination' });

    /**
     * Current historical version inode being viewed
     * @readonly
     */
    $historicalVersionInode = input<string | null>(null, { alias: 'historicalVersionInode' });

    /**
     * Event emitted when page changes
     */
    pageChange = output<number>();

    /**
     * Event emitted when a timeline item action is triggered
     */
    timelineItemAction = output<DotHistoryTimelineItemAction>();

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
     * Get the real index of an item in the history array
     * This is needed because p-scroller's template index is virtual
     */
    getRealIndex(item: DotCMSContentletVersion): number {
        return this.$historyItems().indexOf(item);
    }

    /**
     * Handle accordion tab change
     */
    onAccordionTabChange(_activeTab: string | null): void {
        // TODO: Here you can add additional logic if needed
    }
}
