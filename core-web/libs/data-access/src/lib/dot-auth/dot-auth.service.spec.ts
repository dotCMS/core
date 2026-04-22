import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotAuthConfigPayload, DotAuthConfigView, DotAuthSitesView } from '@dotcms/dotcms-models';

import { DotAuthService } from './dot-auth.service';

describe('DotAuthService', () => {
    let spectator: SpectatorHttp<DotAuthService>;

    const createService = createHttpFactory(DotAuthService);

    beforeEach(() => {
        spectator = createService();
    });

    describe('listSites', () => {
        it('GETs /api/v1/dotauth/sites and unwraps the entity envelope', () => {
            const fake: DotAuthSitesView = {
                system: { configured: false },
                sites: [{ hostId: 'h1', hostName: 'default', status: 'NOT_CONFIGURED' }]
            };

            let received: DotAuthSitesView | undefined;
            spectator.service.listSites().subscribe((view) => (received = view));

            const req = spectator.expectOne('/api/v1/dotauth/sites', HttpMethod.GET);
            req.flush({ entity: fake });

            expect(received).toEqual(fake);
        });

        it('propagates HTTP errors to the caller', () => {
            let caught: unknown;
            spectator.service.listSites().subscribe({
                error: (err) => (caught = err)
            });

            spectator
                .expectOne('/api/v1/dotauth/sites', HttpMethod.GET)
                .flush({}, { status: 500, statusText: 'Server Error' });

            expect(caught).toBeInstanceOf(HttpErrorResponse);
            expect((caught as HttpErrorResponse).status).toBe(500);
        });
    });

    describe('getConfig', () => {
        it('GETs /api/v1/dotauth/sites/{hostId} and unwraps the entity envelope', () => {
            const fake: DotAuthConfigView = {
                hostId: 'SYSTEM_HOST',
                configured: true,
                inherited: false,
                values: { enabled: true }
            };

            let received: DotAuthConfigView | undefined;
            spectator.service
                .getConfig('SYSTEM_HOST')
                .subscribe((view) => (received = view));

            const req = spectator.expectOne(
                '/api/v1/dotauth/sites/SYSTEM_HOST',
                HttpMethod.GET
            );
            req.flush({ entity: fake });

            expect(received).toEqual(fake);
        });

        it('propagates 404 when the host row does not exist', () => {
            let caught: HttpErrorResponse | undefined;
            spectator.service.getConfig('missing').subscribe({
                error: (err: HttpErrorResponse) => (caught = err)
            });

            spectator
                .expectOne('/api/v1/dotauth/sites/missing', HttpMethod.GET)
                .flush({}, { status: 404, statusText: 'Not Found' });

            expect(caught?.status).toBe(404);
        });
    });

    describe('saveConfig', () => {
        it('PUTs the payload to /api/v1/dotauth/sites/{hostId}', () => {
            const payload: DotAuthConfigPayload = {
                values: { enabled: true, clientId: 'abc' }
            };

            spectator.service.saveConfig('h1', payload).subscribe();

            const req = spectator.expectOne('/api/v1/dotauth/sites/h1', HttpMethod.PUT);
            expect(req.request.body).toEqual(payload);
            req.flush({ entity: 'OK' });
        });

        it('surfaces a 400 validation failure to the caller', () => {
            let caught: HttpErrorResponse | undefined;
            spectator.service
                .saveConfig('h1', { values: {} })
                .subscribe({
                    error: (err: HttpErrorResponse) => (caught = err)
                });

            spectator
                .expectOne('/api/v1/dotauth/sites/h1', HttpMethod.PUT)
                .flush({}, { status: 400, statusText: 'Bad Request' });

            expect(caught?.status).toBe(400);
        });
    });

    describe('clearConfig', () => {
        it('DELETEs /api/v1/dotauth/sites/{hostId}', () => {
            spectator.service.clearConfig('h1').subscribe();

            const req = spectator.expectOne('/api/v1/dotauth/sites/h1', HttpMethod.DELETE);
            req.flush(null);
        });

        it('surfaces a 403 when the user cannot edit the site', () => {
            let caught: HttpErrorResponse | undefined;
            spectator.service.clearConfig('h1').subscribe({
                error: (err: HttpErrorResponse) => (caught = err)
            });

            spectator
                .expectOne('/api/v1/dotauth/sites/h1', HttpMethod.DELETE)
                .flush({}, { status: 403, statusText: 'Forbidden' });

            expect(caught?.status).toBe(403);
        });
    });
});
