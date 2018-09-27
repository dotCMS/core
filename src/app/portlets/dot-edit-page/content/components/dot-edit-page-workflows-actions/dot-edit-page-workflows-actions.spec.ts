import { of as observableOf } from 'rxjs';
import { async, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement, Component, Input } from '@angular/core';
import { By } from '@angular/platform-browser';
import { MenuModule, Menu } from 'primeng/primeng';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import {
    DotWorkflowServiceMock,
    mockWorkflowsActions
} from '../../../../../test/dot-workflow-service.mock';
import { mockDotPage } from '../../../../../test/dot-rendered-page.mock';
import { LoginService } from 'dotcms-js/dotcms-js';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { DotPage } from '@portlets/dot-edit-page/shared/models/dot-page.model';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';

@Component({
    selector: 'dot-test-host-component',
    template: `<dot-edit-page-workflows-actions [page]="page"></dot-edit-page-workflows-actions>`
})
class TestHostComponent {
    @Input()
    page: DotPage;
}

describe('DotEditPageWorkflowsActionsComponent', () => {
    let component: TestHostComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let testbed;
    let button: DebugElement;
    let dotWorkflowService: DotWorkflowService;
    let workflowActionDebugEl: DebugElement;
    let workflowActionComponent: DotEditPageWorkflowsActionsComponent;
    let dotGlobalMessageService: DotGlobalMessageService;
    const messageServiceMock = new MockDotMessageService({
        'editpage.actions.fire.confirmation': 'The action "{0}" was executed correctly'
    });

    beforeEach(async(() => {
        testbed = DOTTestBed.configureTestingModule({
            imports: [RouterTestingModule, BrowserAnimationsModule, MenuModule],
            declarations: [DotEditPageWorkflowsActionsComponent, TestHostComponent],
            providers: [
                {
                    provide: DotWorkflowService,
                    useClass: DotWorkflowServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotHttpErrorManagerService,
                DotRouterService
            ]
        });
    }));

    beforeEach(() => {
        fixture = testbed.createComponent(TestHostComponent);
        de = fixture.debugElement;

        component = fixture.componentInstance;
        component.page = {
            ...mockDotPage,
            ...{ workingInode: 'cc2cdf9c-a20d-4862-9454-2a76c1132123' }
        };

        workflowActionDebugEl = de.query(By.css('dot-edit-page-workflows-actions'));
        workflowActionComponent = workflowActionDebugEl.componentInstance;
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);

        button = workflowActionDebugEl.query(By.css('button'));

        dotWorkflowService = workflowActionDebugEl.injector.get(DotWorkflowService);
        spyOn(dotWorkflowService, 'fireWorkflowAction').and.callThrough();
    });

    describe('button', () => {
        describe('enabled', () => {
            beforeEach(() => {
                spyOn(dotWorkflowService, 'getContentWorkflowActions').and.callThrough();
                component.page = {
                    ...mockDotPage,
                    ...{
                        workingInode: 'cc2cdf9c-a20d-4862-9454-2a76c1132123',
                        lockedOn: new Date(1517330117295)
                    }
                };
                fixture.detectChanges();
            });

            it('should have button', () => {
                expect(button).toBeTruthy();
            });

            it('should have right attr in button', () => {
                const attr = button.attributes;
                expect(attr.icon).toEqual('fa fa-ellipsis-v');
                expect(attr.pButton).toBeDefined();
                expect(attr.secondary).toBeDefined();
            });

            it('should get workflow actions when page changes"', () => {
                expect(dotWorkflowService.getContentWorkflowActions).toHaveBeenCalledWith(
                    'cc2cdf9c-a20d-4862-9454-2a76c1132123'
                );
                expect(dotWorkflowService.getContentWorkflowActions).toHaveBeenCalledTimes(1);
            });

            describe('fire actions', () => {
                let splitButtons: DebugElement[];
                let firstButton;
                let secondButton;
                let thirdButton;

                beforeEach(() => {
                    const mainButton: DebugElement = de.query(By.css('button'));
                    mainButton.triggerEventHandler('click', {});
                    fixture.detectChanges();

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
                    expect(
                        workflowActionDebugEl.componentInstance.fired.emit
                    ).toHaveBeenCalledTimes(1);
                });
            });
        });

        describe('disabled', () => {
            beforeEach(() => {
                spyOn(dotWorkflowService, 'getContentWorkflowActions').and.returnValue(
                    observableOf([])
                );
                fixture.detectChanges();
            });

            it('should be disabled', () => {
                expect(button.nativeElement.disabled).toBe(true);
            });
        });
    });

    describe('menu', () => {
        let menu: Menu;

        beforeEach(() => {
            fixture.detectChanges();
            menu = de.query(By.css('p-menu')).componentInstance;
        });

        it('should have menu', () => {
            expect(menu).not.toBe(null);
        });

        it('should set actions', () => {
            expect(menu.model[0].label).toEqual('Assign Workflow');
            expect(menu.model[1].label).toEqual('Save');
            expect(menu.model[2].label).toEqual('Save / Publish');
        });
    });
});
