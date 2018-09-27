import {HttpClient} from './http.service';
import {NotificationService} from './notification.service';
import {Response} from '@angular/http';
import {Inject, Injectable, NgModule} from '@angular/core';
import {Folder} from '../treeable/shared/folder.model';
import {Observable} from 'rxjs/Observable';
import {ErrorObservable} from 'rxjs/observable/ErrorObservable';
import { map, catchError } from 'rxjs/operators';

/**
 * Service allows opertions against dotCMS Folder Endpoints and Operations
 */
@Injectable()
@Inject('httpClient')
@Inject('notificationService')
export class FolderService {
    constructor
    (
        private httpClient: HttpClient,
        private notificationService: NotificationService
    ) {}

    /**
     * Load a folder from the remote dotCMS server based on site and path
     * @param {String} siteName The site name for the uri/folder
     * @param {String} uri The folder path
     */
    loadFolderByURI(siteName: String, uri: String): Observable<Folder> {
        return this.httpClient.get('/api/v1/folder/sitename/' + siteName + '/uri/' + uri)
        .pipe(
          map((res: Response) => this.extractDataFilter(res)),
          // catchError(error => this.handleError(error))
        );
    }

    private extractDataFilter(res: Response): Folder {
        let folder: Folder;
        const obj = JSON.parse(res.text());
        const result: Folder = Object.assign(new Folder(), obj.entity);
        return result;
    }

    private handleError(error: any): ErrorObservable<string> {
        const errMsg = (error.message) ? error.message :
            error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        if (errMsg) {
            console.log(errMsg);
            this.notificationService.displayErrorMessage('There was an error; please try again : ' + errMsg);
            return Observable.throw(errMsg);
        }
    }
}

@NgModule({
  providers: [FolderService, HttpClient, NotificationService]
})
export class DotFolderModule { }
