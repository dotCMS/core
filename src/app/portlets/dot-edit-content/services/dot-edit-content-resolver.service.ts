import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { EditPageService } from '../../../api/services/edit-page/edit-page.service';
import { DotRenderedPage } from '../../dot-edit-page/shared/models/dot-rendered-page.model';


/**
 * With the url return a string of the edit page html
 *
 * @export
 * @class EditContentResolver
 * @implements {Resolve<string>}
 */
@Injectable()
export class EditContentResolver implements Resolve<DotRenderedPage> {
    constructor(private editPageService: EditPageService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotRenderedPage> {
        return this.editPageService.get(route.queryParams.url).map((dotRenderedPage: DotRenderedPage) => {
            // TODO: remove this when we do this: https://github.com/dotCMS/core/issues/13306
            return {
                ...dotRenderedPage,
                title: 'Fake Page Title',
                url: 'Fake Page Url'
            };
        });
    }
}
