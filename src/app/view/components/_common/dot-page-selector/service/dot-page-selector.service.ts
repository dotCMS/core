import { map, pluck, flatMap, toArray } from 'rxjs/operators';
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
    isHost?: boolean;
}

@Injectable()
export class DotPageSelectorService {
    private currentHost: Site;

    constructor(private coreWebService: CoreWebService) {}

    setCurrentHost(host: Site): void {
        console.log('setCurrentHost');
        this.currentHost = host;
    }

    search(param: string): Observable<DotPageSeletorItem[]> {
        console.log('search.service', param);

        return this.shouldSearchPages(param)
            ? this.getPages(param)
            : this.getSites(this.getSiteName(param));
    }

    private getEsResults(query: string): Observable<ResponseView> {
        return this.coreWebService.requestView({
            body: this.getRequestBodyQuery(query),
            method: RequestMethod.Post,
            url: 'es/search'
        });
    }

    private getPages(searchParam: string): Observable<DotPageSeletorItem[]> {
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
                        payload: page,
                        isHost: false
                    };
                }),
                toArray()
            );
    }

    private getPagesSearchQuery(searchParam: string, hostId?: string): {[key: string]: {}} {
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

    private getRequestBodyQuery(query: string): {[key: string]: {}} {
        return {
            query: {
                query_string: {
                    query: query
                }
            }
        };
    }

    private getSites(param?: string): Observable<DotPageSeletorItem[]> {
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
            toArray()
        );
    }

    private getSiteName(site: string): string {
        return site.replace(/\//g, '');
    }

    private isReSearchingForHost(param: string): boolean {
        return this.currentHost && !this.itStartWithFullHost(param);
    }

    private itStartWithFullHost(param: string): boolean {
        return /^\/\/[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b\//.test(param);
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
