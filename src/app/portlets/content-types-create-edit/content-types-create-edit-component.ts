import { Component, ViewEncapsulation } from '@angular/core';
import { BaseComponent } from '../../view/components/_common/_base/base-component';
import { MessageService } from '../../api/services/messages-service';

export interface ContentType {
  clazz: string;
  defaultType: boolean;
  description: string;
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
  variable: string;
  workflow: string;
}

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'content-types-create-edit-component',
    styles: [require('./content-types-create-edit-component.scss')],
    templateUrl: 'content-types-create-edit-component.html',
})
export class ContentTypesCreateEditPortletComponent extends BaseComponent {
    constructor(messageService: MessageService) {
        super([
            'fields',
            'Permissions',
            'publisher_push_history',
        ], messageService);
    }
}