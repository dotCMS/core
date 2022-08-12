import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { filter, pluck, take, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { DotTemplate } from '@models/dot-edit-layout-designer';
import { DataTableColumn } from '@models/data-table';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { MenuItem } from 'primeng/api';
import { ActionHeaderOptions } from '@models/action-header';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotPushPublishDialogService, Site, SiteService } from '@dotcms/dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DialogService } from 'primeng/dynamicdialog';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import {
    DotActionBulkResult,
    DotBulkFailItem
} from '@models/dot-action-bulk-result/dot-action-bulk-result.model';
import { DotContentState } from '@dotcms/dotcms-models';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotSiteBrowserService } from '@services/dot-site-browser/dot-site-browser.service';

@Component({
    selector: 'dot-template-list',
    templateUrl: './dot-template-list.component.html',
    styleUrls: ['./dot-template-list.component.scss']
})
export class DotTemplateListComponent implements OnInit, OnDestroy {
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

    constructor(
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dotMessageService: DotMessageService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotRouterService: DotRouterService,
        private dotSiteService: SiteService,
        private dotTemplatesService: DotTemplatesService,
        private route: ActivatedRoute,
        public dialogService: DialogService,
        private dotSiteBrowserService: DotSiteBrowserService
    ) {}

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

        /**
         * When the portlet reload (from the browser reload button), the site service emits
         * the switchSite$ because the `currentSite` was undefined and the loads the site, that trigger
         * an unwanted reload.
         *
         * This extra work in the filter is to prevent that extra reload.
         *
         */
        let currentHost = this.dotSiteService.currentSite?.hostname || null;

        this.dotSiteService.switchSite$
            .pipe(
                takeUntil(this.destroy$),
                filter((site: Site) => {
                    if (currentHost === null) {
                        currentHost = site?.hostname;

                        return false;
                    }

                    return true;
                })
            )
            .subscribe(() => {
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
    editTemplate(template: DotTemplate): void {
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
    updateSelectedTemplates(templates: DotTemplate[]): void {
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
    setContextMenu(template: DotTemplate): void {
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
                fieldName: 'friendlyName',
                header: this.dotMessageService.get('templates.fieldName.description')
            },
            {
                fieldName: 'modDate',
                format: 'date',
                header: this.dotMessageService.get('templates.fieldName.lastEdit'),
                sortable: true
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
