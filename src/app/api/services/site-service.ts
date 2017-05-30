import {Injectable} from '@angular/core';
import {CoreWebService} from './core-web-service';
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {Subject} from 'rxjs/Subject';
import {LoginService} from './login-service';
import {DotcmsEventsService} from './dotcms-events-service';
import {LoggerService} from './logger.service';

@Injectable()
export class SiteService {
    private sites: Site[];
    private sitesCounter: number;
    private selectedSite: Site;
    private urls: any;

    private _switchSite$: Subject<Site> = new Subject<Site>();
    private _sites$: Subject<Site[]> = new Subject<Site[]>();
    private _sitesCounter$: Subject<number> = new Subject<number>();

    private events: string[] = ['SAVE_SITE', 'PUBLISH_SITE', 'UPDATE_SITE_PERMISSIONS', 'UN_ARCHIVE_SITE', 'UPDATE_SITE'];
    private eventsWithSwitch: string[] = ['ARCHIVE_SITE'];

    constructor(loginService: LoginService, dotcmsEventsService: DotcmsEventsService,
                private coreWebService: CoreWebService, private loggerService: LoggerService) {
        this.urls = {
            allSiteUrl: 'v1/site/currentSite',
            sitesUrl: 'v1/site',
            switchSiteUrl: 'v1/site/switch'
        };

        dotcmsEventsService.subscribeToEvents(this.events).subscribe(eventTypeWrapper => {

            this.loggerService.debug('Capturing Site event', eventTypeWrapper.eventType, eventTypeWrapper.data);

            // Update the sites list
            this.loadSites();
        });

        dotcmsEventsService.subscribeToEvents(this.eventsWithSwitch).subscribe(eventTypeWrapper => {

            this.loggerService.debug('Capturing Site event', eventTypeWrapper.eventType, eventTypeWrapper.data);

            // Update the sites list
            this.loadSitesAndSwitch(eventTypeWrapper.data.data.identifier);
        });

        loginService.watchUser(this.loadSites.bind(this));
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

    get currentSite(): Site {
        return this.selectedSite;
    }

    // TODO: change this when we update the site selector
    get loadedSites(): Site[] {
        return this.sites;
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

    switchSite(siteId: string): Observable<any> {

        this.loggerService.debug('Applying a Site Switch', siteId);

        return this.coreWebService.requestView({
            method: RequestMethod.Put,
            url: `${this.urls.switchSiteUrl}/${siteId}`,
        }).map(response => {
            this.setCurrentSiteIdentifier(siteId);
            return response;
        });
    }

    private setCurrentSiteIdentifier(siteIdentifier: string): void {
        this.selectedSite = Object.assign({}, this.sites.filter(site => site.identifier === siteIdentifier)[0]);
        this._switchSite$.next(this.selectedSite);
    }

    private setNextAndSwitchSite(siteIdentifier: string): void {
        this.selectedSite = Object.assign({}, this.sites.filter(site => site.identifier !== siteIdentifier)[0]);
        this._switchSite$.next(this.selectedSite);

        this.switchSite(this.selectedSite.identifier).subscribe(response => {
            // For now do nothing....
        });
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

    private loadSitesAndSwitch(siteToExclude: string): void {
        this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.urls.allSiteUrl,
        }).subscribe(response => {
            this.setSites(response.entity.sites);
            this.setSitesCounter(response.entity.sitesCounter);

            if (siteToExclude === this.selectedSite.identifier) {// Only if the option we want to excluded is selected
                this.setNextAndSwitchSite(siteToExclude);
            } else {
                this.setCurrentSiteIdentifier(response.entity.currentSite);
            }
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

}

export interface Site {
    hostname: string;
    type: string;
    identifier: string;
}
