import { expect, describe } from '@jest/globals';
import { SpyObject } from '@openng/spectator';
import { Spectator, createComponentFactory, mockProvider } from '@openng/spectator/jest';
import { MockComponent, MockProvider } from 'ng-mocks';
import { Subject, of, throwError } from 'rxjs';

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
jest.mock('gridstack', () => ({
    __esModule: true,
    default: jest.fn()
}));

describe('EditEmaLayoutComponent', () => {
    let spectator: Spectator<EditEmaLayoutComponent>;
    let component: EditEmaLayoutComponent;
    let dotRouter: SpyObject<DotRouterService>;
    let store: SpyObject<InstanceType<typeof UVEStore>>;
    let templateBuilder: TemplateBuilderComponent;
    let dotPageLayoutService: DotPageLayoutService;
    let messageService: MessageService;

    globalThis.structuredClone = jest.fn().mockImplementation((obj) => obj);

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
                    track: jest.fn()
                }
            },
            mockProvider(DotPageLayoutService, {
                save: jest.fn(() => of(PAGE_RESPONSE))
            }),
            mockProvider(DotPageApiService, {
                get: jest.fn(() => of(PAGE_RESPONSE))
            }),
            mockProvider(DotWorkflowsActionsService, {
                getByInode: jest.fn(() => of([]))
            }),
            mockProvider(DotWorkflowActionsFireService),
            {
                provide: GlobalStore,
                useValue: { loggedUser: signal(CurrentUserDataMock) }
            },
            mockProvider(ConfirmationService),
            MockProvider(DotExperimentsService, DotExperimentsServiceMock, 'useValue'),
            MockProvider(DotRouterService, new MockDotRouterJestService(jest), 'useValue'),
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
        jest.clearAllMocks();

        spectator = createComponent();
        component = spectator.component;
        dotRouter = spectator.inject(DotRouterService);
        store = spectator.inject(UVEStore, true);
        dotPageLayoutService = spectator.inject(DotPageLayoutService);
        messageService = spectator.inject(MessageService);

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
            const setUveStatusSpy = jest.spyOn(store, 'setUveStatus');

            templateBuilder.templateChange.emit();

            // tap fires synchronously before debounce — no tick needed
            expect(setUveStatusSpy).toHaveBeenCalledWith(UVE_STATUS.LOADING);

            tick(5000); // flush timer to avoid pending-timer warning
        }));

        it('should trigger a save after 5 secs', fakeAsync(() => {
            const reloadSpy = jest.spyOn(store, 'pageReload');

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
            const saveTemplate = jest.spyOn(component, 'saveTemplate');

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
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(saveSubject.asObservable());

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
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(saveSubject.asObservable());

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
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(saveSubject.asObservable());

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME);

            saveSubject.next(PAGE_RESPONSE);
            saveSubject.complete();
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(false);
        }));

        it('should unlock canvas (disabled=false) on save error', fakeAsync(() => {
            (dotPageLayoutService.save as jest.Mock).mockReturnValue(
                throwError(() => new HttpErrorResponse({ status: 400 }))
            );

            templateBuilder.templateChange.emit();
            tick(DEBOUNCE_TIME);
            spectator.detectChanges();

            expect(templateBuilder.disabled).toBe(false);
        }));
    });
});
