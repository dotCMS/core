import { CoreWebService } from 'dotcms-js/dotcms-js';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { RequestMethod } from '@angular/http';
import { DotRenderedPage } from '../../../portlets/dot-edit-page/shared/models/dot-rendered-page.model';

/**
 * Provide util methods to get a edit page html
 * @export
 * @class EditPageService
 */
@Injectable()
export class EditPageService {
    constructor(private coreWebService: CoreWebService) {}

    get(url: string): Observable<DotRenderedPage> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `v1/page/renderHTML/${url.replace(/^\//, '')}?mode=EDIT_MODE`
            })
            .pluck('bodyJsonObject');
    }
}
