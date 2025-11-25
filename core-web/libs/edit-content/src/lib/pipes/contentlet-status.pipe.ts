import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentStatus } from '../models/dot-edit-content-status.enum';

@Pipe({
    name: 'contentletStatus',
    standalone: true
})
export class ContentletStatusPipe implements PipeTransform {
    private readonly dotMessage = inject(DotMessageService);
    transform(contentlet?: DotCMSContentlet): { label: string; classes: string } {
        if (!contentlet) {
            return {
                label: this.dotMessage.get('New'),
                classes: 'p-chip-blue'
            };
        }

        const contentletStatus = this.getContentletStatus(contentlet);

        return (
            {
                [DotEditContentStatus.PUBLISHED]: {
                    label: this.dotMessage.get('Published'),
                    classes: 'p-chip-success'
                },
                [DotEditContentStatus.DRAFT]: {
                    label: this.dotMessage.get('Draft'),
                    classes: ''
                },
                [DotEditContentStatus.REVISION]: {
                    label: this.dotMessage.get('Revision'),
                    classes: 'p-chip-pink'
                },
                [DotEditContentStatus.ARCHIVED]: {
                    label: this.dotMessage.get('Archived'),
                    classes: 'p-chip-gray'
                },
                [DotEditContentStatus.UNKNOWN]: {
                    label: this.dotMessage.get(''),
                    classes: ''
                }
            } as Record<DotEditContentStatus, { label: string; classes: string }>
        )[contentletStatus];
    }

    getContentletStatus(contentlet) {
        if (contentlet.archived) {
            return DotEditContentStatus.ARCHIVED;
        } else if (contentlet.live && contentlet.working) {
            return DotEditContentStatus.REVISION;
        } else if (contentlet.live && !contentlet.working) {
            return DotEditContentStatus.PUBLISHED;
        } else if (contentlet.working && !contentlet.live) {
            return DotEditContentStatus.DRAFT;
        }

        return DotEditContentStatus.UNKNOWN;
    }
}
