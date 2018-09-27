import { of as observableOf } from 'rxjs';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotWorkflowTaskDetailService } from './dot-workflow-task-detail.service';
import { DotMenuService } from '@services/dot-menu.service';

describe('DotWorkflowTaskDetailService', () => {
    let service: DotWorkflowTaskDetailService;
    let dotMenuService: DotMenuService;
    let injector;

    beforeEach(() => {
        injector = DOTTestBed.configureTestingModule({
            providers: [DotWorkflowTaskDetailService, DotMenuService]
        });

        service = injector.get(DotWorkflowTaskDetailService);
        dotMenuService = injector.get(DotMenuService);
        spyOn(dotMenuService, 'getDotMenuId').and.returnValue(observableOf('456'));
    });

    it('should set data to view', () => {
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
        });

        service.header$.subscribe((header: string) => {
            expect(header).toEqual('This is a header for view');
        });

        service.view({
            header: 'This is a header for view',
            id: '999'
        });
    });

    it('should clear url and undbind', () => {
        service.viewUrl$.subscribe((url: string) => {
            expect(url).toEqual('');
        });

        service.clear();
    });
});
