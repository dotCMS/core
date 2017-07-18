import {MenuItem} from 'primeng/components/common/api';
import {Component, Inject, NgModule} from '@angular/core';
import {Subscription} from 'rxjs';
import EventEmitter = NodeJS.EventEmitter;
import {SiteBrowserState} from '../../core/util/site-browser.state';
import {BreadcrumbModule} from 'primeng/components/breadcrumb/breadcrumb';
import {CommonModule} from '@angular/common';

/**
 * The BreadcrumbComponent provides a PrimeNG Component for providing navigation with dotCMS Components
 * It can respond to the Site or Folder being changed.  In addition the navigation it provides can be clicked on
 * There is no connection to the other components directly.  The interaction is all managed by the [[SiteBrowserState]]
 */
@Component({
    selector: 'breadcrumb',
    styles: [require('./../app.css')],
    template: require('./breadcrumb.html')
})
@Inject('updateService')
export class BreadcrumbComponent {

    private pathItems: MenuItem[];

    constructor(private updateService: SiteBrowserState) {
        this.buildMenuItemsFromURI(this.updateService.getURI());
        updateService.currentSite.subscribe(
            siteName => {
                this.onSiteChange(siteName);
            });
        updateService.currentFolder.subscribe(
            folderName => {
                this.onFolderClick(folderName);
            });
        updateService.currentURI.subscribe(
            uri => {
                this.buildMenuItemsFromURI(uri);
            });
    }

    /**
     * Called when the [[SiteBrowserState]] Site is changed. This is managed via a Subscription
     * @param siteName
     */
    onSiteChange(siteName: string): void {
        this.pathItems = [];
        this.addSiteItem(siteName);
    }

    /**
     * Called when the [[SiteBrowserState]] Folder is changed. This is managed via a Subscription
     * @param folderName
     */
    onFolderClick(folderName: string): void {
        if (!folderName) {
            return;
        }
        let uri: string = this.getCurrentURI() + '/' + folderName;
        this.addFolderItem(folderName);
    }

    private getCurrentURI(): string {
        let uri = '';
        for (let i = 1; i < this.pathItems.length; i++) {
            let pi: MenuItem = this.pathItems[i];
            uri = uri + '/' + pi.label;
        }
        return uri;
    }

    private addSiteItem(siteName: string): void {
        this.pathItems.push({
            command: (event: Event) => {
                this.updateService.changeSite(siteName);
                this.updateService.changeURI(null);
                this.updateService.changeFolder(null);
                setTimeout(() => {
                }, 100);
            }, label: siteName
        });
    }

    private addFolderItem(folderName: string): void {
        let currentURI: string = this.getCurrentURI();
        this.pathItems.push({
            command: (event: Event) => {
                this.updateService.changeURI(currentURI + '/' + folderName);
                setTimeout(() => {
                }, 100);
            }, label: folderName
        });
    }

    private buildMenuItemsFromURI(uri: string): void {
        this.pathItems = [];
        let siteName: string = this.updateService.getSelectedSite();
        if (!siteName) {
            return;
        }
        this.addSiteItem(siteName);
        if (uri) {
            let folders: string[] = uri.split('/');
            for (let i = 0; i < folders.length; i++) {
                this.onFolderClick(folders[i]);
            }
        }
    }
}

@NgModule({
    declarations: [BreadcrumbComponent],
    exports: [BreadcrumbComponent],
    imports: [CommonModule, BreadcrumbModule]
})
export class DotcmsBreadcrumbModule { }
