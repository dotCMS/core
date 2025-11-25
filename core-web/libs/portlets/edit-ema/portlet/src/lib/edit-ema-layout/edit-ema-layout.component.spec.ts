import { expect, describe } from '@jest/globals';
import { SpyObject } from '@ngneat/spectator';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';
import { MockModule, MockProvider } from 'ng-mocks';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentTypeService,
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotPageLayoutService,
    DotRouterService
} from '@dotcms/data-access';
import { CoreWebService, LoginService } from '@dotcms/dotcms-js';
import { TemplateBuilderComponent, TemplateBuilderModule } from '@dotcms/template-builder';
import {
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    MockDotRouterJestService
} from '@dotcms/utils-testing';

import { EditEmaLayoutComponent } from './edit-ema-layout.component';

import { DotActionUrlService } from '../services/dot-action-url/dot-action-url.service';
import { DotPageApiService } from '../services/dot-page-api.service';
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
        imports: [HttpClientTestingModule, MockModule(TemplateBuilderModule)],
        providers: [
            UVEStore,
            DotMessageService,
            DotActionUrlService,
            mockProvider(MessageService),
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            mockProvider(DotContentTypeService),
            mockProvider(CoreWebService),
            mockProvider(DotPageLayoutService, {
                save: jest.fn(() => of(PAGE_RESPONSE))
            }),
            mockProvider(DotPageApiService, {
                get: jest.fn(() => of(PAGE_RESPONSE)),
                getClientPage: jest.fn(() => of(PAGE_RESPONSE))
            }),
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
            )
        ]
    });

    beforeEach(async () => {
        spectator = createComponent();
        component = spectator.component;
        dotRouter = spectator.inject(DotRouterService);
        store = spectator.inject(UVEStore, true);
        dotPageLayoutService = spectator.inject(DotPageLayoutService);
        messageService = spectator.inject(MessageService);

        store.init({
            clientHost: 'http://localhost:3000',
            language_id: '1',
            url: 'test',
            'com.dotmarketing.persona.id': 'SuperCoolDude'
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

        it('should trigger a save after 5 secs', fakeAsync(() => {
            const setUveStatusSpy = jest.spyOn(store, 'setUveStatus');
            const reloadSpy = jest.spyOn(store, 'reload');

            templateBuilder.templateChange.emit();
            tick(5000);

            expect(dotPageLayoutService.save).toHaveBeenCalled();
            expect(reloadSpy).toHaveBeenCalled();
            expect(setUveStatusSpy).toHaveBeenCalledWith(UVE_STATUS.LOADING);

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
});
