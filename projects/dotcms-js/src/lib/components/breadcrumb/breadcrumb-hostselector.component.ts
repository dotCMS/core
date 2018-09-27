import {MenuItem} from 'primeng/components/common/api';
import {Component, Inject, NgModule} from '@angular/core';
import {SiteBrowserState} from '../../core/util/site-browser.state';
import {FileSearchService} from '../../core/file/file-search.service';
import {Site} from '../../core/treeable/shared/site.model';
import {CommonModule} from '@angular/common';
import {BreadcrumbModule, DialogModule} from 'primeng/primeng';
import {DotSiteSelectorModule} from '../site-selector/site-selector.component';

/**
 * The BreadcrumbComponent provides a PrimeNG Component for providing navigation with dotCMS Components
 * It can respond to the Site or Folder being changed.  In addition the navigation it provides can be clicked on
 * There is no connection to the other components directly.  The interaction is all managed by the [[SiteBrowserState]]
 * Unlike the breadcrumb component this component has a built in site selector
 */
@Component({
  selector: 'app-breadcrumb-hostselector',
  template: `<p-breadcrumb [home]="homeItem" [model]="pathItems"></p-breadcrumb>
    <p-dialog header="Select Host" [contentStyle]="{'overflow':'visible'}" [closable]="true" [(visible)]="hostDialog" [responsive]="true" showEffect="fade" [modal]="true" (onAfterHide)="closeHostSelect()" width="500">
      <site-selector></site-selector>
    </p-dialog>`
})
@Inject('updateService')
export class BreadcrumbHostselectorComponent {

  homeItem: MenuItem = {label: 'Host Select', command: (event) => {this.openHostSelectDialog(); }};
  pathItems: MenuItem[];
  hostDialog: boolean;

  constructor(
    private updateService: SiteBrowserState,
    private fileSearchService: FileSearchService
  ) {
    this.buildMenuItemsFromURI(this.updateService.getURI());
    updateService.currentSite.subscribe(
      site => {
        this.onSiteChange(site);
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

  openHostSelectDialog(): void {
    this.hostDialog = true;
  }

  closeHostSelect(): void {
    this.hostDialog = false;
  }

  /**
   * Called when the [[SiteBrowserState]] Site is changed. This is managed via a Subscription
   * @param siteName
   */
  onSiteChange(site: Site): void {
    this.pathItems = [];
    this.addSiteItem(site);
    this.closeHostSelect();
  }

  /**
   * Called when the [[SiteBrowserState]] Folder is changed. This is managed via a Subscription
   * @param folderName
   */
  onFolderClick(folderName: string): void {
    if (!folderName) {
      return;
    }
    const uri: string = this.getCurrentURI() + '/' + folderName;
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
      command: (event: Event) => {
        this.updateService.changeSite(site);
        this.updateService.changeURI(null);
        this.updateService.changeFolder(null);
        this.fileSearchService.changeSearchQuery(null);
        setTimeout(() => {
        }, 100);
      }, label: site ? site.hostname : ''
    });
  }

  private addFolderItem(folderName: string): void {
    const currentURI: string = this.getCurrentURI();
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
  declarations: [BreadcrumbHostselectorComponent],
  exports: [BreadcrumbHostselectorComponent],
  imports: [CommonModule, BreadcrumbModule, DialogModule, DotSiteSelectorModule],
  providers: []
})
export class DotBreadcrumbHostselectorModule { }
