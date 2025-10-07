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
import { DotPushpublishTimelineItemComponent } from './components/dot-pushpublish-timeline-item/dot-pushpublish-timeline-item.component';

import {
    DotHistoryTimelineItemAction,
    DotPushPublishHistoryItem
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
        DotHistoryTimelineItemComponent,
        DotPushpublishTimelineItemComponent
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
    $pagination = input<DotPagination | null>(null, { alias: 'pagination' });

    /**
     * Pagination data for push publish history items
     * @readonly
     */
    $pushPublishHistoryPagination = input<DotPagination | null>(null, {
        alias: 'pushPublishHistoryPagination'
    });

    /**
     * Current historical version inode being viewed
     * @readonly
     */
    $historicalVersionInode = input<string | null>(null, { alias: 'historicalVersionInode' });

    /**
     * List of push publish history items to display (accumulated items from store)
     * @readonly
     */
    $pushPublishHistoryItems = input<DotPushPublishHistoryItem[]>([], {
        alias: 'pushPublishHistoryItems'
    });

    /**
     * Event emitted when page changes
     */
    pageChange = output<number>();

    /**
     * Event emitted when push publish history page changes
     */
    pushPublishPageChange = output<number>();

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
     * Determines if there are push publish history items to display
     */
    readonly $hasPushPublishHistoryItems = computed(
        () => this.$pushPublishHistoryItems().length > 0
    );

    /**
     * Determines if there are more push publish history items to load for infinite scroll
     */
    readonly $hasMorePushPublishItems = computed(() => {
        const pagination = this.$pushPublishHistoryPagination();
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
     * Handle infinite scroll for push publish history when user scrolls near the end
     */
    onPushPublishScrollIndexChange(event: ScrollerLazyLoadEvent): void {
        if (this.shouldLoadMorePushPublish(event) && !this.$isLoading()) {
            this.loadNextPushPublishPage();
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
     * Determine if we should load more push publish items based on scroll position
     */
    private shouldLoadMorePushPublish(event: ScrollerLazyLoadEvent): boolean {
        const { last } = event;
        const totalItems = this.$pushPublishHistoryItems().length;
        const threshold = 5; // Load when 5 items remaining

        return totalItems - last <= threshold && this.$hasMorePushPublishItems();
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
     * Load the next page of push publish history items
     */
    private loadNextPushPublishPage(): void {
        const pagination = this.$pushPublishHistoryPagination();
        if (pagination && this.$hasMorePushPublishItems()) {
            this.pushPublishPageChange.emit(pagination.currentPage + 1);
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
     * Get the real index of a push publish item in the push publish history array
     * This is needed because p-scroller's template index is virtual
     */
    getPushPublishRealIndex(item: DotPushPublishHistoryItem): number {
        return this.$pushPublishHistoryItems().indexOf(item);
    }

    /**
     * Handle accordion tab change
     */
    onAccordionTabChange(_activeTab: string | null): void {
        // TODO: Here you can add additional logic if needed
    }
}
