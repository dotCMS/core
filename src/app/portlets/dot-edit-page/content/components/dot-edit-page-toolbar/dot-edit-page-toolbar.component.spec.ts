import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input, Injectable } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CheckboxModule, ToolbarModule, ButtonModule, ConfirmationService } from 'primeng/primeng';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { mockDotRenderedPageState } from '@tests/dot-rendered-page-state.mock';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPageMode } from '@portlets/dot-edit-page/shared/models/dot-page-mode.enum';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { DotEditPageViewAsControllerModule } from '../dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageStateControllerModule } from '../dot-edit-page-state-controller/dot-edit-page-state-controller.module';
import { DotEditPageInfoModule } from '@portlets/dot-edit-page/components/dot-edit-page-info/dot-edit-page-info.module';
import {
    SiteService,
    LoginService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotcmsEventsService,
    DotcmsConfigService,
    CoreWebService,
    LoggerService,
    StringUtils,
    ApiRoot,
    UserModel
} from 'dotcms-js';
import { SiteServiceMock } from '@tests/site-service.mock';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { LoginServiceMock } from '@tests/login-service.mock';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { mockDotPersona } from '@tests/dot-persona.mock';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { CoreWebServiceMock } from '../../../../../../../projects/dotcms-js/src/lib/core/core-web.service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotWizardModule } from '@components/_common/dot-wizard/dot-wizard.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-edit-page-toolbar [pageState]="pageState"></dot-edit-page-toolbar> `
})
class TestHostComponent {
    @Input() pageState: DotPageRenderState = mockDotRenderedPageState;
}

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(true);
    }
}

describe('DotEditPageToolbarComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotEditPageToolbarComponent;
    let de: DebugElement;
    let deHost: DebugElement;
    let dotLicenseService: DotLicenseService;
    let dotEventsService: DotEventsService;
    let dotMessageDisplayService: DotMessageDisplayService;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotEditPageToolbarComponent],
            imports: [
                HttpClientTestingModule,
                ButtonModule,
                CommonModule,
                CheckboxModule,
                DotSecondaryToolbarModule,
                FormsModule,
                ToolbarModule,
                DotEditPageViewAsControllerModule,
                DotEditPageStateControllerModule,
                DotEditPageInfoModule,
                DotEditPageWorkflowsActionsModule,
                DotPipesModule,
                DotWizardModule
            ],
            providers: [
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'dot.common.whats.changed': 'Whats',
                        'dot.common.cancel': 'Cancel'
                    })
                },
                {
                    provide: DotPageStateService,
                    useValue: {}
                },
                {
                    provide: SiteService,
                    useClass: SiteServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotMessageDisplayService,
                DotEventsService,
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                LoggerService,
                StringUtils,
                { provide: DotRouterService, useClass: MockDotRouterService },
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotGlobalMessageService,
                ApiRoot,
                UserModel,
                DotIframeService
            ]
        });
    }));

    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;

        de = deHost.query(By.css('dot-edit-page-toolbar'));
        component = de.componentInstance;

        dotLicenseService = de.injector.get(DotLicenseService);
        dotEventsService = de.injector.get(DotEventsService);
        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
    });

    describe('elements', () => {
        beforeEach(() => {
            fixtureHost.detectChanges();
        });

        it('should have elements placed correctly', () => {
            const editToolbar = de.query(By.css('dot-secondary-toolbar'));
            const editPageInfo = de.query(
                By.css('dot-secondary-toolbar .main-toolbar-left dot-edit-page-info')
            );
            const editCancelBtn = de.query(
                By.css('dot-secondary-toolbar .main-toolbar-right .edit-page-toolbar__cancel')
            );
            const editWorkflowActions = de.query(
                By.css('dot-secondary-toolbar .main-toolbar-right dot-edit-page-workflows-actions')
            );
            const editStateController = de.query(
                By.css('dot-secondary-toolbar .lower-toolbar-left dot-edit-page-state-controller')
            );
            const whatsChangedCheck = de.query(
                By.css('dot-secondary-toolbar .lower-toolbar-left .dot-edit__what-changed-button')
            );
            const editPageViewAs = de.query(
                By.css(
                    'dot-secondary-toolbar .lower-toolbar-right dot-edit-page-view-as-controller'
                )
            );
            expect(editToolbar).toBeDefined();
            expect(editPageInfo).toBeDefined();
            expect(editCancelBtn).toBeDefined();
            expect(editWorkflowActions).toBeDefined();
            expect(editStateController).toBeDefined();
            expect(whatsChangedCheck).toBeDefined();
            expect(editPageViewAs).toBeDefined();
        });
    });

    describe('dot-edit-page-info', () => {
        it('should have pageState attr', () => {
            fixtureHost.detectChanges();
            const dotEditPageInfo = de.query(By.css('dot-edit-page-info'));
            expect(dotEditPageInfo.componentInstance.pageState).toBe(mockDotRenderedPageState);
        });
    });

    describe('edit-page-toolbar-cancel', () => {
        it('should have right attr', () => {
            fixtureHost.detectChanges();
            const editPageCancelBtn = de.query(By.css('.edit-page-toolbar__cancel'));
            expect(editPageCancelBtn.attributes.class).toBe('edit-page-toolbar__cancel');
            expect(editPageCancelBtn.attributes.secondary).toBeDefined();
            expect(editPageCancelBtn.attributes.tiny).toBeDefined();
            expect(editPageCancelBtn.nativeElement.innerText).toBe('CANCEL');
        });

        it('should emit on click', () => {
            spyOn(component.cancel, 'emit');
            fixtureHost.detectChanges();
            const editPageCancelBtn = de.query(By.css('.edit-page-toolbar__cancel'));
            editPageCancelBtn.triggerEventHandler('click', {});
            expect(component.cancel.emit).toHaveBeenCalled();
        });
    });

    describe('dot-edit-page-workflows-actions', () => {
        it('should have pageState attr', () => {
            fixtureHost.detectChanges();
            const dotEditWorkflowActions = de.query(By.css('dot-edit-page-workflows-actions'));
            expect(dotEditWorkflowActions.componentInstance.page).toBe(
                mockDotRenderedPageState.page
            );
        });

        it('should emit on click', () => {
            spyOn(component.actionFired, 'emit');
            fixtureHost.detectChanges();
            const dotEditWorkflowActions = de.query(By.css('dot-edit-page-workflows-actions'));
            dotEditWorkflowActions.triggerEventHandler('fired', {});
            expect(component.actionFired.emit).toHaveBeenCalled();
        });
    });

    describe('dot-edit-page-state-controller', () => {
        it('should have pageState attr', () => {
            fixtureHost.detectChanges();
            const dotEditPageState = de.query(By.css('dot-edit-page-state-controller'));
            expect(dotEditPageState.componentInstance.pageState).toBe(mockDotRenderedPageState);
        });
    });

    describe('dot-edit-page-view-as-controller', () => {
        it('should have pageState attr', () => {
            fixtureHost.detectChanges();
            const dotEditPageViewAs = de.query(By.css('dot-edit-page-view-as-controller'));
            expect(dotEditPageViewAs.componentInstance.pageState).toBe(mockDotRenderedPageState);
        });
    });

    describe("what's change", () => {
        describe('no license', () => {
            beforeEach(() => {
                spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(false));
                fixtureHost.detectChanges();
            });

            it('should not show', () => {
                const whatsChangedElem = de.query(By.css('.dot-edit__what-changed-button'));
                expect(whatsChangedElem).toBeNull();
            });
        });

        describe('with license', () => {
            it("should hide what's change selector", () => {
                componentHost.pageState.state.mode = DotPageMode.EDIT;
                fixtureHost.detectChanges();

                const whatsChangedElem = de.query(By.css('p-checkbox'));
                expect(whatsChangedElem).toBeNull();
            });

            it("should have what's change selector", () => {
                componentHost.pageState.state.mode = DotPageMode.PREVIEW;
                fixtureHost.detectChanges();

                const whatsChangedElem = de.query(By.css('p-checkbox'));
                expect(whatsChangedElem).toBeDefined();
                expect(whatsChangedElem.componentInstance.label).toBe('Whats');
            });

            it("should hide what's change selector when is not default user", () => {
                componentHost.pageState.state.mode = DotPageMode.PREVIEW;
                componentHost.pageState.viewAs.persona = mockDotPersona;
                fixtureHost.detectChanges();

                const whatsChangedElem = de.query(By.css('p-checkbox'));
                expect(whatsChangedElem).toBeNull();
            });
        });
    });

    describe('events', () => {
        let whatsChangedElem: DebugElement;

        beforeEach(() => {
            spyOn(component.whatschange, 'emit');
            spyOn(dotMessageDisplayService, 'push');

            componentHost.pageState.state.mode = DotPageMode.PREVIEW;
            delete componentHost.pageState.viewAs.persona;
            fixtureHost.detectChanges();
            whatsChangedElem = de.query(By.css('p-checkbox'));
        });

        it("should emit what's change in true", () => {
            whatsChangedElem.triggerEventHandler('onChange', true);
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(true);
        });

        it("should emit what's change in false", () => {
            whatsChangedElem.triggerEventHandler('onChange', false);
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(false);
        });

        it('should set the value of the message to DotMessageDisplayService with the corresponding data', () => {
            dotEventsService.notify('dot-global-message', {
                value: 'test',
                type: DotMessageSeverity.SUCCESS
            });
            expect(dotMessageDisplayService.push).toHaveBeenCalledWith({
                life: 3000,
                severity: DotMessageSeverity.SUCCESS,
                type: DotMessageType.SIMPLE_MESSAGE,
                message: 'test'
            });
        });

        describe('whats change on state change', () => {
            it('should emit when showWhatsChanged is true', () => {
                component.showWhatsChanged = true;
                fixtureHost.detectChanges();
                const dotEditPageState = de.query(By.css('dot-edit-page-state-controller'));
                dotEditPageState.triggerEventHandler('modeChange', DotPageMode.EDIT);

                expect(component.whatschange.emit).toHaveBeenCalledWith(false);
            });

            it('should not emit when showWhatsChanged is false', () => {
                component.showWhatsChanged = false;
                fixtureHost.detectChanges();
                const dotEditPageState = de.query(By.css('dot-edit-page-state-controller'));
                dotEditPageState.triggerEventHandler('modeChange', DotPageMode.EDIT);

                expect(component.whatschange.emit).not.toHaveBeenCalled();
            });
        });
    });
});
