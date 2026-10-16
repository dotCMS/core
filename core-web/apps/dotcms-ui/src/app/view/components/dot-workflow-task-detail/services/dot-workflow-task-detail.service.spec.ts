import { of } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotCurrentUserService, DotSystemConfigService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import { DotCurrentUserServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotWorkflowTaskDetailService } from './dot-workflow-task-detail.service';

describe('DotWorkflowTaskDetailService', () => {
    let service: DotWorkflowTaskDetailService;
    let globalStore: {
        ensureLoaded: ReturnType<typeof vi.fn>;
        layoutIdForPortlet: ReturnType<typeof vi.fn>;
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: GlobalStore,
                    useValue: {
                        ensureLoaded: vi.fn().mockReturnValue(of(undefined)),
                        layoutIdForPortlet: vi.fn().mockReturnValue('456')
                    }
                },
                DotWorkflowTaskDetailService
            ]
        });

        service = TestBed.inject(DotWorkflowTaskDetailService);
        globalStore = TestBed.inject(GlobalStore) as unknown as typeof globalStore;
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

                expect(globalStore.layoutIdForPortlet).toHaveBeenCalledWith('workflow');
                expect(globalStore.layoutIdForPortlet).toHaveBeenCalledTimes(1);
                done();
            });

            service.header$.subscribe((header: string) => {
                expect(header).toEqual('This is a header for view');
            });
        }));

    it('should clear url and undbind', () => {
        service.viewUrl$.subscribe((url: string) => {
            expect(url).toEqual('');
        });

        service.clear();
    });
});

/**
 * Characterization of the workflow task URL (issue #38078), observed at the HTTP boundary: the menu
 * response a test flushes is the only source of the layout id, so the same cases hold whichever
 * object holds the menu. `p_l_id` must be the layout that contains the `workflow` portlet.
 */
describe('DotWorkflowTaskDetailService p_l_id', () => {
    const LAYOUT_SITE = 'aaaa1111-0000-4000-8000-000000000001';
    const LAYOUT_SETTINGS = 'bbbb2222-0000-4000-8000-000000000002';

    const menu = {
        entity: [
            {
                id: LAYOUT_SITE,
                name: 'Site',
                tabName: 'Site',
                tabIcon: 'icon',
                url: '/pages',
                menuItems: [{ id: 'pages', label: 'Pages', url: '/pages', angular: true }]
            },
            {
                id: LAYOUT_SETTINGS,
                name: 'Settings',
                tabName: 'Settings',
                tabIcon: 'icon',
                url: '/c/roles',
                menuItems: [
                    { id: 'roles', label: 'Roles', url: '/c/roles', angular: false },
                    { id: 'workflow', label: 'Workflow', url: '/c/workflow', angular: false }
                ]
            }
        ]
    };

    let service: DotWorkflowTaskDetailService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotCurrentUserService, useClass: DotCurrentUserServiceMock },
                { provide: DotSystemConfigService, useValue: { getSystemConfig: () => of({}) } },
                { provide: LoginService, useClass: LoginServiceMock },
                GlobalStore,
                DotWorkflowTaskDetailService
            ]
        });
        service = TestBed.inject(DotWorkflowTaskDetailService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('waits for the menu and then uses the id of the layout that contains the workflow portlet', () => {
        let url = '';
        service.viewUrl$.subscribe((value) => (url = value));

        service.view({ id: '999' });
        httpMock.expectOne('/api/v1/menu').flush(menu);

        expect(url).toContain(`?p_l_id=${LAYOUT_SETTINGS}&`);
        expect(url).not.toContain(LAYOUT_SITE);
        expect(url).toContain('&p_p_id=workflow');
        expect(url).toContain('&_workflow_taskId=999');
    });
});
