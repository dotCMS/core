import { Observable, of } from 'rxjs';

import { CommonModule, Location } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement, Injectable, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotWizardModule } from '@components/_common/dot-wizard/dot-wizard.module';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotSecondaryToolbarModule } from '@components/dot-secondary-toolbar';
import { DotFormatDateService } from '@dotcms/app/api/services/dot-format-date-service';
import { DotHttpErrorManagerService } from '@dotcms/app/api/services/dot-http-error-manager/dot-http-error-manager.service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { dotEventSocketURLFactory } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotESContentService,
    DotEventsService,
    DotLicenseService,
    DotMessageService,
    DotPropertiesService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    SiteService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import {
    DotExperiment,
    DotPageMode,
    DotPageRender,
    DotPageRenderState,
    ESContent
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    dotcmsContentletMock,
    DotFormatDateServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockDotPersona,
    mockDotRenderedPage,
    mockDotRenderedPageState,
    MockDotRouterService,
    mockUser,
    SiteServiceMock
} from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotEditPageViewAsControllerModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageWorkflowsActionsModule } from '@portlets/dot-edit-page/content/components/dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotPageStateService } from '@portlets/dot-edit-page/content/services/dot-page-state/dot-page-state.service';
import { DotEditPageStateControllerSeoComponent } from '@portlets/dot-edit-page/seo/components/dot-edit-page-state-controller-seo/dot-edit-page-state-controller-seo.component';
import { DotExperimentClassDirective } from '@portlets/shared/directives/dot-experiment-class.directive';

import { DotEditPageToolbarSeoComponent } from './dot-edit-page-toolbar-seo.component';

import { DotEditPageInfoSeoComponent } from '../dot-edit-page-info-seo/dot-edit-page-info-seo.component';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-toolbar-seo
            [pageState]="pageState"
            [runningExperiment]="runningExperiment"
        ></dot-edit-page-toolbar-seo>
    `
})
class TestHostComponent {
    @Input() pageState: DotPageRenderState = mockDotRenderedPageState;
    @Input() runningExperiment: DotExperiment = null;
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

@Injectable()
export class MockDotPropertiesService {
    getKey(): Observable<true> {
        return of(true);
    }
}

export class ActivatedRouteListStoreMock {
    get queryParams() {
        return of({
            mode: DotPageMode.EDIT,
            variantName: 'Original',
            experimentId: '1232121212'
        });
    }
}

describe('DotEditPageToolbarSeoComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let component: DotEditPageToolbarSeoComponent;
    let de: DebugElement;
    let deHost: DebugElement;
    let dotLicenseService: DotLicenseService;
    let dotMessageDisplayService: DotMessageDisplayService;
    let dotDialogService: DialogService;
    let dotPropertiesService: DotPropertiesService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [
                TestHostComponent,
                MockDotIconButtonComponent,
                MockGlobalMessageComponent
            ],
            imports: [
                DotEditPageToolbarSeoComponent,
                HttpClientTestingModule,
                ButtonModule,
                CommonModule,
                CheckboxModule,
                DotSecondaryToolbarModule,
                FormsModule,
                ToolbarModule,
                DotEditPageViewAsControllerModule,
                DotEditPageStateControllerSeoComponent,
                DotEditPageInfoSeoComponent,
                DotEditPageWorkflowsActionsModule,
                DotPipesModule,
                DotMessagePipe,
                DotWizardModule,
                TooltipModule,
                TagModule,
                DotExperimentClassDirective,
                RouterTestingModule.withRoutes([
                    {
                        path: 'edit-page/experiments/pageId/id/reports',
                        component: TestHostComponent
                    }
                ])
            ],
            providers: [
                { provide: DotLicenseService, useClass: MockDotLicenseService },
                {
                    provide: DotMessageService,
                    useValue: new MockDotMessageService({
                        'dot.common.whats.changed': 'Whats',
                        'dot.common.cancel': 'Cancel',
                        'favoritePage.dialog.header': 'Add Favorite Page',
                        'dot.edit.page.toolbar.preliminary.results': 'Preliminary Results',
                        running: 'Running'
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
                DotPropertiesService,
                { provide: ActivatedRoute, useClass: ActivatedRouteListStoreMock },
                { provide: DotPropertiesService, useClass: MockDotPropertiesService }
            ]
        });
    });

    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;

        de = deHost.query(By.css('dot-edit-page-toolbar-seo'));
        component = de.componentInstance;

        dotLicenseService = de.injector.get(DotLicenseService);
        dotMessageDisplayService = de.injector.get(DotMessageDisplayService);
        dotDialogService = de.injector.get(DialogService);
        dotPropertiesService = TestBed.inject(DotPropertiesService);
        spyOn(dotPropertiesService, 'getKey').and.returnValue(of('true'));
    });

    describe('elements', () => {
        beforeEach(() => {
            fixtureHost.detectChanges();
        });

        it('should have elements placed correctly', () => {
            const editToolbar = de.query(By.css('dot-secondary-toolbar'));
            const editPageInfo = de.query(
                By.css('dot-secondary-toolbar .main-toolbar-left dot-edit-page-info-seo')
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

    describe('dot-edit-page-info-seo', () => {
        it('should have the right attr', () => {
            fixtureHost.detectChanges();
            const dotEditPageInfo = de.query(By.css('dot-edit-page-info-seo')).componentInstance;
            expect(dotEditPageInfo.title).toBe('A title');
            expect(dotEditPageInfo.url).toBe('/an/url/test');
            expect(dotEditPageInfo.innerApiLink).toBe(
                'api/v1/page/render/an/url/test?language_id=1'
            );
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

    describe('dot-edit-page-state-controller-seo', () => {
        it('should have pageState attr', () => {
            fixtureHost.detectChanges();
            const dotEditPageState = de.query(By.css('dot-edit-page-state-controller-seo'));
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
                dotcmsContentletMock
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

    describe('Go to Experiment results', () => {
        it('should show an experiment is running an go to results', (done) => {
            const location = de.injector.get(Location);
            componentHost.runningExperiment = { pageId: 'pageId', id: 'id' } as DotExperiment;

            fixtureHost.detectChanges();

            const experimentTag = de.query(By.css('[data-testId="runningExperimentTag"]'));

            experimentTag.nativeElement.click();

            expect(experimentTag.componentInstance.value).toEqual('Running');
            fixtureHost.whenStable().then(() => {
                expect(location.path()).toEqual('/edit-page/experiments/pageId/id/reports');
                done();
            });
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
                const dotEditPageState = de.query(By.css('dot-edit-page-state-controller-seo'));
                dotEditPageState.triggerEventHandler('modeChange', DotPageMode.EDIT);

                expect(component.whatschange.emit).toHaveBeenCalledWith(false);
            });

            it('should not emit when showWhatsChanged is false', () => {
                component.showWhatsChanged = false;
                fixtureHost.detectChanges();
                const dotEditPageState = de.query(By.css('dot-edit-page-state-controller-seo'));
                dotEditPageState.triggerEventHandler('modeChange', DotPageMode.EDIT);

                expect(component.whatschange.emit).not.toHaveBeenCalled();
            });
        });
    });
});
