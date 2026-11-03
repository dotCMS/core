import { of as observableOf } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotWorkflowTaskDetailService } from './dot-workflow-task-detail.service';

import { DotMenuService } from '../../../../api/services/dot-menu.service';

describe('DotWorkflowTaskDetailService', () => {
    let service: DotWorkflowTaskDetailService;
    let dotMenuService: DotMenuService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotMenuService,
                DotWorkflowTaskDetailService
            ]
        });

        service = TestBed.inject(DotWorkflowTaskDetailService);
        dotMenuService = TestBed.inject(DotMenuService);
        vi.spyOn(dotMenuService, 'getDotMenuId').mockReturnValue(observableOf('456'));
    });

    it('should set data to view', () =>
        new Promise<void>((done) => {
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
                expect(dotMenuService.getDotMenuId).toHaveBeenCalledTimes(1);
                done();
            });

            service.header$.subscribe((header: string) => {
                expect(header).toEqual('This is a header for view');
            });
        }));

    // The task component views its task again after the content is saved or closed in the side
    // panel (#38303, FR-025a); the dialog's iframe reloads only when its URL changes.
    it('should give the same task a new URL when it is viewed again, so the dialog reloads', () => {
        const urls: string[] = [];
        service.viewUrl$.subscribe((url: string) => urls.push(url));

        service.view({ id: '999' });
        service.view({ id: '999' });

        const [first, again] = urls.slice(-2);
        expect(again).not.toEqual(first);
        expect(again).toContain('&_workflow_struts_action=/ext/workflows/edit_workflow_task');
        expect(again).toContain('&_workflow_cmd=view');
        expect(again).toContain('&_workflow_taskId=999');
    });

    it('should clear url and undbind', () => {
        service.viewUrl$.subscribe((url: string) => {
            expect(url).toEqual('');
        });

        service.clear();
    });
});
