import { Component, Input, ViewEncapsulation, OnChanges } from '@angular/core';
import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { MessageService } from '../../../api/services/messages-service';
import { DragulaService } from 'ng2-dragula';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'content-type-layout',
    styleUrls: ['./content-types-layout.component.scss'],
    templateUrl: 'content-types-layout.component.html'
})

export class ContentTypesLayoutComponent extends BaseComponent implements OnChanges {
    @Input() contentTypeId: string;

    permissionURL: string;
    pushHistoryURL: string;

    constructor(messageService: MessageService) {
        super([
            'contenttypes.sidebar.components.title',
            'contenttypes.tab.header.fields',
            'contenttypes.sidebar.layouts.title',
            'contenttypes.tab.header.permissions',
            'contenttypes.tab.header.publisher.push.history',
        ], messageService);
    }

    ngOnChanges(changes): void {
        if (changes.contentTypeId.currentValue) {
            this.permissionURL = `/html/content_types/permissions.jsp?contentTypeId=${changes.contentTypeId.currentValue}&popup=true`;
            this.pushHistoryURL = `/html/content_types/push_history.jsp?contentTypeId=${changes.contentTypeId.currentValue}&popup=true`;
        }
    }
}
