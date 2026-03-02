import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentStatus } from '../models/dot-edit-content-status.enum';

/** PrimeNG Tag severity for p-tag */
export type ContentletStatusTagSeverity =
    | 'success'
    | 'secondary'
    | 'info'
    | 'warn'
    | 'danger'
    | 'contrast';

export interface ContentletStatusTagResult {
    label: string;
    severity: ContentletStatusTagSeverity;
}

const STATUS_TAG_CONFIG: Partial<
    Record<DotEditContentStatus, { label: string; severity: ContentletStatusTagSeverity }>
> = {
    [DotEditContentStatus.PUBLISHED]: { label: 'Published', severity: 'success' },
    [DotEditContentStatus.DRAFT]: { label: 'Draft', severity: 'secondary' },
    [DotEditContentStatus.CHANGED]: { label: 'Changed', severity: 'warn' },
    [DotEditContentStatus.ARCHIVED]: { label: 'Archived', severity: 'contrast' },
    [DotEditContentStatus.UNKNOWN]: { label: '', severity: 'secondary' }
};

/**
 * Pipe that returns contentlet status as label + PrimeNG Tag severity for use with p-tag.
 * Returns null when there is no label to show (e.g. UNKNOWN).
 */
@Pipe({
    name: 'contentletStatusTag'
})
export class ContentletStatusTagPipe implements PipeTransform {
    private readonly dotMessage = inject(DotMessageService);

    transform(contentlet?: DotCMSContentlet): ContentletStatusTagResult | null {
        if (!contentlet) {
            return {
                label: this.dotMessage.get('New'),
                severity: 'info'
            };
        }

        const status = this.getContentletStatus(contentlet);
        const config = STATUS_TAG_CONFIG[status];
        const label = this.dotMessage.get(config.label);

        return label ? { label, severity: config.severity } : null;
    }

    private getContentletStatus(contentlet: DotCMSContentlet): DotEditContentStatus {
        if (contentlet.archived) {
            return DotEditContentStatus.ARCHIVED;
        }
        if (contentlet.live) {
            return contentlet.workingInode === contentlet.liveInode
                ? DotEditContentStatus.PUBLISHED
                : DotEditContentStatus.CHANGED;
        }
        if (contentlet.working) {
            return DotEditContentStatus.DRAFT;
        }
        return DotEditContentStatus.UNKNOWN;
    }
}
