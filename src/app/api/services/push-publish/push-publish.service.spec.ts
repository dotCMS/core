import { of as observableOf } from 'rxjs';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { PushPublishService } from './push-publish.service';
import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';
import { DotPushPublishData } from '@models/dot-push-publish-data/dot-push-publish-data';

const mockResponse = {
    errorMessages: [],
    total: 1,
    bundleId: '1234-id-7890-entifier',
    errors: 0
};

const mockFormValue: DotPushPublishData = {
    pushActionSelected: 'publish',
    publishdate: 'Wed Jul 08 2020 10:10:50',
    expiredate: 'Wed Jul 15 2020 22:10:50',
    environment: ['env1'],
    forcePush: true,
    filterKey: 'hol'
};

describe('PushPublishService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([PushPublishService, DotCurrentUserService]);

        this.pushPublishService = this.injector.get(PushPublishService);
        this.dotCurrentUserService = this.injector.get(DotCurrentUserService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => (this.lastConnection = connection));
    });

    it('should get push publish environments', fakeAsync(() => {
        spyOn(this.dotCurrentUserService, 'getCurrentUser').and.returnValue(
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
        this.pushPublishService.getEnvironments().subscribe((items) => (result = items));
        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: JSON.stringify(response)
                })
            )
        );

        tick();
        expect(this.lastConnection.request.url).toContain(
            'environment/loadenvironments/roleId/1234/name=0'
        );
        expect(result).toEqual(response.splice(1));
    }));

    it('should do a post request and push publish an asset', fakeAsync(() => {
        let result: any;
        this.pushPublishService.pushPublishContent('1234567890', mockFormValue).subscribe((res) => {
            result = res._body;
        });
        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockResponse
                })
            )
        );
        tick();
        expect(this.lastConnection.request.url).toContain(
            'DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish'
        );
        expect(this.lastConnection.request.getBody()).toBe(
            // tslint:disable-next-line:max-line-length
            'assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=2020-07-15&remotePublishExpireTime=22-10&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&forcePush=true&filterKey=hol'
        );
        expect(result).toEqual(mockResponse);
    }));

    it('should do a post request and push publish an asset with no filter', fakeAsync(() => {
        const { filterKey, ...formValue } = mockFormValue;

        this.pushPublishService.pushPublishContent('1234567890', formValue).subscribe();
        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockResponse
                })
            )
        );
        tick();
        expect(this.lastConnection.request.getBody()).toBe(
            // tslint:disable-next-line:max-line-length
            'assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=2020-07-15&remotePublishExpireTime=22-10&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&forcePush=true'
        );
    }));

    it('should do a post with the correct URL when is a bundle', fakeAsync(() => {
        let result: any;
        this.pushPublishService
            .pushPublishContent('1234567890', mockFormValue, true)
            .subscribe((res) => {
                result = res._body;
            });
        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: mockResponse
                })
            )
        );
        tick();
        expect(this.lastConnection.request.url).toContain(
            'DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle'
        );
        expect(this.lastConnection.request.getBody()).toBe(
            // tslint:disable-next-line:max-line-length
            'assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=2020-07-15&remotePublishExpireTime=22-10&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&forcePush=true&filterKey=hol'
        );
        expect(result).toEqual(mockResponse);
    }));
});
