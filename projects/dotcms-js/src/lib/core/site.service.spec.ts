import { fakeAsync, tick } from '@angular/core/testing';
import { Response, ResponseOptions, ConnectionBackend } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { Site } from './treeable/shared/site.model';
import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { LoginService } from './login.service';
import { LoginServiceMock } from 'src/app/test/login-service.mock';
import { DotcmsEventsService } from 'dotcms-js/dotcms-js';
import { DotcmsEventsServiceMock } from 'src/app/test/dotcms-events-service.mock';
import { SiteService } from './site.service';

describe('Site Service', () => {
    const currentSite: Site = {
        hostname: 'hostname',
        identifier: '5',
        type: 'type'
    };

    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: DotcmsEventsService, useClass: DotcmsEventsServiceMock },
            SiteService
        ]);

        this.siteService = this.injector.get(SiteService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => {
            this.lastConnection = connection;
            const url = connection.request.url;

            if (url.indexOf('v1/site/currentSite') !== -1) {
                this.lastCurrentSiteConnection = connection;
            } else if (url.indexOf('v1/site/switch') !== -1) {
                this.lastSwitchSiteConnection = connection;
            } else if (url.indexOf('v1/site') !== -1) {
                this.lastPaginateSiteConnection = connection;
            } else if (url.indexOf('content') !== -1) {
                this.lastContentApiConnection = connection;
            }
        });
    });

    it(
        'should tigger switchSite',
        fakeAsync(() => {
            const currentCounter = 5;
            let newCurrentSite: Site;
            const loginService: LoginServiceMock = this.injector.get(LoginService);

            this.siteService.switchSite$.subscribe((site) => (newCurrentSite = site));

            const mockResponse = {
                entity: {
                    currentSite: currentSite,
                    totalRecords: currentCounter
                }
            };

            loginService.tiggerWatchUser();

            this.lastCurrentSiteConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: JSON.stringify(mockResponse)
                    })
                )
            );

            tick();
            expect(this.lastCurrentSiteConnection.request.url).toContain('v1/site/currentSite');
            expect(currentSite).toEqual(mockResponse.entity.currentSite);
            expect(currentCounter).toEqual(mockResponse.entity.totalRecords);
        })
    );

    it(
        'should switch site',
        fakeAsync(() => {
            this.siteService.switchSite(currentSite);
            respondSwitchSiteRequest.bind(this)();

            tick();

            expect(this.lastSwitchSiteConnection.request.url).toContain('v1/site/switch');
            expect(currentSite).toEqual(this.siteService.currentSite);
        })
    );

    it(
        'should refresh sites when an event happend',
        fakeAsync(() => {
            const events: string[] = [
                'SAVE_SITE',
                'PUBLISH_SITE',
                'UPDATE_SITE_PERMISSIONS',
                'UN_ARCHIVE_SITE',
                'UPDATE_SITE',
                'ARCHIVE_SITE'
            ];
            const dotcmsEventsService: DotcmsEventsServiceMock = this.injector.get(
                DotcmsEventsService
            );
            const siteService = this.injector.get(SiteService);
            const data = {
                data: {
                    data: {
                        identifier: '5'
                    }
                },
                eventType: 'ARCHIVE_SITE'
            };

            this.siteService.switchSite(currentSite);

            respondSwitchSiteRequest.bind(this)();

            spyOn(siteService, 'siteEventsHandler');

            dotcmsEventsService.triggerSubscribeToEvents(events, data);

            tick();

            expect(siteService.siteEventsHandler).toHaveBeenCalledWith(data);
        })
    );

    it('get a site by id', () => {
        this.siteService.getSiteById('123').subscribe((res) => {
            expect(res).toEqual({ hostname: 'hello.host.com', identifier: '123' });
        });

        this.lastContentApiConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: JSON.stringify({
                        contentlets: [
                            {
                                hostname: 'hello.host.com',
                                identifier: '123'
                            }
                        ]
                    })
                })
            )
        );
    });

    it('should fire switchToDefaultSite', () => {
        spyOn(this.siteService, 'switchToDefaultSite').and.callThrough();
        this.siteService.switchToDefaultSite().subscribe();
        expect(this.siteService.switchToDefaultSite).toHaveBeenCalledTimes(1);
        expect(this.lastConnection.request.url).toContain(`v1/site/switch`);
        expect(2).toBe(this.lastConnection.request.method); // 2 is a PUT method
    });

    function respondSwitchSiteRequest(): void {
        this.lastSwitchSiteConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: JSON.stringify({
                        entity: {}
                    })
                })
            )
        );
    }
});
