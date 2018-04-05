import { Site } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';

export const mockSites: Site[] = [
    {
        hostname: 'demo.dotcms.com',
        identifier: '123-xyz-567-xxl',
        type: 'abc'
    },
    {
        hostname: 'hello.dotcms.com',
        identifier: '456-xyz-789-xxl',
        type: 'def'
    }
];

export class SiteServiceMock {
    _currentSite: Site;
    private _switchSite$: Subject<Site> = new Subject<Site>();

    get currentSite(): Site {
        return this._currentSite || mockSites[0];
    }

    getSiteById(): Site {
        return null;
    }

    paginateSites(): Observable<Site[]> {
        return Observable.of(mockSites);
    }

    setFakeCurrentSite(site?: Site) {
        this._currentSite = site || mockSites[0];
        this._switchSite$.next(site || mockSites[0]);
    }

    get loadedSites(): Site[] {
        return mockSites;
    }

    get refreshSites$(): Observable<Site> {
        return Observable.of(mockSites[0]);
    }

    get sites$(): Observable<Site[]> {
        return Observable.of(mockSites);
    }

    get sitesCounter$(): Observable<number> {
        return Observable.of(mockSites.length * 3);
    }

    get switchSite$(): Observable<Site> {
        return this._switchSite$.asObservable();
    }
}
