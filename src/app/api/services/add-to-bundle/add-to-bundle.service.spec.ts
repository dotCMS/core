import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of as observableOf } from 'rxjs';
import { AddToBundleService } from './add-to-bundle.service';
import { DotCurrentUserService } from '../dot-current-user/dot-current-user.service';
import { ApiRoot, UserModel, LoggerService, StringUtils, CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { DotAjaxActionResponseView } from '@shared/models/ajax-action-response/dot-ajax-action-response';

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
        spyOn(dotCurrentUserService, 'getCurrentUser').and.returnValue(
            observableOf({
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
            idenfitier: 'id',
            items: mockBundleItems,
            label: 'name',
            numRows: 2
        };

        addToBundleService.getBundles().subscribe((items: any) => {
            expect(items).toBe(mockResponse.items);
        });

        const req = httpMock.expectOne('bundle/getunsendbundles/userid/1234');
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
