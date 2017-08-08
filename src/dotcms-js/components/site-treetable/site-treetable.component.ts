import {Component, Inject, NgModule} from '@angular/core';
import {Message, TreeNode} from 'primeng/components/common/api';
import {Subscription} from 'rxjs';
import {SiteTreetableService} from './site-treetable.service';
import {SiteBrowserState} from '../../core/util/site-browser.state';
import {LoggerService} from '../../core/util/logger.service';
import {NotificationService} from '../../core/util/notification.service';
import {CommonModule} from '@angular/common';
import {TreeTableModule} from 'primeng/components/treetable/treetable';
import {Site} from '../../core/treeable/shared/site.model';

@Component({
    selector: 'site-treetable',
    styles: [require('./../app.css')],
    template: `<div class="ContentSideSections Implementation"
                    (drop)="handleDrop($event, p-column)" (dragover)="handleDragOver($event)">
        <p-treeTable [value]="lazyFiles" [(selection)]="selectedNode" [style]="&#123;'margin-top':'30px'&#125;"
                     (onNodeExpand)="nodeExpand($event)" selectionMode="single">
            <p-column class="browser-dropzone" field="title" header="Name"></p-column>
            <p-column field="modDate" header="Mod Date" [style]="&#123;'width': '100px'&#125;"></p-column>
            <p-column field="type" header="Type" [style]="&#123;'width': '65px'&#125;"></p-column>
        </p-treeTable>
    </div>`
})

@Inject('log')
@Inject('updateService')
export class SiteTreeTableComponent {

    dropzoneStylesVisible = true;
    site: Site;
    msgs: Message[];
    lazyFiles: TreeNode[];
    selectedNode: TreeNode;
    subscription: Subscription;

    constructor(private updateService: SiteBrowserState,
                private log: LoggerService,
                private siteTreetableService: SiteTreetableService,
                private messageService: NotificationService) {

        this.site = updateService.getSelectedSite();
        if (updateService.getURI()) {this.loadFolder(updateService.getURI()); }
        this.subscription = updateService.currentSite
            .subscribe(site => {
                if (site) {this.loadHost(site); }
            });
        this.subscription = updateService.currentURI
            .subscribe(uri => {
                if (uri) {this.loadFolder(uri); }
            });
        setTimeout(() => {
        }, 100);
    }

    /**
     * Deals with the style for the drag and drop of files
     * @param e
     */
    handleDragOver(e: any): void {
        this.dropzoneStylesVisible = true;
    }

    /**
     * Handles uploading files on drag and drop
     * @param e
     */
    handleDrop(e: any): void {
        e.preventDefault();
        let pathToUploadTo: string;
        let files: any[] = e.dataTransfer.files;
        let folderTitle: string = e.path[0].innerText;

        for (let i = 0; i < this.lazyFiles.length; i++) {
            let node: TreeNode = this.lazyFiles[i];
            if (node.data.title === folderTitle && node.data.type === 'folder') {
                pathToUploadTo = node.data.path;
                break;
            }
        }
        this.log.debug('Path 2: ' + pathToUploadTo);
        this.messageService.displayInfoMessage('Path is ' + pathToUploadTo);
        return;
    }

    /**
     * Loads the objects under a host
     * @param siteName
     */
    loadHost(site: Site): void {
        this.site = site;
        this.siteTreetableService.getAssetsUnderSite(site.hostname)
            .subscribe(items => this.lazyFiles = items);
        setTimeout(() => {
        }, 100);
    }

    /**
     * Loades the objects under a folder
     * @param uri
     */
    loadFolder(uri: string): void {
        this.log.debug('loading folder with URI : ' + uri);
        this.siteTreetableService.getAssetsUnderFolder(this.site.hostname, uri)
            .subscribe(items => this.lazyFiles = items);
        this.log.debug('done loading folder with URI : ' + uri);
        setTimeout(() => {
        }, 100);
    }

    /**
     * Handle clicking a node ie.. to show detail on a folder or file
     * @param event
     */
    nodeSelect(event: any): void {
        this.msgs = [];
        this.msgs.push({severity: 'info', summary: 'Node Selected', detail: event.node.data.name});
    }

    /**
     * Unselects the selected node ie... file or folder
     * @param event
     */
    nodeUnselect(event: any): void {
        this.msgs = [];
        this.msgs.push({severity: 'info', summary: 'Node Unselected', detail: event.node.data.name});
    }

    /**
     * Expands a node ie.. a folder
     * @param event
     */
    nodeExpand(event: any): void {
        let pathName: string = (<string> event.node.data.path);
        pathName = pathName.slice(0, pathName.length - 1);
        pathName = pathName.slice(pathName.lastIndexOf('/') + 1, pathName.length);
        this.updateService.changeFolder(pathName);
        this.updateService.changeURI(event.node.data.path);
        if (event.node) {
            this.siteTreetableService.getAssetsUnderFolder(this.site.hostname, event.node.data.path)
                .subscribe(items => this.lazyFiles = items);
        }
        setTimeout(() => {
        }, 100);
    }
}

@NgModule({
    declarations: [SiteTreeTableComponent],
    exports: [SiteTreeTableComponent],
    imports: [CommonModule, TreeTableModule]
})
export class DotcmsSiteTreeTableModule { }
