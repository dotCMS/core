import { Subject } from 'rxjs';

import { Component, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { MenuItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { pluck, skip, take, takeUntil } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSiteBrowserService
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

import { DotTemplatesService } from '../../../api/services/dot-templates/dot-templates.service';
import { ActionHeaderOptions } from '../../../shared/models/action-header/action-header-options.model';
import { DataTableColumn } from '../../../shared/models/data-table/data-table-column';
import { DotBulkInformationComponent } from '../../../view/components/_common/dot-bulk-information/dot-bulk-information.component';
import { DotListingDataTableComponent } from '../../../view/components/dot-listing-data-table/dot-listing-data-table.component';

@Component({
    selector: 'dot-template-list',
    templateUrl: './dot-template-list.component.html',
    styleUrls: ['./dot-template-list.component.scss'],
    standalone: false
})
export class DotTemplateListComponent implements OnInit, OnDestroy {
    private dotAlertConfirmService = inject(DotAlertConfirmService);
    private dotMessageDisplayService = inject(DotMessageDisplayService);
    private dotMessageService = inject(DotMessageService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);
    private dotRouterService = inject(DotRouterService);
    private dotSiteService = inject(SiteService);
    private dotTemplatesService = inject(DotTemplatesService);
    private route = inject(ActivatedRoute);
    dialogService = inject(DialogService);
    private dotSiteBrowserService = inject(DotSiteBrowserService);

    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;
    tableColumns: DataTableColumn[];
    templateBulkActions: MenuItem[];
    actionHeaderOptions: ActionHeaderOptions;
    addToBundleIdentifier: string;
    selectedTemplates: DotTemplate[] = [];

    private isEnterPrise: boolean;
    private hasEnvironments: boolean;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.route.data
            .pipe(pluck('dotTemplateListResolverData'), take(1))
            .subscribe(([isEnterPrise, hasEnvironments]: [boolean, boolean]) => {
                this.isEnterPrise = isEnterPrise;
                this.hasEnvironments = hasEnvironments;
                this.tableColumns = this.setTemplateColumns();
                this.templateBulkActions = this.setTemplateBulkActions();
            });
        this.setAddOptions();

        this.dotSiteService.switchSite$.pipe(skip(1), takeUntil(this.destroy$)).subscribe(() => {
            this.dotRouterService.gotoPortlet('templates');
        });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle selected template.
     *
     * @param {DotTemplate} { template }
     * @memberof DotTemplateListComponent
     */
    editTemplate(event: unknown): void {
        const template = event as DotTemplate;
        this.isTemplateAsFile(template)
            ? this.dotSiteBrowserService.setSelectedFolder(template.identifier).subscribe(() => {
                  this.dotRouterService.goToSiteBrowser();
              })
            : this.dotRouterService.goToEditTemplate(template.identifier);
    }

    /**
     * Handle filter for hide / show archive templates
     * @param {boolean} checked
     *
     * @memberof DotTemplateListComponent
     */
    handleArchivedFilter(checked: boolean): void {
        checked
            ? this.listing.paginatorService.setExtraParams('archive', checked)
            : this.listing.paginatorService.deleteExtraParams('archive');
        this.listing.loadFirstPage();
    }

    /**
     * Keep updated the selected templates in the grid
     * @param {DotTemplate[]} templates
     *
     * @memberof DotTemplateListComponent
     */
    updateSelectedTemplates(event: unknown): void {
        const templates = event as DotTemplate[];
        this.selectedTemplates = templates;
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
     * @param {DotTemplate} template
     * @memberof DotTemplateListComponent
     */
    setContextMenu(event: unknown): void {
        const template = event as DotTemplate;
        this.listing.contextMenuItems = this.setTemplateActions(template).map(
            ({ menuItem }: DotActionMenuItem) => menuItem
        );
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
     * set the labels of dot-state-icon.
     * @returns { [key: string]: string }
     * @memberof DotTemplateListComponent
     */
    setStateLabels(): { [key: string]: string } {
        return {
            archived: this.dotMessageService.get('Archived'),
            published: this.dotMessageService.get('Published'),
            revision: this.dotMessageService.get('Revision'),
            draft: this.dotMessageService.get('Draft')
        };
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
            template.disableInteraction =
                template.identifier.includes('/') || template.identifier === 'SYSTEM_TEMPLATE';

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

    private setAddOptions(): void {
        this.actionHeaderOptions = {
            primary: {
                command: () => {
                    this.dotRouterService.gotoPortlet(`/templates/new`);
                }
            }
        };
    }

    private setTemplateBulkActions(): MenuItem[] {
        return [
            {
                label: this.dotMessageService.get('Publish'),
                command: () => {
                    this.publishTemplate(
                        this.selectedTemplates.map((template) => template.identifier)
                    );
                }
            },
            ...this.setLicenseAndRemotePublishTemplateBulkOptions(),
            {
                label: this.dotMessageService.get('Unpublish'),
                command: () => {
                    this.unPublishTemplate(
                        this.selectedTemplates.map((template) => template.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Archive'),
                command: () => {
                    this.archiveTemplates(
                        this.selectedTemplates.map((template) => template.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Unarchive'),
                command: () => {
                    this.unArchiveTemplate(
                        this.selectedTemplates.map((template) => template.identifier)
                    );
                }
            },
            {
                label: this.dotMessageService.get('Delete'),
                command: () => {
                    this.deleteTemplate(
                        this.selectedTemplates.map((template) => template.identifier)
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
                                          this.listing.loadCurrentPage();
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
        if (this.hasEnvironments) {
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

        if (this.isEnterPrise) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Add-To-Bundle'),
                    command: () => {
                        this.addToBundleIdentifier = template.identifier;
                    }
                }
            });
        }

        return options;
    }

    private setLicenseAndRemotePublishTemplateBulkOptions(): MenuItem[] {
        const bulkOptions: MenuItem[] = [];
        if (this.hasEnvironments) {
            bulkOptions.push({
                label: this.dotMessageService.get('Remote-Publish'),
                command: () => {
                    this.dotPushPublishDialogService.open({
                        assetIdentifier: this.selectedTemplates
                            .map((template) => template.identifier)
                            .toString(),
                        title: this.dotMessageService.get('contenttypes.content.push_publish')
                    });
                }
            });
        }

        if (this.isEnterPrise) {
            bulkOptions.push({
                label: this.dotMessageService.get('Add-To-Bundle'),
                command: () => {
                    this.addToBundleIdentifier = this.selectedTemplates
                        .map((template) => template.identifier)
                        .toString();
                }
            });
        }

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

        this.listing.clearSelection();
        this.listing.loadCurrentPage();
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
        return (this.listing.items as DotTemplate[]).find((template: DotTemplate) => {
            return template.identifier === identifier;
        }).name;
    }
}
