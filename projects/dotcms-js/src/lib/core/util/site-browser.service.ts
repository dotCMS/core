
import {throwError as observableThrowError, Observable} from 'rxjs';
import {Inject, Injectable, NgModule} from '@angular/core';
import {Response} from '@angular/http';
import {HttpClient} from './http.service';
import {Treeable} from '../treeable/shared/treeable.model';
import {Folder} from '../treeable/shared/folder.model';
import {File} from '../file/file.model';
import {NotificationService} from './notification.service';
import { map, catchError } from 'rxjs/operators';

/**
 * SiteBrowserService will allows operations against the set dotCMS Site/Host for Tree operations. Treeable assets
 * in dotCMS are anything that live on a host/folder.
 */
@Injectable()
@Inject('httpClient')
@Inject('notificationService')
export class SiteBrowserService {

    constructor
    (
        private notificationService: NotificationService,
        private httpClient: HttpClient
    ) {}

    /**
     * Returns the Treeable assets (files, folders) under a host/folder
     * @param siteName dotCMS Site to load assets for
     * @returns Observable<R> Gets the Treeable objects. If a file the Treeable will be file as file extends Treeable
     */
    getTreeableAssetsUnderSite(siteName: String): Observable<Treeable[] | string> {
        return this.httpClient.get('/api/v1/browsertree/sitename/' + siteName + '/uri//')
          .pipe(
            map((res: Response) => this.extractDataFilter(res)),
            catchError(error => this.handleError(error))
          );
    }

    /**
     * Returns the Treeable assets (files, folders) under a host/folder
     * @param siteName dotCMS Site to load assets for
     * @param uri Path to load assets from
     * @returns Observable<R> Gets the Treeable objects. If a file the Treeable will be file as file extends Treeable
     */
    getTreeableAssetsUnderFolder(siteName: String, uri: String): Observable<Treeable[] | string> {
        return this.httpClient.get('/api/v1/browsertree/sitename/' + siteName + '/uri/' + uri)
            .pipe(
              map((res: Response) => this.extractDataFilter(res)),
              catchError(error => this.handleError(error))
            );
    }

    private extractDataFilter(res: Response): Treeable[] {
        const treeables: Treeable[] = [];
        const obj = JSON.parse(res.text());
        const results: any[] = obj.entity.result;
        for (let i = 0; i < results.length; i++) {
            const r: any = results[i];
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

    private handleError(error: any): Observable<string> {
        const errMsg = (error.message) ? error.message :
            error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        if (errMsg) {
            console.error(errMsg);
            this.notificationService.displayErrorMessage('There was an error; please try again : ' + errMsg);
            return observableThrowError(errMsg);
        }
    }
}

@NgModule({
  providers: [SiteBrowserService, HttpClient, NotificationService]
})
export class DotSiteBrowserModule {
}
