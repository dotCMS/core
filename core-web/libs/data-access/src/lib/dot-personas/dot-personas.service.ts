import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotPersona } from '@dotcms/dotcms-models';

// Response type for content search endpoints that return contentlets
interface DotContentSearchResponse<T> {
    contentlets: T;
}

/**
 * Provide util methods to get Personas.
 * @export
 * @class DotPersonasService
 */
@Injectable()
export class DotPersonasService {
    private http = inject(HttpClient);

    /**
     * Return Personas.
     * @returns Observable<DotLanguage[]>
     * @memberof DotPersonasService
     */
    get(): Observable<DotPersona[]> {
        return this.http
            .get<
                DotContentSearchResponse<DotPersona[]>
            >('/api/content/respectFrontendRoles/false/render/false/query/+contentType:persona +live:true +deleted:false +working:true')
            .pipe(map((response) => response.contentlets));
    }
}
