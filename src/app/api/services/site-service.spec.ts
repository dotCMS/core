import { fakeAsync, tick } from '@angular/core/testing';
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
            }
        });
    });

    it('should tigger switchSite', fakeAsync(() => {
        let currentCounter = 5;
        let newCurrentSite: Site;
        let loginService: LoginServiceMock = this.injector.get(LoginService);

        this.siteService = this.injector.get(SiteService);
        this.siteService.switchSite$.subscribe( site => newCurrentSite = site);

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
            eventType: 'eventType'
        };

        dotcmsEventsService.tiggerSubscribeTo('ARCHIVE_SITE', data);
        this.lastPaginateSiteConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify({
                entity: [
                    newCurrentSite
                ]
            })
        })));
        respondSwitchSiteRequest.bind(this)();

        tick();
        expect(newCurrentSite).toEqual(this.siteService.currentSite);
    }));

    function respondSwitchSiteRequest(): void {
        this.lastSwitchSiteConnection.mockRespond(new Response(new ResponseOptions({
            body: JSON.stringify({
                entity: {
                }
            })
        })));
    }
});