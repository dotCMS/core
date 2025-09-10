/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotContentTypeService,
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
import { DotCMSContentType, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    CoreWebServiceMock,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotCustomEventHandlerService } from './dot-custom-event-handler.service';

import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../test/dot-test-bed';
import { DotContentletEditorService } from '../../../view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotDownloadBundleDialogService } from '../dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from '../dot-menu.service';
import { DotUiColorsService } from '../dot-ui-colors/dot-ui-colors.service';

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
    let dotContentTypeService: DotContentTypeService;
    let router: Router;

    const createFeatureFlagResponse = (enabled = 'NOT_FOUND', contentType = '*') => ({
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
                Router,
                DotContentTypeService
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
        dotContentTypeService = TestBed.inject(DotContentTypeService);
        router = TestBed.inject(Router);
    };

    const metadata = {};
    const metadata2 = {};
    metadata[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] = true;
    metadata2[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] = false;

    beforeEach(() => {
        setup({
            getKeys: () => of(createFeatureFlagResponse())
        });
    });

    it('should show loading indicator and go to edit page when event is emited by iframe', () => {
        jest.spyOn(dotLoadingIndicatorService, 'show');

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
        jest.spyOn(dotContentletEditorService, 'create');

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
        jest.spyOn(dotContentletEditorService, 'create');

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
        jest.spyOn(dotContentletEditorService, 'create');
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
        expect(dotRouterService.goToEditContentlet).toHaveBeenCalledTimes(1);
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
        expect(dotRouterService.goToEditContentlet).toHaveBeenCalledTimes(1);
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
        expect(dotRouterService.goToEditTask).toHaveBeenCalledTimes(1);
    });

    it('should set colors in the ui', () => {
        jest.spyOn(dotUiColorsService, 'setColors');
        const fakeHtmlEl = { hello: 'html' };
        jest.spyOn<any>(document, 'querySelector').mockReturnValue(fakeHtmlEl);

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

        jest.spyOn(dotGenerateSecurePasswordService, 'open');
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

        jest.spyOn(dotPushPublishDialogService, 'open');
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
        jest.spyOn(dotDownloadBundleDialogService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'download-bundle',
                    data: 'testID'
                }
            })
        );
        expect(dotDownloadBundleDialogService.open).toHaveBeenCalledWith('testID');
        expect(dotDownloadBundleDialogService.open).toHaveBeenCalledTimes(1);
    });

    it('should notify to open download bundle dialog', () => {
        jest.spyOn(dotWorkflowEventHandlerService, 'open');
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
        jest.spyOn(dotEventsService, 'notify');
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
        jest.spyOn(dotLicenseService, 'updateLicense');

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

            jest.spyOn(router, 'navigate');
            jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                of({ metadata } as DotCMSContentType)
            );
        });

        it('should create a contentlet', () => {
            jest.spyOn(dotContentletEditorService, 'create');

            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'create-contentlet',
                        data: { contentType: 'test' }
                    }
                })
            );

            expect(router.navigate).toHaveBeenCalledWith(['content/new/test']);
            expect(router.navigate).toHaveBeenCalledTimes(1);
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
            expect(router.navigate).toHaveBeenCalledTimes(1);
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
            expect(router.navigate).toHaveBeenCalledTimes(1);
        });
    });

    describe('edit content 2 is enabled and contentTypes are limited', () => {
        beforeEach(() => {
            setup({
                getKeys: () => of(createFeatureFlagResponse('true', 'test,test2'))
            });

            jest.spyOn(router, 'navigate');
        });

        it('should create a contentlet', () => {
            jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                of({ metadata } as DotCMSContentType)
            );
            jest.spyOn(dotContentletEditorService, 'create');

            service.handle(
                new CustomEvent('ng-event', {
                    detail: {
                        name: 'create-contentlet',
                        data: { contentType: 'test' }
                    }
                })
            );

            expect(router.navigate).toHaveBeenCalledWith(['content/new/test']);
            expect(router.navigate).toHaveBeenCalledTimes(1);
        });

        it('should edit a a workflow task', () => {
            jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                of({ metadata } as DotCMSContentType)
            );
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
            expect(router.navigate).toHaveBeenCalledTimes(1);
        });

        it('should edit a contentlet', () => {
            jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                of({ metadata } as DotCMSContentType)
            );
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
            expect(router.navigate).toHaveBeenCalledTimes(1);
        });

        it('should not create a contentlet', () => {
            jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                of({ metadata: metadata2 } as DotCMSContentType)
            );
            jest.spyOn(dotContentletEditorService, 'create');

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
            jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                of({ metadata: metadata2 } as DotCMSContentType)
            );

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
            jest.spyOn(dotContentTypeService, 'getContentType').mockReturnValue(
                of({ metadata: metadata2 } as DotCMSContentType)
            );
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
