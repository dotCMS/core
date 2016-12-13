import {Injectable} from '@angular/core';
import {ApiRoot} from '../persistence/ApiRoot';
import {CoreWebService} from './core-web-service';
import {Observable} from 'rxjs/Rx';
import {RequestMethod, Http} from '@angular/http';
import {Subject} from 'rxjs/Subject';
import {LoginService} from './login-service';
import {DotcmsEventsService} from './dotcms-events-service';

@Injectable()
export class SiteService {
    private _sites$: Subject<Site[]> = new Subject();
    private _switchSite$: Subject<Site> = new Subject();
    private site: Site;
    private sites: Site[];
    private sitesCounter: number;

    private _switchSite$: Subject<Site> = new Subject();
    private _sites$: Subject<Site[]> = new Subject();
    private _updatedCurrentSite$: Subject<Site> = new Subject();
    private _archivedCurrentSite$: Subject<Site> = new Subject();
    private _sitesCounter$: Subject<number> = new Subject();
    private urls: any;

    constructor(loginService: LoginService, dotcmsEventsService: DotcmsEventsService,
                private coreWebService: CoreWebService) {
        this.urls = {
            allSiteUrl: 'v1/site/currentSite',
            switchSiteUrl: 'v1/site/switch',
            sitesUrl: 'v1/site'
        };

        dotcmsEventsService.subscribeTo('SAVE_SITE').pluck('data').subscribe( site => {
            this.sites.push(site);
            this._sites$.next(this.sites);
        });

        dotcmsEventsService.subscribeTo('UPDATE_SITE').pluck('data').subscribe(updatedSite => {
            this.sites = this.sites.map(site => site.identifier === updatedSite.identifier ? updatedSite : site);
            this._sites$.next(this.sites);

            if (this.site.identifier === updatedSite.identifier) {
                this.site = updatedSite;

                if (loginService.auth.user.userId !== updatedSite.modUser) {
                    this._updatedCurrentSite$.next(updatedSite);
                }
            }
        });

        dotcmsEventsService.subscribeTo('ARCHIVE_SITE').pluck('data').subscribe( archivedSite => {
            this.sites = this.sites.filter(site => site.identifier !== archivedSite.identifier);
            this._sites$.next(this.sites);

            if (this.site.identifier === archivedSite.identifier) {

                if (loginService.auth.user.userId !== archivedSite.modUser) {
                    this._archivedCurrentSite$.next(archivedSite);
                }

                this.site = this.sites[0];
                this._switchSite$.next(this.site);
            }
        });

        dotcmsEventsService.subscribeTo('UN_ARCHIVE_SITE').pluck('data').subscribe( site => {
            this.sites.push(site);
            this._sites$.next(this.sites);
        });

        dotcmsEventsService.subscribeTo('UPDATE_SITE_PERMISSIONS').pluck('data').subscribe(updatedSite => {
            this.loadSites();
        });

        loginService.watchUser(this.loadSites.bind(this));
    }

    switchSite(siteId: String): Observable<any> {
        return this.coreWebService.requestView({
            method: RequestMethod.Put,
            url: `${this.urls.switchSiteUrl}/${siteId}`,
        }).map(response => {
            this.setCurrentSiteIdentifier(siteId);
            return response;
        });
    }

    get switchSite$(): Observable<Site> {
        return this._switchSite$.asObservable();
    }

    get sites$(): Observable<Site[]> {
        return this._sites$.asObservable();
    }

    get sitesCounter$(): Observable<number>{
        return this._sitesCounter$.asObservable();
    }

    get updatedCurrentSite$(): Observable<Site> {
        return this._updatedCurrentSite$.asObservable();
    }

    get archivedCurrentSite$(): Observable<Site> {
        return this._archivedCurrentSite$.asObservable();
    }

    private setCurrentSiteIdentifier(siteIdentifier: string): void {
        this.site = Object.assign({}, this.sites.filter( site => site.identifier === siteIdentifier)[0]);
        this._switchSite$.next(this.site);
    }

    private loadSites(): void {
        this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.urls.allSiteUrl,
        }).subscribe(response => {
            this.setSites(response.entity.sites);
            this.setSitesCounter(response.entity.sitesCounter);
            this.setCurrentSiteIdentifier(response.entity.currentSite);
        });
    }

    private setSites(sites: Site[]): void {
        this.sites = sites;
        this._sites$.next(this.sites);
    }

    private setSitesCounter(counter: number): void {
        this.sitesCounter = counter;
        this._sitesCounter$.next(this.sitesCounter);
    }
    get currentSite(): Site{
        return this.site;
    }

    /**
     * Return the sites available for an user paginated and filtered.
     *
     * @param filter (String) Text to filter the site names
     * @param archived (Boolean) Indicate if the results should include the archived sites
     * @param page (Int) Number of the page to display
     * @param count (Int) number of sites to show per page
     * @returns {Observable<R>} return a map with the list of paginated sites and if there
     * is a previous and next page that can be displayed
     */
    paginateSites(filter: string, archived: boolean, page: number, count: number): Observable<any> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: `${this.urls.sitesUrl}?filter=${filter}&archived=${archived}&page=${page}&count=${count}`,
        }).map(response => {
            this.setSites(response.entity.sites.results);
            return response.entity;
        });
    }
}

export interface Site {
    hostName: string;
    type: string;
    identifier: string;
}
