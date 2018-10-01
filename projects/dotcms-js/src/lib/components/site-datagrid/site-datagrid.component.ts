import { Component, NgModule, OnInit, ViewChild } from '@angular/core';
import {
    DataGridModule,
    DialogModule,
    FileUpload,
    FileUploadModule,
    PanelModule,
    TreeTable
} from 'primeng/primeng';
import { Site } from '../../core/treeable/shared/site.model';
import { Treeable } from '../../core/treeable/shared/treeable.model';
import { SiteBrowserState } from '../../core/util/site-browser.state';
import { FileService } from '../../core/file/file.services';
import { SiteBrowserService } from '../../core/util/site-browser.service';
import { SettingsStorageService } from '../../core/util/settings-storage.service';
import { FolderService } from '../../core/util/folder.service';
import { FileSearchService } from '../../core/file/file-search.service';
import { Folder } from '../../core/treeable/shared/folder.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { File } from '../../core/file/file.model';

@Component({
    selector: 'site-datagrid',
    styleUrls: ['../app.css'],
    template: `
    <p-dataGrid [value]="treeables" [paginator]="true" [rows]="20" [alwaysShowPaginator]="false">
      <ng-template let-treeable pTemplate="item">
        <div style="padding:3px" class="ui-g-12 ui-md-3">
          <p-panel [style]="{'text-align':'center'}">
            <span [ngSwitch]="treeable.type">
              <span *ngSwitchCase="'folder'" class="">
                <a (click)="selectTreeable(treeable)"><img src="assets/folder.png"></a>
              </span>
              <span *ngSwitchCase="'file_asset'">
                <a (click)="displayDetails(treeable)">
                  <img *ngIf="treeable.isImage() && treeable.extension!='ico'" src="{{dotCMSURL}}/contentAsset/image/{{treeable.inode}}/fileAsset/filter/Thumbnail/thumbnail_w/48/thumbnail_h/48/byInode/true">
                  <span *ngIf="!treeable.isImage() || treeable.extension=='ico'" class="" aria-hidden="true">
                    <img src="assets/file.png">
                  </span>
                </a>
              </span>
            </span>
            <div class="treeable-detail">{{treeable.title}}</div>
          </p-panel>
        </div>
      </ng-template>
    </p-dataGrid>
    <p-dialog header="File Details" [closable]="true" [(visible)]="displayDialog" [responsive]="true" showEffect="fade" [modal]="true" width="1040" (onAfterHide)="onDialogHide()">

      <div class="ui-g" *ngIf="selectedFile">
        <div class="ui-g-12 ui-md-7">
          <img *ngIf="selectedFile.isImage()" src="{{dotCMSURL}}/contentAsset/image/{{selectedFile.inode}}/fileAsset/filter/Thumbnail/thumbnail_w/530/thumbnail_h/400/byInode/true">
          <img *ngIf="!selectedFile.isImage()" src="{{dotCMSURL}}/dA/{{selectedFile.inode}}/225w" onerror="this.style.display='none'">
        </div>
        <div class="ui-g-12 ui-md-5">
          <div class="ui-g-3"><label class="treeable-detail-label">Inode</label></div>
          <div class="ui-g-9"><input class="treeable-detail-input" id="inode" type="text" pInputText [disabled]="true" [(ngModel)]="selectedFile.inode" /></div>
          <div class="ui-g-3"><label class="treeable-detail-label">Identifier</label></div>
          <div class="ui-g-9"><input class="treeable-detail-input" id="identifier" type="text" pInputText [disabled]="true" [(ngModel)]="selectedFile.identifier" /></div>
          <div class="ui-g-3"><label class="treeable-detail-label">Filename</label></div>
          <div class="ui-g-9"><input class="treeable-detail-input" id="fileName" type="text" pInputText [disabled]="true" [(ngModel)]="selectedFile.fileName" /></div>
          <div class="ui-g-3"><label class="treeable-detail-label">Title</label></div>
          <div class="ui-g-9"><input class="treeable-detail-input" id="title" type="text" pInputText [disabled]="true" [(ngModel)]="selectedFile.title" /></div>
          <div class="ui-g-3"><label class="treeable-detail-label">Edititor</label></div>
          <div class="ui-g-9"><input class="treeable-detail-input" id="modUserName" type="text" pInputText [disabled]="true" [(ngModel)]="selectedFile.modUserName" /></div>
          <div class="ui-g-3"><label class="treeable-detail-label">Path</label></div>
          <div class="ui-g-9"><input class="treeable-detail-input" id="path" type="text" pInputText [disabled]="true" [(ngModel)]="selectedFile.path" /></div>
          <div class="ui-g-3"><label class="treeable-detail-label">Mine Type</label></div>
          <div class="ui-g-9"><input class="treeable-detail-input" id="mimeType" type="text" pInputText [disabled]="true" [(ngModel)]="selectedFile.mimeType" /></div>
        </div>
      </div>
    </p-dialog>`
})
export class SiteDatagridComponent implements OnInit {
    @ViewChild('fileUploadWidget')
    fieldUpload: FileUpload;
    dotCMSURL = '';
    site: Site;
    treeables: Treeable[];
    displayDialog: boolean;
    uploadDialog: boolean;
    selectedFile: File;
    uploadedFiles: any[] = [];
    searchQuery: string;

    constructor(
        private updateService: SiteBrowserState,
        private fileService: FileService,
        private siteBrowserService: SiteBrowserService,
        settingsStorageService: SettingsStorageService,
        private folderService: FolderService,
        private fileSearchService: FileSearchService
    ) {
        if (settingsStorageService.getSettings()) {
            this.dotCMSURL = settingsStorageService.getSettings().site;
        }
        this.site = updateService.getSelectedSite();
        if (updateService.getURI()) {
            this.loadFolder(updateService.getURI());
        }
        updateService.currentSite.subscribe((site) => {
            if (site) {
                this.loadSite(site);
            }
        });
        fileSearchService.searchQuery.subscribe((searchQuery) => {
            if (searchQuery && searchQuery.trim() !== '') {
                updateService.changeURI(null);
                this.loadSearchResults(searchQuery);
            } else {
                if (
                    updateService.getSelectedFolder() != null &&
                    updateService.getSelectedFolder().trim() !== ''
                ) {
                    if (updateService.getURI()) {
                        this.loadFolder(updateService.getURI());
                    }
                } else {
                    if (updateService.getSelectedSite()) {
                        this.loadSite(updateService.getSelectedSite());
                    }
                }
            }
        });
        updateService.currentURI.subscribe((uri) => {
            if (uri) {
                this.loadFolder(uri);
            }
        });
        setTimeout(() => {}, 100);
    }

    ngOnInit() {}

    loadSearchResults(searchQuery: string) {
        if (searchQuery !== null && searchQuery.trim().length > 0) {
            searchQuery.trim();
            searchQuery = searchQuery + '*';
            searchQuery = searchQuery.split(' ').join('* ');
            searchQuery =
                '+baseType:4%20+(' +
                (this.site && this.site.identifier ? 'conhost:' + this.site.identifier : '') +
                '%20conhost:SYSTEM_HOST)%20+deleted:false%20+working:true%20+_all:(' +
                searchQuery +
                ')/orderby/modDate%20desc';
            this.fileSearchService
                .search(searchQuery)
                .subscribe((treeables: Treeable[]) => (this.treeables = treeables));
            setTimeout(() => {}, 100);
        }
    }

    loadFolder(uri: string): void {
        this.siteBrowserService
            .getTreeableAssetsUnderFolder(this.site.hostname, uri)
            .subscribe((treeables: Treeable[]) => (this.treeables = treeables));
        setTimeout(() => {}, 100);
    }

    loadSite(site: Site): void {
        this.site = site;
        this.siteBrowserService
            .getTreeableAssetsUnderSite(site.hostname)
            .subscribe((treeables: Treeable[]) => (this.treeables = treeables));
        setTimeout(() => {}, 100);
    }

    selectTreeable(treeable: Folder): void {
        this.updateService.changeURI(treeable.path);
    }

    displayDetails(file: File): void {
        this.displayDialog = true;
        this.selectedFile = file;
    }

    displayUpload(_file: File): void {
        this.uploadDialog = true;
    }

    onDialogHide() {
        this.selectedFile = null;
    }

    onUploadHide() {}

    addFileToUpload(e: any) {
        for (const file of e.files) {
            this.uploadedFiles.push(file);
        }
    }

    clearUploads(_e: any) {
        this.uploadedFiles = [];
    }

    onUpload(e: any) {
        const uri: String = this.updateService.getURI();
        this.folderService
            .loadFolderByURI(this.site.hostname, uri)
            .subscribe((folder: Folder) => this.uploadIntoFolder(folder, e.files));
        setTimeout(() => {}, 100);
        return;
    }

    private uploadIntoFolder(folder: Folder, files: any[]) {
        const fileContentTypeID: string = folder.defaultFileType;
        for (let i = 0; i < files.length; i++) {
            const file: any = files[i];
            this.fileService.uploadFile(file, folder.path, fileContentTypeID);
        }

        this.uploadedFiles = [];
        this.fieldUpload.clear();
        this.uploadDialog = false;
        // Needs to be updated so the file service returns errors and messages and then load on sucussfull upload
        setTimeout(() => {
            this.loadFolder(this.updateService.getURI());
        }, 2000);
        setTimeout(() => {
            this.loadFolder(this.updateService.getURI());
        }, 2000);
        setTimeout(() => {
            this.loadFolder(this.updateService.getURI());
        }, 2000);
    }
}

@NgModule({
    declarations: [SiteDatagridComponent],
    exports: [SiteDatagridComponent],
    imports: [
        DialogModule,
        FileUploadModule,
        DataGridModule,
        CommonModule,
        PanelModule,
        FormsModule
    ],
    providers: []
})
export class DotSiteDatagridModule {}
