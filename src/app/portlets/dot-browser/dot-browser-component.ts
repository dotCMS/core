import {Component, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {Folder} from 'dotcms-js/core/treeable/shared/folder.model';
import {FileService, FolderService, SiteBrowserState, SiteDatagridComponent} from 'dotcms-js/dotcms-js';
import {FileUpload} from 'primeng/primeng';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-browser',
    styleUrls: ['./dot-browser.scss'],
    templateUrl: 'dot-browser.html'
})

export class DotBrowserComponent implements OnInit {

    @ViewChild('siteDatagridWidget') siteDatagrid: SiteDatagridComponent;
    @ViewChild('fileUploadWidget') fieldUpload: FileUpload;
    uploadedFiles: any[] = [];
    uploadDialog: boolean;
    siteView = 'tree';

    constructor(
        private fileService: FileService,
        private folderService: FolderService,
        private updateService: SiteBrowserState
    ) {}
    ngOnInit(): void {}
    changeViewToTree(): void {
        this.siteView = 'tree';
    }
    changeViewToIcon(): void {
        this.siteView = 'icon';
    }

    onUpload(e: any) {
        const uri: String = this.updateService.getURI();
        this.folderService.loadFolderByURI(this.updateService.getSelectedSite().hostname, uri !== null ? uri : '/')
            .subscribe((folder: Folder) => this.uploadIntoFolder(folder, e.files));
        setTimeout(() => {}, 100);
        return;
    }

    onUploadHide() {}

    addFileToUpload(e: any) {
        for (const file of e.files) {
            this.uploadedFiles.push(file);
        }
    }

    clearUploads(e: any) {
        this.uploadedFiles = [];
    }

    displayUpload(file: File): void {
        this.uploadDialog = true;
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
        const uri = this.updateService.getURI();
        // Needs to be updated so the file service returns errors and messages and then load on successful upload
        setTimeout(() => {
            this.updateService.changeURI(uri);
        }, 2000);
        setTimeout(() => {
            this.updateService.changeURI(uri);
        }, 2000);
        setTimeout(() => {
            this.updateService.changeURI(uri);
        }, 2000);
    }
}
