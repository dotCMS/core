import { Pipe, PipeTransform } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

/**
 *
 * Get the current state of a Contentlet
 * @export
 * @class ContentletStatePipe
 * @implements {PipeTransform}
 */
@Pipe({
    name: 'contentletState',
    standalone: false
})
export class ContentletStatePipe implements PipeTransform {
    transform({ live, working, deleted, hasLiveVersion }: DotCMSContentlet) {
        return { live, working, deleted, hasLiveVersion };
    }
}
