import { Pipe, PipeTransform } from '@angular/core';

import { DotContentState } from '@dotcms/dotcms-models';

@Pipe({
    name: 'dotContentletStatus',
    pure: true
})
export class DotContentletStatusPipe implements PipeTransform {
    transform(value: DotContentState): string {
        if (!value) {
            return 'Draft';
        }

        const { live, working, archived, deleted, hasLiveVersion } = value;

        // Check if deleted or archived
        if (deleted || archived) {
            return 'Archived';
        }

        // Check if live is true
        if (live) {
            if (hasLiveVersion && working) {
                return 'Published';
            }
        } else {
            // live is false or not set
            if (hasLiveVersion) {
                return 'Revision';
            }
        }

        return 'Draft';
    }
}
