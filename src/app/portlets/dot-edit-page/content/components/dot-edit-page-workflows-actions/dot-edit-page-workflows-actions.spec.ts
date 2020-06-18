import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement, Component, Input } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture } from '@angular/core/testing';

import { of } from 'rxjs';
import { MenuModule, Menu } from 'primeng/primeng';
import { LoginService } from 'dotcms-js';

import { DOTTestBed } from '@tests/dot-test-bed';
import { DotWorkflowServiceMock } from '@tests/dot-workflow-service.mock';
import { LoginServiceMock } from '@tests/login-service.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { mockDotPage } from '@tests/dot-page-render.mock';
import { mockWorkflowsActions } from '@tests/dot-workflows-actions.mock';

import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPage } from '@portlets/dot-edit-page/shared/models/dot-page.model';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotWorkflowsActionsService } from '@services/dot-workflows-actions/dot-workflows-actions.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { dotcmsContentletMock } from '@tests/dotcms-contentlet.mock';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-workflows-actions [page]="page"></dot-edit-page-workflows-actions>
    `
})
class TestHostComponent {
    @Input() page: DotPage;
}

describe('DotEditPageWorkflowsActionsComponent', () => {
    let component: TestHostComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let testbed;
    let button: DebugElement;
    let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
    let workflowActionDebugEl: DebugElement;
    let workflowActionComponent: DotEditPageWorkflowsActionsComponent;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotWorkflowsActionsService: DotWorkflowsActionsService;
    const messageServiceMock = new MockDotMessageService({
        'editpage.actions.fire.confirmation': 'The action "{0}" was executed correctly'
    });

    beforeEach(
        async(() => {
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
                    {
                        provide: DotWorkflowsActionsService,
                        useValue: {
                            getByInode() {
                                return of(mockWorkflowsActions);
                            }
                        }
                    },
                    DotHttpErrorManagerService,
                    DotRouterService,
                    DotWorkflowActionsFireService
                ]
            });
        })
    );

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

        dotWorkflowActionsFireService = workflowActionDebugEl.injector.get(
            DotWorkflowActionsFireService
        );
        dotWorkflowsActionsService = workflowActionDebugEl.injector.get(DotWorkflowsActionsService);
        spyOn(dotWorkflowActionsFireService, 'fireTo').and.returnValue(of(dotcmsContentletMock));
    });

    describe('button', () => {
        describe('enabled', () => {
            beforeEach(() => {
                spyOn(dotWorkflowsActionsService, 'getByInode').and.callThrough();
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
                expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledWith(
                    'cc2cdf9c-a20d-4862-9454-2a76c1132123'
                );
                expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledTimes(1);
            });

            describe('fire actions', () => {
                let splitButtons: DebugElement[];
                let firstButton;
                let secondButton;
                let thirdButton;

                beforeEach(() => {
                    const mainButton: DebugElement = de.query(By.css('button'));
                    mainButton.triggerEventHandler('click', { currentTarget: mainButton.nativeElement });
                    fixture.detectChanges();

                    splitButtons = de.queryAll(By.css('.ui-menuitem-link'));
                    firstButton = splitButtons[0].nativeElement;
                    secondButton = splitButtons[1].nativeElement;
                    thirdButton = splitButtons[2].nativeElement;
                });

                it('should fire actions on click in the menu items', () => {
                    firstButton.click();
                    expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith(
                        component.page.workingInode,
                        mockWorkflowsActions[0].id
                    );

                    secondButton.click();
                    expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith(
                        component.page.workingInode,
                        mockWorkflowsActions[1].id
                    );

                    thirdButton.click();
                    expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith(
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
                    expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledTimes(1);
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
                spyOn(dotWorkflowsActionsService, 'getByInode').and.returnValue(of([]));
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
