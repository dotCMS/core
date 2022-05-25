import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { take } from 'rxjs/operators';

/**
 * Returns action url for create contentlet dialog
 *
 * @export
 * @class DotCreateContentletResolver
 * @implements {Resolve<Observable<string>>}
 */
@Injectable()
export class DotCreateContentletResolver implements Resolve<Observable<string>> {
    constructor(private dotContentletEditorService: DotContentletEditorService) {}

    resolve(route: ActivatedRouteSnapshot): Observable<string> {
        return this.dotContentletEditorService
            .getActionUrl(route.paramMap.get('contentType'))
            .pipe(take(1));
    }
}
