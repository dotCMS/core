import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement, Component, Input, Injectable } from '@angular/core';
import { By } from '@angular/platform-browser';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { mockDotRenderedPageState } from '@tests/dot-rendered-page-state.mock';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

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
} from '@dotcms/dotcms-js';
import { SiteServiceMock } from '@tests/site-service.mock';
import { DotEditPageWorkflowsActionsModule } from '../dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { LoginServiceMock, mockUser } from '@tests/login-service.mock';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { mockDotPersona } from '@tests/dot-persona.mock';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotWizardModule } from '@components/_common/dot-wizard/dot-wizard.module';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ToolbarModule } from 'primeng/toolbar';
import { ConfirmationService } from 'primeng/api';
import { DotPageMode } from '@models/dot-page/dot-page-mode.enum';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotFormatDateServiceMock } from '@dotcms/app/test/format-date-service.mock';
import { DialogService } from 'primeng/dynamicdialog';
import { DotESContentService } from '@dotcms/app/api/services/dot-es-content/dot-es-content.service';
import { TooltipModule } from 'primeng/tooltip';
import { ESContent } from '@dotcms/app/shared/models/dot-es-content/dot-es-content.model';
import { DotPageRender } from '@dotcms/app/shared/models/dot-page/dot-rendered-page.model';
import { mockDotRenderedPage } from '@dotcms/app/test/dot-page-render.mock';
import { DotPropertiesService } from '@dotcms/app/api/services/dot-properties/dot-properties.service';

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-edit-page-toolbar [pageState]="pageState"></dot-edit-page-toolbar> `
})
class TestHostComponent {
    @Input() pageState: DotPageRenderState = mockDotRenderedPageState;
}

@Component({
    selector: 'dot-icon-button',
    template: ''
})
class MockDotIconButtonComponent {
    @Input() icon: string;
}

@Component({
    selector: 'dot-global-message',
    template: ''
})
class MockGlobalMessageComponent {}

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(true);
    }
}

@Injectable()
class MockDotPageStateService {
    requestFavoritePageData(_urlParam: string): Observable<ESContent> {
        return of();
    }
}

describe('DotEditPageToolbarComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotEditPageToolbarComponent;
    let de: DebugElement;
    let deHost: DebugElement;
    let dotLicenseService: DotLicenseService;
    let dotMessageDisplayService: DotMessageDisplayService;
    let dotDialogService: DialogService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestHostComponent,
                DotEditPageToolbarComponent,
                MockDotIconButtonComponent,
                MockGlobalMessageComponent
            ],
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
                DotWizardModule,
                TooltipModule
            ],
            providers: [
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'dot.common.whats.changed': 'Whats',
                        'dot.common.cancel': 'Cancel',
                        'favoritePage.dialog.header.add.page': 'Add Favorite Page'
                    })
                },
                {
                    provide: DotPageStateService,
                    useClass: MockDotPageStateService
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
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                DotGlobalMessageService,
                ApiRoot,
                UserModel,
                DotIframeService,
                DialogService,
                DotESContentService,
                DotPropertiesService
            ]
        });
    });

    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;

        de = deHost.query(By.css('dot-edit-page-toolbar'));
        component = de.componentInstance;

        dotLicenseService = de.injector.get(DotLicenseService);
        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
        dotDialogService = de.injector.get(DialogService);
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
        it('should have the right attr', () => {
            fixtureHost.detectChanges();
            const dotEditPageInfo = de.query(By.css('dot-edit-page-info')).componentInstance;
            expect(dotEditPageInfo.title).toBe('A title');
            expect(dotEditPageInfo.url).toBe('/an/url/test');
            expect(dotEditPageInfo.apiLink).toBe('api/v1/page/render/an/url/test?language_id=1');
        });
    });

    describe('dot-global-message', () => {
        it('should have show', () => {
            fixtureHost.detectChanges();
            const dotGlobalMessage = de.query(By.css('[data-testId="globalMessage"]'));
            expect(dotGlobalMessage).not.toBeNull();
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
            xit("should have what's change selector", async () => {
                componentHost.pageState.state.mode = DotPageMode.PREVIEW;
                fixtureHost.detectChanges();
                await fixtureHost.whenStable();

                const whatsChangedElem = de.query(By.css('.dot-edit__what-changed-button'));
                expect(whatsChangedElem).toBeDefined();
                expect(whatsChangedElem.componentInstance.label).toBe('Whats');
                expect(whatsChangedElem.componentInstance.binary).toBe(true);
            });

            it("should hide what's change selector", () => {
                componentHost.pageState.state.mode = DotPageMode.EDIT;
                fixtureHost.detectChanges();

                const whatsChangedElem = de.query(By.css('.dot-edit__what-changed-button'));
                expect(whatsChangedElem).toBeNull();
            });

            it("should hide what's change selector when is not default user", () => {
                componentHost.pageState.state.mode = DotPageMode.PREVIEW;
                componentHost.pageState.viewAs.persona = mockDotPersona;
                fixtureHost.detectChanges();

                const whatsChangedElem = de.query(By.css('.dot-edit__what-changed-button'));
                expect(whatsChangedElem).toBeNull();
            });
        });
    });

    describe('Favorite icon', () => {
        it('should change icon on favorite page if contentlet exist', () => {
            componentHost.pageState = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPage()),
                true
            );
            component.showFavoritePageStar = true;

            fixtureHost.detectChanges();

            const favoritePageIcon = de.query(By.css('[data-testId="addFavoritePageButton"]'));
            expect(favoritePageIcon.componentInstance.icon).toBe('grade');
        });

        it('should show empty star icon on favorite page if NO contentlet exist', () => {
            component.showFavoritePageStar = true;

            fixtureHost.detectChanges();

            const favoritePageIcon = de.query(By.css('[data-testId="addFavoritePageButton"]'));
            expect(favoritePageIcon.componentInstance.icon).toBe('star_outline');
        });
    });

    describe('events', () => {
        let whatsChangedElem: DebugElement;
        beforeEach(() => {
            spyOn(component.whatschange, 'emit');
            spyOn(dotMessageDisplayService, 'push');
            spyOn(dotDialogService, 'open');
            spyOn(component.favoritePage, 'emit');

            componentHost.pageState.state.mode = DotPageMode.PREVIEW;
            delete componentHost.pageState.viewAs.persona;
            component.showFavoritePageStar = true;
            fixtureHost.detectChanges();
            whatsChangedElem = de.query(By.css('.dot-edit__what-changed-button'));
        });

        it('should instantiate dialog with DotFavoritePageComponent', () => {
            de.query(By.css('[data-testId="addFavoritePageButton"]')).nativeElement.click();
            fixtureHost.detectChanges();

            expect(component.favoritePage.emit).toHaveBeenCalledTimes(1);
        });

        it('should store RenderedHTML value if PREVIEW MODE', () => {
            expect(component.pageRenderedHtml).toBe(mockDotRenderedPageState.page.rendered);
        });

        it("should emit what's change in true", () => {
            whatsChangedElem.triggerEventHandler('onChange', { checked: true });
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(true);
        });

        it("should emit what's change in false", () => {
            whatsChangedElem.triggerEventHandler('onChange', { checked: false });
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(false);
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
