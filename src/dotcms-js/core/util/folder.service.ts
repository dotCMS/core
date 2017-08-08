import {HttpClient} from './http.service';
import {NotificationService} from './notification.service';
import {LoggerService} from '../../core/util/logger.service';
import {Response} from '@angular/http';
import {Inject, Injectable} from '@angular/core';
import {Folder} from '../treeable/shared/folder.model';
import {Observable} from 'rxjs/Observable';
import {ErrorObservable} from 'rxjs/observable/ErrorObservable';

/**
 * Service allows opertions against dotCMS Folder Endpoints and Operations
 */
@Injectable()
@Inject('httpClient')
@Inject('notificationService')
@Inject('log')
export class FolderService {
    constructor
    (
        private httpClient: HttpClient,
        private notificationService: NotificationService,
        private log: LoggerService
    ) {}

    /**
     * Load a folder from the remote dotCMS server based on site and path
     * @param {String} siteName The site name for the uri/folder
     * @param {String} uri The folder path
     */
    loadFolderByURI(siteName: String, uri: String): Observable <Folder> {
        return this.httpClient.get('/api/v1/folder/sitename/' + siteName + '/uri/' + uri)
            .map((res: Response) => this.extractDataFilter(res))
            .catch(error => this.handleError(error));
    }

    private extractDataFilter(res: Response): Folder {
        let folder: Folder;
        let obj = JSON.parse(res.text());
        let result: Folder = Object.assign(new Folder(), obj.entity);
        return result;
    }

    private handleError(error: any): ErrorObservable {
        let errMsg = (error.message) ? error.message :
            error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        if (errMsg) {
            this.log.error(errMsg);
            this.notificationService.displayErrorMessage('There was an error; please try again : ' + errMsg);
            return Observable.throw(errMsg);
        }
    }
}
