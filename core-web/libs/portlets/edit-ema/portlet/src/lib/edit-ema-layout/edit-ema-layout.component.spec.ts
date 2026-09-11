import { SpyObject } from '@openng/spectator';
import { Spectator, createComponentFactory, mockProvider } from '@openng/spectator/vitest';
import { MockComponent, MockProvider } from 'ng-mocks';
import { Subject, of, throwError } from 'rxjs';
import { Mock, describe, expect, vi } from 'vitest';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import {
    DotAnalyticsTrackerService,
    DotContentTypeService,
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPageLayoutService,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { GlobalStore } from '@dotcms/store';
import { TemplateBuilderComponent } from '@dotcms/template-builder';
import { WINDOW } from '@dotcms/utils';
import {
    CurrentUserDataMock,
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    MockDotRouterJestService
} from '@dotcms/utils-testing';

import { DEBOUNCE_TIME, EditEmaLayoutComponent } from './edit-ema-layout.component';

import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api/dot-page-api.service';
import { PERSONA_KEY } from '../shared/consts';
import { UVE_STATUS } from '../shared/enums';
import { UVEStore } from '../store/dot-uve.store';
import { WithPageApiMethods } from '../store/features/page-api/withPageApi';

const PAGE_RESPONSE = {
    containers: {},
    page: {
        identifier: 'test'
    },
    template: {
        theme: 'testTheme'
    },
    layout: {
        body: {
            rows: [
                {
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: 'test'
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    }
};

// Gridstack has some issues with importing (esm/cjs), Jest need to process it to work using the transformIgnorePatterns, but that takes a lot of time
// So we mock it to avoid that
vi.mock('gridstack', () => ({
    __esModule: true,
    default: vi.fn()
}));

describe('EditEmaLayoutComponent', () => {
    let spectator: Spectator<EditEmaLayoutComponent>;
    let component: EditEmaLayoutComponent;
    let dotRouter: SpyObject<DotRouterService>;
    let store: SpyObject<InstanceType<typeof UVEStore>>;

    // `withPageApi`'s methods reach the UVEStore type through an index signature, so
    // a plain `vi.spyOn(store, ...)` cannot see them. Spying through the feature's own
    // interface keeps the call typed and still installs the spy on the real store.
    const pageApi = () => store as unknown as WithPageApiMethods;
    let templateBuilder: TemplateBuilderComponent;
    let dotPageLayoutService: DotPageLayoutService;
    let messageService: MessageService;

    globalThis.structuredClone = vi.fn().mockImplementation((obj) => obj);

    const createComponent = createComponentFactory({
        component: EditEmaLayoutComponent,
        imports: [MockComponent(TemplateBuilderComponent)],
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            UVEStore,
            DotMessageService,
            DotActionUrlService,
            mockProvider(DialogService),
            mockProvider(MessageService),
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotContentTypeService),
            {
                provide: DotAnalyticsTrackerService,
                useValue: {
                    track: vi.fn()
                }
            },
            mockProvider(DotPageLayoutService, {
                save: vi.fn(() => of(PAGE_RESPONSE))
            }),
            mockProvider(DotPageApiService, {
                get: vi.fn(() => of(PAGE_RESPONSE))
            }),
            mockProvider(DotWorkflowsActionsService, {
                getByInode: vi.fn(() => of([]))
            }),
            mockProvider(DotWorkflowActionsFireService),
            {
                provide: GlobalStore,
                useValue: { loggedUser: signal(CurrentUserDataMock) }
            },
            mockProvider(ConfirmationService),
            MockProvider(DotExperimentsService, DotExperimentsServiceMock, 'useValue'),
            MockProvider(DotRouterService, new MockDotRouterJestService(vi), 'useValue'),
            MockProvider(DotLanguagesService, new DotLanguagesServiceMock(), 'useValue'),
            MockProvider(
                DotLicenseService,
                {
                    isEnterprise: () => of(true)
                },
                'useValue'
            ),
            MockProvider(
                DotContentletLockerService,
                {
                    unlock: (_inode: string) => of({})
                },
                'useValue'
            ),
            MockProvider(
                LoginService,
                {
                    getCurrentUser: () => of({})
                },
                'useValue'
            ),
            {
                provide: WINDOW,
                useValue: window
            }
        ]
    });

    beforeEach(async () => {
        vi.clearAllMocks();

        spectator = createComponent();
        component = spectator.component;
        dotRouter = spectator.inject(DotRouterService);
        store = spectator.inject(UVEStore, true);
        dotPageLayoutService = spectator.inject(DotPageLayoutService);
        messageService = spectator.inject(MessageService);

        // Reset save mock to default — vi.clearAllMocks() does not reset mockReturnValue/
        // mockImplementation overrides, so tests that call mockReturnValue(throwError(...))
        // would contaminate subsequent tests that rely on the default of(PAGE_RESPONSE) behavior.
        (dotPageLayoutService.save as Mock).mockImplementation(() => of(PAGE_RESPONSE));

        store.pageLoad({
            clientHost: 'http://localhost:3000',
            language_id: '1',
            url: 'test',
            [PERSONA_KEY]: 'SuperCoolDude'
        });

        spectator.detectChanges();

        templateBuilder = spectator.debugElement.query(
            By.css('[data-testId="edit-ema-layout"]')
        ).componentInstance;
    });

    describe('Template Change', () => {
        it('should forbid navigation', () => {
            templateBuilder.templateChange.emit();
            expect(dotRouter.forbidRouteDeactivation).toHaveBeenCalled();
        });

        it('should set uveStatus to LOADING immediately when templateChange is emitted', fakeAsync(() => {
            const setUveStatusSpy = vi.spyOn(store, 'setUveStatus');

            templateBuilder.templateChange.emit();

            // tap fires synchronously before debounce — no tick needed
            expect(setUveStatusSpy).toHaveBeenCalledWith(UVE_STATUS.LOADING);

            tick(5000); // flush timer to avoid pending-timer warning
        }));

        it('should trigger a save after 5 secs', fakeAsync(() => {
            const reloadSpy = vi.spyOn(pageApi(), 'pageReload');

            templateBuilder.templateChange.emit();
            tick(5000);

            expect(dotPageLayoutService.save).toHaveBeenCalled();
            expect(reloadSpy).toHaveBeenCalled();

            expect(messageService.add).toHaveBeenNthCalledWith(1, {
                severity: 'info',
                summary: 'Info',
                detail: 'dot.common.message.saving',
                life: 1000
            });

            expect(messageService.add).toHaveBeenNthCalledWith(2, {
                severity: 'success',
                summary: 'Success',
                detail: 'dot.common.message.saved'
            });
        }));

        it('should unlock navigation after saving', fakeAsync(() => {
            templateBuilder.templateChange.emit();
            tick(6000);

            expect(dotRouter.allowRouteDeactivation).toHaveBeenCalled();
        }));

        it('should set isClientReady false after saving', fakeAsync(() => {
            templateBuilder.templateChange.emit();
            tick(6000);

            expect(store.isClientReady()).toBe(false);
        }));

        it('should save right away if we request page leave before the 5 secs', () => {
            const saveTemplate = vi.spyOn(component, 'saveTemplate');

            templateBuilder.templateChange.emit();

            dotRouter.requestPageLeave(); // This is what the guard triggers if the page is forbid to navigate

            expect(saveTemplate).toHaveBeenCalled();

            expect(messageService.add).toHaveBeenNthCalledWith(1, {
                severity: 'info',
                summary: 'Info',
                detail: 'dot.common.message.saving',
                life: 1000
            });

            expect(messageService.add).toHaveBeenNthCalledWith(2, {
                severity: 'success',
                summary: 'Success',
                detail: 'dot.common.message.saved'
            });
        });
    });

    describe('Canvas lock (#layoutSaveInFlight)', () => {
        it('should drop templateChange events and not forbid navigation while save is in-flight', fakeAsync(() => {
            const saveSubject = new Subject();
            (dotPageLayoutService.save as Mock).mockReturnValue(saveSubject.asObservable());

            // First emit starts the debounce; forbidRouteDeactivation called once
            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME); // debounce fires → POST sent → #layoutSaveInFlight = true

            // Save still in-flight. Second emit should be dropped by the in-flight guard.
            templateBuilder.templateChange.emit();

            expect(dotRouter.forbidRouteDeactivation).toHaveBeenCalledTimes(1);

            saveSubject.complete(); // clean up
        }));

        it('should process templateChange events and forbid navigation when not saving', () => {
            templateBuilder.templateChange.emit();

            expect(dotRouter.forbidRouteDeactivation).toHaveBeenCalledTimes(1);
        });

        it('should pass disabled=true to the template builder while save is in-flight', fakeAsync(() => {
            const saveSubject = new Subject();
            (dotPageLayoutService.save as Mock).mockReturnValue(saveSubject.asObservable());

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME);
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(true);

            saveSubject.complete();
        }));

        it('should pass disabled=false to the template builder when not saving', () => {
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(false);
        });

        it('should unlock canvas (disabled=false) after save completes', fakeAsync(() => {
            const saveSubject = new Subject();
            (dotPageLayoutService.save as Mock).mockReturnValue(saveSubject.asObservable());

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME);

            saveSubject.next(PAGE_RESPONSE);
            saveSubject.complete();
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(false);
        }));

        it('should unlock canvas (disabled=false) on save error', fakeAsync(() => {
            (dotPageLayoutService.save as Mock).mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 400 }))
            );

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME);
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(false);
        }));

        it('should unlock canvas when pageReload fails (uveStatus = ERROR) to avoid a permanent lock', fakeAsync(() => {
            const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload').mockImplementation(vi.fn());

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME);
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(true);
            expect(pageReloadSpy).toHaveBeenCalled();

            // Simulate re-fetch failure
            store.setUveStatus(UVE_STATUS.ERROR);
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(false);
        }));

        it('should keep canvas locked through the pageReload window and unlock only when reload completes', fakeAsync(() => {
            // Prevent the real pageReload from running so we can control when it "finishes"
            const pageReloadSpy = vi.spyOn(pageApi(), 'pageReload').mockImplementation(vi.fn());

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME); // POST fires and succeeds synchronously (default mock)
            spectator.detectChanges();

            // POST returned 200 but reload hasn't signalled LOADED yet — canvas must stay locked
            expect(templateBuilder.disabled).toBe(true);
            expect(pageReloadSpy).toHaveBeenCalled();

            // Simulate the reload completing
            store.setUveStatus(UVE_STATUS.LOADED);
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(false);
        }));
    });

    describe('Serialized save paths (AC3 — no concurrent layout saves)', () => {
        it('should not fire a second save when force-save-on-leave is requested while the debounced save is in-flight', fakeAsync(() => {
            const saveSubject = new Subject();
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(saveSubject.asObservable());

            // Debounce fires -> POST #1 sent, #layoutSaveInFlight = true
            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME);

            expect(dotPageLayoutService.save).toHaveBeenCalledTimes(1);

            // User tries to leave while POST #1 is still in flight — must not send POST #2
            dotRouter.requestPageLeave();

            expect(dotPageLayoutService.save).toHaveBeenCalledTimes(1);

            saveSubject.next(PAGE_RESPONSE);
            saveSubject.complete();
        }));

        it('should skip the debounced save when force-save-on-leave already sent one for the same template', fakeAsync(() => {
            const saveSubject = new Subject();
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(saveSubject.asObservable());

            templateBuilder.templateChange.emit();

            // Leave requested before the 5s debounce elapses -> force-save fires right away
            dotRouter.requestPageLeave();

            expect(dotPageLayoutService.save).toHaveBeenCalledTimes(1);

            // The original debounce timer is still armed and will elapse on its own —
            // its switchMap must skip firing a redundant duplicate POST
            tick(DEBOUNCE_TIME);

            expect(dotPageLayoutService.save).toHaveBeenCalledTimes(1);

            saveSubject.next(PAGE_RESPONSE);
            saveSubject.complete();
        }));

        it('should unblock route deactivation from the in-flight save even when the skipped duplicate is the one requesting to leave', fakeAsync(() => {
            const saveSubject = new Subject();
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(saveSubject.asObservable());

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME); // POST #1 in-flight

            dotRouter.requestPageLeave(); // skipped — POST #1 already in-flight

            expect(dotRouter.allowRouteDeactivation).not.toHaveBeenCalled();

            saveSubject.next(PAGE_RESPONSE);
            saveSubject.complete();

            expect(dotRouter.allowRouteDeactivation).toHaveBeenCalled();
        }));

        it('should unblock route deactivation when the force-save-on-leave request itself fails', () => {
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 500 }))
            );

            templateBuilder.templateChange.emit();

            // Leave requested before the 5s debounce elapses -> force-save fires right away
            // and fails. Without allowRouteDeactivation() in a finalize(), the user would be
            // stuck on the page with no way to retry (pageLeaveRequest$ is distinctUntilChanged).
            dotRouter.requestPageLeave();

            expect(dotRouter.allowRouteDeactivation).toHaveBeenCalled();
        });
    });
});
