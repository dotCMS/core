import { Injectable } from '@angular/core';
import { CoreWebService } from 'dotcms-js';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { pluck, } from 'rxjs/operators';
import { RelationshipCardinality } from '../shared/relationship-cardinality.model';

@Injectable()
export class RelationshipService {

    constructor(private coreWebService: CoreWebService) {}

    loadCardinalities(): Observable<RelationshipCardinality[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: 'v1/relationships/cardinalities'
            })
            .pipe(pluck('entity'));
    }
}
