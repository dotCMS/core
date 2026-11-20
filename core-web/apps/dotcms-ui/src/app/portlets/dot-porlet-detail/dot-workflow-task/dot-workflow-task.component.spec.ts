/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@openng/spectator/vitest';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { type Mock, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DebugElement, Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotCurrentUserService,
    DotEventsService,
    DotFormatDateService,
    DotGenerateSecurePasswordService,
    DotGlobalMessageService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotPropertiesService,
    DotRouterService,
    DotUiColorsService,
    DotWizardService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    PushPublishService
} from '@dotcms/data-access';
import {
    ApiRoot,
    DotcmsConfigService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import {
    LoginServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotWorkflowTaskComponent } from './dot-workflow-task.component';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { MockDotUiColorsService } from '../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../view/components/_common/iframe/service/iframe-overlay.service';
import { DotWorkflowTaskDetailComponent } from '../../../view/components/dot-workflow-task-detail/dot-workflow-task-detail.component';
import { DotWorkflowTaskDetailService } from '../../../view/components/dot-workflow-task-detail/services/dot-workflow-task-detail.service';

@Injectable()
class MockDotWorkflowTaskDetailService {
    view = vi.fn();
    setHeader = vi.fn();
}

const messageServiceMock = new MockDotMessageService({
    'workflow.task.dialog.header': 'Task Detail'
});

const createFeatureFlagResponse = (enabled = 'NOT_FOUND') => ({
    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: enabled
});

/** The task the route names. */
const TASK_ID = '74cabf7a-0e9d-48b6-ab1c-8f76d0ad31e0';

describe('DotWorkflowTaskComponent', () => {
    /** The route's params, as the router emits them on each navigation that changes them. */
    let params$: BehaviorSubject<Params>;
    /** The URL's query params: the panel params a task's link carries. */
    let queryParams$: BehaviorSubject<Params>;
    let fixture: ComponentFixture<DotWorkflowTaskComponent>;
    let de: DebugElement;
    let component: DotWorkflowTaskComponent;
    let dotRouterService: DotRouterService;
    let dotIframeService: DotIframeService;
    let dotWorkflowTaskDetailService: DotWorkflowTaskDetailService;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;
    let taskDetail: DebugElement;
    /** What `onPanelEvents` hands back; the component calls it when it goes away. */
    let releasePanelEvents: Mock;

    /**
     * Builds the component and runs its first change detection, which views the route's task.
     *
     * @param messageService What the app's `DotMessageService` is.
     * @param beforeInit Runs before the first change detection, which initialises the component.
     */
    const setup = (messageService: unknown, beforeInit?: () => void): void => {
        TestBed.configureTestingModule({
            imports: [
                DotWorkflowTaskDetailComponent,
                BrowserAnimationsModule,
                RouterTestingModule,
                DotWorkflowTaskComponent
            ],
            providers: [
                {
                    provide: ActivatedRoute,
                    useFactory: () => {
                        params$ = new BehaviorSubject<Params>({ asset: TASK_ID });
                        queryParams$ = new BehaviorSubject<Params>({});

                        return {
                            params: params$,
                            queryParams: queryParams$,
                            snapshot: { params: { asset: TASK_ID } }
                        };
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageService
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotCustomEventHandlerService,
                DotLicenseService,
                DotMenuService,
                IframeOverlayService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                DotIframeService,
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotDownloadBundleDialogService,
                DotWorkflowEventHandlerService,
                PushPublishService,
                ApiRoot,
                UserModel,
                LoggerService,
                StringUtils,
                provideHttpClient(),
                provideHttpClientTesting(),
                DotCurrentUserService,
                DotMessageDisplayService,
                DotcmsConfigService,
                DotWizardService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotFormatDateService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotGenerateSecurePasswordService,
                DotEventsService,
                mockProvider(DotContentTypeService),
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKeys: () => of(createFeatureFlagResponse())
                    }
                }
            ]
        });

        TestBed.overrideComponent(DotWorkflowTaskComponent, {
            set: {
                providers: [
                    {
                        provide: DotWorkflowTaskDetailService,
                        useClass: MockDotWorkflowTaskDetailService
                    }
                ]
            }
        });

        fixture = TestBed.createComponent(DotWorkflowTaskComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        // Get the service from the component's injector since it has its own provider
        dotWorkflowTaskDetailService = de.injector.get(DotWorkflowTaskDetailService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotIframeService = TestBed.inject(DotIframeService);
        dotCustomEventHandlerService = TestBed.inject(DotCustomEventHandlerService);
        releasePanelEvents = vi.fn();
        Object.assign(dotCustomEventHandlerService, {
            onPanelEvents: vi.fn().mockReturnValue(releasePanelEvents)
        });
        vi.spyOn(dotIframeService, 'reloadData');
        beforeInit?.();
        fixture.detectChanges();
        taskDetail = de.query(By.css('dot-workflow-task-detail'));
    };

    beforeEach(() => setup(messageServiceMock));

    /**
     * On a fresh load of a task's URL the dialog opens while the app's messages are still loading,
     * and `get` answers with the key until then. The task shows at once, so the dialog opens in
     * the order the URL names it, under a side panel the URL also names; its header is set once
     * the messages load, so it reads "Task Detail", never `workflow.task.dialog.header` (#38303,
     * T177).
     */
    describe('before the messages load', () => {
        let messagesReady$: Subject<void>;

        beforeEach(() => {
            messagesReady$ = new Subject<void>();
            let loaded = false;
            const loading = {
                ready$: messagesReady$,
                get: (key: string, ...args: string[]) =>
                    loaded ? messageServiceMock.get(key, ...args) : key
            };
            messagesReady$.subscribe(() => (loaded = true));

            TestBed.resetTestingModule();
            setup(loading);
        });

        it('should show the task at once, without a header', () => {
            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(1);
            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith({ id: TASK_ID });
            expect(dotWorkflowTaskDetailService.setHeader).not.toHaveBeenCalled();
        });

        it('should set the translated header once the messages load, and not show the task again', () => {
            messagesReady$.next();

            expect(dotWorkflowTaskDetailService.setHeader).toHaveBeenCalledTimes(1);
            expect(dotWorkflowTaskDetailService.setHeader).toHaveBeenCalledWith('Task Detail');
            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(1);
        });
    });

    it('should call workflow task modal', () => {
        expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledWith({ id: TASK_ID });
        expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(1);
        expect(dotWorkflowTaskDetailService.setHeader).toHaveBeenCalledWith('Task Detail');
    });

    /**
     * The route is reused across tasks (no `reuseRoute: false` on `/c/:id/:asset`), so the task
     * follows the route's params: another task shows in the dialog, and a navigation that keeps
     * the task (Back with its Edit Content panel open) leaves the dialog as it is.
     */
    describe('following the route', () => {
        it('should show the task a later navigation names', () => {
            params$.next({ asset: 'other-task' });

            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(2);
            expect(dotWorkflowTaskDetailService.view).toHaveBeenLastCalledWith({
                id: 'other-task'
            });
        });

        it('should not load the task again for a navigation that keeps it', () => {
            params$.next({ asset: TASK_ID });

            expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(1);
        });
    });

    it('should redirect to /workflow and refresh data when modal closed', () => {
        taskDetail.triggerEventHandler('shutdown', {});
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/workflow');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('workflow');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should redirect to /workflow when edit-task-executed-workflow event is triggered', () => {
        vi.spyOn(component, 'onCloseWorkflowTaskEditor');
        taskDetail.triggerEventHandler('custom', {
            detail: {
                name: 'edit-task-executed-workflow'
            }
        });
        expect(component.onCloseWorkflowTaskEditor).toHaveBeenCalledTimes(1);
    });

    it('should redirect to /workflow when close event is triggered', () => {
        taskDetail.triggerEventHandler('custom', {
            detail: {
                name: 'close'
            }
        });
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('/c/workflow');
        expect(dotRouterService.gotoPortlet).toHaveBeenCalledTimes(1);
        expect(dotIframeService.reloadData).toHaveBeenCalledWith('workflow');
        expect(dotIframeService.reloadData).toHaveBeenCalledTimes(1);
    });

    it('should call to dotCustomEventHandlerService with the correct callbaack', () => {
        vi.spyOn(dotCustomEventHandlerService, 'handle');
        const mockEvent = {
            detail: {
                name: 'workflow-wizard',
                data: {
                    workflow: {
                        actionInputs: []
                    },
                    callback: 'test',
                    inode: '123'
                }
            }
        };

        taskDetail.triggerEventHandler('custom', mockEvent);
        expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith(mockEvent);
    });

    /**
     * The detail's "Edit Content" sends `edit-contentlet` (`view_workflow_task.jsp`, `doEdit`):
     * the content opens in the side panel above the task dialog. The detail is viewed again on
     * every save in a panel, and on a close `DotCustomEventHandlerService` reports (after the
     * legacy editor only), so it shows the content's current title and step; the handlers are
     * registered with that service, so a panel the task's URL reopens gets them too (#38303,
     * FR-025a, stack review L7-2).
     */
    describe('edit-contentlet from the task detail', () => {
        let openContent: Mock;

        beforeEach(() => {
            openContent = vi
                .spyOn(dotCustomEventHandlerService, 'openContent')
                .mockImplementation(() => undefined) as unknown as Mock;
            vi.spyOn(dotCustomEventHandlerService, 'handle');

            taskDetail.triggerEventHandler('custom', {
                detail: {
                    name: 'edit-contentlet',
                    data: { inode: 'content-inode', contentType: 'Blog' }
                }
            });
        });

        it('should open the content through openContent with the inode and content type the detail sent', () => {
            expect(openContent).toHaveBeenCalledTimes(1);
            expect(openContent.mock.calls[0]).toEqual([
                { mode: 'edit', inode: 'content-inode', contentType: 'Blog' }
            ]);
            expect(dotCustomEventHandlerService.handle).not.toHaveBeenCalled();
        });
    });

    describe('when a side panel saves or closes', () => {
        const TASK = { id: TASK_ID };

        /** The panel handlers the component registered on init. */
        const registered = (): { saved: () => void; closed: () => void } => {
            const onPanelEvents = (
                dotCustomEventHandlerService as unknown as { onPanelEvents: Mock }
            ).onPanelEvents;

            expect(
                onPanelEvents,
                'the component registers its panel handlers'
            ).toHaveBeenCalledTimes(1);

            return onPanelEvents.mock.calls[0][0];
        };

        it.each(['saved', 'closed'] as const)(
            'should view the same task again, once, when a panel reports %s',
            (event) => {
                registered()[event]();

                expect(dotWorkflowTaskDetailService.view).toHaveBeenCalledTimes(2);
                expect(dotWorkflowTaskDetailService.view).toHaveBeenLastCalledWith(TASK);
            }
        );

        it.each(['saved', 'closed'] as const)(
            'should keep the task open when a panel reports %s, not return to the task list',
            (event) => {
                registered()[event]();

                expect(dotRouterService.gotoPortlet).not.toHaveBeenCalled();
            }
        );

        it('should release the handlers when the task closes', () => {
            registered();

            fixture.destroy();

            expect(releasePanelEvents).toHaveBeenCalledTimes(1);
        });
    });

    /**
     * A task's link may also name a side panel. The portlet behind leaves it to the task detail,
     * which opens it once its dialog has shown: a dialog that shows later stacks above any panel,
     * so the panel must come second, whichever of the menu call and the panel's chunk is faster
     * (#38303, stack review L7-6).
     */
    describe('a panel the task URL names', () => {
        const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

        let openContent: Mock;

        beforeEach(() => {
            openContent = vi
                .spyOn(dotCustomEventHandlerService, 'openContent')
                .mockImplementation(() => undefined) as unknown as Mock;
            queryParams$.next({ editContent: IDENTIFIER, editContentLang: '2' });
        });

        it('should wait for the task dialog to show', () => {
            expect(openContent).not.toHaveBeenCalled();
        });

        it('should open it once the task dialog has shown, once', () => {
            taskDetail.triggerEventHandler('shown', undefined);
            taskDetail.triggerEventHandler('shown', undefined);

            expect(openContent).toHaveBeenCalledTimes(1);
            expect(openContent).toHaveBeenCalledWith({
                mode: 'edit',
                identifier: IDENTIFIER,
                languageId: 2
            });
        });
    });

    /**
     * Back or Forward onto a task's entry that still names a panel the author left (a menu click
     * away closes the panel and writes nothing to that entry) opens nothing: the panel URL service
     * drops the params, and the task only follows the app's own navigations. The task reads the
     * URL during the navigation that shows it, when the router can tell a Back from the app's own
     * navigation, not once its dialog has shown, after the navigation ended (#38303, review R4).
     */
    describe('a panel the task URL names on Back or Forward', () => {
        const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

        let openContent: Mock;
        let currentNavigation: Mock;

        beforeEach(() => {
            TestBed.resetTestingModule();
            setup(messageServiceMock, () => {
                currentNavigation = vi
                    .spyOn(TestBed.inject(Router), 'currentNavigation')
                    .mockReturnValue({ trigger: 'popstate' } as ReturnType<
                        Router['currentNavigation']
                    >) as unknown as Mock;
                queryParams$.next({ editContent: IDENTIFIER, editContentLang: '2' });
            });
            // The Back navigation ends once the route is shown.
            currentNavigation.mockReturnValue(null);
            openContent = vi
                .spyOn(dotCustomEventHandlerService, 'openContent')
                .mockImplementation(() => undefined) as unknown as Mock;
        });

        it('should not open it once the task dialog has shown', () => {
            taskDetail.triggerEventHandler('shown', undefined);

            expect(openContent).not.toHaveBeenCalled();
        });

        it('should open a panel a later navigation of the app names', () => {
            taskDetail.triggerEventHandler('shown', undefined);
            queryParams$.next({ editContent: IDENTIFIER, editContentLang: '1' });

            expect(openContent).toHaveBeenCalledTimes(1);
            expect(openContent).toHaveBeenCalledWith({
                mode: 'edit',
                identifier: IDENTIFIER,
                languageId: 1
            });
        });
    });
});
