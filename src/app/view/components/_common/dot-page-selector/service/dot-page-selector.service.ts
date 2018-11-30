import { map, pluck, flatMap, toArray, tap, switchMap } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { Site, ResponseView, CoreWebService } from 'dotcms-js';
import {
    DotPageSeletorItem,
    DotPageSelectorResults,
    DotSimpleURL
} from '../models/dot-page-selector.models';

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

const HOST_FULL_REGEX = /^\/\/[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b\//;
const PAGE_BASE_TYPE_QUERY = '+basetype:5';

@Injectable()
export class DotPageSelectorService {
    private currentHost: Site;

    constructor(private coreWebService: CoreWebService) {}

    /**
     * Get page asset by identifier
     *
     * @param {string} identifier
     * @returns {Observable<DotPageSeletorItem>}
     * @memberof DotPageSelectorService
     */
    getPageById(identifier: string): Observable<DotPageSeletorItem> {
        return this.coreWebService
            .requestView({
                body: this.getRequestBodyQuery(
                    `${PAGE_BASE_TYPE_QUERY} +identifier:*${identifier}*`
                ),
                method: RequestMethod.Post,
                url: 'es/search'
            })
            .pipe(
                pluck('contentlets'),
                flatMap((pages: DotPageAsset[]) => pages),
                map((page: DotPageAsset) => {
                    return {
                        label: `//${page.hostName}${page.path}`,
                        payload: page
                    };
                })
            );
    }

    /**
     * Set the host to perform page searches
     *
     * @param {Site} host
     * @memberof DotPageSelectorService
     */
    setCurrentHost(host: Site): void {
        this.currentHost = host;
    }

    /**
     * Search for host, page or both
     *
     * @param {string} param
     * @returns {Observable<DotPageSelectorResults>}
     * @memberof DotPageSelectorService
     */
    search(param: string): Observable<DotPageSelectorResults> {
        if (this.isTwoStepSearch(param)) {
            return this.fullSearch(param);
        } else {
            return this.conditionalSearch(param);
        }
    }

    private conditionalSearch(param: string): Observable<DotPageSelectorResults> {
        return this.shouldSearchPages(param)
            ? this.getPages(param)
            : this.getSites(this.getSiteName(param));
    }

    private fullSearch(param: string): Observable<DotPageSelectorResults> {
        const host = this.parseUrl(param).host;

        return this.getSites(host).pipe(
            tap((results: DotPageSelectorResults) => {
                this.setCurrentHost(<Site>results.data[0].payload);
            }),
            switchMap(() => this.getPages(param))
        );
    }

    private getEsResults(query: string): Observable<ResponseView> {
        return this.coreWebService.requestView({
            body: this.getRequestBodyQuery(query),
            method: RequestMethod.Post,
            url: 'es/search'
        });
    }

    private getPages(query: string): Observable<DotPageSelectorResults> {
        return this.coreWebService
            .requestView({
                body: this.getPagesSearchQuery(query),
                method: RequestMethod.Post,
                url: 'es/search'
            })
            .pipe(
                pluck('contentlets'),
                flatMap((pages: DotPageAsset[]) => pages),
                map((page: DotPageAsset) => {
                    return {
                        label: `//${page.hostName}${page.path}`,
                        payload: page
                    };
                }),
                toArray(),
                map((items: DotPageSeletorItem[]) => {
                    return {
                        data: items,
                        type: 'page',
                        query: query.replace(HOST_FULL_REGEX, '')
                    };
                })
            );
    }

    private getPagesSearchQuery(param: string): { [key: string]: {} } {
        const parsedUrl: DotSimpleURL = this.parseUrl(param);

        let query = `${PAGE_BASE_TYPE_QUERY} +path:*${this.cleanPath(parsedUrl ? parsedUrl.pathname : param)}*`;

        if (this.currentHost) {
            query += ` +conhostName:${this.currentHost.hostname}`;
        }

        return this.getRequestBodyQuery(query);
    }

    private cleanPath(path: string): string {
        return path.replace(/\//g, '\\/');
    }

    private getRequestBodyQuery(query: string): { [key: string]: {} } {
        return {
            query: {
                query_string: {
                    query: query
                }
            }
        };
    }

    private getSites(param: string): Observable<DotPageSelectorResults> {
        const query = `+contenttype:Host +host.hostName:*${this.getSiteName(param)}*`;

        return this.getEsResults(query).pipe(
            pluck('contentlets'),
            flatMap((sites: Site[]) => sites),
            map((site: Site) => {
                return {
                    label: `//${site.hostname}/`,
                    payload: site,
                    isHost: true
                };
            }),
            toArray(),
            map((items: DotPageSeletorItem[]) => {
                return {
                    data: items,
                    type: 'site',
                    query: param
                };
            })
        );
    }

    private getSiteName(site: string): string {
        return site.replace(/\//g, '');
    }

    private isHostAndPath(param: string): boolean {
        const url: DotSimpleURL | { [key: string]: string } = this.parseUrl(param);
        return url && !!(url.host && url.pathname.length > 0);
    }

    private isReSearchingForHost(query: string): boolean {
        return this.isSearchingForHost(query) && this.hostChanged(query);
    }

    private hostChanged(query: string): boolean {
        const parsedURL = this.parseUrl(query);
        return this.currentHost && parsedURL && this.currentHost.hostname !== parsedURL.host;
    }

    private isSearchingForHost(query: string): boolean {
        return query.startsWith('//') && !query.endsWith('/');
    }


    private isTwoStepSearch(param): boolean {
         return this.isHostAndPath(param) && ( !this.currentHost ||  this.hostChanged(param));
    }

    private parseUrl(query: string): DotSimpleURL {
        if (this.isSearchingForHost(query)) {
            try {
                const url = new URL(`http:${query}`);
                return { host: url.host, pathname: url.pathname.substr(1) };
            } catch {
                return null;
            }
        } else {
            return null;
        }
    }

    private shouldSearchPages(query: string): boolean {
        const parsedURL = this.parseUrl(query);
        if ( !parsedURL || this.isReSearchingForHost(query)) {
            this.currentHost = null;
        }

        return !!(this.currentHost || !this.isSearchingForHost(query));
    }
}
