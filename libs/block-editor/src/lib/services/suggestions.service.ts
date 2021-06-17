import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { pluck } from 'rxjs/operators';
import { CoreWebService } from '../../../../dotcms-js/src/public_api'

@Injectable()
export class SuggestionsService {

    constructor(private coreWebSerice: CoreWebService) { }

    getContentTypes(filter = ''): Observable<unknown[]> {
        return this.coreWebSerice.requestView({
            url: `/api/v1/contenttype?filter=${filter}&orderby=modDate&direction=DESC&per_page=40`
        }).pipe(pluck('entity'))
    }

    getContentlets(contentType = ''): Observable<unknown[]> {
        return this.coreWebSerice.requestView({
            url: `/api/content/render/false/query/+contentType:${contentType}%20+languageId:1%20+deleted:false%20+working:true/orderby/modDate%20desc`
        }).pipe(pluck('contentlets'))
        
    }
}
