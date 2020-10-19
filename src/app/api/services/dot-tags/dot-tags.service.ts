import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { DotTag } from '@models/dot-tag';
import { map, pluck } from 'rxjs/operators';

/**
 * Provide util methods to get Tags available in the system.
 * @export
 * @class DotTagsService
 */
@Injectable()
export class DotTagsService {
    constructor(private coreWebService: CoreWebService) {}

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
