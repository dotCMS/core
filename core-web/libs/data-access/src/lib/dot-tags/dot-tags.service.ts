import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, pluck } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotTag } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get Tags available in the system.
 * @export
 * @class DotTagsService
 */
@Injectable()
export class DotTagsService {
    private coreWebService = inject(CoreWebService);

    /**
     * Get tags suggestions
     * @returns Observable<DotTag[]>
     * @memberof DotTagDotTagsServicesService
     */
    getSuggestions(name?: string): Observable<DotTag[]> {
        return this.coreWebService
            .requestView({
                url: `v1/tags${name ? `?name=${name}` : ''}`
            })
            .pipe(
                pluck('bodyJsonObject'),
                map((tags: { [key: string]: DotTag }) => {
                    return Object.entries(tags).map(([_key, value]) => value);
                })
            );
    }
}
