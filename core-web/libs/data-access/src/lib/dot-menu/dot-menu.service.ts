import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse, DotMenu } from '@dotcms/dotcms-models';

const MENU_URL = '/api/v1/menu';

/**
 * Stateless client for the signed-in user's navigation menu.
 *
 * It only performs the request: every subscription sends one `GET /api/v1/menu` and nothing is kept
 * between calls. The one in-memory copy of the menu lives in the navigation store (`GlobalStore`),
 * which decides when to ask again.
 */
@Injectable({ providedIn: 'root' })
export class DotMenuService {
    readonly #http = inject(HttpClient);

    /**
     * Requests the menu of the signed-in backend user: the tool groups sorted by the user's
     * `tabOrder`, each with its portlets. Nothing is sent until the result is subscribed to.
     *
     * @returns the menu groups; an HTTP error reaches the subscriber unchanged
     */
    getMenus(): Observable<DotMenu[]> {
        return this.#http.get<DotCMSResponse<DotMenu[]>>(MENU_URL).pipe(map((res) => res.entity));
    }
}
