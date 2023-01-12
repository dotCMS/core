/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { ApiRoot, UserModel, LoggerService, StringUtils, CoreWebService } from '@dotcms/dotcms-js';
import { DotAjaxActionResponseView, DotCurrentUser } from '@dotcms/dotcms-models';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { AddToBundleService } from './add-to-bundle.service';

import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';


describe('AddToBundleService', () => {
    let injector: TestBed;
    let addToBundleService: AddToBundleService;
    let dotCurrentUserService: DotCurrentUserService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                AddToBundleService,
                DotCurrentUserService
            ]
        });
        injector = getTestBed();
        addToBundleService = injector.get(AddToBundleService);
        dotCurrentUserService = injector.get(DotCurrentUserService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get bundle list', () => {
        jest.spyOn(dotCurrentUserService, 'getCurrentUser').mockReturnValue(
            of(<DotCurrentUser>{
                userId: '1234'
            })
        );

        const mockBundleItems = [
            {
                name: 'My bundle',
                id: '1234'
            },
            {
                name: 'My bundle 2',
                id: '1sdf5-23fs-dsf2-sf3oj23p4p42d'
            }
        ];

        const mockResponse = {
            idenitier: 'id',
            items: mockBundleItems,
            label: 'name',
            numRows: 2
        };

        addToBundleService.getBundles().subscribe((items: any) => {
            expect(items).toBe(mockResponse.items);
        });

        const req = httpMock.expectOne('api/bundle/getunsendbundles/userid/1234');
        expect(req.request.method).toBe('GET');
        req.flush(mockResponse);
    });

    it('should do a post request and add to bundle', () => {
        const mockResponse = {
            errorMessages: [],
            total: 1,
            bundleId: '1234-id-7890-entifier',
            errors: 0,
            _body: {}
        };

        const mockBundleData = {
            id: '1234',
            name: 'my bundle'
        };

        const assetIdentifier = '1234567890';

        addToBundleService
            .addToBundle(assetIdentifier, mockBundleData)
            .subscribe((action: DotAjaxActionResponseView) => {
                expect(action).toEqual(mockResponse);
            });

        const req = httpMock.expectOne((_req) => true);
        expect(
            req.request.url.indexOf(
                'DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle'
            )
        ).toBeGreaterThan(-1);
        expect(req.request.method).toEqual('POST');
        expect(req.request.body).toEqual(
            `assetIdentifier=${assetIdentifier}&bundleName=${mockBundleData.name}&bundleSelect=${mockBundleData.id}`
        );
        req.flush(mockResponse);
    });

    afterEach(() => {
        httpMock.verify();
    });
});
