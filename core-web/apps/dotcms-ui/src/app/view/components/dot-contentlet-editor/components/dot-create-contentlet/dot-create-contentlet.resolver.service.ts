import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { map, take } from 'rxjs/operators';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';

/**
 * Returns action url for create contentlet dialog
 *
 * @export
 * @class DotCreateContentletResolver
 * @implements {Resolve<Observable<string | null>>}
 */
@Injectable()
export class DotCreateContentletResolver implements Resolve<Observable<string | null>> {
    private dotContentletEditorService = inject(DotContentletEditorService);

    resolve(route: ActivatedRouteSnapshot): Observable<string | null> {
        // When the create flow is opened from a folder context (e.g. Content Drive), a `folder`
        // inode is passed as a route query param. Append it to the action URL loaded in the legacy
        // editor iframe so its Host/Folder field pre-selects that folder (edit_contentlet.jsp reads
        // request.getParameter("folder")).
        const folder = route.queryParamMap.get('folder');

        return this.dotContentletEditorService
            .getActionUrl(route.paramMap.get('contentType') ?? '')
            .pipe(
                take(1),
                map((url) => this.appendFolder(url, folder))
            );
    }

    private appendFolder(url: string | null, folder: string | null): string | null {
        if (!url || !folder) {
            return url;
        }

        const separator = url.includes('?') ? '&' : '?';

        return `${url}${separator}folder=${encodeURIComponent(folder)}`;
    }
}
