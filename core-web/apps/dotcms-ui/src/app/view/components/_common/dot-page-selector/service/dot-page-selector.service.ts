import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { mergeMap, map } from 'rxjs/operators';

import { DotCMSResponse, Site } from '@dotcms/dotcms-models';

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

// Response type for ES search endpoints that return contentlets
interface DotESSearchResponse<T> {
    contentlets: T;
}

const PAGE_BASE_TYPE_QUERY = '+basetype:5';
const MAX_RESULTS_SIZE = 20;

@Injectable()
export class DotPageSelectorService {
    private http = inject(HttpClient);

    /**
     * Get page asset by identifier
     *
     * @param {string} identifier
     * @returns {Observable<DotPageSelectorItem>}
     * @memberof DotPageSelectorService
     */
    getPageById(identifier: string): Observable<DotPageSelectorItem> {
        return this.http
            .post<
                DotESSearchResponse<DotPageAsset[]>
            >('/api/es/search', this.getRequestBodyQuery(`${PAGE_BASE_TYPE_QUERY} +identifier:*${identifier}*`))
            .pipe(
                map((response) => response.contentlets),
                mergeMap((pages: DotPageAsset[]) => pages),
                map((page: DotPageAsset) => this.getPageSelectorItem(page))
            );
    }

    getPages(path: string): Observable<DotPageSelectorItem[]> {
        return this.http
            .get<
                DotCMSResponse<DotPageAsset[]>
            >(`/api/v1/page/search?path=${path}&onlyLiveSites=true&live=false`)
            .pipe(
                map((response) => response.entity),
                map((pages: DotPageAsset[]) => {
                    return pages.map((page: DotPageAsset) => this.getPageSelectorItem(page));
                })
            );
    }

    getFolders(path: string): Observable<DotPageSelectorItem[]> {
        return this.http
            .post<DotCMSResponse<DotFolder[]>>('/api/v1/folder/byPath', { path: path })
            .pipe(
                map((response) => response.entity),
                map((folder: DotFolder[]) => {
                    return folder.map((folder: DotFolder) => this.getPageSelectorItem(folder));
                })
            );
    }

    getSites(param: string, specific?: boolean): Observable<DotPageSelectorItem[]> {
        let query = '+contenttype:Host -identifier:SYSTEM_HOST +host.hostName:';
        query += specific ? this.getSiteName(param) : `*${this.getSiteName(param)}*`;

        return this.http
            .post<
                DotESSearchResponse<Site[]>
            >('/api/es/search', param ? this.getRequestBodyQuery(query) : this.getRequestBodyQuery(query, MAX_RESULTS_SIZE))
            .pipe(
                map((response) => response.contentlets),
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
