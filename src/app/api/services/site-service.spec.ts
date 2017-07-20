import { fakeAsync, tick, async } from '@angular/core/testing';
import {
  Response,
  ResponseOptions,
  ConnectionBackend,
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { CoreWebService } from './core-web-service';
import { DOTTestBed } from '../../test/dot-test-bed';
import { RequestMethod } from '@angular/http';
import { LoginService } from './login-service';
import { DotcmsEventsService } from './dotcms-events-service';
import { DotcmsEventsServiceMock } from '../../test/dotcms-events-service.mock';
import { LoginServiceMock } from '../../test/login-service.mock';
import { SiteService, Site } from './site-service';
import { Observable } from "rxjs/Observable";

describe('Site Service', () => {
    let currentSite: Site =  {
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
            let url = connection.request.url;

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

    it('should tigger switchSite', fakeAsync(() => {
        let currentCounter = 5;
        let newCurrentSite: Site;
        let loginService: LoginServiceMock = this.injector.get(LoginService);

        this.siteService.switchSite$.subscribe(site => newCurrentSite = site);

        let mockResponse = {
                entity: {
                    currentSite: currentSite,
                    totalRecords: currentCounter,
                }
            };

        loginService.tiggerWatchUser();

        this.lastCurrentSiteConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify(mockResponse)
        })));

        tick();
        expect(this.lastCurrentSiteConnection.request.url).toContain('v1/site/currentSite');
        expect(currentSite).toEqual(mockResponse.entity.currentSite);
        expect(currentCounter).toEqual(mockResponse.entity.totalRecords);
    }));

    it('should switch site', fakeAsync(() => {
        this.siteService.switchSite(currentSite);
        respondSwitchSiteRequest.bind(this)();

        tick();

        expect(this.lastSwitchSiteConnection.request.url).toContain('v1/site/switch');
        expect(currentSite).toEqual(this.siteService.currentSite);
    }));

    it('should switch site when a ARCHIVE_SITE event happend', fakeAsync(() => {
        let newCurrentSite: Site =  {
            hostname: 'hostname2',
            identifier: '6',
            type: 'type2'
        };

        this.siteService.switchSite(currentSite);
        respondSwitchSiteRequest.bind(this)();

        let dotcmsEventsService: DotcmsEventsServiceMock = this.injector.get(DotcmsEventsService);

        let data = {
            data: {
                data: {
                    identifier: '5'
                }
            },
            eventType: 'ARCHIVE_SITE'
        };

        dotcmsEventsService.tiggerSubscribeTo('ARCHIVE_SITE', data);
        tick();
        this.lastPaginateSiteConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify({
                entity: [
                    newCurrentSite
                ]
            })
        })));
        respondSwitchSiteRequest.bind(this)();

        expect(newCurrentSite).toEqual(this.siteService.currentSite);
    }));

    it('should refresh sites when an event happend', fakeAsync(() => {
        let events: string[] = ['SAVE_SITE', 'PUBLISH_SITE', 'UPDATE_SITE_PERMISSIONS', 'UN_ARCHIVE_SITE', 'UPDATE_SITE', 'ARCHIVE_SITE'];
        let dotcmsEventsService: DotcmsEventsServiceMock = this.injector.get(DotcmsEventsService);
        let siteService = this.injector.get(SiteService);
        let data = {
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
    }));

    it('get a site by id', () => {
        this.siteService.getSiteById('123').subscribe(res => {
            expect(res).toEqual({ hostname: 'hello.host.com', identifier: '123' });
        });

        this.lastContentApiConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify({
                'contentlets': [
                    {
                        'hostname': 'hello.host.com',
                        'identifier': '123'
                    }
                ]
            })
        })));
    });

    function respondSwitchSiteRequest(): void {
        this.lastSwitchSiteConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify({
                entity: {
                }
            })
        })));
    }
});