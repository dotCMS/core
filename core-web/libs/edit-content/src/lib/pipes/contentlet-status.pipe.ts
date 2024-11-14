import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentStatus } from '../models/dot-edit-content-status.enum';

// TODO: Impove this pipe to use the dynamic statuses from the workflow
@Pipe({
    name: 'contentletStatus',
    standalone: true
})
export class ContentletStatusPipe implements PipeTransform {
    private readonly dotMessage = inject(DotMessageService);

    private readonly STATUS_CONFIG: Partial<
        Record<DotEditContentStatus, { label: string; classes: string }>
    > = {
        [DotEditContentStatus.PUBLISHED]: {
            label: 'Published',
            classes: 'p-chip-success'
        },
        [DotEditContentStatus.DRAFT]: {
            label: 'Draft',
            classes: ''
        },
        [DotEditContentStatus.CHANGED]: {
            label: 'Changed',
            classes: 'p-chip-pink'
        },
        [DotEditContentStatus.ARCHIVED]: {
            label: 'Archived',
            classes: 'p-chip-gray'
        },
        [DotEditContentStatus.UNKNOWN]: {
            label: '',
            classes: ''
        }
    };

    transform(contentlet?: DotCMSContentlet): { label: string; classes: string } {
        if (!contentlet) {
            // New: Content has not been saved yet
            return {
                label: this.dotMessage.get('New'),
                classes: 'p-chip-blue'
            };
        }

        const status = this.getContentletStatus(contentlet);
        const config = this.STATUS_CONFIG[status];

        return {
            label: this.dotMessage.get(config.label),
            classes: config.classes
        };
    }

    private getContentletStatus(contentlet: DotCMSContentlet): DotEditContentStatus {
        // Archived: Content has been archived
        if (contentlet.archived) return DotEditContentStatus.ARCHIVED;

        // Changed: Has both live and working versions (unpublished changes exist)
        if (contentlet.live && contentlet.working) return DotEditContentStatus.CHANGED;

        // Published: Has live version but no working version (no unpublished changes)
        if (contentlet.live && !contentlet.working) return DotEditContentStatus.PUBLISHED;

        // Draft: Has working version but no live version (never published)
        if (contentlet.working && !contentlet.live) return DotEditContentStatus.DRAFT;

        // Unknown: None of the above conditions match
        return DotEditContentStatus.UNKNOWN;
    }
}
