import { of as observableOf } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture } from '@angular/core/testing';

import { LoginService } from 'dotcms-js';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotWorkflowTaskDetailService } from './services/dot-workflow-task-detail.service';
import { DotWorkflowTaskDetailComponent } from './dot-workflow-task-detail.component';
import { DotIframeDialogComponent } from '../dot-iframe-dialog/dot-iframe-dialog.component';
import { DotIframeDialogModule } from '../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '@services/dot-menu.service';
import { LoginServiceMock } from '../../../test/login-service.mock';

describe('DotWorkflowTaskDetailComponent', () => {
    let component: DotWorkflowTaskDetailComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotWorkflowTaskDetailComponent>;

    let dotIframeDialog: DebugElement;
    let dotIframeDialogComponent: DotIframeDialogComponent;

    let dotWorkflowTaskDetailService: DotWorkflowTaskDetailService;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotWorkflowTaskDetailComponent],
            providers: [
                DotWorkflowTaskDetailService,
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
            ],
            imports: [DotIframeDialogModule, RouterTestingModule, BrowserAnimationsModule]
        });
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotWorkflowTaskDetailComponent);
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

            spyOn(component, 'onClose').and.callThrough();
            spyOn(dotWorkflowTaskDetailService, 'clear');
            spyOn(component.close, 'emit');
            spyOn(component.custom, 'emit');
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
                dotIframeDialog.triggerEventHandler('close', {});
                expect(dotWorkflowTaskDetailService.clear).toHaveBeenCalledTimes(1);
                expect(component.close.emit).toHaveBeenCalledTimes(1);
            });

            it('should call clear and emit close', () => {
                dotIframeDialog.triggerEventHandler('custom', { hello: 'world' });
                expect(component.custom.emit).toHaveBeenCalledWith({ hello: 'world' });
            });
        });
    });
});
