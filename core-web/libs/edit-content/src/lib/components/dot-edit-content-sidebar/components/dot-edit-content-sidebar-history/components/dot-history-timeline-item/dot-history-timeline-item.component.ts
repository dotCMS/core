import { CommonModule, DatePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    input,
    inject,
    output,
    signal
} from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
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
    imports: [
        CommonModule,
        AvatarModule,
        ButtonModule,
        MenuModule,
        TagModule,
        TooltipModule,
        DotGravatarDirective,
        DotMessagePipe,
        DotRelativeDatePipe
    ],
    providers: [DatePipe],
    templateUrl: './dot-history-timeline-item.component.html',
    styleUrls: ['./dot-history-timeline-item.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotHistoryTimelineItemComponent {
    private readonly datePipe = inject(DatePipe);
    private readonly dotMessageService = inject(DotMessageService);

    /**
     * The version item to display
     * @readonly
     */
    $item = input.required<DotCMSContentletVersion>({ alias: 'item' });

    /**
     * The index of this item in the timeline (0-based)
     * Used to determine which actions are available
     * @readonly
     */
    $itemIndex = input<number>(0, { alias: 'itemIndex' });

    /**
     * Whether this timeline item is currently active (being viewed)
     * @readonly
     */
    $isActive = input<boolean>(false, { alias: 'isActive' });

    /**
     * Event emitted when an action is triggered on the timeline item
     */
    actionTriggered = output<DotHistoryTimelineItemAction>();

    /**
     * Signal for cached translations map
     * Contains static translations for menu labels
     */
    private readonly $labels = signal({
        restore: this.dotMessageService.get('edit.content.sidebar.history.menu.restore'),
        compare: this.dotMessageService.get('edit.content.sidebar.history.menu.compare'),
        delete: this.dotMessageService.get('edit.content.sidebar.history.menu.delete')
    });

    /**
     * Computed signal that generates menu items for version actions
     * Uses reactive approach with computed signal for better performance
     * Filters actions based on item position and business rules
     */
    readonly $menuItems = computed(() => {
        const labels = this.$labels();
        const item = this.$item();

        const items = [];

        if (!item.live) {
            items.push({
                id: 'restore',
                label: labels.restore,
                command: () =>
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.RESTORE,
                        item
                    })
            });
        }
        if (!item.working) {
            items.push({
                id: 'compare',
                label: labels.compare,
                command: () =>
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.COMPARE,
                        item
                    })
            });
        }

        if (!item.working || !item.live) {
            items.push({
                id: 'delete',
                label: labels.delete,
                command: () =>
                    this.actionTriggered.emit({
                        type: DotHistoryTimelineItemActionType.DELETE,
                        item
                    })
            });
        }

        return items;
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
