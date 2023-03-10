import { Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';

import { catchError, map, take } from 'rxjs/operators';

import {
    DotHttpErrorHandled,
    DotHttpErrorManagerService
} from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotContentTypesInfoService, DotCrudService } from '@dotcms/data-access';
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
export class DotContentTypeEditResolver implements Resolve<DotCMSContentType> {
    constructor(
        private contentTypesInfoService: DotContentTypesInfoService,
        private crudService: DotCrudService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService,
        private loginService: LoginService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotCMSContentType> {
        if (route.paramMap.get('id')) {
            return this.getContentType(route.paramMap.get('id'));
        } else {
            const contentType = this.getFilterByParam(route) || route.paramMap.get('type');

            return this.getDefaultContentType(contentType);
        }
    }

    private getFilterByParam(route: ActivatedRouteSnapshot): string {
        return route.data && route.data.filterBy;
    }

    private getContentType(id: string): Observable<DotCMSContentType> {
        return this.crudService.getDataById('v1/contenttype', id).pipe(
            take(1),
            catchError((err: HttpErrorResponse) => {
                return this.dotHttpErrorManagerService.handle(err).pipe(
                    map((res: DotHttpErrorHandled) => {
                        if (!res.redirected) {
                            this.dotRouterService.gotoPortlet('/content-types-angular', true);
                        }

                        return null;
                    })
                );
            })
        );
    }

    private getDefaultContentType(type: string): Observable<DotCMSContentType> {
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
        });
    }
}
