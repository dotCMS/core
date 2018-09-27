import { Injectable } from '@angular/core';
import { CoreWebService } from './core-web.service';
import { Observable } from 'rxjs/Rx';
import { RequestMethod } from '@angular/http';
import { Subject } from 'rxjs/Subject';
import { LoginService, Auth } from './login.service';
import { DotcmsEventsService } from './dotcms-events.service';
import { LoggerService } from './logger.service';

/**
 * Provide methods and data to hable the sites.
 * @export
 */
@Injectable()
export class SiteService {
    private selectedSite: Site;
    private urls: any;
    private events: string[] = [
        'SAVE_SITE',
        'PUBLISH_SITE',
        'UPDATE_SITE_PERMISSIONS',
        'UN_ARCHIVE_SITE',
        'UPDATE_SITE',
        'ARCHIVE_SITE'
    ];
    private _switchSite$: Subject<Site> = new Subject<Site>();
    private _refreshSites$: Subject<Site> = new Subject<Site>();

    constructor(
        loginService: LoginService,
        dotcmsEventsService: DotcmsEventsService,
        private coreWebService: CoreWebService,
        private loggerService: LoggerService
    ) {
        this.urls = {
            currentSiteUrl: 'v1/site/currentSite',
            sitesUrl: 'v1/site',
            switchSiteUrl: 'v1/site/switch'
        };

        dotcmsEventsService
            .subscribeToEvents(['ARCHIVE_SITE', 'UPDATE_SITE'])
            .subscribe(data => this.eventResponse(data));

        dotcmsEventsService
            .subscribeToEvents(this.events)
            .subscribe(data => this.siteEventsHandler(data));

        loginService.watchUser((auth: Auth) => {
            if (!auth.isLoginAs) {
                this.loadCurrentSite();
            }
        });
    }

    /**
     * Manage the response when an event happen
     * @param void eventTypeWrapper
     * @returns *
     * @memberof SiteService
     */
    eventResponse(eventTypeWrapper): void {
        this.loggerService.debug(
            'Capturing Site event',
            eventTypeWrapper.eventType,
            eventTypeWrapper.data
        );
        // TODO the backend needs a change in the response 'data.data'.
        const siteIdentifier = eventTypeWrapper.data.data.identifier;
        if (siteIdentifier === this.selectedSite.identifier) {
            if (eventTypeWrapper.eventType !== 'ARCHIVE_SITE') {
                this.loadCurrentSite();
            }
        }
    }

    /**
     * Refresh the sites list if a event happen
     * @param any eventTypeWrapper
     * @memberof SiteService
     */
    siteEventsHandler(eventTypeWrapper): void {
        this._refreshSites$.next(eventTypeWrapper.data.data);
    }

    /**
     * Observable trigger when an site event happens
     * @readonly
     * @memberof SiteService
     */
    get refreshSites$(): Observable<Site> {
        return this._refreshSites$.asObservable();
    }

    /**
     * Observable tigger when the current site is changed
     * @readonly
     * @memberof SiteService
     */
    get switchSite$(): Observable<Site> {
        return this._switchSite$.asObservable();
    }

    /**
     * Return the current site for the login user.
     * @readonly
     * @memberof SiteService
     */
    get currentSite(): Site {
        return this.selectedSite;
    }

    /**
     * Switch To Default Site in the BE and returns it.
     *
     * @returns Observable<Site>
     * @memberof SiteService
     */
    switchToDefaultSite(): Observable<Site> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: 'v1/site/switch'
            })
            .pluck('entity');
    }

    /**
     * Get a site by the id
     *
     * @param string id
     * @returns Observable<Site>
     * @memberof SiteService
     */
    getSiteById(id: string): Observable<Site> {
        return this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: `content/render/false/query/+contentType:host%20+identifier:${id}`
            })
            .pluck('contentlets')
            .map(sites => sites[0]);
    }

    /**
     * Change the current site
     * @param Site site
     * @memberof SiteService
     */
    switchSite(site: Site): void {
        this.loggerService.debug('Applying a Site Switch', site.identifier);

        this.coreWebService
            .requestView({
                method: RequestMethod.Put,
                url: `${this.urls.switchSiteUrl}/${site.identifier}`
            })
            .subscribe(() => this.setCurrentSite(site));
    }

    private setCurrentSite(site: Site): void {
        if (this.selectedSite) {
            this._switchSite$.next(Object.assign({}, site));
        }

        this.selectedSite = site;
    }

    private loadCurrentSite(): void {
        this.coreWebService
            .requestView({
                method: RequestMethod.Get,
                url: this.urls.currentSiteUrl
            })
            .pluck('entity')
            .subscribe(currentSite => {
                this.setCurrentSite(<Site>currentSite);
            });
    }
}

export interface Site {
    hostname: string;
    type: string;
    identifier: string;
}
