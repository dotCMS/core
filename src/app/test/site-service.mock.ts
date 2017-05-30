import { Site } from '../api/services/site-service';
import { Observable } from 'rxjs/Observable';

export class SiteServiceMock {
    private mockSites: Site[] = [
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
}