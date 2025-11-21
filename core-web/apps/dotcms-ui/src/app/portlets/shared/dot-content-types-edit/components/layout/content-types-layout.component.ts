import { Observable } from 'rxjs';

import { AsyncPipe, CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabsModule } from 'primeng/tabs';

import { take } from 'rxjs/operators';

import { DotCurrentUserService, DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';
import {
    DotApiLinkComponent,
    DotAutofocusDirective,
    DotCopyButtonComponent,
    DotIconComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotInlineEditComponent } from '../../../../../view/components/_common/dot-inline-edit/dot-inline-edit.component';
import { IframeComponent } from '../../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';
import { DotSecondaryToolbarComponent } from '../../../../../view/components/dot-secondary-toolbar/dot-secondary-toolbar.component';
import { DotAddToMenuComponent } from '../../../dot-content-types-listing/components/dot-add-to-menu/dot-add-to-menu.component';
import { ContentTypesFieldsListComponent } from '../fields/content-types-fields-list';
import { FieldDragDropService } from '../fields/service';

@Component({
    selector: 'dot-content-type-layout',
    styleUrls: ['./content-types-layout.component.scss'],
    templateUrl: 'content-types-layout.component.html',
    imports: [
        CommonModule,
        AsyncPipe,
        TabsModule,
        SplitButtonModule,
        ButtonModule,
        InputTextModule,
        DotSecondaryToolbarComponent,
        DotIconComponent,
        DotApiLinkComponent,
        DotCopyButtonComponent,
        DotMessagePipe,
        DotAutofocusDirective,
        DotInlineEditComponent,
        DotPortletBoxComponent,
        IframeComponent,
        DotAddToMenuComponent,
        ContentTypesFieldsListComponent
    ]
})
export class ContentTypesLayoutComponent implements OnChanges, OnInit {
    private dotMessageService = inject(DotMessageService);
    private dotMenuService = inject(DotMenuService);
    private fieldDragDropService = inject(FieldDragDropService);
    private dotEventsService = inject(DotEventsService);
    private dotCurrentUserService = inject(DotCurrentUserService);

    @Input() contentType: DotCMSContentType;
    @Output() openEditDialog: EventEmitter<unknown> = new EventEmitter();
    @Output() changeContentTypeName: EventEmitter<string> = new EventEmitter();
    @ViewChild('contentTypeNameInput') contentTypeNameInput: ElementRef;
    @ViewChild('dotEditInline') dotEditInline: DotInlineEditComponent;

    permissionURL: string;
    pushHistoryURL: string;
    relationshipURL: string;
    contentTypeNameInputSize: number;
    showPermissionsTab: Observable<boolean>;
    addToMenuContentType = false;

    actions: MenuItem[];

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
     * @param {MouseEvent} event
     * @memberof ContentTypesLayoutComponent
     */
    editInlineActivate(event: MouseEvent): void {
        this.contentTypeNameInputSize = event.target['offsetWidth'];
    }

    /**
     * Based on keyboard input executes an action to Change Name/Hide Input/Change Input's size
     *
     * @param {KeyboardEvent} event
     * @memberof ContentTypesLayoutComponent
     */
    inputValueHandler(event: KeyboardEvent): void {
        if (event.key === 'Enter') {
            this.fireChangeName();
        } else if (event.key === 'Escape') {
            this.dotEditInline.hideContent();
        } else {
            const newInputSize = event.target['value'].length * 8 + 22;
            this.contentTypeNameInputSize = newInputSize > 485 ? 485 : newInputSize;
        }
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

    /**
     * Show Dialog of Add To Menu
     * @memberof ContentTypesLayoutComponent
     */
    addContentInMenu() {
        this.addToMenuContentType = true;
    }
}
