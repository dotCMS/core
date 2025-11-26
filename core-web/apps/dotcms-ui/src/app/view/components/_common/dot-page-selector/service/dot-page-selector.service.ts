import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { flatMap, map, pluck } from 'rxjs/operators';

import { CoreWebService, Site } from '@dotcms/dotcms-js';

import { DotFolder, DotPageSelectorItem } from '../models/dot-page-selector.models';

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
    hostName: string;
    lastReview: string;
    stInode: string;
    url?: string;
}

const PAGE_BASE_TYPE_QUERY = '+basetype:5';
const MAX_RESULTS_SIZE = 20;

@Injectable()
export class DotPageSelectorService {
    private coreWebService = inject(CoreWebService);

    /**
     * Get page asset by identifier
     *
     * @param {string} identifier
     * @returns {Observable<DotPageSelectorItem>}
     * @memberof DotPageSelectorService
     */
    getPageById(identifier: string): Observable<DotPageSelectorItem> {
        return this.coreWebService
            .requestView<DotPageAsset[]>({
                body: this.getRequestBodyQuery(
                    `${PAGE_BASE_TYPE_QUERY} +identifier:*${identifier}*`
                ),
                method: 'POST',
                url: '/api/es/search'
            })
            .pipe(
                pluck('contentlets'),
                flatMap((pages: DotPageAsset[]) => pages),
                map((page: DotPageAsset) => this.getPageSelectorItem(page))
            );
    }

    getPages(path: string): Observable<DotPageSelectorItem[]> {
        return this.coreWebService
            .requestView<DotPageAsset[]>({
                url: `v1/page/search?path=${path}&onlyLiveSites=true&live=false`
            })
            .pipe(
                pluck('entity'),
                map((pages: DotPageAsset[]) => {
                    return pages.map((page: DotPageAsset) => this.getPageSelectorItem(page));
                })
            );
    }

    getFolders(path: string): Observable<DotPageSelectorItem[]> {
        return this.coreWebService
            .requestView<DotFolder[]>({
                url: `/api/v1/folder/byPath`,
                body: { path: path },
                method: 'POST'
            })
            .pipe(
                pluck('entity'),
                map((folder: DotFolder[]) => {
                    return folder.map((folder: DotFolder) => this.getPageSelectorItem(folder));
                })
            );
    }

    getSites(param: string, specific?: boolean): Observable<DotPageSelectorItem[]> {
        let query = '+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:';
        query += specific ? this.getSiteName(param) : `*${this.getSiteName(param)}*`;

        return this.coreWebService
            .requestView<Site[]>({
                body: param
                    ? this.getRequestBodyQuery(query)
                    : this.getRequestBodyQuery(query, MAX_RESULTS_SIZE),
                method: 'POST',
                url: '/api/es/search'
            })
            .pipe(
                pluck('contentlets'),
                map((sites: Site[]) => {
                    return sites.map((site) => {
                        return { payload: site, label: `//${site.hostname}/` };
                    });
                })
            );
    }

    private getRequestBodyQuery(
        query: string,
        size?: number
    ): { [key: string]: Record<string, unknown> } {
        let obj = {
            query: {
                query_string: {
                    query: query
                }
            }
        };
        if (size) {
            obj = Object.assign(obj, { size: size });
        }

        return obj;
    }

    private getSiteName(site: string): string {
        return site.replace(/\//g, '').replace(' ', '?');
    }

    private getPageSelectorItem(item: DotPageAsset | DotFolder): DotPageSelectorItem {
        return {
            label: `//${item.hostName}${item.path}`,
            payload: item
        };
    }
}
