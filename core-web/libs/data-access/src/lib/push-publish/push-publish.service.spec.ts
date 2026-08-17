import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/jest';
import { format } from 'date-fns';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ApiRoot, LoggerService, StringUtils, UserModel } from '@dotcms/dotcms-js';
import {
    DotAjaxActionResponseView,
    DotCurrentUser,
    DotPushPublishData
} from '@dotcms/dotcms-models';
import { DotFormatDateServiceMock } from '@dotcms/utils-testing';

import { PushPublishService } from './push-publish.service';

import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';
import { DotFormatDateService } from '../dot-format-date/dot-format-date.service';

const mockResponse: DotAjaxActionResponseView = {
    _body: {},
    errorMessages: [],
    total: 1,
    bundleId: '1234-id-7890-entifier',
    errors: 0
};

const mockFormValue: DotPushPublishData = {
    pushActionSelected: 'publish',
    publishDate: 'Wed Jul 08 2020 10:10:50',
    expireDate: undefined,
    environment: ['env1'],
    filterKey: 'hol',
    timezoneId: 'Costa Rica'
};

describe('PushPublishService', () => {
    let spectator: SpectatorHttp<PushPublishService>;
    let dotCurrentUserService: DotCurrentUserService;
    const createHttp = createHttpFactory({
        service: PushPublishService,
        mocks: [DotCurrentUserService],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
            ApiRoot,
            LoggerService,
            UserModel,
            StringUtils
        ]
    });

    beforeEach(() => {
        spectator = createHttp();
        dotCurrentUserService = spectator.inject(DotCurrentUserService);
    });

    it('should get push publish environments', () => {
        const user: DotCurrentUser = {
            admin: false,
            email: 'test@example.com',
            givenName: 'John',
            surname: 'Doe',
            userId: '1234',
            roleId: '1234'
        };
        jest.spyOn(dotCurrentUserService, 'getCurrentUser').mockReturnValue(of(user));

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

        spectator.service.getEnvironments().subscribe((items) => {
            expect(items).toEqual(response.splice(1));
        });

        spectator
            .expectOne('/api/environment/loadenvironments/roleId/1234', HttpMethod.GET)
            .flush(response);
    });

    it('should do a post request and push publish an asset', () => {
        const assetIdentifier = '1234567890 +0';
        const assetIdentifierEncoded = encodeURIComponent(assetIdentifier);
        spectator.service
            .pushPublishContent(assetIdentifier, mockFormValue, false)
            .subscribe((items) => {
                expect(items).toEqual(mockResponse);
            });

        const currentDateStr = new Date().toISOString().split('T')[0];
        const currentTimeStr = format(new Date(), 'HH-mm');

        const req = spectator.expectOne(
            '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
            HttpMethod.POST
        );

        expect(req.request.body).toBe(
            `assetIdentifier=${assetIdentifierEncoded}&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=${currentDateStr}&remotePublishExpireTime=${currentTimeStr}&timezoneId=Costa Rica&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&filterKey=hol`
        );
        req.flush(mockResponse);
    });

    it('should do a post request and push publish Remove an asset', () => {
        const formValue: DotPushPublishData = { ...mockFormValue, publishDate: undefined };
        const currentDateStr = new Date().toISOString().split('T')[0];
        const currentTimeStr = format(new Date(), 'HH-mm');

        spectator.service.pushPublishContent('1234567890', formValue, false).subscribe((items) => {
            expect(items).toEqual(mockResponse);
        });

        const req = spectator.expectOne(
            '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
            HttpMethod.POST
        );
        expect(req.request.body).toBe(
            `assetIdentifier=1234567890&remotePublishDate=${currentDateStr}&remotePublishTime=${currentTimeStr}&remotePublishExpireDate=${currentDateStr}&remotePublishExpireTime=${currentTimeStr}&timezoneId=Costa Rica&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&filterKey=hol`
        );
        req.flush(mockResponse);
    });

    it('should do a post request and push publish an asset with no filter', () => {
        const formValue: DotPushPublishData = { ...mockFormValue, filterKey: undefined };
        const currentDateStr = new Date().toISOString().split('T')[0];
        const currentTimeStr = format(new Date(), 'HH-mm');

        spectator.service.pushPublishContent('1234567890', formValue, false).subscribe((items) => {
            expect(items).toEqual(mockResponse);
        });

        const req = spectator.expectOne(
            '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
            HttpMethod.POST
        );

        expect(req.request.body).toBe(
            `assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=${currentDateStr}&remotePublishExpireTime=${currentTimeStr}&timezoneId=Costa Rica&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=`
        );
        req.flush(mockResponse);
    });

    it('should do a post request and push publish an asset with no filter', () => {
        const formValue: DotPushPublishData = { ...mockFormValue, filterKey: undefined };
        const currentDateStr = new Date().toISOString().split('T')[0];
        const currentTimeStr = format(new Date(), 'HH-mm');

        spectator.service.pushPublishContent('1234567890', formValue, false).subscribe((items) => {
            expect(items).toEqual(mockResponse);
        });

        const req = spectator.expectOne(
            '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
            HttpMethod.POST
        );
        expect(req.request.body).toBe(
            `assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=${currentDateStr}&remotePublishExpireTime=${currentTimeStr}&timezoneId=Costa Rica&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=`
        );

        req.flush(mockResponse);
    });

    it('should do a post with the correct URL when is a bundle', () => {
        const currentDateStr = new Date().toISOString().split('T')[0];
        const currentTimeStr = format(new Date(), 'HH-mm');

        spectator.service
            .pushPublishContent('1234567890', mockFormValue, true)
            .subscribe((items) => {
                expect(items).toEqual(mockResponse);
            });

        const req = spectator.expectOne(
            '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle',
            HttpMethod.POST
        );
        expect(req.request.body).toBe(
            `assetIdentifier=1234567890&remotePublishDate=2020-07-08&remotePublishTime=10-10&remotePublishExpireDate=${currentDateStr}&remotePublishExpireTime=${currentTimeStr}&timezoneId=Costa Rica&iWantTo=publish&whoToSend=env1&bundleName=&bundleSelect=&filterKey=hol`
        );
        req.flush(mockResponse);
    });

    describe('pushPublishAssets', () => {
        const SETTINGS = {
            whereToSend: 'env1,env2',
            iWantTo: 'publish' as const,
            publishDate: '2026-09-01',
            publishTime: '10-00',
            expireDate: '2026-10-01',
            expireTime: '23-59',
            filterKey: 'hol',
            timezoneId: 'Costa Rica'
        };

        it('should post the split date fields through untouched', () => {
            // The whole point of this method over `pushPublishContent`: the payload arrives already
            // in the servlet's shape, so nothing is re-parsed and the chosen timezone survives.
            spectator.service.pushPublishAssets('id-1', SETTINGS).subscribe((items) => {
                expect(items).toEqual(mockResponse);
            });

            const req = spectator.expectOne(
                '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
                HttpMethod.POST
            );

            expect(req.request.body).toBe(
                'assetIdentifier=id-1&remotePublishDate=2026-09-01&remotePublishTime=10-00' +
                    '&remotePublishExpireDate=2026-10-01&remotePublishExpireTime=23-59' +
                    '&timezoneId=Costa Rica&iWantTo=publish&whoToSend=env1,env2' +
                    '&bundleName=&bundleSelect=&filterKey=hol'
            );
            req.flush(mockResponse);
        });

        it('should send several identifiers comma-joined', () => {
            // `RemotePublishAjaxAction` splits `assetIdentifier` on "," — bulk needs no new endpoint.
            spectator.service.pushPublishAssets('id-1,id-2', SETTINGS).subscribe();

            const req = spectator.expectOne(
                '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
                HttpMethod.POST
            );

            expect(req.request.body).toContain('assetIdentifier=id-1%2Cid-2');
            req.flush(mockResponse);
        });

        it('should omit the filter key when there is none', () => {
            // Expiring takes no filter, so an empty value is a real case. The servlet reads the
            // parameter straight into `getFilterDescriptorByKey`, so it is left out rather than
            // sent blank.
            spectator.service.pushPublishAssets('id-1', { ...SETTINGS, filterKey: '' }).subscribe();

            const req = spectator.expectOne(
                '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
                HttpMethod.POST
            );

            expect(req.request.body).not.toContain('filterKey');
            req.flush(mockResponse);
        });

        it('should record the environments it last pushed to', () => {
            spectator.service.pushPublishAssets('id-1', SETTINGS).subscribe();

            spectator
                .expectOne(
                    '/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/publish',
                    HttpMethod.POST
                )
                .flush(mockResponse);

            expect(spectator.service.lastEnvironmentPushed).toEqual(['env1', 'env2']);
        });
    });
});
