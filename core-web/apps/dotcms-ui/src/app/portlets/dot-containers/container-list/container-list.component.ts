import { Component, ViewChild } from '@angular/core';
import { DotListingDataTableComponent } from '@components/dot-listing-data-table/dot-listing-data-table.component';
import { DotContainer } from '@models/container/dot-container.model';
import { DotContentState } from '@dotcms/dotcms-models';
import { DotContainerListStore } from '@portlets/dot-containers/container-list/store/dot-container-list.store';

@Component({
    selector: 'dot-container-list',
    templateUrl: './container-list.component.html',
    styleUrls: ['./container-list.component.scss'],
    providers: [DotContainerListStore]
})
export class ContainerListComponent {
    vm$ = this.store.vm$;

    @ViewChild('listing', { static: false })
    listing: DotListingDataTableComponent;

    constructor(private store: DotContainerListStore) {}

    /**
     * get the attributes that define the state of a container.
     * @param {DotContainer} { live, working, deleted, hasLiveVersion}
     * @returns DotContentState
     * @memberof ContainerListComponent
     */
    getContainerState({ live, working, deleted }: DotContainer): DotContentState {
        return { live, working, deleted, hasLiveVersion: live };
    }

    /**
     * Handle filter for hide / show archive containers
     * @param {boolean} checked
     *
     * @memberof ContainerListComponent
     */
    handleArchivedFilter(checked: boolean): void {
        checked
            ? this.listing.paginatorService.setExtraParams('archive', checked)
            : this.listing.paginatorService.deleteExtraParams('archive');
        this.listing.loadFirstPage();
    }

    /**
     * Keep updated the selected containers in the grid
     * @param {DotContainer[]} containers
     *
     * @memberof ContainerListComponent
     */
    updateSelectedContainers(containers: DotContainer[]): void {
        this.store.updateSelectedContainers(containers);
    }

    /**
     * Reset bundle state to null
     *
     * @memberof ContainerListComponent
     */
    resetBundleIdentifier(): void {
        this.store.updateBundleIdentifier(null);
    }

    /**
     * Set the actions of each template based o current state.
     * @param {DotContainer} container
     ** @returns DotActionMenuItem[]
     * @memberof DotContainerListComponent
     */
    setContainerActions(container: DotContainer): DotActionMenuItem[] {
        let options: DotActionMenuItem[];
        if (container.deleted) {
            options = this.setArchiveContainerActions(container);
        } else {
            options = this.setBaseContainerOptions(container);
            options = [...options, ...this.setCopyContainerOptions(container)];

            if (!container.live) {
                options = [
                    ...options,
                    ...this.setLicenseAndRemotePublishContainerOptions(container),
                    ...this.setUnPublishAndArchiveContainerOptions(container)
                ];
            }
        }

        return options;
    }

    private setUnPublishAndArchiveContainerOptions(template: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (template.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Unpublish'),
                    command: () => {
                        this.unPublishContainer([template.identifier]);
                    }
                }
            });
        } else {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Archive'),
                    command: () => {
                        this.archiveContainers([template.identifier]);
                    }
                }
            });
        }

        return options;
    }

    private setLicenseAndRemotePublishContainerOptions(
        container: DotContainer
    ): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (this.hasEnvironments) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Remote-Publish'),
                    command: () => {
                        this.dotPushPublishDialogService.open({
                            assetIdentifier: container.identifier,
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
                        this.addToBundleIdentifier = container.identifier;
                    }
                }
            });
        }

        return options;
    }

    private setCopyContainerOptions(container: DotContainer): DotActionMenuItem[] {
        return !container.locked
            ? [
                  {
                      menuItem: {
                          label: this.dotMessageService.get('Duplicate'),
                          command: () => {
                              //
                          }
                      }
                  }
              ]
            : [];
    }

    private setBaseContainerOptions(container: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];

        if (!container.locked) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('edit'),
                    command: () => {
                        this.editContainer(container);
                    }
                }
            });
        }

        if (!container.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('publish'),
                    command: () => {
                        this.publishContainer(container.identifier);
                    }
                }
            });
        }

        return options;
    }

    private setArchiveContainerActions(container: DotContainer): DotActionMenuItem[] {
        const options: DotActionMenuItem[] = [];
        if (!container.live) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Unarchive'),
                    command: () => {
                        this.unArchiveContainer([container.identifier]);
                    }
                }
            });
        }

        if (!container.locked) {
            options.push({
                menuItem: {
                    label: this.dotMessageService.get('Delete'),
                    command: () => {
                        this.deleteContainer([container.identifier]);
                    }
                }
            });
        }

        return options;
    }

    /**
     * Identify if is a container as File based on the identifier path.
     * @param {DotContainer} {identifier}
     * @returns boolean
     * @memberof DotContainerListComponent
     */
    isContainerAsFile({ identifier }: DotContainer): boolean {
        return identifier.includes('/');
    }

    /**
     * Handle selected container.
     *
     * @param {DotContainer} { container }
     * @memberof DotContainerListComponent
     */
    editContainer(container: DotContainer): void {
        this.isContainerAsFile(container)
            ? this.dotSiteBrowserService.setSelectedFolder(container.identifier).subscribe(() => {
                  this.dotRouterService.goToSiteBrowser();
              })
            : this.dotRouterService.goToEditContainer(container.identifier);
    }

    private deleteContainer(identifiers: string[]): void {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotContainersService
                    .delete(identifiers)
                    .pipe(take(1))
                    .subscribe((response: DotActionBulkResult) => {
                        this.notifyResult(response, 'message.template.full_delete');
                    });
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('Delete-Container'),
            message: this.dotMessageService.get('message.container.confirm.delete.container')
        });
    }

    private publishContainer(identifier: string): void {
        this.dotContainersService
            .publish(identifier)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container_list.published');
            });
    }

    private unPublishContainer(identifiers: string[]): void {
        this.dotContainersService
            .unPublish(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container.unpublished');
            });
    }

    private unArchiveContainer(identifiers: string[]): void {
        this.dotContainersService
            .unArchive(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container.undelete');
            });
    }

    private archiveContainers(identifiers: string[]): void {
        this.dotContainersService
            .archive(identifiers)
            .pipe(take(1))
            .subscribe((response: DotActionBulkResult) => {
                this.notifyResult(response, 'message.container.delete');
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
