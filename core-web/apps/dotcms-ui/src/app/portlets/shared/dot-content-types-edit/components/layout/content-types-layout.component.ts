import { Observable } from 'rxjs';

import { AsyncPipe } from '@angular/common';
import {
    Component,
    ElementRef,
    OnInit,
    computed,
    effect,
    inject,
    input,
    output,
    viewChild
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabsModule } from 'primeng/tabs';

import { take } from 'rxjs/operators';

import {
    DotCurrentUserService,
    DotEventsService,
    DotMessageService,
    DotPropertiesService
} from '@dotcms/data-access';
import { DotCMSContentType, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotClipboardUtil, DotMessagePipe } from '@dotcms/ui';

import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { DotInlineEditComponent } from '../../../../../view/components/_common/dot-inline-edit/dot-inline-edit.component';
import { IframeComponent } from '../../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';
import { DotAddToMenuComponent } from '../../../dot-content-types-listing/components/dot-add-to-menu/dot-add-to-menu.component';
import { ContentTypesFieldsListComponent } from '../fields/content-types-fields-list';
import { FieldDragDropService } from '../fields/service';
import { DotStyleEditorBuilderComponent } from '../style-editor/dot-style-editor-builder.component';

@Component({
    selector: 'dot-content-type-layout',
    templateUrl: 'content-types-layout.component.html',
    providers: [DotClipboardUtil],
    imports: [
        AsyncPipe,
        TabsModule,
        SplitButtonModule,
        ButtonModule,
        InputTextModule,
        MenuModule,
        DotMessagePipe,
        DotPortletBoxComponent,
        IframeComponent,
        DotAddToMenuComponent,
        ContentTypesFieldsListComponent,
        DotStyleEditorBuilderComponent
    ]
})
export class ContentTypesLayoutComponent implements OnInit {
    #dotMessageService = inject(DotMessageService);
    #dotMenuService = inject(DotMenuService);
    #fieldDragDropService = inject(FieldDragDropService);
    #dotEventsService = inject(DotEventsService);
    #dotCurrentUserService = inject(DotCurrentUserService);
    #dotPropertiesService = inject(DotPropertiesService);
    #dotClipboardUtil = inject(DotClipboardUtil);

    $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });
    openEditDialog = output<unknown>();
    changeContentTypeName = output<string>();
    $contentTypeNameInput = viewChild.required<ElementRef>('contentTypeNameInput');
    $dotEditInline = viewChild.required<DotInlineEditComponent>('dotEditInline');

    permissionURL: string;
    pushHistoryURL: string;
    relationshipURL: string;
    contentTypeNameInputSize: number;
    showPermissionsTab: Observable<boolean>;
    readonly $showStyleEditorTab = toSignal(
        this.#dotPropertiesService.getFeatureFlag(
            FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR_FOR_TRADITIONAL_PAGES
        ),
        { initialValue: false }
    );
    addToMenuContentType = false;

    actions: MenuItem[];

    /** Context menu items derived from the current content type. */
    readonly $menuItems = computed<MenuItem[]>(() => {
        const ct = this.$contentType();

        return [
            {
                label: this.#dotMessageService.get('contenttypes.content.add_to_menu'),
                icon: 'pi pi-plus-circle',
                command: () => this.addContentInMenu()
            },
            {
                label: this.#dotMessageService.get('contenttypes.content.open.api'),
                icon: 'pi pi-external-link',
                command: () => window.open(`/api/v1/contenttype/id/${ct.id}`, '_blank')
            },
            {
                label: this.#dotMessageService.get('contenttypes.content.copy.id'),
                icon: 'pi pi-copy',
                command: () => this.#dotClipboardUtil.copy(ct.id)
            },
            {
                label: this.#dotMessageService.get(
                    'contenttypes.content.copy.variable',
                    ct.variable
                ),
                icon: 'pi pi-copy',
                command: () => this.#dotClipboardUtil.copy(ct.variable)
            }
        ];
    });

    ngOnInit(): void {
        this.showPermissionsTab = this.#dotCurrentUserService.hasAccessToPortlet('permissions');
        this.#fieldDragDropService.setBagOptions();
        this.loadActions();
    }

    constructor() {
        effect(() => {
            const ct = this.$contentType();
            if (ct) {
                this.#dotMenuService
                    .getDotMenuId('content-types-angular')
                    .pipe(take(1))
                    .subscribe((id: string) => {
                        this.relationshipURL = `/c/portal/layout?p_l_id=${id}&p_p_id=content-types&_content_types_struts_action=%2Fext%2Fstructure%2Fview_relationships&_content_types_structure_id=${ct.id}`;
                    });
                this.permissionURL = `/html/content_types/permissions.jsp?contentTypeId=${ct.id}&popup=true`;
                this.pushHistoryURL = `/html/content_types/push_history.jsp?contentTypeId=${ct.id}&popup=true`;
            }
        });
    }

    /**
     * Emits add-row event to add new row
     *
     * @memberof ContentTypesLayoutComponent
     */
    fireAddRowEvent(): void {
        this.#dotEventsService.notify('add-row');
    }

    /**
     * Emits new name to parent component and close Edit Inline mode
     *
     * @memberof ContentTypesLayoutComponent
     */
    fireChangeName(): void {
        const contentTypeName = this.$contentTypeNameInput().nativeElement.value.trim();
        this.changeContentTypeName.emit(contentTypeName);
        this.$contentType().name = contentTypeName;
        this.$dotEditInline().hideContent();
    }

    /**
     * Sets the size of the H4 display to set it in the content textbox to eliminate UI jumps
     *
     * @param {MouseEvent} event
     * @memberof ContentTypesLayoutComponent
     */
    editInlineActivate(event: MouseEvent): void {
        this.contentTypeNameInputSize = event.target['offsetWidth'] + 20;
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
            this.$dotEditInline().hideContent();
        } else {
            const newInputSize = event.target['value'].length * 8 + 22;
            this.contentTypeNameInputSize = newInputSize > 485 ? 485 : newInputSize;
        }
    }

    private loadActions(): void {
        this.actions = [
            {
                label: this.#dotMessageService.get('contenttypes.dropzone.rows.add'),
                command: () => {
                    this.fireAddRowEvent();
                }
            },
            {
                label: this.#dotMessageService.get('contenttypes.dropzone.rows.tab_divider'),
                command: () => {
                    this.#dotEventsService.notify('add-tab-divider');
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
