import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, inject } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { TooltipModule } from 'primeng/tooltip';

import { DotCopyButtonComponent, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import { DotPushPublishHistoryItem } from '../../../../../../models/dot-edit-content.model';

/**
 * Component that displays a single push publish timeline item with version details.
 * Shows version information, user details, and status chips without action menu.
 * This component is specifically designed for push publish history display.
 *
 * @example
 * ```html
 * <dot-pushpublish-timeline-item
 *   [item]="versionItem"
 *   [isActive]="false">
 * </dot-pushpublish-timeline-item>
 * ```
 */
@Component({
    selector: 'dot-pushpublish-timeline-item',
    imports: [
        CommonModule,
        AvatarModule,
        TooltipModule,
        DotCopyButtonComponent,
        DotMessagePipe,
        DotRelativeDatePipe
    ],
    providers: [DatePipe],
    templateUrl: './dot-pushpublish-timeline-item.component.html',
    styleUrls: ['./dot-pushpublish-timeline-item.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPushpublishTimelineItemComponent {
    private readonly datePipe = inject(DatePipe);

    /**
     * The push publish history item to display
     * @readonly
     */
    $item = input.required<DotPushPublishHistoryItem>({ alias: 'item' });

    /**
     * The index of this item in the timeline (0-based)
     * Used for styling purposes
     * @readonly
     */
    $itemIndex = input<number>(0, { alias: 'itemIndex' });

    /**
     * Computed signal that determines the timeline marker CSS class for push publish items
     * All push publish items use the same marker style since they all represent successful publications
     */
    readonly $timelineMarkerClass = computed(() => {
        // All push publish items use a consistent gray marker
        return '';
    });

    /**
     * Computed signal that returns the first 6 characters of the bundle ID for display
     */
    readonly $truncatedBundleId = computed(() => {
        const bundleId = this.$item().bundleId;
        return bundleId ? bundleId.substring(0, 6) : '';
    });
}
