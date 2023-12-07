import { Pipe, PipeTransform, inject } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Pipe({
    name: 'contentletStatus',
    standalone: true
})
export class ContentletStatusPipe implements PipeTransform {
    private readonly dotMessage = inject(DotMessageService);
    transform(contentlet: DotCMSContentlet): { label: string; classes: string } {
        switch (true) {
            case contentlet?.live && !contentlet?.working:
                return {
                    label: this.dotMessage.get('Published'),
                    classes: 'p-chip-success p-chip-sm'
                };

            case contentlet.working && !contentlet.live:
                return { label: this.dotMessage.get('Draft'), classes: 'p-chip-sm' };

            case contentlet?.archived:
                return {
                    label: this.dotMessage.get('Archived'),
                    classes: 'p-chip-gray p-chip-sm'
                };

            case contentlet?.working && contentlet?.live:
                return {
                    label: this.dotMessage.get('Revision'),
                    classes: 'p-chip-pink p-chip-sm'
                };

            default:
                return { label: '', classes: 'p-chip-sm' };
        }
    }
}
