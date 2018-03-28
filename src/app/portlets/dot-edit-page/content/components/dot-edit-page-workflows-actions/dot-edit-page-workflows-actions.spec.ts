import { CommonModule } from '@angular/common';
import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { Observable } from 'rxjs/Observable';
import { SplitButton } from 'primeng/primeng';
import { SplitButtonModule } from 'primeng/components/splitbutton/splitbutton';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotWorkflowServiceMock, mockWorkflows } from '../../../../../test/dot-workflow-service.mock';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { DotWorkflowAction } from '../../../../../shared/models/dot-workflow-action/dot-workflow-action.model';
import { DotWorkflowService } from '../../../../../api/services/dot-workflow/dot-workflow.service';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';
import { DotHttpErrorManagerService } from '../../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { mockResponseView } from '../../../../../test/response-view.mock';

describe('DotEditPageWorkflowsActionsComponent', () => {
    let component: DotEditPageWorkflowsActionsComponent;
    let fixture: ComponentFixture<DotEditPageWorkflowsActionsComponent>;
    let de: DebugElement;
    let testbed;
    let actionButton: DebugElement;
    let dotWorkflowService: DotWorkflowService;

    beforeEach(
        async(() => {
            testbed = DOTTestBed.configureTestingModule({
                imports: [RouterTestingModule, BrowserAnimationsModule],
                declarations: [DotEditPageWorkflowsActionsComponent],
                providers: [
                    {
                        provide: DotWorkflowService,
                        useClass: DotWorkflowServiceMock
                    },
                    { provide: LoginService, useClass: LoginServiceMock },
                    DotHttpErrorManagerService,
                    DotRouterService
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = testbed.createComponent(DotEditPageWorkflowsActionsComponent);
        de = fixture.debugElement;

        component = fixture.componentInstance;
        component.label = 'ACTIONS';
        component.inode = 'cc2cdf9c-a20d-4862-9454-2a76c1132123';

        dotWorkflowService = de.injector.get(DotWorkflowService);
        spyOn(dotWorkflowService, 'fireWorkflowAction').and.callThrough();
    });

    describe('button enabled', () => {
        beforeEach(() => {
            spyOn(dotWorkflowService, 'getContentWorkflowActions').and.callThrough();

            fixture.detectChanges();
            actionButton = de.query(By.css('.edit-page-toolbar__actions'));
        });

        it('should have a workflow actions element', () => {
            expect(actionButton).toBeTruthy();
        });

        it('should have a workflow actions with label "ACTIONS"', () => {
            expect(actionButton.nativeElement.textContent).toContain('ACTIONS');
        });

        it('should set action split buttons params', () => {
            const actionsButton: SplitButton = actionButton.componentInstance;
            expect(actionsButton.model[0].label).toEqual('Assign Workflow');
            expect(actionsButton.model[1].label).toEqual('Save');
            expect(actionsButton.model[2].label).toEqual('Save / Publish');
        });

        describe('fire actions', () => {
            let splitButtons: DebugElement[];
            let firstButton;
            let secondButton;
            let thirdButton;

            beforeEach(() => {
                splitButtons = de.queryAll(By.css('.ui-menuitem-link'));
                firstButton = splitButtons[0].nativeElement;
                secondButton = splitButtons[1].nativeElement;
                thirdButton = splitButtons[2].nativeElement;
            });

            it('should fire actions on click in the menu items', () => {
                firstButton.click();
                expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(component.inode, mockWorkflows[0].id);

                secondButton.click();
                expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(component.inode, mockWorkflows[1].id);

                thirdButton.click();
                expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(component.inode, mockWorkflows[2].id);
            });

            it('should refresh the action list after fire action', () => {
                firstButton.click();
                expect(dotWorkflowService.getContentWorkflowActions).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('button disabled', () => {
        beforeEach(() => {
            // spyOn(dotWorkflowService, 'getContentWorkflowActions').and.returnValue(Observable.of([]));
            spyOn(dotWorkflowService, 'getContentWorkflowActions').and.callThrough();

            fixture.detectChanges();
            actionButton = de.query(By.css('.edit-page-toolbar__actions'));
        });
        it('should be disabled', () => {
            expect(actionButton.componentInstance.disabled).toBe(false);
        });
    });
});
