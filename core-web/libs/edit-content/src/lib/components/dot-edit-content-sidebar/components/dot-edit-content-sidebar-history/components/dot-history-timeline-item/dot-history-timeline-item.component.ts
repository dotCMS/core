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
     * Cached translations map for all labels used in the component
     */
    private readonly labels = computed(() => ({
        preview: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.preview'),
        restore: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.restore'),
        compare: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.compare'),
        delete: this.dotMessagePipe.transform('edit.content.sidebar.history.menu.delete')
    }));

    /**
     * Gets menu items for version actions
     * @param item - The version item to create menu for
     * @returns Array of menu items with their respective commands
     */
    getVersionMenuItems(item: DotCMSContentletVersion) {
        const labels = this.labels();

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
    }

    /**
     * Gets the timeline marker CSS class based on the content status
     */
    getTimelineMarkerClass(item: DotCMSContentletVersion | undefined): string {
        // Safety check for undefined item
        if (!item) {
            return '';
        }

        if (item.live) {
            return 'timeline-item__marker--live';
        } else if (item.working) {
            return 'timeline-item__marker--draft';
        }

        return '';
    }
}
