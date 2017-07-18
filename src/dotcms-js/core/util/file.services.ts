import {Injectable} from '@angular/core';
import {NotificationService} from './notification.service';
import {LoggerService} from './logger.service';
import {HttpClient} from './http.service';
import {Observable} from 'rxjs';
import {SiteBrowserState} from './site-browser.state';

/**
 * Can be used for CRUD operations on dotCMS FileAssets. Not all CRUD operations are currently implemented
 */
@Injectable()
export class FileService {

    constructor
    (private httpClient: HttpClient,
     private siteBrowserState: SiteBrowserState,
     private loggerService: LoggerService,
     private notificationService: NotificationService) {
    }

    /**
     * Will save an array of local files and upload them to dotCMS. The dotCMS File Object will be built by the upload method
     * which means it will not set a Structure/Content Type and will not set the Identifier if the file already exists
     * in dotCMS.  dotCMS will create a new version if the file already exists on the uploaded path or create a new one.
     * The Structure/Content Type is set accordig to the default type on the uploaded dotCMS Folder
     * @param fileList array of File objects to be POSTED to dotCMS
     */
    saveFiles(fileList: File[]): void {

    }

    /**
     * Will upload a local file to dotCMS
     * @param file File from teh File System to upload
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
            hostFolder: this.siteBrowserState.getSelectedSite() + ':' + path,
            stInode: fileContentTypeID,
            title: file.name,
            type: file.type
        };
        this.httpClient.filePut('/api/content/publish/1', file, data).subscribe();
    }

    /**
     * Will post to dotCMS an array of the FolderNames to create. If the folder already exists nothing will happen.
     * Even if there are failure it will create the Folders it is able to and log any error on folders it could not create
     * @param directories list of local File System directories to create on dotCMS
     */
    // TODO : SHOULD MOVE TO FolderServices
    uploadDirectories(directories: File[]): void {

    }

    private handleError(error: any): Observable<string> {
        let errMsg = (error.message) ? error.message :
            error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        if (errMsg) {
            this.loggerService.error(errMsg);
            this.notificationService.displayErrorMessage('There was an error uploading file; please try again : ' + errMsg);
            return Observable.throw(errMsg);
        }
    }

}
