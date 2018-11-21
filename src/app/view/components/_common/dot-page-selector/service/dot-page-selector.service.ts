import { map, pluck, flatMap, toArray, tap, switchMap } from 'rxjs/operators';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RequestMethod } from '@angular/http';
import { Site, ResponseView, CoreWebService } from 'dotcms-js';

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

export interface DotPageSeletorItem {
    label: string;
    payload: DotPageAsset | Site;
}

export interface DotPageSelectorResults {
    data: DotPageSeletorItem[];
    type: string;
    query: string;
}

const HOST_FULL_REGEX = /^\/\/[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b\//;
const URL_ABS_REGEX = /^\/\/?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/;

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
                body: this.getRequestBodyQuery(`+basetype:5 +identifier:*${identifier}*`),
                method: RequestMethod.Post,
                url: 'es/search'
            })
            .pipe(
                pluck('contentlets'),
                flatMap((pages: DotPageAsset[]) => pages),
                map((page: DotPageAsset) => {
                    return {
                        label: `//coming.from.server${page.path}`,
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
        const host = this.parseHost(param).pop();

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

    private getPages(searchParam: string): Observable<DotPageSelectorResults> {
        return this.coreWebService
            .requestView({
                body: this.getPagesSearchQuery(
                    searchParam,
                    this.currentHost ? this.currentHost.identifier : null
                ),
                method: RequestMethod.Post,
                url: 'es/search'
            })
            .pipe(
                pluck('contentlets'),
                flatMap((pages: DotPageAsset[]) => pages),
                map((page: DotPageAsset) => {
                    return {
                        label: `//coming.from.server${page.path}`,
                        payload: page
                    };
                }),
                toArray(),
                map((items: DotPageSeletorItem[]) => {
                    return {
                        data: items,
                        type: 'page',
                        query: searchParam.replace(HOST_FULL_REGEX, '')
                    };
                })
            );
    }

    private getPagesSearchQuery(searchParam: string, hostId?: string): { [key: string]: {} } {
        const parsedQuery = this.parseHost(searchParam);

        let query = `+basetype:5 +path:*${parsedQuery[0]}*`;

        query +=
            parsedQuery.length > 1
                ? ` +conhostName:*${parsedQuery[1]}*`
                : hostId
                ? ` +conhost:${hostId}`
                : '';

        return this.getRequestBodyQuery(query);
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

    private isAbsoluteUrl(param: string): boolean {
        return URL_ABS_REGEX.test(param);
    }

    private isReSearchingForHost(param: string): boolean {
        return this.currentHost && !this.itStartWithFullHost(param);
    }

    private itStartWithFullHost(param: string): boolean {
        return HOST_FULL_REGEX.test(param);
    }

    private isTwoStepSearch(param): boolean {
        return !this.currentHost && this.isAbsoluteUrl(param);
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

    private shouldSearchPages(param: string): boolean {
        if (this.isReSearchingForHost(param)) {
            this.currentHost = null;
        }

        return !!(this.currentHost || !param.startsWith('//'));
    }
}
