import {inject, TestBed} from '@angular/core/testing';
import {BaseRequestOptions, ConnectionBackend, Http, ResponseOptions, Response} from '@angular/http';
import {MockBackend, MockConnection} from '@angular/http/testing';

import {JWTAuthService} from './jwt-auth.service';
import {LocalStoreService} from './local-store.service';
import {LoggerService} from './logger.service';
import {Logger} from 'angular2-logger/core';
import {NotificationService} from './notification.service';
import {SettingsStorageService} from './settings-storage.service';
import {SiteBrowserState} from './site-browser.state';
import {AppConfig} from '../app.config';

describe('JWT Auth Service', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                BaseRequestOptions,
                MockBackend,
                Logger,
                {
                    deps: [MockBackend, BaseRequestOptions],
                    provide: Http,
                    useFactory: function(backend: ConnectionBackend, defaultOptions: BaseRequestOptions): Http {
                        return new Http(backend, defaultOptions);
                    }
                },
                {provide: AppConfig, useValue: AppConfig},
                {provide: JWTAuthService, useClass: JWTAuthService},
                {provide: LoggerService, useClass: LoggerService},
                {provide: NotificationService, useClass: NotificationService},
                {provide: SettingsStorageService, useClass: SettingsStorageService},
                {provide: LocalStoreService, useClass: LocalStoreService},
                {provide: SiteBrowserState, useClass: SiteBrowserState}
            ],
        });
    });

    let service: JWTAuthService;
    let backend: MockBackend;
    let store: SettingsStorageService;

    beforeEach(inject([JWTAuthService, MockBackend, SettingsStorageService], (jWTAuthService: JWTAuthService, mockBackend: MockBackend, settingsStorageService: SettingsStorageService) => {
        service = jWTAuthService;
        backend = mockBackend;
        store = settingsStorageService;
    }));

    it('should login and get auth token', (done) => {
        backend.connections.subscribe((connection: MockConnection) => {
            let options = new ResponseOptions({
                body: JSON.stringify({
                    'entity': {
                        'token': 'This is a Auth Token'
                    },
                }),
                status: 200
            });
            let a = new Response(options);
            connection.mockRespond(a);
        });

        spyOn(store, 'storeSettings');
        service.login('http://demo37.dotcms.com', 'admin@dotcms.com', 'admin').subscribe(res => {
            expect(res).toBe('This is a Auth Token');
            expect(store.storeSettings).toHaveBeenCalledWith('http://demo37.dotcms.com', 'This is a Auth Token');
            done();
        });
    });

    it('should fail at login', (done) => {
        backend.connections.subscribe((connection: MockConnection) => {
            let options = new ResponseOptions({
                body: JSON.stringify({
                    'errors': [{
                        'errorCode': 'authentication-failed',
                        'message': 'Authentication failed'
                    }]
                }),
                status: 401,
                statusText: 'Unauthorized',
            });
            let mockedRes = new Response(options);
            mockedRes.ok = false;
            connection.mockRespond(mockedRes);
        });

        service.login('http://demo37.dotcms.com', 'admin@dotcms.com', 'admin').subscribe(res => {
        }, (err) => {
            console.log(err);
            done();
        });
    });
});