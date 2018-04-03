import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement, Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';
import { SplitButton } from 'primeng/primeng';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotWorkflowServiceMock, mockWorkflows } from '../../../../../test/dot-workflow-service.mock';
import { mockDotPage } from '../../../../../test/dot-rendered-page.mock';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { DotWorkflowService } from '../../../../../api/services/dot-workflow/dot-workflow.service';
import { DotRouterService } from '../../../../../api/services/dot-router/dot-router.service';
import { DotHttpErrorManagerService } from '../../../../../api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { DotPage } from '../../../shared/models/dot-page.model';

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
    let workflowActionComponent: DebugElement;

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
        workflowActionComponent = de.query(By.css('dot-edit-page-workflows-actions'));

        dotWorkflowService = workflowActionComponent.injector.get(DotWorkflowService);
        spyOn(dotWorkflowService, 'fireWorkflowAction').and.callThrough();
        spyOn(dotWorkflowService, 'getContentWorkflowActions').and.callThrough();
        fixture.detectChanges();
    });

    describe('button enabled', () => {
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
            fixture.detectChanges();
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
                fixture.detectChanges();
            });

            it('should fire actions on click in the menu items', () => {
                firstButton.click();
                expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(component.page.workingInode, mockWorkflows[0].id);

                secondButton.click();
                expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(component.page.workingInode, mockWorkflows[1].id);

                thirdButton.click();
                expect(dotWorkflowService.fireWorkflowAction).toHaveBeenCalledWith(component.page.workingInode, mockWorkflows[2].id);
            });

            it('should refresh the action list after fire action', () => {
                firstButton.click();
                expect(dotWorkflowService.getContentWorkflowActions).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('button disabled', () => {
        it('should be disabled', () => {
            expect(actionButton.componentInstance.disabled).toBe(false);
        });
    });
});
