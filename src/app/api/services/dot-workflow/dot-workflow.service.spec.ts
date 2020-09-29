import { DotWorkflowService } from './dot-workflow.service';
import { mockWorkflows } from '../../../test/dot-workflow-service.mock';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';

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
