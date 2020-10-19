import { pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { Observable } from 'rxjs';
import { CoreWebService } from 'dotcms-js';

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
     * @returns Observable<DotLanguage[]>
     * @memberof DotPersonasService
     */
    get(): Observable<DotPersona[]> {
        return this.coreWebService
            .requestView({
                url:
                    'content/respectFrontendRoles/false/render/false/query/+contentType:persona +live:true +deleted:false +working:true'
            })
            .pipe(pluck('contentlets'));
    }
}
