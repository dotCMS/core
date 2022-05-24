import { of as observableOf, Observable, Subject, merge } from 'rxjs';
import { Site } from '@dotcms/dotcms-js';

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
    private _switchSite$: Subject<Site> = new Subject<Site>();

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
        this._switchSite$.next(site || mockSites[0]);
    }

    // eslint-disable-next-line @typescript-eslint/no-empty-function
    switchSite(_site: Site) {}

    getCurrentSite(): Observable<Site> {
        return merge(observableOf(mockSites[0]), this.switchSite$);
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

    get switchSite$(): Observable<Site> {
        return this._switchSite$.asObservable();
    }
}
