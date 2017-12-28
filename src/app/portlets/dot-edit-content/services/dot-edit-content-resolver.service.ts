import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';

import { FAKE_EDIT_PAGE_HTML } from '../fake-edit-page-html';
import { EditPageService } from '../../../api/services/edit-page/edit-page.service';


/**
 * With the url return a string of the edit page html
 *
 * @export
 * @class EditContentResolver
 * @implements {Resolve<string>}
 */
@Injectable()
export class EditContentResolver implements Resolve<string> {
    constructor(private editPageService: EditPageService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<string> {
        return this.editPageService.get(route.queryParams.url);
    }
}
