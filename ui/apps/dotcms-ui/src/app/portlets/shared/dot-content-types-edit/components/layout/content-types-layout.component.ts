import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotMenuService } from '@services/dot-menu.service';
import { FieldDragDropService } from '../fields/service';
import { take } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import { DotCurrentUserService } from '@services/dot-current-user/dot-current-user.service';
import { Observable } from 'rxjs';
import { DotInlineEditComponent } from '@components/_common/dot-inline-edit/dot-inline-edit.component';

@Component({
    selector: 'dot-content-type-layout',
    styleUrls: ['./content-types-layout.component.scss'],
    templateUrl: 'content-types-layout.component.html'
})
export class ContentTypesLayoutComponent implements OnChanges, OnInit {
    @Input() contentType: DotCMSContentType;
    @Output() openEditDialog: EventEmitter<unknown> = new EventEmitter();
    @Output() changeContentTypeName: EventEmitter<string> = new EventEmitter();
    @ViewChild('contentTypeNameInput') contentTypeNameInput: ElementRef;
    @ViewChild('dotEditInline') dotEditInline: DotInlineEditComponent;

    permissionURL: string;
    pushHistoryURL: string;
    relationshipURL: string;
    contentTypeNameInputSize: string;
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

    /**
     * Emits add-row event to add new row
     *
     * @memberof ContentTypesLayoutComponent
     */
    fireAddRowEvent(): void {
        this.dotEventsService.notify('add-row');
    }

    /**
     * Emits new name to parent component and close Edit Inline mode
     *
     * @memberof ContentTypesLayoutComponent
     */
    fireChangeName(): void {
        const contentTypeName = this.contentTypeNameInput.nativeElement.value.trim();
        this.changeContentTypeName.emit(contentTypeName);
        this.contentType.name = contentTypeName;
        this.dotEditInline.hideContent();
    }

    /**
     * Sets the size of the H4 display to set it in the content textbox to eliminate UI jumps
     *
     * @memberof ContentTypesLayoutComponent
     */
    editInlineActivate(event): void {
        this.contentTypeNameInputSize = event.target.offsetWidth;
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
