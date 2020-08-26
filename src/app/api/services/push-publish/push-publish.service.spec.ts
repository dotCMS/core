import { of as observableOf } from 'rxjs';
import { PushPublishService } from './push-publish.service';
import {
    ConnectionBackend,
    ResponseOptions,
    Response,
    RequestOptions,
    BaseRequestOptions,
    Http
} from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';
import { DotPushPublishData } from '@models/dot-push-publish-data/dot-push-publish-data';
import { ApiRoot, CoreWebService, LoggerService, StringUtils, UserModel } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

const mockResponse = {
    errorMessages: [],
    total: 1,
    bundleId: '1234-id-7890-entifier',
    errors: 0
};

const mockFormValue: DotPushPublishData = {
    pushActionSelected: 'publish',
    publishDate: 'Wed Jul 08 2020 10:10:50',
    expireDate: 'Wed Jul 15 2020 22:10:50',
    environment: ['env1'],
    filterKey: 'hol'
};

describe('PushPublishService', () => {
    let pushPublishService: PushPublishService;
    let dotCurrentUserService: DotCurrentUserService;
    let connectionBackend: MockBackend;
    let lastConnection;

    beforeEach(() => {
        this.injector = TestBed.configureTestingModule({
            providers: [
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                Http,
                PushPublishService,
                DotCurrentUserService,
                ApiRoot,
                LoggerService,
                UserModel,
                StringUtils
            ]
        });

        pushPublishService = TestBed.get(PushPublishService);
        dotCurrentUserService = TestBed.get(DotCurrentUserService);
        connectionBackend = TestBed.get(ConnectionBackend);
        connectionBackend.connections.subscribe((connection: any) => (lastConnection = connection));
    });

    it(
        'should get push publish environments',
        fakeAsync(() => {
            spyOn(dotCurrentUserService, 'getCurrentUser').and.returnValue(
                observableOf({
                    roleId: '1234'
                })
            );

            const response = [
                {
                    name: '',
                    id: '0'
                },
                {
                    name: 'environment1',
                    id: '1sdf5-23fs-dsf2-sf3oj23p4p42d'
                },
                {
                    name: 'environment2',
                    id: '1s24z-23fs-d232-sf334fdf4p42d'
                }
            ];

            let result: any;
            pushPublishService.getEnvironments().subscribe(items => (result = items));
            lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: JSON.stringify(response)
                    })
                )
            );

            tick();
            expect(lastConnection.request.url).toContain(
                'environment/loadenvironments/roleId/1234/name=0'
            );
            expect(result).toEqual(response.splice(1));
        })
    );

    it(
        'should do a post request and push publish an asset',
        fakeAsync(() => {
            let result: any;
            pushPublishService
                .pushPublishContent('1234567890', mockFormValue, false)
                .subscribe(res => {
                    result = res._body;
                });
            lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockResponse
                    })
                )
            );
            tick();
            expect(lastConnection.request.url).toContain(
                'DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish'
            );
            expect(lastConnection.request.getBody()).toBe(
                // tslint:disable-next-line:max-line-length
                'assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=2020-07-15&remotePublishExpireTime=22-10&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&filterKey=hol'
            );
            expect(result).toEqual(mockResponse);
        })
    );

    it(
        'should do a post request and push publish an asset with no filter',
        fakeAsync(() => {
            const formValue: DotPushPublishData = { ...mockFormValue, filterKey: null };

            pushPublishService.pushPublishContent('1234567890', formValue, false).subscribe();
            lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockResponse
                    })
                )
            );
            tick();
            expect(lastConnection.request.getBody()).toBe(
                // tslint:disable-next-line:max-line-length
                'assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=2020-07-15&remotePublishExpireTime=22-10&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect='
            );
        })
    );

    it(
        'should do a post with the correct URL when is a bundle',
        fakeAsync(() => {
            let result: any;
            pushPublishService
                .pushPublishContent('1234567890', mockFormValue, true)
                .subscribe(res => {
                    result = res._body;
                });
            lastConnection.mockRespond(
                new Response(
                    new ResponseOptions({
                        body: mockResponse
                    })
                )
            );
            tick();
            expect(lastConnection.request.url).toContain(
                'DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle'
            );
            expect(lastConnection.request.getBody()).toBe(
                // tslint:disable-next-line:max-line-length
                'assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=2020-07-15&remotePublishExpireTime=22-10&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&filterKey=hol'
            );
            expect(result).toEqual(mockResponse);
        })
    );
});
