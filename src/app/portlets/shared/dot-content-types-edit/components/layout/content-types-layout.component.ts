import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotMenuService } from '@services/dot-menu.service';
import { FieldDragDropService } from '../fields/service';
import { take } from 'rxjs/internal/operators/take';
import { MenuItem } from 'primeng/api';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotCMSContentType } from 'dotcms-models';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { Observable } from 'rxjs';

@Component({
    selector: 'dot-content-type-layout',
    styleUrls: ['./content-types-layout.component.scss'],
    templateUrl: 'content-types-layout.component.html'
})
export class ContentTypesLayoutComponent implements OnChanges, OnInit {
    @Input() contentType: DotCMSContentType;
    @Output() openEditDialog: EventEmitter<any> = new EventEmitter();
    permissionURL: string;
    pushHistoryURL: string;
    relationshipURL: string;
    showPermissionsTab: Observable<boolean>;

    actions: MenuItem[];

    constructor(
        private dotMessageService: DotMessageService,
        private dotMenuService: DotMenuService,
        private fieldDragDropService: FieldDragDropService,
        private dotEventsService: DotEventsService,
        private dotCurrentUserService: DotCurrentUserService
    ) {}

    ngOnInit(): void {
        this.showPermissionsTab = this.dotCurrentUserService.hasAccessToPortlet('permissions');
        this.fieldDragDropService.setBagOptions();
        this.loadActions();
    }

    ngOnChanges(changes): void {
        if (changes.contentType.currentValue) {
            this.dotMenuService
                .getDotMenuId('content-types-angular')
                .pipe(take(1))
                .subscribe((id: string) => {
                    // tslint:disable-next-line:max-line-length
                    this.relationshipURL = `c/portal/layout?p_l_id=${id}&p_p_id=content-types&_content_types_struts_action=%2Fext%2Fstructure%2Fview_relationships&_content_types_structure_id=${this.contentType.id}`;
                });
            this.permissionURL = `/html/content_types/permissions.jsp?contentTypeId=${this.contentType.id}&popup=true`;
            this.pushHistoryURL = `/html/content_types/push_history.jsp?contentTypeId=${this.contentType.id}&popup=true`;
        }
    }

    fireAddRowEvent(): void {
        this.dotEventsService.notify('add-row');
    }

    private loadActions(): void {
        this.actions = [
            {
                label: this.dotMessageService.get('contenttypes.dropzone.rows.add'),
                command: () => {
                    this.fireAddRowEvent();
                }
            },
            {
                label: this.dotMessageService.get('contenttypes.dropzone.rows.tab_divider'),
                command: () => {
                    this.dotEventsService.notify('add-tab-divider');
                }
            }
        ];
    }
}
