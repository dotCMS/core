import { Injectable } from '@angular/core';
import { DotPersona } from '../../../shared/models/dot-persona/dot-persona.model';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { CoreWebService } from 'dotcms-js/dotcms-js';

/**
 * Provide util methods to get Personas.
 * @export
 * @class DotPersonasService
 */
@Injectable()
export class DotPersonasService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Return Personas.
     * @returns {Observable<DotLanguage[]>}
     * @memberof DotPersonasService
     */
    get(): Observable<DotPersona[]> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: 'content/query/structurename:persona'
            })
            .pluck('contentlets');
    }
}
