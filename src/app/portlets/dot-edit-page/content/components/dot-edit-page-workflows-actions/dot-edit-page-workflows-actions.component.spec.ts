import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement, Component, Input } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { of } from 'rxjs';
import { MenuModule, Menu, ConfirmationService } from 'primeng/primeng';
import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from 'dotcms-js';
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
import { DotWizardService } from '@services/dot-wizard/dot-wizard.service';
import { PushPublishService } from '@services/push-publish/push-publish.service';
import { MockPushPublishService } from '@portlets/shared/dot-content-types-listing/dot-content-types.component.spec';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { BaseRequestOptions, ConnectionBackend, Http, RequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';

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
        'editpage.actions.fire.confirmation': 'The action "{0}" was executed correctly',
        'editpage.actions.fire.error.add.environment': 'place holder text',
        'Workflow-Action': 'Workflow Action'
    });

    beforeEach(
        async(() => {
            testbed = TestBed.configureTestingModule({
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
                        provide: PushPublishService,
                        useClass: MockPushPublishService
                    },
                    { provide: ConnectionBackend, useClass: MockBackend },
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: RequestOptions, useClass: BaseRequestOptions },
                    Http,
                    DotWorkflowsActionsService,
                    DotHttpErrorManagerService,
                    DotRouterService,
                    DotWorkflowActionsFireService,
                    DotWizardService,
                    DotMessageDisplayService,
                    DotAlertConfirmService,
                    ConfirmationService,
                    DotGlobalMessageService,
                    DotEventsService,
                    DotcmsEventsService,
                    DotEventsSocket,
                    { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                    DotcmsConfigService,
                    LoggerService,
                    StringUtils
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
                spyOn(dotWorkflowsActionsService, 'getByInode').and.returnValue(
                    of(mockWorkflowsActions)
                );
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
                    mainButton.triggerEventHandler('click', {
                        currentTarget: mainButton.nativeElement
                    });
                    fixture.detectChanges();

                    splitButtons = de.queryAll(By.css('.ui-menuitem-link'));
                    firstButton = splitButtons[0].nativeElement;
                    secondButton = splitButtons[1].nativeElement;
                    thirdButton = splitButtons[2].nativeElement;
                });

                describe('with sub actions / action Inputs', () => {
                    const mockData = {
                        assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
                        comments: 'ds',
                        environment: ['37fe23d5-588d-4c61-a9ea-70d01e913344'],
                        expireDate: '2020-08-11 19:59',
                        filterKey: 'Intelligent.yml',
                        publishDate: '2020-08-05 17:59',
                        pushActionSelected: 'publishexpire'
                    };

                    const mappedData = {
                        assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
                        comments: 'ds',
                        expireDate: '2020-08-11',
                        expireTime: '19-59',
                        filterKey: 'Intelligent.yml',
                        iWantTo: 'publishexpire',
                        publishDate: '2020-08-05',
                        publishTime: '17-59',
                        whereToSend: '37fe23d5-588d-4c61-a9ea-70d01e913344'
                    };

                    let dotWizardService: DotWizardService;
                    let pushPublishService: PushPublishService;
                    let dotMessageDisplayService: DotMessageDisplayService;
                    beforeEach(() => {
                        dotWizardService = de.injector.get(DotWizardService);
                        pushPublishService = de.injector.get(PushPublishService);
                        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
                    });

                    it('should fire actions after wizard data was collected', () => {
                        spyOn(dotWorkflowsActionsService, 'setWizardInput');
                        firstButton.click();
                        dotWizardService.output$(mockData);

                        expect(dotWorkflowsActionsService.setWizardInput).toHaveBeenCalledWith(
                            mockWorkflowsActions[0],
                            'Workflow Action'
                        );

                        expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith(
                            component.page.workingInode,
                            mockWorkflowsActions[0].id,
                            mappedData
                        );
                    });

                    it('should show and alert when there is no environments and push publish action', () => {
                        spyOn(pushPublishService, 'getEnvironments').and.returnValue(of([]));
                        spyOn(dotMessageDisplayService, 'push');

                        firstButton.click();
                        expect(dotWorkflowActionsFireService.fireTo).not.toHaveBeenCalled();
                        expect(dotMessageDisplayService.push).toHaveBeenCalledWith({
                            life: 3000,
                            message: messageServiceMock.get(
                                'editpage.actions.fire.error.add.environment'
                            ),
                            severity: DotMessageSeverity.ERROR,
                            type: DotMessageType.SIMPLE_MESSAGE
                        });
                    });
                });

                it('should fire actions on click in the menu items', () => {
                    secondButton.click();
                    expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith(
                        component.page.workingInode,
                        mockWorkflowsActions[1].id,
                        undefined
                    );

                    thirdButton.click();
                    expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith(
                        component.page.workingInode,
                        mockWorkflowsActions[2].id,
                        undefined
                    );
                });

                it('should show success message after fired action in the menu items', () => {
                    spyOn(dotGlobalMessageService, 'display');
                    secondButton.click();
                    fixture.detectChanges();
                    expect(dotGlobalMessageService.display).toHaveBeenCalledWith(
                        `The action "${mockWorkflowsActions[1].name}" was executed correctly`
                    );
                });

                it('should refresh the action list after fire action', () => {
                    secondButton.click();
                    expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledTimes(1);
                });

                it('should emit event after action was fired', () => {
                    spyOn(workflowActionComponent.fired, 'emit');
                    secondButton.click();
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
            spyOn(dotWorkflowsActionsService, 'getByInode').and.returnValue(
                of(mockWorkflowsActions)
            );
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
