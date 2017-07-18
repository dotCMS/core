import {Response} from '@angular/http';
import {Observable} from 'rxjs';
import {Injectable} from '@angular/core';
import {HttpClient} from '../../core/util/http.service';
import {NotificationService} from '../../core/util/notification.service';
import {Site} from '../../core/treeable/shared/site.model';
import {ErrorObservable} from 'rxjs/observable/ErrorObservable';
import {LoggerService} from '../../core/util/logger.service';

@Injectable()
export class SiteSelectorService {

    constructor
    (
        private httpClient: HttpClient,
        private log: LoggerService,
        private notificationService: NotificationService
    ) {}

    /**
     * Returns a list of sites searcing the hostname
     * @param searchQuery
     * @returns {Observable<R|T>}
     */
    filterForSites(searchQuery: string): Observable<Site[]> {
    return this.httpClient.get('/api/v1/site?filter=' + searchQuery + '&archived=false')
        .map((res: Response) => this.extractDataFilter(res))
        .catch(err => this.handleError(err));
    }

    /**
     * Returns all sites
     * @returns {Observable<R|T>}
     */
    getSites(): Observable<Site[]> {
        return this.httpClient.get('/api/v1/site/')
            .map((res: Response) => this.extractDataDropdown(res))
            .catch(err => this.handleError(err));
    }

    private extractDataDropdown(res: Response): Site[] {
        let obj = JSON.parse(res.text());
        return obj.entity;
    }

    private extractDataFilter(res: Response): Site[] {
        let obj = JSON.parse(res.text());
        return obj.entity;
    }

    private handleError(error: any): ErrorObservable {
        // we need use a remote logging infrastructure at some point
        let errMsg = (error.message) ? error.message :
            error.status ? `${error.status} - ${error.statusText}` : 'Server error';
        if (errMsg) {
            this.log.error(errMsg);
            this.notificationService.displayErrorMessage('There was an error; please try again : ' + errMsg);
            return Observable.throw(errMsg);
        }
    }

}
