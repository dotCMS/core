import { Site } from '../api/services/site-service';
import { Observable } from 'rxjs/Observable';

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

    get loadedSites(): Site[] {
        return this.mockSites;
    }

    get sites$(): Observable<Site[]> {
        return Observable.of(this.mockSites);
    }

    get sitesCounter$(): Observable<number>{
        return Observable.of(this.mockSites.length * 3);
    }

    paginateSites(filter: string, archived: boolean, page: number, count: number): Observable<Site[]> {
        return Observable.of(this.mockSites);
    }

    get switchSite$(): Observable<Site> {
        return Observable.of(this.mockSites[0]);
    }

    get refreshSites$(): Observable<Site> {
        return Observable.of(this.mockSites[0]);
    }
}