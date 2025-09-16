import { mockProvider } from '@ngneat/spectator/jest';
import { throwError } from 'rxjs';

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { getTestBed, TestBed } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotRouterService,
    DotAlertConfirmService,
    DotMessageService,
    DotFormatDateService
} from '@dotcms/data-access';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import {
    CoreWebServiceMock,
    LoginServiceMock,
    DotMessageDisplayServiceMock,
    MockDotRouterService,
    DotFormatDateServiceMock,
    mockResponseView
} from '@dotcms/utils-testing';

import { DotAddToMenuService, DotCreateCustomTool } from './add-to-menu.service';

const customToolData: DotCreateCustomTool = {
    contentTypes: 'Blog',
    dataViewMode: 'card',
    portletName: 'test'
};

describe('DotAddToMenuService', () => {
    let injector: TestBed;
    let dotAddToMenuService: DotAddToMenuService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let coreWebService: CoreWebService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                ConfirmationService,
                DotAddToMenuService,
                DotAlertConfirmService,
                DotHttpErrorManagerService,
                mockProvider(DotMessageService)
            ]
        });
        injector = getTestBed();
        dotAddToMenuService = injector.inject(DotAddToMenuService);
        dotHttpErrorManagerService = injector.inject(DotHttpErrorManagerService);
        coreWebService = injector.inject(CoreWebService);
        httpMock = injector.inject(HttpTestingController);
    });

    it('should clean up Portlet Id value', () => {
        const dirtyName = `test dirty 2 &! end`;
        const cleanName = dotAddToMenuService.cleanUpPorletId(dirtyName);
        expect(cleanName).toBe('test-dirty-2-end');
    });

    it('should create a custom tool portlet', () => {
        const url = `v1/portlet/custom`;

        dotAddToMenuService.createCustomTool(customToolData).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual({
            ...customToolData,
            portletId: `${customToolData.portletName}_${customToolData.dataViewMode}`
        });
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw null on create custom tool error 400', () => {
        const error404 = mockResponseView(400);
        jest.spyOn(dotHttpErrorManagerService, 'handle');
        jest.spyOn(coreWebService, 'requestView').mockReturnValue(throwError(error404));

        dotAddToMenuService.createCustomTool(customToolData).subscribe((response: string) => {
            expect(response).toEqual(null);
        });
        expect(dotHttpErrorManagerService.handle).not.toHaveBeenCalled();
    });

    it('should throw error 500 on create custom tool error', () => {
        const error404 = mockResponseView(500);
        jest.spyOn(dotHttpErrorManagerService, 'handle');
        jest.spyOn(coreWebService, 'requestView').mockReturnValue(throwError(error404));

        dotAddToMenuService.createCustomTool(customToolData).subscribe((response: string) => {
            expect(response).toEqual(null);
        });
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(500));
    });

    it('should add to layout a custom tool portlet', () => {
        const url = `v1/portlet/custom/c_${customToolData.portletName}_${customToolData.dataViewMode}/_addtolayout/123`;

        dotAddToMenuService
            .addToLayout({
                portletName: customToolData.portletName,
                dataViewMode: customToolData.dataViewMode,
                layoutId: '123'
            })
            .subscribe((response: string) => {
                expect(response).toEqual('ok');
            });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('PUT');
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw error 400 on add to layout custom portlet', () => {
        const error404 = mockResponseView(400);
        jest.spyOn(dotHttpErrorManagerService, 'handle');
        jest.spyOn(coreWebService, 'requestView').mockReturnValue(throwError(error404));

        dotAddToMenuService
            .addToLayout({
                portletName: customToolData.portletName,
                dataViewMode: customToolData.dataViewMode,
                layoutId: '123'
            })
            .subscribe((response: string) => {
                expect(response).toEqual(null);
            });
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(mockResponseView(400));
    });

    afterEach(() => {
        httpMock.verify();
    });
});
