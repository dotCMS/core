import { Pipe, PipeTransform } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Pipe({
    name: 'dotContentletStatus',
    standalone: true,
    pure: true
})
export class DotContentletStatusPipe implements PipeTransform {
    transform(value: DotCMSContentlet): string {
        if (value?.archived) {
            return 'Archived';
        }

        if (value?.live) {
            return 'Published';
        }

        return 'Draft';
    }
}
