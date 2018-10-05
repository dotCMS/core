import { MenuItem } from 'primeng/components/common/api';
import { Component, Inject, NgModule } from '@angular/core';
import { SiteBrowserState } from '../../core/util/site-browser.state';
import { BreadcrumbModule } from 'primeng/components/breadcrumb/breadcrumb';
import { CommonModule } from '@angular/common';
import { Site } from '../../core/treeable/shared/site.model';

/**
 * The BreadcrumbComponent provides a PrimeNG Component for providing navigation with dotCMS Components
 * It can respond to the Site or Folder being changed.  In addition the navigation it provides can be clicked on
 * There is no connection to the other components directly.  The interaction is all managed by the [[SiteBrowserState]]
 */
@Component({
    selector: 'breadcrumb',
    styleUrls: ['../app.css'],
    template: `<p-breadcrumb [model]="pathItems"></p-breadcrumb>`
})
@Inject('updateService')
export class BreadcrumbComponent {
    pathItems: MenuItem[];

    constructor(private updateService: SiteBrowserState) {
        this.buildMenuItemsFromURI(this.updateService.getURI());
        updateService.currentSite.subscribe((site) => {
            this.onSiteChange(site);
        });
        updateService.currentFolder.subscribe((folderName) => {
            this.onFolderClick(folderName);
        });
        updateService.currentURI.subscribe((uri) => {
            this.buildMenuItemsFromURI(uri);
        });
    }

    /**
     * Called when the [[SiteBrowserState]] Site is changed. This is managed via a Subscription
     * @param siteName
     */
    onSiteChange(site: Site): void {
        this.pathItems = [];
        this.addSiteItem(site);
    }

    /**
     * Called when the [[SiteBrowserState]] Folder is changed. This is managed via a Subscription
     * @param folderName
     */
    onFolderClick(folderName: string): void {
        if (!folderName) {
            return;
        }
        this.addFolderItem(folderName);
    }

    private getCurrentURI(): string {
        let uri = '';
        for (let i = 1; i < this.pathItems.length; i++) {
            const pi: MenuItem = this.pathItems[i];
            uri = uri + '/' + pi.label;
        }
        return uri;
    }

    private addSiteItem(site: Site): void {
        this.pathItems.push({
            command: (_event: Event) => {
                this.updateService.changeSite(site);
                this.updateService.changeURI(null);
                this.updateService.changeFolder(null);
                setTimeout(() => {}, 100);
            },
            label: site ? site.hostname : ''
        });
    }

    private addFolderItem(folderName: string): void {
        const currentURI: string = this.getCurrentURI();
        this.pathItems.push({
            command: (_event: Event) => {
                this.updateService.changeURI(currentURI + '/' + folderName);
                setTimeout(() => {}, 100);
            },
            label: folderName
        });
    }

    private buildMenuItemsFromURI(uri: string): void {
        this.pathItems = [];
        const site: Site = this.updateService.getSelectedSite();
        if (!site || !site.hostname) {
            return;
        }
        this.addSiteItem(site);
        if (uri) {
            const folders: string[] = uri.split('/');
            for (let i = 0; i < folders.length; i++) {
                this.onFolderClick(folders[i]);
            }
        }
    }
}

@NgModule({
    declarations: [BreadcrumbComponent],
    exports: [BreadcrumbComponent],
    imports: [CommonModule, BreadcrumbModule],
    providers: []
})
export class DotBreadcrumbModule {}
