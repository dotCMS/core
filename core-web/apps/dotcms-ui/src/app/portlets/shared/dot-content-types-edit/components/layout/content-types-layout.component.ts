import {
    Component,
    ElementRef,
    OnInit,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TabsModule } from 'primeng/tabs';

import { filter, map } from 'rxjs/operators';

import { DotEventsService, DotMessageService } from '@dotcms/data-access';
import { DotCMSContentType, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotClipboardUtil, DotMessagePipe } from '@dotcms/ui';

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
    #fieldDragDropService = inject(FieldDragDropService);
    #dotEventsService = inject(DotEventsService);
    #dotClipboardUtil = inject(DotClipboardUtil);
    #router = inject(Router);
    #route = inject(ActivatedRoute);

    $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });
    openEditDialog = output<unknown>();
    changeContentTypeName = output<string>();
    $contentTypeNameInput = viewChild.required<ElementRef>('contentTypeNameInput');
    $dotEditInline = viewChild.required<DotInlineEditComponent>('dotEditInline');

    permissionURL: string;
    pushHistoryURL: string;
    contentTypeNameInputSize: number;
    readonly $showStyleEditorTab = signal<boolean>(
        this.#route.snapshot.data['featuredFlags']?.[FeaturedFlags.FEATURE_FLAG_UVE_STYLE_EDITOR] ??
            false
    );
    readonly $showPermissionsTab = signal<boolean>(
        this.#route.snapshot.data['tabPermissions']?.showPermissionsTab ?? false
    );
    readonly $activeTab = signal(this.#route.firstChild?.snapshot.url[0]?.path ?? 'fields');
    readonly $addToMenuContentType = signal(false);

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
        this.#fieldDragDropService.setBagOptions();
        this.loadActions();
    }

    constructor() {
        effect(() => {
            const ct = this.$contentType();
            if (ct) {
                this.permissionURL = `/html/content_types/permissions.jsp?contentTypeId=${ct.id}&popup=true`;
                this.pushHistoryURL = `/html/content_types/push_history.jsp?contentTypeId=${ct.id}&popup=true`;
            }
        });

        // Keep $activeTab in sync with browser back/forward navigation.
        this.#router.events
            .pipe(
                filter((e) => e instanceof NavigationEnd),
                map(() => this.#route.firstChild?.snapshot.url[0]?.path ?? 'fields'),
                takeUntilDestroyed()
            )
            .subscribe((tab) => this.$activeTab.set(tab));
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

    onTabChange(tab: unknown): void {
        this.$activeTab.set(tab as string);
        this.#router.navigate([tab as string], { relativeTo: this.#route });
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
        this.$addToMenuContentType.set(true);
    }
}
