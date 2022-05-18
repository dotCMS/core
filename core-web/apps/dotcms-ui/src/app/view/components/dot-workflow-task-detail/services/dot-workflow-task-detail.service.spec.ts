import { of as observableOf } from 'rxjs';
import { DotWorkflowTaskDetailService } from './dot-workflow-task-detail.service';
import { DotMenuService } from '@services/dot-menu.service';
import { TestBed } from '@angular/core/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('DotWorkflowTaskDetailService', () => {
    let service: DotWorkflowTaskDetailService;
    let dotMenuService: DotMenuService;
    let injector;

    beforeEach(() => {
        injector = TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                DotMenuService,
                DotWorkflowTaskDetailService,
                { provide: CoreWebService, useClass: CoreWebServiceMock }
            ]
        });

        service = injector.get(DotWorkflowTaskDetailService);
        dotMenuService = injector.get(DotMenuService);
        spyOn(dotMenuService, 'getDotMenuId').and.returnValue(observableOf('456'));
    });

    it('should set data to view', (done) => {
        service.view({
            header: 'This is a header for view',
            id: '999'
        });
        service.viewUrl$.subscribe((url: string) => {
            expect(url).toEqual(
                [
                    `/c/portal/layout`,
                    `?p_l_id=456`,
                    `&p_p_id=workflow`,
                    `&p_p_action=1`,
                    `&p_p_state=maximized`,
                    `&p_p_mode=view`,
                    `&_workflow_struts_action=/ext/workflows/edit_workflow_task`,
                    `&_workflow_cmd=view`,
                    `&_workflow_taskId=999`
                ].join('')
            );

            expect(dotMenuService.getDotMenuId).toHaveBeenCalledWith('workflow');
            done();
        });

        service.header$.subscribe((header: string) => {
            expect(header).toEqual('This is a header for view');
        });
    });

    it('should clear url and undbind', () => {
        service.viewUrl$.subscribe((url: string) => {
            expect(url).toEqual('');
        });

        service.clear();
    });
});
