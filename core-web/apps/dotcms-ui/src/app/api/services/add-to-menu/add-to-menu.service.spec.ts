import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { MockDotHttpErrorManagerService } from '@dotcms/utils-testing';

import {
    DotAddToMenuService,
    DotCreateCustomTool,
    DotCustomToolToLayout
} from './add-to-menu.service';

const customToolData: DotCreateCustomTool = {
    contentTypes: 'Blog',
    dataViewMode: 'card',
    portletName: 'test'
};

describe('DotAddToMenuService', () => {
    let dotAddToMenuService: DotAddToMenuService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let httpTesting: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotAddToMenuService,
                {
                    provide: DotHttpErrorManagerService,
                    useClass: MockDotHttpErrorManagerService
                }
            ]
        });

        dotAddToMenuService = TestBed.inject(DotAddToMenuService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
        httpTesting = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should clean up Portlet Id value', () => {
        const dirtyName = `test dirty 2 &! end`;
        const cleanName = dotAddToMenuService.cleanUpPorletId(dirtyName);
        expect(cleanName).toBe('test-dirty-2-end');
    });

    it('should create a custom tool portlet', () => {
        dotAddToMenuService.createCustomTool(customToolData).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        const req = httpTesting.expectOne('/api/v1/portlet/custom');
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
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        dotAddToMenuService.createCustomTool(customToolData).subscribe((response: string) => {
            expect(response).toEqual(null);
        });

        const req = httpTesting.expectOne('/api/v1/portlet/custom');
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).not.toHaveBeenCalled();
    });

    it('should throw error 500 on create custom tool error', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        dotAddToMenuService.createCustomTool(customToolData).subscribe((response: string) => {
            expect(response).toEqual(null);
        });

        const req = httpTesting.expectOne('/api/v1/portlet/custom');
        req.flush(null, { status: 500, statusText: 'Internal Server Error' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });

    it('should add to layout a custom tool portlet', () => {
        const layoutData: DotCustomToolToLayout = {
            portletName: customToolData.portletName,
            dataViewMode: customToolData.dataViewMode,
            layoutId: '123'
        };

        dotAddToMenuService.addToLayout(layoutData).subscribe((response: string) => {
            expect(response).toEqual('ok');
        });

        const req = httpTesting.expectOne(
            `/api/v1/portlet/custom/c_${customToolData.portletName}_${customToolData.dataViewMode}/_addtolayout/123`
        );
        expect(req.request.method).toBe('PUT');
        req.flush({
            entity: 'ok'
        });
    });

    it('should throw error 400 on add to layout custom portlet', () => {
        jest.spyOn(dotHttpErrorManagerService, 'handle');

        const layoutData: DotCustomToolToLayout = {
            portletName: customToolData.portletName,
            dataViewMode: customToolData.dataViewMode,
            layoutId: '123'
        };

        dotAddToMenuService.addToLayout(layoutData).subscribe((response: string) => {
            expect(response).toEqual(null);
        });

        const req = httpTesting.expectOne(
            `/api/v1/portlet/custom/c_${customToolData.portletName}_${customToolData.dataViewMode}/_addtolayout/123`
        );
        req.flush(null, { status: 400, statusText: 'Bad Request' });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalled();
    });
});
