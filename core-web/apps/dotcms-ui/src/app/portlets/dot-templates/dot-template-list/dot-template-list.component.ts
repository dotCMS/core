import { patchState, signalState } from '@ngrx/signals';

import { CommonModule } from '@angular/common';
import {
    Component,
    DestroyRef,
    ElementRef,
    OnInit,
    effect,
    inject,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { LazyLoadEvent, MenuItem, SharedModule, SortEvent } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ContextMenu } from 'primeng/contextmenu';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { SkeletonModule } from 'primeng/skeleton';
import { Table, TableModule } from 'primeng/table';

import { map, take } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService,
    PushPublishService
} from '@dotcms/data-access';
import { DotPushPublishDialogService, SiteService } from '@dotcms/dotcms-js';
import {
    DotActionBulkResult,
    DotActionMenuItem,
    DotBulkFailItem,
    DotContentState,
    DotMessageSeverity,
    DotMessageType,
    DotTemplate
} from '@dotcms/dotcms-models';
import {
    DotAddToBundleComponent,
    DotContentletStatusChipComponent,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { DotTemplatesService } from '../../../api/services/dot-templates/dot-templates.service';
import { ActionHeaderOptions } from '../../../shared/models/action-header/action-header-options.model';
import { DataTableColumn } from '../../../shared/models/data-table/data-table-column';
import { DotBulkInformationComponent } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotEmptyStateComponent } from '../../../view/components/_common/dot-empty-state/dot-empty-state.component';

interface TemplateListState {
    tableColumns: DataTableColumn[];
    templateBulkActions: MenuItem[];
    actionHeaderOptions: ActionHeaderOptions | null;
    addToBundleIdentifier: string | null;
    selectedTemplates: DotTemplate[];
    hasEnvironments: boolean;
    templates: DotTemplate[];
    loading: boolean;
    totalRecords: number;
    first: number;
    page: number;
    perPage: number;
    sortField: string;
    sortOrder: number;
    archive: boolean;
    filter: string;
}

@Component({
    selector: 'dot-template-list',
    templateUrl: './dot-template-list.component.html',
    styleUrls: ['./dot-template-list.component.scss'],
    imports: [
        CommonModule,
        FormsModule,
        DotMessagePipe,
        DotRelativeDatePipe,
        SharedModule,
        CheckboxModule,
        MenuModule,
        ButtonModule,
        DotAddToBundleComponent,
        DynamicDialogModule,
        DotEmptyStateComponent,
        AutoFocusModule,
        TableModule,
        SkeletonModule,
        InputTextModule,
        DotContentletStatusChipComponent,
        ContextMenu
    ],
    providers: [DotTemplatesService, DialogService, DotSiteBrowserService]
})
export class DotTemplateListComponent implements OnInit {
    private dotAlertConfirmService = inject(DotAlertConfirmService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private dotMessageService = inject(DotMessageService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);
    private dotRouterService = inject(DotRouterService);
    private dotSiteService = inject(SiteService);
    private dotTemplatesService = inject(DotTemplatesService);
    private pushPublishService = inject(PushPublishService);
    private destroyRef = inject(DestroyRef);
    private dotSiteBrowserService = inject(DotSiteBrowserService);

    dialogService = inject(DialogService);

    dataTable = viewChild<Table>('dataTable');
    globalSearch = viewChild<ElementRef>('globalSearch');
    contextMenu = viewChild<ContextMenu>('contextMenu');

    selectedTemplates: DotTemplate[] = [];
    filter = '';
    contextMenuItems: MenuItem[] = [];

    readonly MIN_ROWS_PER_PAGE = 40;
    readonly rowsPerPageOptions = [20, this.MIN_ROWS_PER_PAGE, 60];

    /**
     * Effect that clears selected items when templates change
     */
    protected readonly $cleanSelectedItems = effect(() => {
        this.$state.templates();
        this.selectedTemplates = [];
        patchState(this.$state, { selectedTemplates: [] });
    });

    readonly $state = signalState<TemplateListState>({
        tableColumns: [],
        templateBulkActions: [],
        actionHeaderOptions: null,
        addToBundleIdentifier: null,
        selectedTemplates: [],
        hasEnvironments: false,
        templates: [],
        loading: false,
        totalRecords: 0,
        first: 0,
        page: 1,
        perPage: this.MIN_ROWS_PER_PAGE,
        sortField: 'modDate',
        sortOrder: -1,
        archive: false,
        filter: ''
    });

    ngOnInit(): void {
        // Initialize immediately without waiting for environments
        patchState(this.$state, {
            tableColumns: this.setTemplateColumns(),
            templateBulkActions: this.setTemplateBulkActions()
        });

        // Sync filter with state
        this.filter = this.$state.filter();

        // Load environments asynchronously in the background
        this.loadEnvironments();

        // Load initial templates
        this.loadTemplates();

        // Listen for site changes using SiteService
        this.dotSiteService.switchSite$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
            this.dotRouterService.gotoPortlet('templates');
        });
    }

    /**
     * Load push-publish environments asynchronously and update menus when ready.
     * This is non-blocking and doesn't delay route activation.
     *
     * @private
     * @memberof DotTemplateListComponent
     */
    private loadEnvironments(): void {
        this.pushPublishService
            .getEnvironments()
            .pipe(
                map((environments) => !!environments.length),
                take(1)
            )
            .subscribe((hasEnvironments: boolean) => {
                // Update environments flag and bulk actions menu with Remote-Publish option if available
                patchState(this.$state, {
                    hasEnvironments,
                    templateBulkActions: this.setTemplateBulkActions()
                });
            });
    }

    /**
     * Handle selected template.
     *
     * @param {DotTemplate} template
     * @memberof DotTemplateListComponent
     */
    editTemplate(template: DotTemplate): void {
        this.isTemplateAsFile(template)
            ? this.dotSiteBrowserService.setSelectedFolder(template.identifier).subscribe(() => {
                  this.dotRouterService.goToSiteBrowser();
              })
            : this.dotRouterService.goToEditTemplate(template.identifier);
    }

    /**
     * Handle row click/double click
     * @param {DotTemplate} template
     * @memberof DotTemplateListComponent
     */
    onRowClick(template: DotTemplate): void {
        this.editTemplate(template);
    }

    /**
     * Handle filter for hide / show archive templates
     * @param {boolean} checked
     *
     * @memberof DotTemplateListComponent
     */
    handleArchivedFilter(checked: boolean): void {
        patchState(this.$state, { archive: checked, first: 0, page: 1 });
        this.loadTemplates();
    }

    /**
     * Handle selection change from table
     * @memberof DotTemplateListComponent
     */
    onSelectionChange(): void {
        patchState(this.$state, { selectedTemplates: this.selectedTemplates });
    }

    /**
     * Set the actions of each template based o current state.
     * @param {DotTemplate} template
     ** @returns DotActionMenuItem[]
     * @memberof DotTemplateListComponent
     */
    setTemplateActions(template: DotTemplate): DotActionMenuItem[] {
        let options: DotActionMenuItem[];
        if (template.deleted) {
            options = this.setArchiveTemplateActions(template);
        } else {
            options = this.setBaseTemplateOptions(template);
            if (template.canPublish) {
                options = [
                    ...options,
                    ...this.setLicenseAndRemotePublishTemplateOptions(template),
                    ...this.setUnPublishAndArchiveTemplateOptions(template)
                ];
            }

            options = [...options, ...this.setCopyTemplateOptions(template)];
        }

        return options;
    }

    /**
     * set the content menu items of the listing to be shown, base on the template status.
     * @param {Event} event
     * @param {DotTemplate} template
     * @memberof DotTemplateListComponent
     */
    setContextMenu(event: Event, template: DotTemplate): void {
        if (template.disableInteraction) {
            event.preventDefault();
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        this.contextMenuItems = this.setTemplateActions(template).map(
            ({ menuItem }: DotActionMenuItem) => menuItem
        );
        if (this.contextMenu()) {
            this.contextMenu().show(event);
        }
    }

    /**
     * get the attributes that define the state of a template.
     * @param {DotTemplate} { live, working, deleted, hasLiveVersion}
     * @returns DotContentState
     * @memberof DotTemplateListComponent
     */
    getTemplateState({ live, working, deleted, hasLiveVersion }: DotTemplate): DotContentState {
        return { live, working, deleted, hasLiveVersion };
    }

    /**
     * Set the selected folder in the Site Browser portlet.
     *
     * @param {Event} event
     * @param {string} path
     * @memberof DotTemplateListComponent
     */
    goToFolder(event: Event, path: string) {
        event.stopPropagation();

        this.dotSiteBrowserService.setSelectedFolder(path).subscribe(() => {
            this.dotRouterService.goToSiteBrowser();
        }); // This takes one under the hood
    }

    /**
     * Map table results to add the disableInteraction property.
     * @param {DotTemplate[]} templates
     * @returns DotTemplate[]
     * @memberof DotTemplateListComponent
     */
    mapTableItems(templates: DotTemplate[]): DotTemplate[] {
        return templates.map((template) => {
            // SYSTEM_TEMPLATE is completely disabled
            // Advanced templates (file-based) are clickable but not selectable and no context menu
            template.disableInteraction =
                template.identifier === 'SYSTEM_TEMPLATE' || template.identifier.includes('/');

            return template;
        });
    }

    /**
     * Identify if is a template as File based on the identifier path.
     * @param {DotTemplate} {identifier}
     * @returns boolean
     * @memberof DotTemplateListComponent
     */
    isTemplateAsFile({ identifier }: DotTemplate): boolean {
        return identifier.includes('/');
    }

    handleButtonClick(): void {
        this.dotRouterService.gotoPortlet(`/templates/new`);
    }

    clearAddToBundle(): void {
        patchState(this.$state, { addToBundleIdentifier: null });
    }

    private setTemplateColumns(): DataTableColumn[] {
        return [
            {
                fieldName: 'name',
                header: this.dotMessageService.get('templates.fieldName.name'),
                sortable: true
            },
            {
                fieldName: 'status',
                header: this.dotMessageService.get('templates.fieldName.status'),
                width: '8%'
            },
            {
                fieldName: 'theme',
                header: this.dotMessageService.get('templates.fieldName.theme')
            },
            {
                fieldName: 'friendlyName',
                header: this.dotMessageService.get('templates.fieldName.description')
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: this.dotMessageService.get('templates.fieldName.lastEdit'),
                sortable: true,
                textAlign: 'left'
            }
        ];
    }

    private setTemplateBulkActions(): MenuItem[] {
        return [
            {
                label: this.dotMessageService.get('Publish'),
                command: () => {
                    this.publishTemplate(
                        this.$state.selectedTemplates().map((template) => template.identifier)
                    );
                }
            },
            ...this.setLicenseAndRemotePublishTemplateBulkOptions(),
            {
                label: this.dotMessageService.get('Unpublish'),
                command: () => {
                    this.unPublishTemplate(
                        this.$state.selectedTemplates().map((template) => template.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Archive'),
                command: () => {
                    this.archiveTemplates(
                        this.$state.selectedTemplates().map((template) => template.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Unarchive'),
                command: () => {
                    this.unArchiveTemplate(
                        this.$state.selectedTemplates().map((template) => template.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Delete'),
                command: () => {
                    this.deleteTemplate(
                        this.$state.selectedTemplates().map((template) => template.identifier)
                    );
                }
            }
        ];
    }

    private setCopyTemplateOptions(template: DotTemplate): DotActionMenuItem[] {
        return template.canWrite
            ? [
                  {
                      menuItem: {
                          label: this.dotMessageService.get('Copy'),
                          command: () => {
                              this.dotTemplatesService
                                  .copy(template.identifier)
                                  .pipe(take(1))
                                  .subscribe((response: DotTemplate) => {
                                      if (response) {
                                          this.showToastNotification(
                                              this.dotMessageService.get('message.template.copy')
                                          );
                                          this.loadCurrentPage();
                                      }
                                  });
                          }
                      }
                  }
              ]
            : [];
    }

    private setUnPublishAndArchiveTemplateOptions(template: DotTemplate): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (template.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Unpublish'),
                    command: () => {
                        this.unPublishTemplate([template.identifier]);
                    }
                }
            });
        } else {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Archive'),
                    command: () => {
                        this.archiveTemplates([template.identifier]);
                    }
                }
            });
        }

        return options;
    }

    private setLicenseAndRemotePublishTemplateOptions(template: DotTemplate): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (this.$state.hasEnvironments()) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Remote-Publish'),
                    command: () => {
                        this.dotPushPublishDialogService.open({
                            assetIdentifier: template.identifier,
                            title: this.dotMessageService.get('contenttypes.content.push_publish')
                        });
                    }
                }
            });
        }

        options.push({
            menuItem: {
                label: this.dotMessageService.get('Add-To-Bundle'),
                command: () => {
                    patchState(this.$state, { addToBundleIdentifier: template.identifier });
                }
            }
        });

        return options;
    }

    private setLicenseAndRemotePublishTemplateBulkOptions(): MenuItem[] {
        const bulkOptions: MenuItem[] = [];
        if (this.$state.hasEnvironments()) {
            bulkOptions.push({
                label: this.dotMessageService.get('Remote-Publish'),
                command: () => {
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: this.$state
                            .selectedTemplates()
                            .map((template) => template.identifier)
                            .toString(),
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    });
                }
            });
        }

        bulkOptions.push({
            label: this.dotMessageService.get('Add-To-Bundle'),
            command: () => {
                patchState(this.$state, {
                    addToBundleIdentifier: this.$state
                        .selectedTemplates()
                        .map((template) => template.identifier)
                        .toString()
                });
            }
        });

        return bulkOptions;
    }

    private setBaseTemplateOptions(template: DotTemplate): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];

        if (template.canWrite) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('edit'),
                    command: () => {
                        this.editTemplate(template);
                    }
                }
            });
        }

        if (template.canPublish) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('publish'),
                    command: () => {
                        this.publishTemplate([template.identifier]);
                    }
                }
            });
        }

        return options;
    }

    private setArchiveTemplateActions(template: DotTemplate): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (template.canPublish) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Unarchive'),
                    command: () => {
                        this.unArchiveTemplate([template.identifier]);
                    }
                }
            });
        }

        if (template.canWrite) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Delete'),
                    command: () => {
                        this.deleteTemplate([template.identifier]);
                    }
                }
            });
        }

        return options;
    }

    private deleteTemplate(identifiers: string[]): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotTemplatesService
                    .delete(identifiers)
                    .pipe(take(1))
                    .subscribe((response: DotActionBulkResult) => {
                        this.notifyResult(response, 'message.template.full_delete');
                    });
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('Delete-Template'),
            message: this.dotMessageService.get('message.template.confirm.delete.template')
        });
    }

    private publishTemplate(identifiers: string[]): void {
        this.dotTemplatesService
            .publish(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.template_list.published');
            });
    }

    private unPublishTemplate(identifiers: string[]): void {
        this.dotTemplatesService
            .unPublish(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.template.unpublished');
            });
    }

    private unArchiveTemplate(identifiers: string[]): void {
        this.dotTemplatesService
            .unArchive(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.template.undelete');
            });
    }

    private archiveTemplates(identifiers: string[]): void {
        this.dotTemplatesService
            .archive(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.template.delete');
            });
    }

    private notifyResult(response: DotActionBulkResult, messageKey: string): void {
        if (response.fails.length) {
            this.showErrorDialog({
                ...response,
                fails: this.getFailsInfo(response.fails),
                action: this.dotMessageService.get(messageKey)
            });
        } else {
            this.showToastNotification(this.dotMessageService.get(messageKey));
        }

        this.clearSelection();
        this.loadCurrentPage();
    }

    private showToastNotification(message: string): void {
        this.dotMessageDisplayService.push({
            life: 3000,
            message: message,
            severity: DotMessageSeverity.SUCCESS,
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }

    private showErrorDialog(result: DotActionBulkResult): void {
        this.dialogService.open(DotBulkInformationComponent, {
            header: this.dotMessageService.get('Results'),
            width: '40rem',
            contentStyle: { 'max-height': '500px', overflow: 'auto' },
            baseZIndex: 10000,
            data: result
        });
    }

    private getFailsInfo(items: DotBulkFailItem[]): DotBulkFailItem[] {
        return items.map((item: DotBulkFailItem) => {
            return { ...item, description: this.getTemplateName(item.element) };
        });
    }

    private getTemplateName(identifier: string): string {
        return (
            this.$state.templates().find((template: DotTemplate) => {
                return template.identifier === identifier;
            })?.name || ''
        );
    }

    /**
     * Load templates from the service
     * @private
     * @memberof DotTemplateListComponent
     */
    private loadTemplates(): void {
        patchState(this.$state, { loading: true });

        const state = this.$state();
        const direction = state.sortOrder === -1 ? 'DESC' : 'ASC';

        this.dotTemplatesService
            .getFiltered({
                page: state.page,
                per_page: state.perPage,
                orderby: state.sortField,
                direction,
                archive: state.archive,
                filter: state.filter || ''
            })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (response) => {
                    const mappedTemplates = this.mapTableItems(response.templates);
                    patchState(this.$state, {
                        templates: mappedTemplates,
                        loading: false,
                        totalRecords: response.totalRecords
                    });
                },
                error: () => {
                    patchState(this.$state, { loading: false });
                }
            });
    }

    /**
     * Handle pagination event
     * @param {LazyLoadEvent} event
     * @memberof DotTemplateListComponent
     */
    onPage(event: LazyLoadEvent): void {
        const page = event.first && event.rows ? Math.floor(event.first / event.rows) + 1 : 1;
        patchState(this.$state, {
            first: event.first || 0,
            page,
            perPage: event.rows || this.MIN_ROWS_PER_PAGE
        });
        this.loadTemplates();
    }

    /**
     * Handle sort event
     * @param {SortEvent} event
     * @memberof DotTemplateListComponent
     */
    onSort(event: SortEvent): void {
        patchState(this.$state, {
            sortField: event.field || 'modDate',
            sortOrder: event.order || -1
        });
        this.loadTemplates();
    }

    /**
     * Handle first change event (for pagination sync)
     * Basically primeNG Table handles the change of the first on every OnChange
     * Making it lose the reference if you do a sort and do not handle this manually
     *
     * Check this issue to know if we are able to remove this function
     * since its a legacy issue that they are basically ignoring.
     * https://github.com/primefaces/primeng/issues/11898#issuecomment-1831076132
     * @memberof DotTemplateListComponent
     */
    onFirstChange(): void {
        const dataTable = this.dataTable();

        if (dataTable) {
            dataTable.first = this.$state.first();
        }
    }

    /**
     * Clear selection
     * @memberof DotTemplateListComponent
     */
    clearSelection(): void {
        this.selectedTemplates = [];
        patchState(this.$state, { selectedTemplates: [] });
        if (this.dataTable()) {
            this.dataTable().selection = [];
        }
    }

    /**
     * Reload current page
     * @memberof DotTemplateListComponent
     */
    loadCurrentPage(): void {
        this.loadTemplates();
    }

    /**
     * Handle filter input change
     * @param {string} value
     * @memberof DotTemplateListComponent
     */
    onFilterChange(value: string): void {
        this.filter = value;
        patchState(this.$state, { filter: value, first: 0, page: 1 });
        this.loadTemplates();
    }

    /**
     * Focus first row (for keyboard navigation)
     * @memberof DotTemplateListComponent
     */
    focusFirstRow(): void {
        // Implementation for focusing first row if needed
    }
}
