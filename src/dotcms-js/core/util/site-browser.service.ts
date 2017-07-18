import {Injectable} from '@angular/core';
import {Response} from '@angular/http';
import {HttpClient} from './http.service';
import {Observable} from 'rxjs';
import {Treeable} from '../treeable/shared/treeable.model';
import {Folder} from '../treeable/shared/folder.model';
import {File} from '../treeable/shared/file.model';
import {NotificationService} from './notification.service';
import {ErrorObservable} from 'rxjs/observable/ErrorObservable';
import {LoggerService} from '../../core/util/logger.service';

/**
 * SiteBrowserService will allows operations against the set dotCMS Site/Host for Tree operations. Treeable assets
 * in dotCMS are anything that live on a host/folder.
 */
@Injectable()
export class SiteBrowserService {

    constructor
    (
        private httpClient: HttpClient,
        private notificationService: NotificationService,
        private log: LoggerService
    ) {}

    /**
     * Returns the Treeable assets (files, folders) under a host/folder
     * @param siteName dotCMS Site to load assets for
     * @returns {Observable<R>} Gets the Treeable objects. If a file the Treeable will be File as File extends Treeable
     */
    getTreeableAssetsUnderSite(siteName: String): Observable < Treeable[] > {
        return this.httpClient.get('/api/v1/browsertree/sitename/' + siteName + '/uri//')
            .map((res: Response) => this.extractDataFilter(res))
            .catch(error => this.handleError(error));
    }

    /**
     * Returns the Treeable assets (files, folders) under a host/folder
     * @param siteName dotCMS Site to load assets for
     * @param uri Path to load assets from
     * @returns {Observable<R>} Gets the Treeable objects. If a file the Treeable will be File as File extends Treeable
     */
    getTreeableAssetsUnderFolder(siteName: String, uri: String): Observable <Treeable[]> {
        return this.httpClient.get('/api/v1/browsertree/sitename/' + siteName + '/uri/' + uri)
            .map((res: Response) => this.extractDataFilter(res))
            .catch(error => this.handleError(error));
    }

    private extractDataFilter(res: Response): Treeable[] {
        let treeables: Treeable[] = [];
        let obj = JSON.parse(res.text());
        let results: any[] = obj.entity.result;
        for (let i = 0; i < results.length; i++) {
            let r: any = results[i];
            let t: Treeable;
            if (r.type === 'folder') {
                t = Object.assign(new Folder(), r);
            } else if (r.type === 'file_asset') {
                t = Object.assign(new File(), r);
            }
            treeables[i] = t;
        }
        return treeables;
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
