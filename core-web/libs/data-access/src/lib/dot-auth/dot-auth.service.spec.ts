import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotAuthConfigPayload, DotAuthConfigView, DotAuthSitesView } from '@dotcms/dotcms-models';

import { DotAuthService } from './dot-auth.service';

describe('DotAuthService', () => {
    let service: DotAuthService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotAuthService, provideHttpClient(), provideHttpClientTesting()]
        });
        service = TestBed.inject(DotAuthService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpMock.verify());

    it('listSites hits GET /api/v1/dotauth/sites and unwraps entity', (done) => {
        const fake: DotAuthSitesView = {
            system: { configured: false },
            sites: [{ hostId: 'h1', hostName: 'default', status: 'NOT_CONFIGURED' }]
        };
        service.listSites().subscribe((view) => {
            expect(view).toEqual(fake);
            done();
        });
        const req = httpMock.expectOne('/api/v1/dotauth/sites');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: fake });
    });

    it('getConfig hits GET /api/v1/dotauth/sites/{hostId}', (done) => {
        const fake: DotAuthConfigView = {
            hostId: 'SYSTEM_HOST',
            configured: true,
            inherited: false,
            values: { enabled: true }
        };
        service.getConfig('SYSTEM_HOST').subscribe((view) => {
            expect(view).toEqual(fake);
            done();
        });
        const req = httpMock.expectOne('/api/v1/dotauth/sites/SYSTEM_HOST');
        expect(req.request.method).toBe('GET');
        req.flush({ entity: fake });
    });

    it('saveConfig PUTs the payload', (done) => {
        const payload: DotAuthConfigPayload = { values: { enabled: true, clientId: 'abc' } };
        service.saveConfig('h1', payload).subscribe(() => done());
        const req = httpMock.expectOne('/api/v1/dotauth/sites/h1');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(payload);
        req.flush({ entity: 'OK' });
    });

    it('clearConfig DELETEs the host row', (done) => {
        service.clearConfig('h1').subscribe(() => done());
        const req = httpMock.expectOne('/api/v1/dotauth/sites/h1');
        expect(req.request.method).toBe('DELETE');
        req.flush(null);
    });
});
