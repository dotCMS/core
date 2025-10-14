import { Spectator, createComponentFactory, byTestId } from '@ngneat/spectator/jest';
import { Observable, of } from 'rxjs';

import { CommonModule, DatePipe, Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, Injectable, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DialogService } from 'primeng/dynamicdialog';
import { TagModule } from 'primeng/tag';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import {
    DotAlertConfirmService,
    DotDevicesService,
    DotESContentService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotPersonalizeService,
    DotPropertiesService,
    DotRouterService,
    DotSessionStorageService,
    DotGlobalMessageService,
    DotIframeService,
    DotFormatDateService,
    DotPageStateService,
    DotWorkflowActionsFireService
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
    ESContent,
    RUNNING_UNTIL_DATE_FORMAT
} from '@dotcms/dotcms-models';
import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import {
    dotcmsContentletMock,
    DotcmsConfigServiceMock,
    DotDevicesServiceMock,
    DotFormatDateServiceMock,
    LoginServiceMock,
    mockDotContainers,
    mockDotDevices,
    mockDotLanguage,
    mockDotLayout,
    MockDotMessageService,
    mockDotPage,
    mockDotPersona,
    mockDotRenderedPage,
    mockDotRenderedPageState,
    MockDotRouterService,
    mockDotTemplate,
    mockUser,
    SiteServiceMock
} from '@dotcms/utils-testing';

import { DotEditPageToolbarSeoComponent } from './dot-edit-page-toolbar-seo.component';

import { dotEventSocketURLFactory } from '../../../../../test/dot-test-bed';
import { DotWizardComponent } from '../../../../../view/components/_common/dot-wizard/dot-wizard.component';
import { DotContentletEditorService } from '../../../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotLanguageSelectorComponent } from '../../../../../view/components/dot-language-selector/dot-language-selector.component';
import { DotSecondaryToolbarComponent } from '../../../../../view/components/dot-secondary-toolbar/dot-secondary-toolbar.component';
import { DotExperimentClassDirective } from '../../../../shared/directives/dot-experiment-class.directive';
import { DotEditPageViewAsControllerModule } from '../../../content/components/dot-edit-page-view-as-controller/dot-edit-page-view-as-controller.module';
import { DotEditPageWorkflowsActionsModule } from '../../../content/components/dot-edit-page-workflows-actions/dot-edit-page-workflows-actions.module';
import { DotEditPageInfoSeoComponent } from '../dot-edit-page-info-seo/dot-edit-page-info-seo.component';
import { DotEditPageStateControllerSeoComponent } from '../dot-edit-page-state-controller-seo/dot-edit-page-state-controller-seo.component';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-edit-page-toolbar-seo
            [pageState]="pageState"
            [runningExperiment]="runningExperiment"></dot-edit-page-toolbar-seo>
    `,
    standalone: false
})
class TestHostComponent {
    @Input() pageState: DotPageRenderState = mockDotRenderedPageState;
    @Input() runningExperiment: DotExperiment = null;
}

@Component({
    selector: 'dot-global-message',
    template: '',
    standalone: false
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
class MockDotPersonalizeService {
    personalized = jest.fn().mockReturnValue(of([]));
}

@Injectable()
class MockDotWorkflowActionsFireService {
    fireWorkflowAction = jest.fn().mockReturnValue(of({}));
}

@Injectable()
export class MockDotPropertiesService {
    getFeatureFlag(): Observable<true> {
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
    let spectator: Spectator<TestHostComponent>;
    let component: DotEditPageToolbarSeoComponent;
    let dotLicenseService: DotLicenseService;
    let dotMessageDisplayService: DotMessageDisplayService;
    let dotDialogService: DialogService;
    let dotPropertiesService: DotPropertiesService;

    const createComponent = createComponentFactory({
        component: TestHostComponent,
        declarations: [MockGlobalMessageComponent],
        imports: [
            DotEditPageToolbarSeoComponent,
            AvatarModule,
            BadgeModule,
            ButtonModule,
            CommonModule,
            CheckboxModule,
            DotSecondaryToolbarComponent,
            FormsModule,
            ToolbarModule,
            DotEditPageViewAsControllerModule,
            DotEditPageStateControllerSeoComponent,
            DotEditPageInfoSeoComponent,
            DotEditPageWorkflowsActionsModule,
            DotSafeHtmlPipe,
            DotMessagePipe,
            DotWizardComponent,
            TooltipModule,
            TagModule,
            DotExperimentClassDirective,
            DotLanguageSelectorComponent,
            RouterTestingModule.withRoutes([
                {
                    path: 'edit-page/experiments/pageId/id/reports',
                    component: TestHostComponent
                }
            ]),
            NoopAnimationsModule
        ],
        providers: [
            DotSessionStorageService,
            { provide: DotLicenseService, useClass: MockDotLicenseService },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.common.whats.changed': 'Whats',
                    'dot.common.cancel': 'Cancel',
                    'favoritePage.dialog.header': 'Add Favorite Page',
                    'dot.edit.page.toolbar.preliminary.results': 'Preliminary Results',
                    running: 'Running',
                    'dot.common.until': 'until'
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
            DotContentletEditorService,
            { provide: DotPersonalizeService, useClass: MockDotPersonalizeService },
            {
                provide: DotWorkflowActionsFireService,
                useClass: MockDotWorkflowActionsFireService
            },
            { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
            { provide: DotcmsConfigService, useClass: DotcmsConfigServiceMock },
            {
                provide: CoreWebService,
                useValue: {
                    request: jest.fn().mockReturnValue(of({})),
                    requestView: jest.fn().mockReturnValue(
                        of({
                            header: jest.fn().mockReturnValue(null),
                            contentlets: mockDotDevices
                        })
                    )
                }
            },
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
            { provide: DotPropertiesService, useClass: MockDotPropertiesService },
            provideHttpClient(),
            provideHttpClientTesting()
        ],
        componentProviders: [{ provide: DotDevicesService, useClass: DotDevicesServiceMock }]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.query(DotEditPageToolbarSeoComponent);

        dotLicenseService = spectator.inject(DotLicenseService);
        dotMessageDisplayService = spectator.inject(DotMessageDisplayService);
        dotDialogService = spectator.inject(DialogService);
        dotPropertiesService = spectator.inject(DotPropertiesService);
        jest.spyOn(dotPropertiesService, 'getFeatureFlag').mockReturnValue(of(true));
    });

    describe('elements', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should have elements placed correctly', () => {
            const editToolbar = spectator.query('dot-secondary-toolbar');
            const editPageInfo = spectator.query(
                'dot-secondary-toolbar .main-toolbar-left dot-edit-page-info-seo'
            );
            const editCancelBtn = spectator.query(
                'dot-secondary-toolbar .main-toolbar-right .edit-page-toolbar__cancel'
            );
            const editWorkflowActions = spectator.query(
                'dot-secondary-toolbar .main-toolbar-right dot-edit-page-workflows-actions'
            );
            const editStateController = spectator.query(
                'dot-secondary-toolbar .lower-toolbar-left dot-edit-page-state-controller'
            );
            const whatsChangedCheck = spectator.query(
                'dot-secondary-toolbar .lower-toolbar-left .dot-edit__what-changed-button'
            );
            const editPageViewAs = spectator.query(
                'dot-secondary-toolbar .lower-toolbar-right dot-edit-page-view-as-controller'
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
            spectator.detectChanges();
            const dotEditPageInfo = spectator.query(DotEditPageInfoSeoComponent);
            expect(dotEditPageInfo.title).toBe('A title');
            expect(dotEditPageInfo.url).toBe('/an/url/test');
        });
    });

    describe('dot-global-message', () => {
        it('should have show', () => {
            spectator.detectChanges();
            const dotGlobalMessage = spectator.query(byTestId('globalMessage'));
            expect(dotGlobalMessage).not.toBeNull();
        });
    });

    describe('dot-edit-page-workflows-actions', () => {
        it('should have pageState attr', () => {
            spectator.detectChanges();
            const dotEditWorkflowActions = spectator.debugElement.query(
                By.css('dot-edit-page-workflows-actions')
            );
            expect(dotEditWorkflowActions.componentInstance.page).toBe(
                mockDotRenderedPageState.page
            );
        });

        it('should emit on click', () => {
            jest.spyOn(component.actionFired, 'emit');
            spectator.detectChanges();
            spectator.triggerEventHandler('dot-edit-page-workflows-actions', 'fired', {});
            expect(component.actionFired.emit).toHaveBeenCalled();
        });
    });

    describe('dot-edit-page-state-controller-seo', () => {
        it('should have pageState attr', () => {
            spectator.detectChanges();
            const dotEditPageState = spectator.query(DotEditPageStateControllerSeoComponent);
            expect(dotEditPageState.pageState).toBe(mockDotRenderedPageState);
        });
    });

    describe('dot-edit-page-view-as-controller', () => {
        it('should have pageState attr', () => {
            spectator.detectChanges();
            const dotEditPageViewAs = spectator.debugElement.query(
                By.css('dot-edit-page-view-as-controller-seo')
            );
            expect(dotEditPageViewAs.componentInstance.pageState).toBe(mockDotRenderedPageState);
        });
    });

    describe("what's change", () => {
        describe('no license', () => {
            it('should not show', () => {
                jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(false));
                // Recreate the component to trigger ngOnInit with the new mock
                spectator = createComponent();
                component = spectator.query(DotEditPageToolbarSeoComponent);
                spectator.detectChanges();

                const whatsChangedElem = spectator.query('.dot-edit__what-changed-button');
                expect(whatsChangedElem).toBeNull();
            });
        });

        describe('with license', () => {
            xit("should have what's change selector", async () => {
                spectator.component.pageState.state.mode = DotPageMode.PREVIEW;
                spectator.detectChanges();
                await spectator.fixture.whenStable();

                const whatsChangedElem = spectator.query('.dot-edit__what-changed-button');
                expect(whatsChangedElem).toBeDefined();

                if (whatsChangedElem) {
                    const whatsChangedDebugElem = spectator.debugElement.query(
                        By.css('.dot-edit__what-changed-button')
                    );
                    const whatsChangedComponent = whatsChangedDebugElem?.componentInstance;
                    expect(whatsChangedComponent?.label).toBe('Whats');
                    expect(whatsChangedComponent?.binary).toBe(true);
                }
            });

            it("should hide what's change selector", () => {
                const newPageRender = new DotPageRender({
                    ...mockDotRenderedPage(),
                    viewAs: {
                        ...mockDotRenderedPage().viewAs,
                        mode: DotPageMode.EDIT
                    }
                });
                const newPageState = new DotPageRenderState(mockUser(), newPageRender);
                spectator.setInput('pageState', newPageState);
                spectator.detectChanges();

                const whatsChangedElem = spectator.query('.dot-edit__what-changed-button');
                expect(whatsChangedElem).toBeNull();
            });

            it("should hide what's change selector when is not default user", () => {
                const newPageRender = new DotPageRender({
                    ...mockDotRenderedPage(),
                    viewAs: {
                        ...mockDotRenderedPage().viewAs,
                        mode: DotPageMode.PREVIEW,
                        persona: mockDotPersona
                    }
                });
                const newPageState = new DotPageRenderState(mockUser(), newPageRender);
                spectator.setInput('pageState', newPageState);
                spectator.detectChanges();

                const whatsChangedElem = spectator.query('.dot-edit__what-changed-button');
                expect(whatsChangedElem).toBeNull();
            });
        });
    });

    describe('Favorite icon', () => {
        it('should change icon on favorite page if contentlet exist', () => {
            spectator.component.pageState = new DotPageRenderState(
                mockUser(),
                new DotPageRender(mockDotRenderedPage()),
                dotcmsContentletMock
            );
            component.showFavoritePageStar = true;

            spectator.detectChanges();

            const favoritePageIcon = spectator.debugElement.query(
                By.css('[data-testId="addFavoritePageButton"]')
            );
            expect(favoritePageIcon.componentInstance.icon).toBe('pi pi-star-fill');
        });

        it('should show empty star icon on favorite page if NO contentlet exist', () => {
            component.showFavoritePageStar = true;

            spectator.detectChanges();

            const favoritePageIcon = spectator.debugElement.query(
                By.css('[data-testId="addFavoritePageButton"]')
            );
            expect(favoritePageIcon.componentInstance.icon).toBe('pi pi-star');
        });
    });

    describe('Go to Experiment results', () => {
        it('should show an experiment is running an go to results', (done) => {
            const location = spectator.inject(Location);
            spectator.component.runningExperiment = {
                pageId: 'pageId',
                id: 'id',
                scheduling: { endDate: 2 }
            } as DotExperiment;

            const expectedStatus =
                'Running until ' + new DatePipe('en-US').transform(2, RUNNING_UNTIL_DATE_FORMAT);

            spectator.detectChanges();

            const experimentTag = spectator.debugElement.query(
                By.css('[data-testId="runningExperimentTag"]')
            );

            experimentTag.nativeElement.click();

            expect(experimentTag.componentInstance.value).toEqual(expectedStatus);
            spectator.fixture.whenStable().then(() => {
                expect(location.path()).toEqual('/edit-page/experiments/pageId/id/reports');
                done();
            });
        });
    });

    describe('events', () => {
        beforeEach(() => {
            jest.spyOn(component.whatschange, 'emit');
            jest.spyOn(dotMessageDisplayService, 'push');
            jest.spyOn(dotDialogService, 'open');
            jest.spyOn(component.favoritePage, 'emit');

            spectator.component.pageState.state.mode = DotPageMode.PREVIEW;
            delete spectator.component.pageState.viewAs.persona;
            component.showFavoritePageStar = true;
            spectator.detectChanges();
        });

        it('should instantiate dialog with DotFavoritePageComponent', () => {
            spectator.click(byTestId('addFavoritePageButton'));
            spectator.detectChanges();

            expect(component.favoritePage.emit).toHaveBeenCalledTimes(1);
        });

        it('should store RenderedHTML value if PREVIEW MODE', () => {
            expect(component.pageRenderedHtml).toBe(mockDotRenderedPageState.page.rendered);
        });

        it("should emit what's change in true", () => {
            spectator.triggerEventHandler('.dot-edit__what-changed-button', 'onChange', {
                checked: true
            });
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(true);
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
        });

        it("should emit what's change in false", () => {
            spectator.triggerEventHandler('.dot-edit__what-changed-button', 'onChange', {
                checked: false
            });
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(false);
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
        });

        describe('whats change on state change', () => {
            it('should emit when showWhatsChanged is true', () => {
                component.showWhatsChanged = true;
                spectator.detectChanges();
                spectator.triggerEventHandler(
                    'dot-edit-page-state-controller-seo',
                    'modeChange',
                    DotPageMode.EDIT
                );

                expect(component.whatschange.emit).toHaveBeenCalledWith(false);
                expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            });

            it('should not emit when showWhatsChanged is false', () => {
                component.showWhatsChanged = false;
                spectator.detectChanges();
                spectator.triggerEventHandler(
                    'dot-edit-page-state-controller-seo',
                    'modeChange',
                    DotPageMode.EDIT
                );

                expect(component.whatschange.emit).not.toHaveBeenCalled();
            });
        });
    });

    it('should have a new api link', async () => {
        const initialLink = component.apiLink;

        const host = `api/v1/page/render${spectator.component.pageState.page.pageURI}`;
        const newLanguageId = 2;
        const expectedLink = `${host}?language_id=${newLanguageId}`;

        spectator.setInput(
            'pageState',
            new DotPageRenderState(
                mockUser(),
                new DotPageRender({
                    containers: mockDotContainers(),
                    layout: mockDotLayout(),
                    page: { ...mockDotPage(), languageId: 2 },
                    template: mockDotTemplate(),
                    canCreateTemplate: true,
                    numberContents: 1,
                    viewAs: {
                        language: mockDotLanguage,
                        mode: DotPageMode.PREVIEW
                    }
                }),
                dotcmsContentletMock
            )
        );

        spectator.detectChanges();
        await spectator.fixture.whenStable();

        expect(component.apiLink).toBe(expectedLink);
        expect(component.apiLink).not.toBe(initialLink);
    });
});
