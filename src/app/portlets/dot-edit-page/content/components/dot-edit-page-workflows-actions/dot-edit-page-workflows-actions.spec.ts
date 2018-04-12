import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement, Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';
import { SplitButton } from 'primeng/primeng';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotWorkflowServiceMock, mockWorkflowsActions } from '../../../../../test/dot-workflow-service.mock';
import { mockDotPage } from '../../../../../test/dot-rendered-page.mock';
import { LoginService } from 'dotcms-js/dotcms-js';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { DotWorkflowService } from '../../../../../api/services/dot-workflow/dot-workflow.service';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';
import { DotHttpErrorManagerService } from '../../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { DotPage } from '../../../shared/models/dot-page.model';
import { DotGlobalMessageService } from '../../../../../view/components/_common/dot-global-message/dot-global-message.service';
import { Observable } from 'rxjs/Observable';

@Component({
    selector: 'dot-test-host-component',
    template: `<dot-edit-page-workflows-actions [page]="page" [label]="label"></dot-edit-page-workflows-actions>`
})
class TestHostComponent {
    @Input() page: DotPage;
    @Input() label: string;
}

describe('DotEditPageWorkflowsActionsComponent', () => {
    let component: TestHostComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let testbed;
    let actionButton: DebugElement;
    let dotWorkflowService: DotWorkflowService;
    let workflowActionDebugEl: DebugElement;
    let workflowActionComponent: DotEditPageWorkflowsActionsComponent;
    let dotGlobalMessageService: DotGlobalMessageService;
    const messageServiceMock = new MockDotMessageService({
        'editpage.actions.fire.confirmation': 'The action "{0}" was executed correctly'
    });

    beforeEach(
        async(() => {
            testbed = DOTTestBed.configureTestingModule({
                imports: [RouterTestingModule, BrowserAnimationsModule],
                declarations: [DotEditPageWorkflowsActionsComponent, TestHostComponent],
                providers: [
                    {
                        provide: DotWorkflowService,
                        useClass: DotWorkflowServiceMock
                    },
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: LoginService, useClass: LoginServiceMock },
                    DotHttpErrorManagerService,
                    DotRouterService
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = testbed.createComponent(TestHostComponent);
        de = fixture.debugElement;

        component = fixture.componentInstance;
        component.label = 'ACTIONS';
        component.page = { ...mockDotPage, ...{ workingInode: 'cc2cdf9c-a20d-4862-9454-2a76c1132123' } };

        actionButton = de.query(By.css('.edit-page-toolbar__actions'));
        workflowActionDebugEl = de.query(By.css('dot-edit-page-workflows-actions'));
        workflowActionComponent = workflowActionDebugEl.componentInstance;
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);

        dotWorkflowService = workflowActionDebugEl.injector.get(DotWorkflowService);
        spyOn(dotWorkflowService, 'fireWorkflowAction').and.callThrough();
    });

    describe('button', () => {
        describe('enabled', () => {
            beforeEach(() => {
                spyOn(dotWorkflowService, 'getContentWorkflowActions').and.callThrough();
                fixture.detectChanges();
            });

            it('should have a workflow actions element', () => {
                expect(true).toBeTruthy();
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

            it('should get workflow actions when page changes"', () => {
                component.page = {
                    ...mockDotPage,
                    ...{
                        workingInode: 'cc2cdf9c-a20d-4862-9454-2a76c1132123',
                        lockedOn: new Date(1517330117295)
                    }
                };
                fixture.detectChanges();
                expect(dotWorkflowService.getContentWorkflowActions).toHaveBeenCalledWith('cc2cdf9c-a20d-4862-9454-2a76c1132123');
                expect(dotWorkflowService.getContentWorkflowActions).toHaveBeenCalledTimes(2);
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
                    expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(
                        component.page.workingInode,
                        mockWorkflowsActions[0].id
                    );

                    secondButton.click();
                    expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(
                        component.page.workingInode,
                        mockWorkflowsActions[1].id
                    );

                    thirdButton.click();
                    expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(
                        component.page.workingInode,
                        mockWorkflowsActions[2].id
                    );
                });

                it('should show success message after fired action in the menu items', () => {
                    spyOn(dotGlobalMessageService, 'display');
                    firstButton.click();
                    fixture.detectChanges();
                    expect(dotGlobalMessageService.display).toHaveBeenCalledWith(
                        `The action "${mockWorkflowsActions[0].name}" was executed correctly`
                    );
                });

                it('should refresh the action list after fire action', () => {
                    firstButton.click();
                    expect(dotWorkflowService.getContentWorkflowActions).toHaveBeenCalledTimes(1);
                });

                it('should emit event after action was fired', () => {
                    spyOn(workflowActionComponent.fired, 'emit');
                    firstButton.click();
                    fixture.detectChanges();
                    expect(workflowActionDebugEl.componentInstance.fired.emit).toHaveBeenCalledTimes(1);
                });
            });
        });

        describe('disabled', () => {
            beforeEach(() => {
                spyOn(dotWorkflowService, 'getContentWorkflowActions').and.returnValue(Observable.of([]));
                fixture.detectChanges();
            });

            it('should be disabled', () => {
                expect(actionButton.componentInstance.disabled).toBe(true);
            });
        });
    });
});
