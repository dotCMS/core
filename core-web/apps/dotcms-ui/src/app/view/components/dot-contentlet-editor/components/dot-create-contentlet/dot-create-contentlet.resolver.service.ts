import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

/**
 * Returns action url for create contentlet dialog
 *
 * @export
 * @class DotCreateContentletResolver
 * @implements {Resolve<Observable<string>>}
 */
@Injectable()
export class DotCreateContentletResolver implements Resolve<Observable<string>> {
    private dotContentletEditorService = inject(DotContentletEditorService);

    resolve(route: ActivatedRouteSnapshot): Observable<string> {
        return this.dotContentletEditorService
            .getActionUrl(route.paramMap.get('contentType'))
            .pipe(take(1));
    }
}
