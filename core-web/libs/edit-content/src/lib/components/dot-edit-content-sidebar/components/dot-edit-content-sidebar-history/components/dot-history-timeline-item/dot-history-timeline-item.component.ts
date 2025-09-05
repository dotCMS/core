import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, inject, output } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotCMSContentletVersion } from '@dotcms/dotcms-models';
import { DotGravatarDirective, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

import {
    DotHistoryTimelineItemAction,
    DotHistoryTimelineItemActionType
} from '../../../../../../models/dot-edit-content.model';

/**
 * Component that displays a single history timeline item with version details and actions.
 * Shows version information, user details, status chips, and provides action menu.
 *
 * @example
 * ```html
 * <dot-history-timeline-item
 *   [item]="versionItem"
 *   (actionTriggered)="onTimelineItemAction($event)">
 * </dot-history-timeline-item>
 * ```
 */
@Component({
    selector: 'dot-history-timeline-item',
    standalone: true,
    imports: [
        CommonModule,
        AvatarModule,
        ButtonModule,
        ChipModule,
        MenuModule,
        TooltipModule,
        DotGravatarDirective,
        DotMessagePipe,
        DotRelativeDatePipe
    ],
    providers: [DatePipe, DotMessagePipe],
    templateUrl: './dot-history-timeline-item.component.html',
    styleUrls: ['./dot-history-timeline-item.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHistoryTimelineItemComponent {
    private readonly datePipe = inject(DatePipe);
    private readonly dotMessagePipe = inject(DotMessagePipe);

    /**
     * The version item to display
     * @readonly
     */
    $item = input.required<DotCMSContentletVersion>({ alias: 'item' });

    /**
     * Event emitted when an action is triggered on the timeline item
     */
    actionTriggered = output<DotHistoryTimelineItemAction>();

    /**
     * Computed signal for cached translations map
     * Ensures reactive updates when language changes
     */
    private readonly $labels = computed(() => ({
        preview: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.preview'),
        restore: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.restore'),
        compare: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.compare'),
        delete: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.delete')
    }));

    /**
     * Computed signal that generates menu items for version actions
     * Uses reactive approach with computed signal for better performance
     */
    readonly $menuItems = computed(() => {
        const labels = this.$labels();
        const item = this.$item();

        return [
            {
                label: labels.preview,
                disabled: true,
                command: () =>
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.PREVIEW,
                        item
                    })
            },
            {
                label: labels.restore,
                disabled: true,
                command: () =>
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.RESTORE,
                        item
                    })
            },
            {
                label: labels.compare,
                disabled: true,
                command: () =>
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.COMPARE,
                        item
                    })
            },
            {
                label: labels.delete,
                disabled: true,
                command: () =>
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.DELETE,
                        item
                    })
            }
        ];
    });

    /**
     * Computed signal that determines the timeline marker CSS class based on content status
     * Uses reactive approach for better performance and consistency
     */
    readonly $timelineMarkerClass = computed(() => {
        const item = this.$item();

        if (item.live) {
            return 'dot-history-timeline-item__marker--live';
        } else if (item.working) {
            return 'dot-history-timeline-item__marker--draft';
        }

        return '';
    });
}
