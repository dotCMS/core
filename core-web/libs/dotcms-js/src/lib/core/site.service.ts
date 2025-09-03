import { Observable, Subject, merge, of } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { map, pluck, startWith, switchMap, take, tap } from 'rxjs/operators';

import { CoreWebService } from './core-web.service';
import { DotcmsEventsService } from './dotcms-events.service';
import { LoggerService } from './logger.service';
import { LoginService } from './login.service';
import { DotEventTypeWrapper } from './models/dot-events/dot-event-type-wrapper';

/**
 * @deprecated
 * This service is deprecated do not use it in new code.
 * If you need to interact with the sites use the DotSiteService from @dotcms/data-access.
 *
 * Provide methods and data to hable the sites.
 * @export
 */
@Injectable({
    providedIn: 'root'
})
export class SiteService {
    private coreWebService = inject(CoreWebService);
    private loggerService = inject(LoggerService);

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

    constructor() {
        const loginService = inject(LoginService);
        const dotcmsEventsService = inject(DotcmsEventsService);

        this.urls = {
            currentSiteUrl: 'v1/site/currentSite',
            sitesUrl: 'v1/site',
            switchSiteUrl: 'v1/site/switch'
        };

        dotcmsEventsService
            .subscribeToEvents<Site>(['ARCHIVE_SITE', 'UPDATE_SITE'])
            .subscribe((event: DotEventTypeWrapper<Site>) => this.eventResponse(event));

        dotcmsEventsService
            .subscribeToEvents<Site>(this.events)
            .subscribe(({ data }: DotEventTypeWrapper<Site>) => this.siteEventsHandler(data));

        dotcmsEventsService
            .subscribeToEvents<Site>(['SWITCH_SITE'])
            .subscribe(({ data }: DotEventTypeWrapper<Site>) => this.setCurrentSite(data));

        loginService.watchUser(() => this.loadCurrentSite());
    }

    /**
     * Manage the response when an event happen
     *
     * @memberof SiteService
     */
    eventResponse({ name, data }: DotEventTypeWrapper<Site>): void {
        this.loggerService.debug('Capturing Site event', name, data);

        const siteIdentifier = data.identifier;
        if (siteIdentifier === this.selectedSite.identifier) {
            name === 'ARCHIVE_SITE'
                ? this.switchToDefaultSite()
                      .pipe(
                          take(1),
                          switchMap((site) => this.switchSite(site))
                      )
                      .subscribe()
                : this.loadCurrentSite();
        }
    }

    /**
     * Refresh the sites list if a event happen
     *
     * @memberof SiteService
     */
    siteEventsHandler(site: Site): void {
        this._refreshSites$.next(site);
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
        return this._switchSite$.asObservable().pipe(startWith(this.selectedSite));
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
                method: 'PUT',
                url: 'v1/site/switch'
            })
            .pipe(pluck('entity'));
    }

    /**
     * Get a site by the id
     *
     * @param {string} id
     * @return {*}  {Observable<Site>}
     * @memberof SiteService
     */
    getSiteById(id: string): Observable<Site> {
        return this.coreWebService
            .requestView({
                url: `/api/content/render/false/query/+contentType:host%20+identifier:${id}`
            })
            .pipe(
                pluck('contentlets'),
                map((sites: Site[]) => sites[0])
            );
    }

    /**
     * Switch site by the id
     * This method gets a new site by the id and switch to it
     *
     * @param {string} id
     * @return {*}  {Observable<Site>}
     * @memberof SiteService
     */
    switchSiteById(id: string): Observable<Site> {
        this.loggerService.debug('Applying a Site Switch');

        return this.getSiteById(id).pipe(
            switchMap((site) => {
                // If there is a site we switch to it
                return site ? this.switchSite(site) : of(null);
            }),
            take(1)
        );
    }

    /**
     * Change the current site
     *
     * @param {Site} site
     * @return {*}  {Observable<Site>}
     * @memberof SiteService
     */
    switchSite(site: Site): Observable<Site> {
        this.loggerService.debug('Applying a Site Switch', site.identifier);

        return this.coreWebService
            .requestView({
                method: 'PUT',
                url: `${this.urls.switchSiteUrl}/${site.identifier}`
            })
            .pipe(
                take(1),
                tap(() => this.setCurrentSite(site)),
                map(() => site)
            );
    }

    /**
     * Get the current site
     * @returns Observable<Site>
     * @memberof SiteService
     */
    getCurrentSite(): Observable<Site> {
        return merge(
            this.selectedSite ? of(this.selectedSite) : this.requestCurrentSite(),
            this.switchSite$
        );
    }

    private requestCurrentSite(): Observable<Site> {
        return this.coreWebService
            .requestView({
                url: this.urls.currentSiteUrl
            })
            .pipe(pluck('entity'));
    }

    private setCurrentSite(site: Site): void {
        this.selectedSite = site;
        this._switchSite$.next({ ...site });
    }

    private loadCurrentSite(): void {
        this.getCurrentSite()
            .pipe(take(1))
            .subscribe((currentSite: Site) => {
                this.setCurrentSite(currentSite);
            });
    }
}

/**
 * @deprecated
 * This interface is deprecated do not use it in new code.
 * If you need to interact with the sites use the DotSiteService from @dotcms/data-access.
 *
 * @export
 * @interface Site
 */
export interface Site {
    hostname: string;
    type: string;
    identifier: string;
    archived: boolean;
    googleMap?: string;
}
