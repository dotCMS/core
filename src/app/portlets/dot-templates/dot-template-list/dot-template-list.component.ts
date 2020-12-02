import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { pluck, take } from 'rxjs/operators';
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
import { DotPushPublishDialogService } from 'dotcms-js';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DialogService } from 'primeng/dynamicdialog';
import { DotBulkInformationComponent } from '@components/_common/dot-bulk-information/dot-bulk-information.component';
import {
    DotActionBulkResult,
    DotBulkFailItem
} from '@models/dot-action-bulk-result/dot-action-bulk-result.model';

@Component({
    selector: 'dot-template-list',
    templateUrl: './dot-template-list.component.html',
    styleUrls: ['./dot-template-list.component.scss']
})
export class DotTemplateListComponent implements OnInit, OnDestroy {
    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;
    tableColumns: DataTableColumn[];
    firstPage: DotTemplate[];
    templateBulkActions: MenuItem[];
    actionHeaderOptions: ActionHeaderOptions;
    addToBundleIdentifier: string;
    selectedTemplates: DotTemplate[] = [];

    private isEnterPrise: boolean;
    private hasEnvironments: boolean;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private route: ActivatedRoute,
        private dotMessageService: DotMessageService,
        private dotTemplatesService: DotTemplatesService,
        private dotMessageDisplayService: DotMessageDisplayService,
        private dotPushPublishDialogService: DotPushPublishDialogService,
        private dotRouterService: DotRouterService,
        public dialogService: DialogService
    ) {}

    ngOnInit(): void {
        this.route.data
            .pipe(pluck('dotTemplateListResolverData'), take(1))
            .subscribe(
                ([templates, isEnterPrise, hasEnvironments]: [DotTemplate[], boolean, boolean]) => {
                    this.firstPage = templates;
                    this.isEnterPrise = isEnterPrise;
                    this.hasEnvironments = hasEnvironments;
                    this.tableColumns = this.setTemplateColumns();
                    this.templateBulkActions = this.setTemplateBulkActions();
                }
            );
        this.setAddOptions();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle selected template.
     *
     * @param {DotTemplate} { identifier }
     * @memberof DotTemplateListComponent
     */
    editTemplate({ identifier }: DotTemplate): void {
        this.dotRouterService.goToEditTemplate(identifier);
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
            options = [
                ...options,
                ...this.setUnlockTemplateOptions(template),
                ...this.setCopyTemplateOptions(template)
            ];
        }

        return options;
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
                header: this.dotMessageService.get('templates.fieldName.status')
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
                model: [
                    {
                        command: () => {
                            this.dotRouterService.gotoPortlet('/templates/new/designer');
                        },
                        label: this.dotMessageService.get('design-template')
                    },
                    {
                        command: () => {
                            this.dotRouterService.gotoPortlet('/templates/new/advanced');
                        },
                        label: this.dotMessageService.get('code-template')
                    }
                ]
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

    private setUnlockTemplateOptions(template: DotTemplate): DotActionMenuItem[] {
        return template.locked && template.canWrite
            ? [
                  {
                      menuItem: {
                          label: this.dotMessageService.get('unlock'),
                          command: () => {
                              this.dotTemplatesService
                                  .unlock(template.identifier)
                                  .pipe(take(1))
                                  .subscribe(() => {
                                      this.showToastNotification(
                                          this.dotMessageService.get('message.template.unlocked')
                                      );
                                      this.listing.loadCurrentPage();
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
        if (confirm(this.dotMessageService.get('message.template.confirm.delete.template'))) {
            this.dotTemplatesService
                .delete(identifiers)
                .pipe(take(1))
                .subscribe((response: DotActionBulkResult) => {
                    this.notifyResult(response, 'message.template.full_delete');
                });
        }
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
                fails: this.getFailsInfo(response.fails)
            });
        } else {
            this.showToastNotification(this.dotMessageService.get(messageKey));
        }
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
        return this.selectedTemplates.find((template: DotTemplate) => {
            return template.identifier === identifier;
        }).name;
    }
}
