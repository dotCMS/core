import { Injectable, NgModule } from '@angular/core';
import { NotificationService } from '../util/notification.service';
import { HttpClient } from '../util/http.service';
import { SiteBrowserState } from '../util/site-browser.state';
import { FileSearchService } from './file-search.service';

/**
 * Can be used for CRUD operations on dotCMS FileAssets. Not all CRUD operations are currently implemented
 */
@Injectable()
export class FileService {
    constructor(private httpClient: HttpClient, private siteBrowserState: SiteBrowserState) {}

    /**
     * ******  NOT IMPLEMENTED YET *******
     * Will save an array of local files and upload them to dotCMS. The dotCMS file Object will be built by the upload method
     * which means it will not set a Structure/Content Type and will not set the Identifier if the file already exists
     * in dotCMS.  dotCMS will create a new version if the file already exists on the uploaded path or create a new one.
     * The Structure/Content Type is set accordig to the default type on the uploaded dotCMS Folder
     * @param fileList array of file objects to be POSTED to dotCMS
     */
    // TODO : NOT IMPLEMENTED YET
    saveFiles(_fileList: File[]): void {}

    /**
     * Will upload a local file to dotCMS
     * @param file file from teh file System to upload
     * @param path dotCMS Path to upload file to
     */
    uploadFile(file: File, path: string, fileContentTypeID: string): void {
        let data: {
            stInode: string;
            hostFolder: string;
            title: string;
            fileName: string;
            type: string;
        } = {
            fileName: file.name,
            hostFolder: this.siteBrowserState.getSelectedSite().hostname + ':' + path,
            stInode: fileContentTypeID,
            title: file.name,
            type: file.type
        };
        this.httpClient.filePut('/api/content/save/1', file, data).subscribe();
    }

    /**
     * Will post to dotCMS an array of the FolderNames to create. If the folder already exists nothing will happen.
     * Even if there are failure it will create the Folders it is able to and log any error on folders it could not create
     * @param directories list of local file System directories to create on dotCMS
     */
    // TODO : SHOULD MOVE TO FolderServices
    uploadDirectories(_directories: File[]): void {}

    // private handleError(error: any): Observable<string> {
    //     let errMsg = (error.message) ? error.message :
    //         error.status ? `${error.status} - ${error.statusText}` : 'Server error';
    //     if (errMsg) {
    //         console.log(errMsg);
    //         this.notificationService.displayErrorMessage('There was an error uploading file; please try again : ' + errMsg);
    //         return Observable.throw(errMsg);
    //     }
    // }
}

@NgModule({
    providers: [HttpClient, NotificationService, FileService, FileSearchService]
})
export class DotFileModule {}
