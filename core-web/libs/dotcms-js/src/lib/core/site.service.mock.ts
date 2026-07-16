import { of as observableOf, Observable, Subject, merge, of } from 'rxjs';

import { switchMap, skip, startWith } from 'rxjs/operators';

import { Site } from './site.service';

export const mockSites: Site[] = [
    {
        hostname: 'demo.dotcms.com',
        identifier: '123-xyz-567-xxl',
        type: 'abc',
        archived: false
    },
    {
        hostname: 'hello.dotcms.com',
        identifier: '456-xyz-789-xxl',
        type: 'def',
        archived: false
    }
];

export class SiteServiceMock {
    _currentSite: Site;
    private _currentSite$: Subject<Site> = new Subject<Site>();

    get currentSite(): Site {
        return this._currentSite || mockSites[0];
    }

    getSiteById(): Observable<Site> {
        return observableOf(mockSites[0]);
    }

    paginateSites(): Observable<Site[]> {
        return observableOf(mockSites);
    }

    setFakeCurrentSite(site?: Site) {
        this._currentSite = site || mockSites[0];
        this._currentSite$.next(site || mockSites[0]);
    }

    switchSiteById(): Observable<Site> {
        return this.getSiteById().pipe(switchMap((site) => this.switchSite(site)));
    }

    switchSite(site: Site): Observable<Site> {
        return of(site);
    }

    getCurrentSite(): Observable<Site> {
        return merge(observableOf(mockSites[0]), this.switchSite$);
    }

    /**
     * Returns an Observable that immediately emits the current selected site upon subscription,
     * then emits whenever the site changes.
     *
     * @readonly
     */
    get currentSite$(): Observable<Site> {
        return this._currentSite$.asObservable().pipe(startWith(this.currentSite));
    }

    get loadedSites(): Site[] {
        return mockSites;
    }

    get refreshSites$(): Observable<Site> {
        return observableOf(mockSites[0]);
    }

    get sites$(): Observable<Site[]> {
        return observableOf(mockSites);
    }

    get sitesCounter$(): Observable<number> {
        return observableOf(mockSites.length * 3);
    }

    /**
     * Returns an Observable that only emits when the site selector changes after you're already subscribed.
     *
     * @readonly
     */
    get switchSite$(): Observable<Site> {
        return this.currentSite$.pipe(skip(1));
    }
}
