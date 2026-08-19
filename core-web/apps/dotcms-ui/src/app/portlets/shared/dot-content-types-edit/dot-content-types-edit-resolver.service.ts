import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { catchError, map, take } from 'rxjs/operators';

import {
    DotContentTypesInfoService,
    DotCrudService,
    DotHttpErrorHandled,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotCMSContentType } from '@dotcms/dotcms-models';

/**
 * With the url return a content type by id or a default content type
 *
 * @export
 * @class DotContentTypeEditResolver
 * @implements {Resolve<ContentType>}
 */
@Injectable()
export class DotContentTypeEditResolver implements Resolve<DotCMSContentType | null> {
    private contentTypesInfoService = inject(DotContentTypesInfoService);
    private crudService = inject(DotCrudService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotRouterService = inject(DotRouterService);
    private loginService = inject(LoginService);

    resolve(route: ActivatedRouteSnapshot): Observable<DotCMSContentType | null> {
        // Read once: `paramMap.get` returns `string | null` and calling it twice does not carry the
        // first check's narrowing to the second.
        const id = route.paramMap.get('id');

        if (id) {
            return this.getContentType(id);
        }

        return this.getDefaultContentType(
            this.getFilterByParam(route) || route.paramMap.get('type') || ''
        );
    }

    private getFilterByParam(route: ActivatedRouteSnapshot): string | undefined {
        return route.data && route.data['filterBy'];
    }

    private getContentType(id: string): Observable<DotCMSContentType | null> {
        return this.crudService.getDataById<DotCMSContentType>('v1/contenttype', id).pipe(
            take(1),
            catchError((err: HttpErrorResponse) => {
                return this.dotHttpErrorManagerService.handle(err).pipe(
                    map((res: DotHttpErrorHandled) => {
                        if (!res.redirected) {
                            this.dotRouterService.gotoPortlet('/content-types-angular', {
                                replaceUrl: true
                            });
                        }

                        return null;
                    })
                );
            })
        );
    }

    private getDefaultContentType(type: string): Observable<DotCMSContentType> {
        // The seed for a content type that does not exist yet: the endpoint fills in `id`, `iDate`,
        // `modDate` and the rest on save, which is why they are null here against a model that
        // describes what the endpoint *returns*.
        return of({
            baseType: type,
            clazz: this.contentTypesInfoService.getClazz(type),
            defaultType: false,
            fields: [],
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            iDate: null,
            id: null,
            layout: [],
            modDate: null,
            multilingualable: false,
            nEntries: 0,
            name: null,
            owner: this.loginService.auth.user.userId,
            system: false,
            variable: null,
            versionable: false,
            workflows: []
        } as unknown as DotCMSContentType);
    }
}
