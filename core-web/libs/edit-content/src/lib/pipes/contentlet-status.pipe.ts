import { Pipe, PipeTransform } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Pipe({
    name: 'contentletStatus',
    standalone: true
})
export class ContentletStatusPipe implements PipeTransform {
    transform(contentlet: DotCMSContentlet): { label: string; classes: string } {
        switch (true) {
            case contentlet?.live && !contentlet?.working:
                return { label: 'Published', classes: 'p-chip-success p-chip-sm' };

            case contentlet.working && !contentlet.live:
                return { label: 'Draft', classes: 'p-chip-sm' };

            case contentlet?.archived:
                return { label: 'Archived', classes: 'p-chip-gray p-chip-sm' };

            case contentlet?.working && contentlet?.live:
                return { label: 'Revision', classes: 'p-chip-pink p-chip-sm' };

            default:
                return { label: '', classes: 'p-chip-sm' };
        }
    }
}
