import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentStatus } from '../models/dot-edit-content-status.enum';

@Pipe({
    name: 'contentletStatus'
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
        if (contentlet.archived) {
            return DotEditContentStatus.ARCHIVED;
        }

        // Live content handling
        if (contentlet.live) {
            // Compare working and live inodes to determine if content has changed
            if (contentlet.workingInode === contentlet.liveInode) {
                return DotEditContentStatus.PUBLISHED;
            } else {
                return DotEditContentStatus.CHANGED;
            }
        }

        // Draft: Not live and has working version
        if (contentlet.working) {
            return DotEditContentStatus.DRAFT;
        }

        // Unknown: None of the above conditions match
        return DotEditContentStatus.UNKNOWN;
    }
}
