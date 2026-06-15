import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

import { ComponentStatus, DotCMSContentletVersion, DotPagination } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotHistoryTimelineItemComponent } from './components/dot-history-timeline-item/dot-history-timeline-item.component';
import { DotHistoryTimelineListComponent } from './components/dot-history-timeline-list/dot-history-timeline-list.component';
import { DotPushpublishTimelineItemComponent } from './components/dot-pushpublish-timeline-item/dot-pushpublish-timeline-item.component';

import {
    DotHistoryTimelineItemAction,
    DotPushPublishHistoryItem
} from '../../../../models/dot-edit-content.model';
import { DotEditContentSidebarSectionComponent } from '../dot-edit-content-sidebar-section/dot-edit-content-sidebar-section.component';

/**
 * Component that displays content version history in the sidebar.
 * Shows a timeline of all content versions with their details.
 */
@Component({
    selector: 'dot-edit-content-sidebar-history',
    imports: [
        SkeletonModule,
        TooltipModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        DotEditContentSidebarSectionComponent,
        DotHistoryTimelineItemComponent,
        DotHistoryTimelineListComponent,
        DotPushpublishTimelineItemComponent
    ],
    providers: [DatePipe, DotMessagePipe],
    templateUrl: './dot-edit-content-sidebar-history.component.html',
    styleUrls: ['./dot-edit-content-sidebar-history.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex flex-col h-full min-h-0'
    }
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
    $historypagination = input<DotPagination | null>(null, { alias: 'historyPagination' });

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
     * Event emitted when history page changes
     */
    historyPageChange = output<number>();

    /**
     * Event emitted when push publish history page changes
     */
    pushPublishPageChange = output<number>();

    /**
     * Event emitted when a timeline item action is triggered
     */
    timelineItemAction = output<DotHistoryTimelineItemAction>();

    /**
     * Event emitted when delete all push publish history is requested
     */
    deletePushPublishHistory = output<void>();

    /**
     * Determines if the history is in a loading state
     */
    readonly $isLoading = computed(() => this.$status() === ComponentStatus.LOADING);

    /**
     * Determines if there are history items to display
     */
    readonly $hasHistoryItems = computed(() => this.$historyItems().length > 0);

    /**
     * Determines if there are more items to load for infinite scroll.
     * Assumes `currentPage` is 1-based (API contract): page 1 covers the first
     * `perPage` items, so `currentPage * perPage` is the count loaded so far. A
     * 0-based `currentPage` would make this always true and loop — guard upstream
     * if the contract ever changes.
     */
    readonly $hasMoreItems = computed(() => {
        const pagination = this.$historypagination();
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
     * Load the next page of history items
     */
    private loadNextPage(): void {
        const pagination = this.$historypagination();
        if (pagination && this.$hasMoreItems()) {
            this.historyPageChange.emit(pagination.currentPage + 1);
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
     */
    getRealIndex(item: DotCMSContentletVersion): number {
        return this.$historyItems().indexOf(item);
    }

    /**
     * Get the index of a push publish item in the list.
     */
    getPushPublishIndex(item: DotPushPublishHistoryItem): number {
        return this.$pushPublishHistoryItems().indexOf(item);
    }

    getMarkerClass(item: DotCMSContentletVersion): string {
        if (item.live) return 'border-green-500!';
        if (item.working) return 'border-yellow-500!';
        return 'border-gray-400!';
    }

    /**
     * Load more version items when the Versions timeline reaches its end.
     */
    onTimelineReachedEnd(): void {
        if (this.$hasMoreItems() && !this.$isLoading()) {
            this.loadNextPage();
        }
    }

    /**
     * Load more push publish items when the Push Publish timeline reaches its end.
     */
    onPushPublishTimelineReachedEnd(): void {
        if (this.$hasMorePushPublishItems() && !this.$isLoading()) {
            this.loadNextPushPublishPage();
        }
    }

    /**
     * Menu items for push publish actions
     */
    readonly $menuItems = computed(() => [
        {
            label: this.dotMessagePipe.transform(
                'edit.content.sidebar.history.push.publish.delete.all'
            ),
            icon: 'pi pi-trash',
            command: () => this.deletePushPublishHistory.emit()
        }
    ]);
}
