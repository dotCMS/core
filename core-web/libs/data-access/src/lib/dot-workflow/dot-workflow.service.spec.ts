/* eslint-disable @typescript-eslint/no-explicit-any */

/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed, getTestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock, mockWorkflows } from '@dotcms/utils-testing';

import { DotWorkflowService } from './dot-workflow.service';

describe('DotWorkflowService', () => {
    let injector: TestBed;
    let dotWorkflowService: DotWorkflowService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotWorkflowService
            ]
        });
        injector = getTestBed();
        dotWorkflowService = injector.get(DotWorkflowService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get workflows', () => {
        dotWorkflowService.get().subscribe((res: any) => {
            expect(res).toEqual([
                {
                    hello: 'world',
                    hola: 'mundo'
                }
            ]);
        });

        const req = httpMock.expectOne('v1/workflow/schemes');
        expect(req.request.method).toBe('GET');
        req.flush({
            entity: [
                {
                    hello: 'world',
                    hola: 'mundo'
                }
            ]
        });
    });

    it('should get default workflow', () => {
        const defaultSystemWorkflow = mockWorkflows.filter((workflow) => workflow.system);

        dotWorkflowService.getSystem().subscribe((res: any) => {
            expect(res).toEqual(defaultSystemWorkflow[0]);
        });

        httpMock.expectOne('v1/workflow/schemes');
    });

    afterEach(() => {
        httpMock.verify();
    });
});
