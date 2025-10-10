import { of as observableOf } from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import {
    DotcmsEventsService,
    DotEventsSocket,
    LoggerService,
    LoginService
} from '@dotcms/dotcms-js';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotWorkflowTaskDetailComponent } from './dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from './services/dot-workflow-task-detail.service';

import { DotMenuService } from '../../../api/services/dot-menu.service';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotIframeDialogComponent } from '../dot-iframe-dialog/dot-iframe-dialog.component';

describe('DotWorkflowTaskDetailComponent', () => {
    let component: DotWorkflowTaskDetailComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotWorkflowTaskDetailComponent>;

    let dotIframeDialog: DebugElement;
    let dotIframeDialogComponent: DotIframeDialogComponent;

    let dotWorkflowTaskDetailService: DotWorkflowTaskDetailService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                DotWorkflowTaskDetailComponent,
                DotIframeDialogComponent,
                RouterTestingModule,
                BrowserAnimationsModule
            ],
            providers: [
                DotWorkflowTaskDetailService,
                IframeOverlayService,
                DotIframeService,
                DotRouterService,
                DotUiColorsService,
                DotcmsEventsService,
                DotEventsSocket,
                LoggerService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMenuService,
                    useValue: {
                        getDotMenuId() {
                            return observableOf('999');
                        }
                    }
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotWorkflowTaskDetailComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotWorkflowTaskDetailService = de.injector.get(DotWorkflowTaskDetailService);
        fixture.detectChanges();

        dotIframeDialog = de.query(By.css('dot-iframe-dialog'));
        dotIframeDialogComponent = dotIframeDialog.componentInstance;
    });

    it('should have dot-iframe-dialog', () => {
        expect(dotIframeDialog).toBeTruthy();
    });

    describe('with data', () => {
        beforeEach(() => {
            dotWorkflowTaskDetailService.view({
                id: '123'
            });

            jest.spyOn(component, 'onClose');
            jest.spyOn(dotWorkflowTaskDetailService, 'clear');
            jest.spyOn(component.shutdown, 'emit');
            jest.spyOn(component.custom, 'emit');
            fixture.detectChanges();
        });

        it('should have dot-iframe-dialog url set', () => {
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
                dotIframeDialog.triggerEventHandler('shutdown', {});
                expect(dotWorkflowTaskDetailService.clear).toHaveBeenCalledTimes(1);
                expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
            });

            it('should call clear and emit close', () => {
                const customEvent = new CustomEvent('custom', {
                    detail: {
                        hello: 'world'
                    }
                });

                dotIframeDialog.triggerEventHandler('custom', customEvent);
                expect(component.custom.emit).toHaveBeenCalledWith(customEvent);
                expect(component.custom.emit).toHaveBeenCalledTimes(1);
            });
        });
    });
});
