import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { pluck, take, } from 'rxjs/operators';
import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';

/**
 *Provide method to handle with the Relationship fields
 *
 * @export
 * @class DotRelationshipService
 */
@Injectable()
export class DotRelationshipService {

    constructor(private coreWebService: CoreWebService) {}

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
            .pipe(
                take(1),
                pluck('entity'),
            );
    }
}
