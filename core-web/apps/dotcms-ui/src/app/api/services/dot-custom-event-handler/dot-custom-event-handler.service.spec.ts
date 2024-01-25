/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import { DotContentletEditorService } from '@components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@dotcms/app/test/dot-test-bed';
import {
    DotAlertConfirmService,
    DotCurrentUserService,
    DotEventsService,
    DotGenerateSecurePasswordService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageDisplayService,
    DotPropertiesService,
    DotRouterService,
    DotWorkflowActionsFireService,
    DotIframeService,
    DotGlobalMessageService,
    DotFormatDateService,
    DotWizardService,
    DotWorkflowEventHandlerService,
    PushPublishService
} from '@dotcms/data-access';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '@services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from '@services/dot-menu.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';

describe('DotCustomEventHandlerService', () => {
    let service: DotCustomEventHandlerService;
    let dotLoadingIndicatorService: DotLoadingIndicatorService;
    let dotRouterService: DotRouterService;
    let dotUiColorsService: DotUiColorsService;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let dotGenerateSecurePasswordService: DotGenerateSecurePasswordService;
    let dotContentletEditorService: DotContentletEditorService;
    let dotDownloadBundleDialogService: DotDownloadBundleDialogService;
    let dotWorkflowEventHandlerService: DotWorkflowEventHandlerService;
    let dotEventsService: DotEventsService;
    let dotLicenseService: DotLicenseService;
    let router: Router;

    const createFeatureFlagResponse = (
        enabled: string = 'NOT_FOUND',
        contentType: string = '*'
    ) => ({
        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: enabled,
        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_CONTENT_TYPE]: contentType
    });

    const setup = (dotPropertiesMock: unknown) => {
        TestBed.resetTestingModule().configureTestingModule({
            providers: [
                DotCustomEventHandlerService,
                DotLoadingIndicatorService,
                DotMenuService,
                DotPushPublishDialogService,
                DotWorkflowEventHandlerService,
                DotRouterService,
                DotContentletEditorService,
                PushPublishService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                ApiRoot,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                UserModel,
                StringUtils,
                DotcmsEventsService,
                LoggerService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                LoggerService,
                DotCurrentUserService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                DotWizardService,
                DotHttpErrorManagerService,
                DotAlertConfirmService,
                ConfirmationService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotEventsService,
                DotIframeService,
                DotDownloadBundleDialogService,
                DotGenerateSecurePasswordService,
                LoginService,
                DotLicenseService,
                { provide: DotPropertiesService, useValue: dotPropertiesMock },
                Router
            ],
            imports: [RouterTestingModule, HttpClientTestingModule]
        });

        service = TestBed.inject(DotCustomEventHandlerService);
        dotLoadingIndicatorService = TestBed.inject(DotLoadingIndicatorService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotUiColorsService = TestBed.inject(DotUiColorsService);
        dotContentletEditorService = TestBed.inject(DotContentletEditorService);
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotGenerateSecurePasswordService = TestBed.inject(DotGenerateSecurePasswordService);
        dotDownloadBundleDialogService = TestBed.inject(DotDownloadBundleDialogService);
        dotWorkflowEventHandlerService = TestBed.inject(DotWorkflowEventHandlerService);
        dotEventsService = TestBed.inject(DotEventsService);
        dotLicenseService = TestBed.inject(DotLicenseService);
        router = TestBed.inject(Router);
    };

    beforeEach(() => {
        setup({
            getKeys: () => of(createFeatureFlagResponse())
        });
    });

    it('should show loading indicator and go to edit page when event is emited by iframe', () => {
        spyOn(dotLoadingIndicatorService, 'show');

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-page',
                    data: {
                        url: 'some/url',
                        languageId: '2',
                        hostId: '123'
                    }
                }
            })
        );

        expect(dotLoadingIndicatorService.show).toHaveBeenCalledTimes(1);
        expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
            url: 'some/url',
            language_id: '2',
            host_id: '123'
        });
    });

    it('should create a contentlet', () => {
        spyOn(dotContentletEditorService, 'create');

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'create-contentlet',
                    data: { url: 'hello.world.com' }
                }
            })
        );

        expect(dotContentletEditorService.create).toHaveBeenCalledWith({
            data: {
                url: 'hello.world.com'
            }
        });
    });

    it('should create a host', () => {
        spyOn(dotContentletEditorService, 'create');

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'create-host',
                    data: { url: 'hello.world.com' }
                }
            })
        );

        expect(dotContentletEditorService.create).toHaveBeenCalledWith({
            data: {
                url: 'hello.world.com'
            }
        });
    });

    it('should create a contentlet from edit page', () => {
        spyOn(dotContentletEditorService, 'create');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'create-contentlet-from-edit-page',
                    data: { url: 'hello.world.com' }
                }
            })
        );

        expect(dotContentletEditorService.create).toHaveBeenCalledWith({
            data: {
                url: 'hello.world.com'
            }
        });
    });

    it('should edit a contentlet', () => {
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-contentlet',
                    data: {
                        inode: '123'
                    }
                }
            })
        );
        expect(dotRouterService.goToEditContentlet).toHaveBeenCalledWith('123');
    });

    it('should edit a host', () => {
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-host',
                    data: {
                        inode: '123'
                    }
                }
            })
        );
        expect(dotRouterService.goToEditContentlet).toHaveBeenCalledWith('123');
    });

    it('should edit a a workflow task', () => {
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'edit-task',
                    data: {
                        inode: '123'
                    }
                }
            })
        );
        expect(dotRouterService.goToEditTask).toHaveBeenCalledWith('123');
    });

    it('should set colors in the ui', () => {
        spyOn(dotUiColorsService, 'setColors');
        const fakeHtmlEl = { hello: 'html' };
        spyOn<any>(document, 'querySelector').and.returnValue(fakeHtmlEl);

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'company-info-updated',
                    payload: {
                        colors: {
                            primary: '#fff',
                            secondary: '#000',
                            background: '#ccc'
                        }
                    }
                }
            })
        );
        expect<any>(dotUiColorsService.setColors).toHaveBeenCalledWith(fakeHtmlEl, {
            primary: '#fff',
            secondary: '#000',
            background: '#ccc'
        });
    });

    it('should notify to open generate secure password dialog', () => {
        const dataMock = {
            password: '123'
        };

        spyOn(dotGenerateSecurePasswordService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'generate-secure-password',
                    data: dataMock
                }
            })
        );

        expect<any>(dotGenerateSecurePasswordService.open).toHaveBeenCalledWith(dataMock);
    });

    it('should notify to open push publish dialog', () => {
        const dataMock = {
            assetIdentifier: '123',
            dateFilter: true,
            removeOnly: true,
            isBundle: false
        };

        spyOn(dotPushPublishDialogService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'push-publish',
                    data: dataMock
                }
            })
        );

        expect<any>(dotPushPublishDialogService.open).toHaveBeenCalledWith(dataMock);
    });

    it('should notify to open download bundle dialog', () => {
        spyOn(dotDownloadBundleDialogService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'download-bundle',
                    data: 'testID'
                }
            })
        );
        expect(dotDownloadBundleDialogService.open).toHaveBeenCalledWith('testID');
    });

    it('should notify to open download bundle dialog', () => {
        spyOn(dotWorkflowEventHandlerService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'workflow-wizard',
                    data: 'testData'
                }
            })
        );
        expect<any>(dotWorkflowEventHandlerService.open).toHaveBeenCalledWith('testData');
    });

    it('should notify to open contnt compare dialog', () => {
        spyOn(dotEventsService, 'notify');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'compare-contentlet',
                    data: 'testData'
                }
            })
        );
        expect<any>(dotEventsService.notify).toHaveBeenCalledWith('compare-contentlet', 'testData');
    });

    it("should update license when 'license-changed' event is received", () => {
        spyOn(dotLicenseService, 'updateLicense');

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'license-changed'
                }
            })
        );
        expect(dotLicenseService.updateLicense).toHaveBeenCalled();
    });

    describe('edit content 2 is enabled and contentTypes are catchall', () => {
        beforeEach(() => {
            setup({
                getKeys: () => of(createFeatureFlagResponse('true'))
            });

            spyOn(router, 'navigate');
        });

        it('should create a contentlet', () => {
            spyOn(dotContentletEditorService, 'create');

            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'create-contentlet',
                        data: { contentType: 'test' }
                    }
                })
            );

            expect(router.navigate).toHaveBeenCalledWith(['content/new/test']);
        });

        it('should edit a a workflow task', () => {
            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'edit-task',
                        data: {
                            inode: '123',
                            contentType: 'test'
                        }
                    }
                })
            );

            expect(router.navigate).toHaveBeenCalledWith(['content/123']);
        });

        it('should edit a contentlet', () => {
            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'edit-contentlet',
                        data: {
                            inode: '123',
                            contentType: 'test'
                        }
                    }
                })
            );
            expect(router.navigate).toHaveBeenCalledWith(['content/123']);
        });
    });

    describe('edit content 2 is enabled and contentTypes are limited', () => {
        beforeEach(() => {
            setup({
                getKeys: () => of(createFeatureFlagResponse('true', 'test,test2'))
            });

            spyOn(router, 'navigate');
        });

        it('should create a contentlet', () => {
            spyOn(dotContentletEditorService, 'create');

            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'create-contentlet',
                        data: { contentType: 'test' }
                    }
                })
            );

            expect(router.navigate).toHaveBeenCalledWith(['content/new/test']);
        });

        it('should edit a a workflow task', () => {
            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'edit-task',
                        data: {
                            inode: '123',
                            contentType: 'test2'
                        }
                    }
                })
            );

            expect(router.navigate).toHaveBeenCalledWith(['content/123']);
        });

        it('should edit a contentlet', () => {
            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'edit-contentlet',
                        data: {
                            inode: '123',
                            contentType: 'test2'
                        }
                    }
                })
            );
            expect(router.navigate).toHaveBeenCalledWith(['content/123']);
        });

        it('should not create a contentlet', () => {
            spyOn(dotContentletEditorService, 'create');

            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'create-contentlet',
                        data: { contentType: 'not in the list' }
                    }
                })
            );

            expect(router.navigate).not.toHaveBeenCalledWith(['content/new/test']);
        });

        it('should not edit a a workflow task', () => {
            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'edit-task',
                        data: {
                            inode: '123',
                            contentType: 'not in the list'
                        }
                    }
                })
            );

            expect(router.navigate).not.toHaveBeenCalledWith(['content/123']);
        });

        it('should not edit a contentlet', () => {
            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'edit-contentlet',
                        data: {
                            inode: '123',
                            contentType: 'not in the list'
                        }
                    }
                })
            );
            expect(router.navigate).not.toHaveBeenCalledWith(['content/123']);
        });
    });
});
