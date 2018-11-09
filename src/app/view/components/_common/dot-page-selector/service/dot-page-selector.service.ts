import { map, pluck } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CoreWebService } from 'dotcms-js';
import { RequestMethod } from '@angular/http';
import { Site } from 'dotcms-js';

export interface DotPageAsset {
    template: string;
    owner: string;
    identifier: string;
    friendlyname: string;
    modDate: string;
    cachettl: string;
    pagemetadata: string;
    languageId: number;
    title: string;
    showOnMenu: string;
    inode: string;
    seodescription: string;
    folder: string;
    __DOTNAME__?: string;
    path?: string;
    sortOrder: number;
    seokeywords: string;
    modUser: string;
    host: string;
    lastReview: string;
    stInode: string;
    url?: string;
}

@Injectable()
export class DotPageSelectorService {
    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get all the pages in the folder
     *
     * @param string searchParam
     * @returns Observable<DotPageAsset[]>
     * @memberof DotPageSelectorService
     */
    getPagesInFolder(searchParam: string, hostId?: string): Observable<DotPageAsset[]> {
        const parsedQuery = this.parseHost(searchParam);
        let query = `+basetype:5 +path:*${parsedQuery[0]}*`;
        query +=
            parsedQuery.length > 1
                ? ` +conhostName:*${parsedQuery[1]}*`
                : hostId ? ` +conhost:${hostId}` : '';

        return this.coreWebService
            .requestView({
                body: {
                    query: {
                        query_string: {
                            query: query
                        }
                    }
                },
                method: RequestMethod.Post,
                url: 'es/search'
            })
            .pipe(pluck('contentlets'));
    }

    /**
     * Get a page by id
     *
     * @param string identifier
     * @memberof DotPageSelectorService
     */
    getPage(identifier: string): Observable<DotPageAsset> {
        return this.coreWebService
            .requestView({
                body: {
                    query: {
                        query_string: {
                            query: `+basetype:5 +identifier:${identifier}`
                        }
                    }
                },
                method: RequestMethod.Post,
                url: 'es/search'
            })
            .pipe(pluck('contentlets'), map((pages: DotPageAsset[]) => pages[0]));
    }

    /**
     * Get all sited of filtered by name.
     *
     * @param string identifier
     * @memberof DotPageSelectorService
     */
    getSites(filter?: string): Observable<Site[]> {
        let url = 'v1/site/';
        url += filter ? `?filter=${filter}` : '';
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: url
            })
            .pipe(pluck('entity'));
    }

    private parseHost(query: string): string[] {
        const host = query.match(/^\/\/[^/]*\//g);
        const search: string[] = [];
        if (host) {
            search.push(query.replace(host[0], '').replace(/\//g, '\\/'));
            search.push(host[0].substr(2).slice(0, -1));
            return search;
        }
        return [query.replace(/\//g, '\\/')];
    }
}
