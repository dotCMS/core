import { of as observableOf, Observable } from 'rxjs';
import {
    DotHttpErrorManagerService,
    DotHttpErrorHandled
} from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { CrudService } from '@services/crud';
import { ContentTypesInfoService } from '@services/content-types-info';
import { DotCMSContentType } from 'dotcms-models';
import { LoginService, ResponseView } from 'dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { take, map, catchError } from 'rxjs/operators';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';

/**
 * With the url return a content type by id or a default content type
 *
 * @export
 * @class ContentTypeEditResolver
 * @implements {Resolve<ContentType>}
 */
@Injectable()
export class ContentTypeEditResolver implements Resolve<DotCMSContentType> {
    constructor(
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService,
        private loginService: LoginService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<DotCMSContentType> {
        if (route.paramMap.get('id')) {
            return this.getContentType(route.paramMap.get('id'));
        } else {
            return this.getDefaultContentType(route.paramMap.get('type'));
        }
    }

    private getContentType(id: string): Observable<DotCMSContentType> {
        return this.crudService.getDataById('v1/contenttype', id).pipe(
            take(1),
            catchError((err: ResponseView) => {
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
        return observableOf({
            ...dotcmsContentTypeBasicMock,
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
            name: null,
            owner: this.loginService.auth.user.userId,
            system: false,
            variable: null,
            versionable: false,
            workflows: []
        });
    }
}
