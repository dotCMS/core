
import {throwError as observableThrowError,  Observable ,  BehaviorSubject } from 'rxjs';
import { Inject, Injectable } from '@angular/core';

import { Response } from '@angular/http';
import { HttpClient } from '../util/http.service';
import { Treeable } from '../treeable/shared/treeable.model';
import { File } from './file.model';
import { map, catchError } from 'rxjs/operators';
import { ErrorObservable } from 'rxjs/observable/ErrorObservable';

@Injectable()
@Inject('httpClient')
export class FileSearchService {
    searchQuery: Observable<string>;
    private searchQuerySubject: BehaviorSubject<string> = new BehaviorSubject<string>(null);

    constructor(private httpClient: HttpClient) {
        this.searchQuery = this.searchQuerySubject.asObservable();
    }

    changeSearchQuery(query: string): void {
        this.searchQuerySubject.next(query);
    }
    getSearchQuery(): string {
        return <string>this.searchQuerySubject.getValue();
    }

    search(query: string): Observable<Treeable[] | string> {
        return this.httpClient.get('/api/content/render/false/query/' + query).pipe(
            map((res: Response) => this.extractDataFilter(res)),
            catchError((error) => this.handleError(error))
        );
    }

    private extractDataFilter(res: Response): Treeable[] {
        const treeables: Treeable[] = [];
        const obj = JSON.parse(res.text());
        const results: any[] = obj.contentlets;
        for (let i = 0; i < results.length; i++) {
            const r: any = results[i];
            let t: File;
            t = Object.assign(new File(), r);
            t.modUserName = r.modUser;
            t.mimeType = r.metaData.contentType;
            t.type = 'file_asset';
            treeables[i] = t;
        }
        return treeables;
    }

    private handleError(error: any): ErrorObservable<string> {
        const errMsg = this.getError(error) ? `${error.status} - ${error.statusText}` : 'Server error';
        if (errMsg) {
            // this.log.error(errMsg);
            console.error('There was an error; please try again : ' + errMsg);
            return observableThrowError(errMsg);
        }
    }

    private getError(error: any): string {
        return error.message ? error.message : error.status;
    }
}
