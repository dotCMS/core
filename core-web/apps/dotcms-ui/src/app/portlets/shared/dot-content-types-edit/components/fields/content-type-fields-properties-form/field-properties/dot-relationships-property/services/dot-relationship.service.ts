import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map, take } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';

/**
 *Provide method to handle with the Relationship fields
 *
 * @export
 * @class DotRelationshipService
 */
@Injectable()
export class DotRelationshipService {
    private http = inject(HttpClient);

    /**
     *Return all the cardinalities options allow
     *
     * @returns {Observable<DotRelationshipCardinality[]>}
     * @memberof DotRelationshipService
     */
    loadCardinalities(): Observable<DotRelationshipCardinality[]> {
        return this.http
            .get<
                DotCMSResponse<DotRelationshipCardinality[]>
            >('/api/v1/relationships/cardinalities')
            .pipe(
                take(1),
                map((response) => response.entity)
            );
    }
}
