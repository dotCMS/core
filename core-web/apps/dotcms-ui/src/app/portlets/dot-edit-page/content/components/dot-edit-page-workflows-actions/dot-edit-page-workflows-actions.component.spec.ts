/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';

import {
    DotAlertConfirmService,
    DotEventsService,
    DotFormatDateService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService,
    DotWorkflowService,
    PushPublishService
} from '@dotcms/data-access';
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
import { DotMessageSeverity, DotMessageType, DotPage } from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    DotWorkflowServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockDotPage,
    MockPushPublishService,
    mockWorkflowsActions
} from '@dotcms/utils-testing';

import { DotEditPageWorkflowsActionsComponent } from './dot-edit-page-workflows-actions.component';

import { dotEventSocketURLFactory } from '../../../../../test/dot-test-bed';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-workflows-actions [page]="page" />
    `,
    standalone: false
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
    let dotWorkflowEventHandlerService: DotWorkflowEventHandlerService;
    const messageServiceMock = new MockDotMessageService({
        'editpage.actions.fire.confirmation': 'The action "{0}" was executed correctly',
        'editpage.actions.fire.error.add.environment': 'place holder text',
        'Workflow-Action': 'Workflow Action'
    });

    beforeEach(() => {
        testbed = TestBed.configureTestingModule({
            imports: [
                RouterTestingModule,
                BrowserAnimationsModule,
                MenuModule,
                HttpClientTestingModule,
                ButtonModule,
                DotEditPageWorkflowsActionsComponent
            ],
            declarations: [TestHostComponent],
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
                { provide: CoreWebService, useClass: CoreWebServiceMock },
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
                DotFormatDateService,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                LoggerService,
                StringUtils,
                DotWorkflowEventHandlerService,
                DotIframeService
            ]
        });
    });

    beforeEach(() => {
        fixture = testbed.createComponent(TestHostComponent);
        de = fixture.debugElement;

        component = fixture.componentInstance;
        component.page = {
            ...mockDotPage(),
            ...{ workingInode: 'cc2cdf9c-a20d-4862-9454-2a76c1132123' }
        };

        workflowActionDebugEl = de.query(By.css('dot-edit-page-workflows-actions'));
        workflowActionComponent = workflowActionDebugEl.componentInstance;
        dotGlobalMessageService = de.injector.get(DotGlobalMessageService);
        button = workflowActionDebugEl.query(By.css('p-button'));

        dotWorkflowActionsFireService = workflowActionDebugEl.injector.get(
            DotWorkflowActionsFireService
        );
        dotWorkflowsActionsService = workflowActionDebugEl.injector.get(DotWorkflowsActionsService);
        jest.spyOn(dotWorkflowActionsFireService, 'fireTo').mockReturnValue(
            of(dotcmsContentletMock)
        );
    });

    describe('p-button', () => {
        describe('enabled', () => {
            beforeEach(() => {
                jest.spyOn(dotWorkflowsActionsService, 'getByInode').mockReturnValue(
                    of(mockWorkflowsActions)
                );
                component.page = {
                    ...mockDotPage(),
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
                expect(attr.icon).toEqual('pi pi-ellipsis-v');
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
                    const mainButton: DebugElement = de.query(By.css('p-button'));
                    mainButton.triggerEventHandler('click', {
                        currentTarget: mainButton.nativeElement
                    });
                    fixture.detectChanges();

                    splitButtons = de.queryAll(By.css('.p-menuitem-content'));
                    firstButton = splitButtons[0].nativeElement;
                    secondButton = splitButtons[1].nativeElement;
                    thirdButton = splitButtons[2].nativeElement;
                });

                describe('with sub actions / action Inputs', () => {
                    const mockData = {
                        assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
                        comments: 'ds',
                        pathToMove: '/test/',
                        environment: ['37fe23d5-588d-4c61-a9ea-70d01e913344'],
                        expireDate: '2020-08-11 19:59',
                        filterKey: 'Intelligent.yml',
                        publishDate: '2020-08-05 17:59',
                        pushActionSelected: 'publishexpire'
                    };

                    const mappedData: { [key: string]: any } = {
                        assign: '654b0931-1027-41f7-ad4d-173115ed8ec1',
                        comments: 'ds',
                        expireDate: '2020-08-11',
                        expireTime: '19-59',
                        filterKey: 'Intelligent.yml',
                        iWantTo: 'publishexpire',
                        publishDate: '2020-08-05',
                        publishTime: '17-59',
                        whereToSend: '37fe23d5-588d-4c61-a9ea-70d01e913344',
                        pathToMove: '/test/',
                        contentlet: {}
                    };

                    let dotWizardService: DotWizardService;
                    let pushPublishService: PushPublishService;
                    let dotMessageDisplayService: DotMessageDisplayService;
                    beforeEach(() => {
                        dotWizardService = de.injector.get(DotWizardService);
                        pushPublishService = de.injector.get(PushPublishService);
                        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
                        dotWorkflowEventHandlerService = de.injector.get(
                            DotWorkflowEventHandlerService
                        );
                    });

                    it('should fire actions after wizard data was collected', () => {
                        jest.spyOn(dotWorkflowEventHandlerService, 'setWizardInput');
                        firstButton.click();
                        dotWizardService.output$(mockData);

                        expect(dotWorkflowEventHandlerService.setWizardInput).toHaveBeenCalledWith(
                            mockWorkflowsActions[0],
                            'Workflow Action'
                        );

                        expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith({
                            actionId: mockWorkflowsActions[0].id,
                            inode: component.page.workingInode,
                            data: mappedData
                        });
                    });

                    it('should show and alert when there is no environments and push publish action', () => {
                        jest.spyOn(pushPublishService, 'getEnvironments').mockReturnValue(of([]));
                        jest.spyOn(dotMessageDisplayService, 'push');

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

                it('should fire actions on click on secondButton', () => {
                    secondButton.click();
                    expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith({
                        actionId: mockWorkflowsActions[1].id,
                        inode: component.page.workingInode,
                        data: undefined
                    });
                });

                it('should fire actions on click on thirdButton', () => {
                    thirdButton.click();
                    expect(dotWorkflowActionsFireService.fireTo).toHaveBeenCalledWith({
                        actionId: mockWorkflowsActions[2].id,
                        inode: component.page.workingInode,
                        data: undefined
                    });
                });

                it('should show success message after fired action in the menu items', () => {
                    jest.spyOn(dotGlobalMessageService, 'display');
                    secondButton.click();
                    fixture.detectChanges();
                    expect(dotGlobalMessageService.display).toHaveBeenCalledWith(
                        `The action "${mockWorkflowsActions[1].name}" was executed correctly`
                    );
                });

                it('should refresh the action list after fire action', () => {
                    secondButton.click();
                    expect(dotWorkflowsActionsService.getByInode).toHaveBeenCalledTimes(2); // initial ngOnChanges & action.
                });

                it('should emit event after action was fired', () => {
                    jest.spyOn(workflowActionComponent.fired, 'emit');
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
                jest.spyOn(dotWorkflowsActionsService, 'getByInode').mockReturnValue(of([]));
                fixture.detectChanges();
            });

            it('should be disabled', () => {
                expect(button.componentInstance.disabled).toBe(true);
            });
        });
    });

    describe('menu', () => {
        let menu: Menu;

        beforeEach(() => {
            jest.spyOn(dotWorkflowsActionsService, 'getByInode').mockReturnValue(
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
