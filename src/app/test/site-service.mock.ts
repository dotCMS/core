import { Site } from 'dotcms-js/dotcms-js';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs';

export class SiteServiceMock {
    public mockSites: Site[] = [
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

    currentSite: Site;
    private _switchSite$: Subject<Site> = new Subject<Site>();

    getSiteById(): Site {
        return null;
    }

    paginateSites(filter: string, archived: boolean, page: number, count: number): Observable<Site[]> {
        return Observable.of(this.mockSites);
    }

    setFakeCurrentSite(site?: Site) {
        this.currentSite = site || this.mockSites[0];
        this._switchSite$.next(site || this.mockSites[0]);
    }

    get loadedSites(): Site[] {
        return this.mockSites;
    }

    get refreshSites$(): Observable<Site> {
        return Observable.of(this.mockSites[0]);
    }

    get sites$(): Observable<Site[]> {
        return Observable.of(this.mockSites);
    }

    get sitesCounter$(): Observable<number> {
        return Observable.of(this.mockSites.length * 3);
    }

    get switchSite$(): Observable<Site> {
        return this._switchSite$.asObservable();
    }
}
