import {Injectable} from '@angular/core';
import {CoreWebService} from './core-web-service';
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {Subject} from 'rxjs/Subject';
import {LoginService} from './login-service';
import {DotcmsEventsService} from './dotcms-events-service';
import {LoggerService} from './logger.service';

/**
 * Provide methods and data to hable the sites.
 * @export
 * @class SiteService
 */
@Injectable()
export class SiteService {
    private sitesCounter: number;
    private selectedSite: Site;
    private urls: any;

    private _switchSite$: Subject<Site> = new Subject<Site>();
    private _sitesCounter$: Subject<number> = new Subject<number>();

    constructor(loginService: LoginService, dotcmsEventsService: DotcmsEventsService,
                private coreWebService: CoreWebService, private loggerService: LoggerService) {

        this.urls = {
            currentSiteUrl: 'v1/site/currentSite',
            sitesUrl: 'v1/site',
            switchSiteUrl: 'v1/site/switch'
        };

        dotcmsEventsService.subscribeTo('ARCHIVE_SITE').subscribe(eventTypeWrapper => {
            this.loggerService.debug('Capturing Site event', eventTypeWrapper.eventType, eventTypeWrapper.data);

            let siteToExclude = eventTypeWrapper.data.data.identifier;

            if (siteToExclude === this.selectedSite.identifier) {
                this.getOneSite().subscribe( site => this.switchSite(site));
            }
        });

        loginService.watchUser(this.loadCurrentSite.bind(this));
    }

    /**
     * Observable tigger when the current site is changed
     * @readonly
     * @type {Observable<Site>}
     * @memberof SiteService
     */
    get switchSite$(): Observable<Site> {
        return this._switchSite$.asObservable();
    }

    /**
     * Observable tigger when the number of sites is changed
     * @readonly
     * @type {Observable<Site>}
     * @memberof SiteService
     */
    get sitesCounter$(): Observable<number> {
        return this._sitesCounter$.asObservable();
    }

    /**
     * Return the current site for the login user.
     * @readonly
     * @type {Site}
     * @memberof SiteService
     */
    get currentSite(): Site {
        return this.selectedSite;
    }

    /**
     * Change the current site
     * @param {Site} site
     * @memberof SiteService
     */
    switchSite(site: Site): void {
        this.loggerService.debug('Applying a Site Switch', site.identifier);

        this.coreWebService.requestView({
            method: RequestMethod.Put,
            url: `${this.urls.switchSiteUrl}/${site.identifier}`,
        }).subscribe(() => this.setCurrentSite(site));
    }

    private setCurrentSite(site: Site): void {
        this.selectedSite = site;
        this._switchSite$.next(Object.assign({}, site));
    }

    private loadCurrentSite(): void {
        this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.urls.currentSiteUrl,
        }).pluck('entity')
        .subscribe(entity => {
            this._sitesCounter$.next(entity['totalRecords']);
            this.setCurrentSite(entity['currentSite']);
        });
    }

    private getOneSite(): Observable<Site> {
        return this.coreWebService.requestView({
            method: RequestMethod.Get,
            url: this.urls.sitesUrl,
        }).map(response => {
            return response.entity[0];
        });
    }
}

export interface Site {
    hostname: string;
    type: string;
    identifier: string;
}
