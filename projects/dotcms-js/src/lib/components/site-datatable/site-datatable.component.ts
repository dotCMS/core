import { Component, NgModule } from '@angular/core';
import { Treeable } from '../../core/treeable/shared/treeable.model';
import { SiteBrowserState } from '../../core/util/site-browser.state';
import { SiteBrowserService } from '../../core/util/site-browser.service';
import { SettingsStorageService } from '../../core/util/settings-storage.service';
import { NotificationService } from '../../core/util/notification.service';
import { Folder } from '../../core/treeable/shared/folder.model';
import { CommonModule } from '@angular/common';
import { DataTableModule } from 'primeng/components/datatable/datatable';
import { FileService } from '../../core/file/file.services';
import { Site } from '../../core/treeable/shared/site.model';

/**
 * The SiteDataTableComponent is a PrimeNG Component which provides a DataTable to display dotCMS Host/Folder Navigation
 * The Component is Supscribed to the [[SiteBrowserState]] to monitor when Folders and Site changes occur
 * In additiont to this it will respond to dragging Files and Folders from the local filesystem and upload them to dotCMS
 * The SiteDataTableComponent is able to display [[Treeable]] assets
 */
@Component({
    selector: 'site-datatable',
    styleUrls: ['../app.css'],
    template: `<div (drop)="handleDrop($event)" (dragover)="handleDragOver($event)">
        <p-dataTable [value]="treeables" selectionMode="single"
                     (onRowDblclick)="doubleClick($event)" (onRowSelect)="selectTreeable($event)">
            <p-column class="browser-dropzone" field="title" header="Name" [sortable]="true">
                <ng-template let-col let-treeable="rowData" pTemplate type="body">
                <span [ngSwitch]="treeable.type">
                    <span *ngSwitchCase="'folder'" class="{{treeable.type=='folder' ? 'fa fa-folder aria-hidden=true':''}}"></span>
                    <span *ngSwitchCase="'file_asset'">
                        <img *ngIf="treeable.isImage() && treeable.extension!='ico'" src="{{dotCMSURL}}/contentAsset/image/{{treeable.identifier}}/fileAsset/filter/Thumbnail/thumbnail_w/25/thumbnail_h/25">
                        <span *ngIf="!treeable.isImage() || treeable.extension=='ico'" class="fa fa-file" aria-hidden="true"></span>
                    </span>
                </span>
                    <span>{{treeable.title}}</span>
                </ng-template>
            </p-column>
            <p-column field="type" header="Type" [sortable]="true" [style]="{'width':'100px'}">
                <ng-template let-col let-treeable="rowData" pTemplate type="body">
                    <span>{{treeable.displayType}}</span>
                </ng-template>
            </p-column>
            <p-column field="live" header="Status" [style]="{'width':'70px'}">
                <ng-template let-col let-treeable="rowData" pTemplate type="body">
                    <div *ngIf="treeable.type=='file_asset'">
                        <span *ngIf="treeable.live" class="liveIcon"></span>
                        <span *ngIf="!treeable.live" class="workingIcon"></span>
                    </div>
                </ng-template>
            </p-column>
            <p-column field="modUserName" header="Editor" [sortable]="true" [style]="{'width':'150px'}"></p-column>
            <p-column field="modDate" header="Modified" [sortable]="true" [style]="{'width':'200px'}">
                <ng-template let-col let-treeable="rowData" pTemplate type="body">
                    <span>{{treeable.modDate | date: 'MM/dd/yy hh:mm:ss a'}}</span>
                </ng-template>
            </p-column>
        </p-dataTable>
    </div>`
})
export class SiteDatatableComponent {
    dotCMSURL = '';
    site: Site;
    treeables: Treeable[];

    constructor(
        private updateService: SiteBrowserState,
        private fileService: FileService,
        private siteBrowserService: SiteBrowserService,
        settingsStorageService: SettingsStorageService,
        private messageService: NotificationService
    ) {
        if (settingsStorageService.getSettings()) {
            this.dotCMSURL = settingsStorageService.getSettings().site;
        }
        this.site = updateService.getSelectedSite();
        if (updateService.getURI()) {
            this.loadFolder(updateService.getURI());
        }
        updateService.currentSite.subscribe((site) => {
            if (site && site.hostname) {
                this.loadSite(site);
            }
        });
        updateService.currentURI.subscribe((uri) => {
            if (uri) {
                this.loadFolder(uri);
            }
        });
        setTimeout(() => {}, 100);
    }

    /**
     * Listens to the double click event.  If the row clicked is a folder the [[SiteBrowserState]] will be called to
     * change the folder. This will cause this component to load the new folder and of course anyone else
     * subscribed to the [[SiteBrowserState]] folder state
     * @param event
     */
    doubleClick(event: any): void {
        if (event.data.type !== 'folder') {
            return;
        }
        let pathName: string = <string>event.data.path;
        pathName = pathName.slice(0, pathName.length - 1);
        pathName = pathName.slice(pathName.lastIndexOf('/') + 1, pathName.length);
        this.updateService.changeFolder(pathName);
        this.updateService.changeURI(event.data.path);
        const folder: Folder = event.data;
        let uri = this.updateService.getURI();
        if (!uri) {
            uri = '';
        }
        this.loadFolder(folder.path);
        setTimeout(() => {}, 100);
    }

    /**
     * On single click of a row this function will update the [[SiteBrowserState]] selected [[Treeable]]
     * @param event
     */
    selectTreeable(event: any): void {
        this.updateService.changeTreeable(event.data);
    }

    /**
     * This function is called when the [[SiteBrowserState]] uri is changed
     * @param uri
     */
    loadFolder(uri: string): void {
        this.siteBrowserService
            .getTreeableAssetsUnderFolder(this.site.hostname, uri)
            .subscribe((treeables: Treeable[]) => (this.treeables = treeables));
        setTimeout(() => {}, 100);
    }

    /**
     * This function is called when the [[SiteBrowserState]] Site is changed
     * @param siteName
     */
    loadSite(site: Site): void {
        this.site = site;
        this.siteBrowserService
            .getTreeableAssetsUnderSite(site.hostname)
            .subscribe((treeables: Treeable[]) => (this.treeables = treeables));
        setTimeout(() => {}, 100);
    }

    handleDragOver(_e: any): void {}

    /**
     * Deals with the drop of 1 to many files and folders from the filesystem
     * @param e
     * @returns boolean
     */
    handleDrop(e: any): void {
        e.preventDefault();
        let pathToUploadTo: string;
        let fileContentTypeID: string;
        const files: File[] = e.dataTransfer.files;
        const folderTitle: string = e.path[0].innerText.trim();
        for (let i = 0; i < this.treeables.length; i++) {
            const node: Treeable = this.treeables[i];
            if (node.title === folderTitle && node.type === 'folder') {
                pathToUploadTo = (<Folder>node).path;
                fileContentTypeID = (<Folder>node).defaultFileType;
                break;
            }
        }
        for (let i = 0; i < files.length; i++) {
            const file: File = files[i];
            if (fileContentTypeID == null || fileContentTypeID.trim() === '') {
                console.error('st inode is empty');
            }
            this.fileService.uploadFile(file, pathToUploadTo, fileContentTypeID);
        }
        this.messageService.displayInfoMessage('Path is ' + pathToUploadTo);
        return;
    }
}

@NgModule({
    declarations: [SiteDatatableComponent],
    exports: [SiteDatatableComponent],
    imports: [CommonModule, DataTableModule],
    providers: [FileService, SiteBrowserService, SettingsStorageService, NotificationService]
})
export class DotSiteDatatableModule {}
