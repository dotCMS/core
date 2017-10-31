import { Injectable } from '@angular/core';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs/Observable';
import { CrudService } from '../../api/services/crud';
import { ContentType } from './shared/content-type.model';
import { ContentTypesInfoService } from '../../api/services/content-types-info';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DotRouterService } from '../../api/services/dot-router-service';

/**
 * With the url return a content type by id or a default content type
 *
 * @export
 * @class ContentTypeResolver
 * @implements {Resolve<ContentType>}
 */
@Injectable()
export class ContentTypeResolver implements Resolve<ContentType> {
    constructor(
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private loginService: LoginService,
        private dotRouterService: DotRouterService
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
            .take(1)
            .map((contentType: ContentType) => {
                if (contentType) {
                    return contentType;
                } else {
                    this.dotRouterService.gotoPortlet('/content-types-angular');
                    return null;
                }
            })
            .catch(() => {
                this.dotRouterService.gotoPortlet('/content-types-angular', true);
                return Observable.of(null);
            });
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
