import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of as observableOf } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { CoreWebServiceMock, LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { DotWorkflowTaskDetailComponent } from './dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from './services/dot-workflow-task-detail.service';

import { DotMenuService } from '../../../api/services/dot-menu.service';
import { dotEventSocketURLFactory } from '../../../test/dot-test-bed';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotIframeDialogComponent } from '../dot-iframe-dialog/dot-iframe-dialog.component';

describe('DotWorkflowTaskDetailComponent', () => {
    let spectator: Spectator<DotWorkflowTaskDetailComponent>;
    let dotWorkflowTaskDetailService: DotWorkflowTaskDetailService;

    const createComponent = createComponentFactory({
        component: DotWorkflowTaskDetailComponent,
        imports: [DotIframeDialogComponent, HttpClientTestingModule],
        providers: [
            DotWorkflowTaskDetailService,
            DotIframeService,
            DotUiColorsService,
            IframeOverlayService,
            DotcmsEventsService,
            DotEventsSocket,
            DotcmsConfigService,
            LoggerService,
            StringUtils,
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: CoreWebService, useClass: CoreWebServiceMock },
            { provide: DotRouterService, useClass: MockDotRouterService },
            {
                provide: ActivatedRoute,
                useValue: {
                    snapshot: {
                        queryParams: {}
                    }
                }
            },
            {
                provide: DotMenuService,
                useValue: {
                    getDotMenuId() {
                        return observableOf('999');
                    }
                }
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        dotWorkflowTaskDetailService = spectator.debugElement.injector.get(
            DotWorkflowTaskDetailService
        );
        spectator.detectChanges();
    });

    it('should have dot-iframe-dialog', () => {
        const dotIframeDialog = spectator.query('dot-iframe-dialog');
        expect(dotIframeDialog).toBeTruthy();
    });

    describe('with data', () => {
        beforeEach(() => {
            dotWorkflowTaskDetailService.view({
                id: '123'
            });

            jest.spyOn(spectator.component, 'onClose');
            jest.spyOn(dotWorkflowTaskDetailService, 'clear');
            jest.spyOn(spectator.component.shutdown, 'emit');
            jest.spyOn(spectator.component.custom, 'emit');
            spectator.detectChanges();
        });

        it('should have dot-iframe-dialog url set', () => {
            const dotIframeDialogComponent = spectator.query(DotIframeDialogComponent);
            expect(dotIframeDialogComponent.url).toEqual(
                [
                    `/c/portal/layout`,
                    `?p_l_id=999`,
                    `&p_p_id=workflow`,
                    `&p_p_action=1`,
                    `&p_p_state=maximized`,
                    `&p_p_mode=view`,
                    `&_workflow_struts_action=/ext/workflows/edit_workflow_task`,
                    `&_workflow_cmd=view`,
                    `&_workflow_taskId=123`
                ].join('')
            );
        });

        describe('events', () => {
            it('should call clear and emit close', () => {
                spectator.triggerEventHandler('dot-iframe-dialog', 'shutdown', {});
                expect(dotWorkflowTaskDetailService.clear).toHaveBeenCalledTimes(1);
                expect(spectator.component.shutdown.emit).toHaveBeenCalledTimes(1);
            });

            it('should call clear and emit close', () => {
                const customEvent = new CustomEvent('custom', {
                    detail: {
                        hello: 'world'
                    }
                });

                spectator.triggerEventHandler('dot-iframe-dialog', 'custom', customEvent);
                expect(spectator.component.custom.emit).toHaveBeenCalledWith(customEvent);
                expect(spectator.component.custom.emit).toHaveBeenCalledTimes(1);
            });
        });
    });
});
