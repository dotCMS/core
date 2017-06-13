import { ActivatedRoute, Params, UrlSegment, Router } from '@angular/router';
import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { Component, ViewEncapsulation } from '@angular/core';
import { CrudService } from '../../../../api/services/crud';
import { MessageService } from '../../../../api/services/messages-service';
import { Observable } from 'rxjs/Observable';

export interface ContentType {
  clazz: string;
  defaultType: boolean;
  description?: string;
  detailPage?: string;
  fields?: Array<string>;
  fixed: boolean;
  folder: string;
  host: string;
  iDate?: Date;
  id?: string;
  modDate?: Date;
  name: string;
  owner: string;
  system: boolean;
  urlMapPattern?: string;
  variable?: string;
  workflow?: string;
}

/**
 * Portlet component for Content Types
 *
 * @export
 * @class ContentTypesCreateEditPortletComponent
 * @extends {BaseComponent}
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'content-types-create-edit',
    styles: [require('./content-types-create-edit.component.scss')],
    templateUrl: 'content-types-create-edit.component.html',
})
export class ContentTypesCreateEditPortletComponent extends BaseComponent {
    private contentTypeItem: Observable<any>;

    constructor(messageService: MessageService, private route: ActivatedRoute,
    private crudService: CrudService, public router: Router) {
        super([
            'fields',
            'Permissions',
            'publisher_push_history',
        ], messageService);
    }

    ngOnInit(): void {
        let urlSegment: UrlSegment[] = this.route.snapshot.url;
        let params: Params = this.route.snapshot.params;

        if (urlSegment[0].path === 'edit' && params.id) {
            this.contentTypeItem = this.crudService.getDataById('/v1/contenttype', params.id);
        }
    }
}
