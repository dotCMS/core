/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@ngneat/spectator';
import { of, Subject } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, EventEmitter, Injectable, Output } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By, Title } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotDownloadBundleDialogModule } from '@components/_common/dot-download-bundle-dialog/dot-download-bundle-dialog.module';
import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotCustomEventHandlerService } from '@dotcms/app/api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotUiColorsService } from '@dotcms/app/api/services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotCurrentUserService,
    DotEventsService,
    DotGenerateSecurePasswordService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotRouterService,
    DotSessionStorageService,
    DotWorkflowActionsFireService,
    DotGlobalMessageService,
    DotIframeService,
    DotWizardService,
    DotWorkflowEventHandlerService,
    PushPublishService,
    DotFormatDateService,
    DotPageStateService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { DotPageRender, DotPageRenderState } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    MockDotMessageService,
    mockDotRenderedPage,
    MockDotRouterService,
    mockUser
} from '@dotcms/utils-testing';
import { DotEditPageNavDirective } from '@portlets/dot-edit-page/main/dot-edit-page-nav/directives/dot-edit-page-nav.directive';
import { DotExperimentClassDirective } from '@portlets/shared/directives/dot-experiment-class.directive';

import { DotEditPageMainComponent } from './dot-edit-page-main.component';

import { DotEditPageNavComponent } from '../dot-edit-page-nav/dot-edit-page-nav.component';
import { DotEditPageNavModule } from '../dot-edit-page-nav/dot-edit-page-nav.module';

@Injectable()
class MockDotContentletEditorService {
    close$ = new Subject();
}

@Injectable()
class MockDotPageStateService {
    reload$ = new Subject();
    state$ = new Subject();

    get(): void {
        //
    }

    reload(): void {
        this.reload$.next(
            new DotPageRenderState(mockUser(), new DotPageRender(mockDotRenderedPage()))
        );
    }
}

@Component({
    selector: 'dot-edit-contentlet',
    template: ''
})
class MockDotEditContentletComponent {
    @Output() custom = new EventEmitter<any>();
}

describe('DotEditPageMainComponent', () => {
    let fixture: ComponentFixture<DotEditPageMainComponent>;
    let route: ActivatedRoute;
    let dotContentletEditorService: DotContentletEditorService;
    let dotPageStateService: DotPageStateService;
    let dotRouterService: DotRouterService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;
    let editContentlet: MockDotEditContentletComponent;
    let titleService: Title;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.layout': 'Layout',
        'editpage.toolbar.nav.properties': 'Properties'
    });

    const mockDotRenderedPageState: DotPageRenderState = new DotPageRenderState(
        mockUser(),
        new DotPageRender(mockDotRenderedPage())
    );

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule.withRoutes([
                    {
                        component: DotEditPageMainComponent,
                        path: ''
                    }
                ]),
                DotEditPageNavModule,
                DotDownloadBundleDialogModule,
                HttpClientTestingModule,
                DotExperimentClassDirective,
                DotEditPageNavDirective
            ],
            declarations: [DotEditPageMainComponent, MockDotEditContentletComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({
                            content: new DotPageRender(mockDotRenderedPage())
                        }),
                        snapshot: {
                            queryParams: {
                                url: '/about-us/index'
                            }
                        },
                        queryParams: of({ mode: 'a', variantName: 'b', experimentId: 'c' })
                    }
                },

                {
                    provide: DotContentletEditorService,
                    useClass: MockDotContentletEditorService
                },
                {
                    provide: DotPageStateService,
                    useClass: MockDotPageStateService
                },
                DotCustomEventHandlerService,
                DotLoadingIndicatorService,
                DotWorkflowEventHandlerService,
                PushPublishService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                PushPublishService,
                ApiRoot,
                DotFormatDateService,
                UserModel,
                StringUtils,
                DotcmsEventsService,
                LoggerService,
                DotGenerateSecurePasswordService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                LoggerService,
                DotCurrentUserService,
                DotMessageDisplayService,
                DotWizardService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotEventsService,
                DotIframeService,
                LoginService,
                DotLicenseService,
                Title,
                mockProvider(DotSessionStorageService),
                mockProvider(DotContentTypeService)
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageMainComponent);
        route = fixture.debugElement.injector.get(ActivatedRoute);
        route = TestBed.inject(ActivatedRoute);
        route.data = of({
            content: mockDotRenderedPageState
        });
        dotContentletEditorService = fixture.debugElement.injector.get(DotContentletEditorService);
        dotRouterService = fixture.debugElement.injector.get(DotRouterService);
        dotPageStateService = fixture.debugElement.injector.get(DotPageStateService);
        dotCustomEventHandlerService = fixture.debugElement.injector.get(
            DotCustomEventHandlerService
        );
        editContentlet = fixture.debugElement.query(
            By.css('dot-edit-contentlet')
        ).componentInstance;
        titleService = fixture.debugElement.injector.get(Title);
        fixture.detectChanges();

        spyOn<any>(route, 'queryParams').and.returnValue(of({}));
    });

    it('should have router-outlet', () => {
        expect(fixture.debugElement.query(By.css('router-outlet'))).not.toBeNull();
    });

    it('should have dot-edit-page-nav', () => {
        expect(fixture.debugElement.query(By.css('dot-edit-page-nav'))).not.toBeNull();
    });

    it('should bind correctly pageState param', () => {
        const nav: DotEditPageNavComponent = fixture.debugElement.query(
            By.css('dot-edit-page-nav')
        ).componentInstance;
        expect(nav.pageState).toEqual(mockDotRenderedPageState);
    });

    it('should not call goToEditPage if the dialog is closed without new page properties', () => {
        spyOn(dotPageStateService, 'get').and.callThrough();

        dotContentletEditorService.close$.next(true);
        expect(dotRouterService.goToEditPage).not.toHaveBeenCalled();
        expect(dotPageStateService.get).not.toHaveBeenCalled();
    });

    it('should call goToEditPage if page properties were saved with different URLs', () => {
        spyOn(dotPageStateService, 'get').and.callThrough();
        editContentlet.custom.emit({
            detail: {
                name: 'save-page',
                payload: {
                    htmlPageReferer: '/index'
                }
            }
        });

        dotContentletEditorService.close$.next(true);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            url: '/index',
            language_id: '1'
        });
        dotContentletEditorService.close$.next(true);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledTimes(1);
        expect(dotPageStateService.get).not.toHaveBeenCalled();
    });

    it('should call get if page properties were saved with equal URLs', () => {
        spyOn(dotPageStateService, 'get').and.callThrough();
        editContentlet.custom.emit({
            detail: {
                name: 'save-page',
                payload: {
                    htmlPageReferer: '/about-us/index'
                }
            }
        });

        dotContentletEditorService.close$.next(true);
        expect(dotPageStateService.get).toHaveBeenCalledWith({
            url: '/about-us/index',
            viewAs: { language: 1 }
        });
    });

    it('should set the page title correctly', () => {
        spyOn(titleService, 'getTitle').and.callThrough();
        const initialTitle = titleService.getTitle().split(' - ');
        const res: DotPageRender = new DotPageRender(mockDotRenderedPage());
        const subtTitle = initialTitle.length > 1 ? initialTitle[initialTitle.length - 1] : '';

        expect(titleService.getTitle()).toBe(
            `${res.page.title}${subtTitle ? ` - ${subtTitle}` : ''}`
        );
    });

    describe('handle custom events from contentlet editor', () => {
        it('should reload page when url attribute in dialog has been changed', () => {
            editContentlet.custom.emit({
                detail: {
                    name: 'save-page',
                    payload: {
                        htmlPageReferer:
                            '/about-us/index2?com.dotmarketing.htmlpage.language=1&host_id=48190c8c-42c4-46af-8d1a-0cd5db894797'
                    }
                }
            });
            dotContentletEditorService.close$.next(true);

            expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                url: '/about-us/index2',
                language_id: mockDotRenderedPage().page.languageId.toString()
            });
        });

        it('should go to site-browser when page is deleted', () => {
            editContentlet.custom.emit({
                detail: {
                    name: 'deleted-page'
                }
            });
            expect(dotRouterService.goToSiteBrowser).toHaveBeenCalledTimes(1);
        });

        it('should call dotCustomEventHandlerService on customEvent', () => {
            spyOn(dotCustomEventHandlerService, 'handle');
            editContentlet.custom.emit({
                detail: {
                    name: 'random'
                }
            });
            expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith({
                detail: {
                    name: 'random'
                }
            });
        });
    });

    describe('Edit Page in Variant Mode', () => {
        it('should add class edit-page-variant-mode to the page nav if exist mode, variationName, experimentId as query params', () => {
            const nav: DotEditPageNavComponent = fixture.debugElement.query(
                By.css('dot-edit-page-nav')
            ).nativeElement;

            expect(nav).toHaveClass('edit-page-variant-mode');
        });
    });
});
