import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { pluck, take } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';

import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';

/**
 *Provide method to handle with the Relationship fields
 *
 * @export
 * @class DotRelationshipService
 */
@Injectable()
export class DotRelationshipService {
    private coreWebService = inject(CoreWebService);

    /**
     *Return all the cardinalities options allow
     *
     * @returns {Observable<DotRelationshipCardinality[]>}
     * @memberof DotRelationshipService
     */
    loadCardinalities(): Observable<DotRelationshipCardinality[]> {
        return this.coreWebService
            .requestView({
                url: 'v1/relationships/cardinalities'
            })
            .pipe(take(1), pluck('entity'));
    }
}
