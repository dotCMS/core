import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotPersona } from '@dotcms/dotcms-models';

/**
 * Provide util methods to get Personas.
 * @export
 * @class DotPersonasService
 */
@Injectable()
export class DotPersonasService {
    private coreWebService = inject(CoreWebService);

    /**
     * Return Personas.
     * @returns Observable<DotLanguage[]>
     * @memberof DotPersonasService
     */
    get(): Observable<DotPersona[]> {
        return this.coreWebService
            .requestView({
                url: 'content/respectFrontendRoles/false/render/false/query/+contentType:persona +live:true +deleted:false +working:true'
            })
            .pipe(map((x) => x?.contentlets));
    }
}
