import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { pluck, take, } from 'rxjs/operators';
import { DotRelationshipCardinality } from '../../../../shared/dot-relationship-cardinality.model';

/**
 * Provide method to handle with the Relationship field
 */
@Injectable()
export class RelationshipService {

    constructor(private coreWebService: CoreWebService) {}

    loadCardinalities(): Observable<DotRelationshipCardinality[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: 'v1/relationships/cardinalities'
            })
            .pipe(
                take(1),
                pluck('entity'),
            );
    }
}
