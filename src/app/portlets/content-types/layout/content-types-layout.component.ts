import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { DotMenuService } from '@services/dot-menu.service';
import { FieldDragDropService } from '../fields/service';

@Component({
    selector: 'dot-content-type-layout',
    styleUrls: ['./content-types-layout.component.scss'],
    templateUrl: 'content-types-layout.component.html'
})
export class ContentTypesLayoutComponent implements OnChanges, OnInit {
    @Input()
    contentTypeId: string;

    permissionURL: string;
    pushHistoryURL: string;
    relationshipURL: string;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        private dotMessageService: DotMessageService,
        private dotMenuService: DotMenuService,
        private fieldDragDropService: FieldDragDropService
    ) {}

    ngOnInit(): void {
        this.fieldDragDropService.setBagOptions();
        this.dotMessageService
            .getMessages([
                'contenttypes.sidebar.components.title',
                'contenttypes.tab.fields.header',
                'contenttypes.sidebar.layouts.title',
                'contenttypes.tab.permissions.header',
                'contenttypes.tab.publisher.push.history.header',
                'contenttypes.tab.relationship.header'
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });
    }

    ngOnChanges(changes): void {
        if (changes.contentTypeId.currentValue) {
            this.dotMenuService.getDotMenuId('content-types-angular').subscribe((id) => {
                // tslint:disable-next-line:max-line-length
                this.relationshipURL = `c/portal/layout?p_l_id=${id}&p_p_id=content-types&_content_types_struts_action=%2Fext%2Fstructure%2Fview_relationships&_content_types_structure_id=${
                    changes.contentTypeId.currentValue
                }`;
            });

            this.permissionURL = `/html/content_types/permissions.jsp?contentTypeId=${
                changes.contentTypeId.currentValue
            }&popup=true`;
            this.pushHistoryURL = `/html/content_types/push_history.jsp?contentTypeId=${
                changes.contentTypeId.currentValue
            }&popup=true`;
        }
    }
}
