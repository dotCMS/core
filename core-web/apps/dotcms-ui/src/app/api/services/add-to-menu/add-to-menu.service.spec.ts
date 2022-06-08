import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '@tests/login-service.mock';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';

import { DotFormatDateServiceMock } from '@dotcms/app/test/format-date-service.mock';
import { DotAddToMenuService, DotCreateCustomTool } from './add-to-menu.service';
import { mockResponseView } from '@dotcms/app/test/response-view.mock';
import { throwError } from 'rxjs';

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
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                ConfirmationService,
                DotAddToMenuService,
                DotAlertConfirmService,
                DotHttpErrorManagerService
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
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

        dotAddToMenuService.createCustomTool(customToolData).subscribe((response: string) => {
            expect(response).toEqual(null);
        });
        expect(dotHttpErrorManagerService.handle).not.toHaveBeenCalled();
    });

    it('should throw error 500 on create custom tool error', () => {
        const error404 = mockResponseView(500);
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

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
        spyOn(dotHttpErrorManagerService, 'handle').and.callThrough();
        spyOn(coreWebService, 'requestView').and.returnValue(throwError(error404));

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
