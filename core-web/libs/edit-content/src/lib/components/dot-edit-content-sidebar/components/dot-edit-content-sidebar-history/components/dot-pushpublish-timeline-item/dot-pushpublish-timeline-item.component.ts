import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';

import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

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
    imports: [AvatarModule, DotCopyButtonComponent, DotMessagePipe, DatePipe],
    templateUrl: './dot-pushpublish-timeline-item.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotPushpublishTimelineItemComponent {
    /**
     * The push publish history item to display
     * @readonly
     */
    $item = input.required<DotPushPublishHistoryItem>({ alias: 'item' });
}
