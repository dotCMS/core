import { DotHttpErrorManagerService, DotHttpErrorHandled } from '../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { CrudService } from '../../../api/services/crud';
import { ContentType } from '../shared/content-type.model';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { LoginService, ResponseView } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { take, map, catchError } from 'rxjs/operators';

/**
 * With the url return a content type by id or a default content type
 *
 * @export
 * @class ContentTypeEditResolver
 * @implements {Resolve<ContentType>}
 */
@Injectable()
export class ContentTypeEditResolver implements Resolve<ContentType> {
    constructor(
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotRouterService: DotRouterService,
        private loginService: LoginService
    ) {}

    resolve(route: ActivatedRouteSnapshot): Observable<ContentType> {
        if (route.paramMap.get('id')) {
            return this.getContentType(route.paramMap.get('id'));
        } else {
            return this.getDefaultContentType(route.paramMap.get('type'));
        }
    }

    private getContentType(id: string): Observable<ContentType> {
        return this.crudService
            .getDataById('v1/contenttype', id)
            .pipe(
                take(1),
                catchError((err: ResponseView) => {
                    return this.dotHttpErrorManagerService.handle(err).pipe(
                        map((res: DotHttpErrorHandled) => {
                            if (!res.redirected) {
                                this.dotRouterService.gotoPortlet('/content-types-angular', true);
                            }

                            return null;
                        }
                    ));
                })
            );
    }

    private getDefaultContentType(type: string): Observable<ContentType> {
        return Observable.of({
            owner: this.loginService.auth.user.userId,
            baseType: type.toUpperCase(),
            clazz: this.contentTypesInfoService.getClazz(type),
            defaultType: false,
            fixed: false,
            folder: 'SYSTEM_FOLDER',
            host: null,
            name: null,
            system: false
        });
    }
}
