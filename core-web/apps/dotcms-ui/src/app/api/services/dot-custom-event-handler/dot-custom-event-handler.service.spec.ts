/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@openng/spectator/vitest';
import { NEVER, of } from 'rxjs';
import { Mock, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Injector } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
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
    DotPushPublishDialogService,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { FeaturedFlags } from '@dotcms/dotcms-models';
// The opener is the handler's dependency; the app also lazy-loads the library's routes (#38303).
// eslint-disable-next-line @nx/enforce-module-boundaries
import {
    DotEditContentOpenerService,
    DotEditContentOpenOptions,
    DotEditContentRequest
} from '@dotcms/edit-content';
import { DotSidePanelRef } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import {
    createFakeContentlet,
    DotFormatDateServiceMock,
    DotMessageDisplayServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotCustomEventHandlerService } from './dot-custom-event-handler.service';

import { MockDotUiColorsService } from '../../../test/dot-test-bed';
import { DotDownloadBundleDialogService } from '../dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from '../dot-menu.service';

/** The handler once it can be told which injector the panels it opens live under (#38303). */
type WithInjector = DotCustomEventHandlerService & { setInjector?: (injector: Injector) => void };

describe('DotCustomEventHandlerService', () => {
    let service: DotCustomEventHandlerService;
    let dotLoadingIndicatorService: DotLoadingIndicatorService;
    let dotRouterService: DotRouterService;
    let dotUiColorsService: DotUiColorsService;
    let dotPushPublishDialogService: DotPushPublishDialogService;
    let dotGenerateSecurePasswordService: DotGenerateSecurePasswordService;
    let dotDownloadBundleDialogService: DotDownloadBundleDialogService;
    let dotWorkflowEventHandlerService: DotWorkflowEventHandlerService;
    let dotEventsService: DotEventsService;
    let dotLicenseService: DotLicenseService;
    let dotIframeService: DotIframeService;
    let router: Router;
    let getKeys: Mock;

    const createFeatureFlagResponse = (enabled = 'NOT_FOUND', contentType = '*') => ({
        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: enabled,
        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_CONTENT_TYPE]: contentType
    });

    const setup = (dotPropertiesMock: { getKeys: Mock }) => {
        getKeys = dotPropertiesMock.getKeys;

        TestBed.resetTestingModule().configureTestingModule({
            providers: [
                DotCustomEventHandlerService,
                DotLoadingIndicatorService,
                DotMenuService,
                DotPushPublishDialogService,
                DotWorkflowEventHandlerService,
                DotRouterService,
                PushPublishService,
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                ApiRoot,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
                UserModel,
                StringUtils,
                LoggerService,
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
                DotContentTypeService,
                mockProvider(DotEditContentOpenerService, {
                    open: vi.fn(() => new DotSidePanelRef(() => undefined))
                }),
                provideHttpClient(),
                provideHttpClientTesting()
            ],
            imports: [RouterTestingModule]
        });

        service = TestBed.inject(DotCustomEventHandlerService);
        dotLoadingIndicatorService = TestBed.inject(DotLoadingIndicatorService);
        dotRouterService = TestBed.inject(DotRouterService);
        dotUiColorsService = TestBed.inject(DotUiColorsService);
        dotPushPublishDialogService = TestBed.inject(DotPushPublishDialogService);
        dotGenerateSecurePasswordService = TestBed.inject(DotGenerateSecurePasswordService);
        dotDownloadBundleDialogService = TestBed.inject(DotDownloadBundleDialogService);
        dotWorkflowEventHandlerService = TestBed.inject(DotWorkflowEventHandlerService);
        dotEventsService = TestBed.inject(DotEventsService);
        dotLicenseService = TestBed.inject(DotLicenseService);
        dotIframeService = TestBed.inject(DotIframeService);
        router = TestBed.inject(Router);
    };

    beforeEach(() => {
        setup({
            getKeys: vi.fn(() => of(createFeatureFlagResponse()))
        });
    });

    it('should show loading indicator and go to edit page when event is emited by iframe', () => {
        vi.spyOn(dotLoadingIndicatorService, 'show');

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

    it('should edit a workflow task even when the event names a content type', () => {
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

        expect(dotRouterService.goToEditTask).toHaveBeenCalledWith('123');
        expect(dotRouterService.goToEditTask).toHaveBeenCalledTimes(1);
    });

    it('should set colors in the ui', () => {
        vi.spyOn(dotUiColorsService, 'setColors');
        const fakeHtmlEl = { hello: 'html' } as unknown as HTMLElement;
        vi.spyOn(document, 'querySelector').mockReturnValue(fakeHtmlEl);

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

        vi.spyOn(dotGenerateSecurePasswordService, 'open');
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

        vi.spyOn(dotPushPublishDialogService, 'open');
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

    it('should open push publish dialog even when feature-flag lookup has not resolved', () => {
        setup({
            getKeys: vi.fn(() => NEVER)
        });

        const dataMock = {
            assetIdentifier: '456',
            dateFilter: false,
            removeOnly: false,
            isBundle: false
        };

        vi.spyOn(dotPushPublishDialogService, 'open');
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'push-publish',
                    data: dataMock
                }
            })
        );

        expect(dotPushPublishDialogService.open).toHaveBeenCalledWith(dataMock);
    });

    it('should notify to open download bundle dialog', () => {
        vi.spyOn(dotDownloadBundleDialogService, 'open');
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
        vi.spyOn(dotWorkflowEventHandlerService, 'open');
        const mockWorkflowEvent = {
            workflow: {
                actionInputs: []
            },
            inode: 'testInode',
            contentType: 'testContentType',
            languageId: 1,
            identifier: 'testIdentifier'
        };
        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'workflow-wizard',
                    data: mockWorkflowEvent
                }
            })
        );
        expect<any>(dotWorkflowEventHandlerService.open).toHaveBeenCalledWith(mockWorkflowEvent);
    });

    it('should notify to open contnt compare dialog', () => {
        vi.spyOn(dotEventsService, 'notify');
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
        vi.spyOn(dotLicenseService, 'updateLicense');

        service.handle(
            new CustomEvent('ng-event', {
                detail: {
                    name: 'license-changed'
                }
            })
        );
        expect(dotLicenseService.updateLicense).toHaveBeenCalled();
    });

    /**
     * #38303 (R11) — the legacy portlets' content events open the side panel through the opener,
     * whatever editor the content type uses. The panel lives under `DotSidePanelHostComponent`,
     * which registers its own injector here, so a panel finds the `MessageService` and the
     * `<p-toast>` that component provides. Saving or closing reloads the portlet that asked.
     */
    describe('content events open the side panel', () => {
        /** The portlet `MockDotRouterService` reports as current. */
        const PORTLET_ID = '123-567';

        /** Stands in for `DotSidePanelHostComponent`'s injector. */
        let sidePanelHostInjector: Injector;

        /** The opener's `open`, as the mock records it. */
        const opened = () => TestBed.inject(DotEditContentOpenerService).open as Mock;

        /** The request the handler asked the opener for, on its only `open`. */
        const openedRequest = (): DotEditContentRequest => {
            expect(opened().mock.calls.length, 'the handler opens one panel').toBe(1);

            return opened().mock.calls[0][0] as DotEditContentRequest;
        };

        /** The options the handler handed the opener, on its only `open`. */
        const openOptions = (): DotEditContentOpenOptions => {
            expect(opened().mock.calls.length, 'the handler opens one panel').toBe(1);

            return opened().mock.calls[0][1] as DotEditContentOpenOptions;
        };

        /**
         * Sends one of the legacy portlets' events, as `IframePortletLegacyComponent` does, and
         * waits for the handler to load the opener, which the admin app imports on demand.
         */
        const send = async (name: string, data: Record<string, unknown>): Promise<void> => {
            service.handle(new CustomEvent('ng-event', { detail: { name, data } }));
            await vi.dynamicImportSettled();
        };

        beforeEach(() => {
            opened().mockClear();
            sidePanelHostInjector = Injector.create({
                providers: [],
                parent: TestBed.inject(Injector)
            });
            (service as WithInjector).setInjector?.(sidePanelHostInjector);
        });

        describe('edit-contentlet', () => {
            it('should open the contentlet for editing, naming the portlet that asked', async () => {
                await send('edit-contentlet', { inode: '123', contentType: 'Blog' });

                expect(openedRequest()).toEqual({
                    mode: 'edit',
                    inode: '123',
                    contentType: 'Blog',
                    portletId: PORTLET_ID
                });
            });

            it('should not route anywhere', async () => {
                vi.spyOn(router, 'navigate');

                await send('edit-contentlet', { inode: '123', contentType: 'Blog' });

                expect((router.navigate as Mock).mock.calls.length).toBe(0);
            });
        });

        describe('edit-host', () => {
            it('should open the site for editing with no content type, which the opener fetches', async () => {
                await send('edit-host', { inode: 'host-inode' });

                const request = openedRequest();
                expect(request).toEqual({
                    mode: 'edit',
                    inode: 'host-inode',
                    portletId: PORTLET_ID
                });
                expect(request.contentType).toBeUndefined();
            });
        });

        describe('create-contentlet', () => {
            /**
             * The content search and the Site Browser put the language the portlet shows on the
             * create URL (`&lang=<id>`), and the create opens in it; the rest of the URL is ignored.
             */
            it('should create in the language the JSP url names', async () => {
                await send('create-contentlet', {
                    url: '/c/portal/layout?p_l_id=1&p_p_id=content&_content_cmd=new&lang=2',
                    contentType: 'Blog',
                    folderPath: 'default/'
                });

                expect(openedRequest()).toEqual({
                    mode: 'new',
                    contentType: 'Blog',
                    folderPath: 'default/',
                    languageId: 2,
                    portletId: PORTLET_ID
                });
            });

            it('should open a create of the content type in the folder, ignoring the JSP url', async () => {
                await send('create-contentlet', {
                    url: '/c/portal/layout?p_l_id=1&p_p_id=content&_content_cmd=new',
                    contentType: 'Blog',
                    folderPath: 'default/level1/level2/'
                });

                const request = openedRequest();
                expect(request).toEqual({
                    mode: 'new',
                    contentType: 'Blog',
                    folderPath: 'default/level1/level2/',
                    portletId: PORTLET_ID
                });
                expect(request).not.toHaveProperty('url');
                // No `lang` on the url: the opener's default language applies.
                expect(request).not.toHaveProperty('languageId');
            });
        });

        describe('create-host', () => {
            it('should open a create of a site, ignoring the JSP url', async () => {
                await send('create-host', { url: '/c/portal/layout?p_l_id=1&p_p_id=sites' });

                const request = openedRequest();
                expect(request).toEqual({
                    mode: 'new',
                    contentType: 'Host',
                    portletId: PORTLET_ID
                });
                expect(request).not.toHaveProperty('url');
            });
        });

        // The UVE's own selector event: the UVE handles it now, so it never reaches the opener here.
        describe('create-contentlet-from-edit-page', () => {
            it('should not open a panel', async () => {
                await send('create-contentlet-from-edit-page', { url: 'hello.world.com' });

                expect(opened().mock.calls.length).toBe(0);
            });
        });

        describe.each([
            ['edit-contentlet', { inode: '123', contentType: 'Blog' }],
            ['edit-host', { inode: 'host-inode' }],
            ['create-contentlet', { contentType: 'Blog', folderPath: 'default/' }],
            ['create-host', { url: '/c/portal/layout?p_p_id=sites' }]
        ])('%s', (name, data) => {
            it('should hand the opener the injector the side panel host registered, not the root one', async () => {
                expect(
                    typeof (service as WithInjector).setInjector,
                    'the side panel host can register its injector'
                ).toBe('function');

                await send(name, data);

                // Compared by identity: printing an injector runs the worker out of memory.
                expect(openOptions().injector === sidePanelHostInjector).toBe(true);
            });

            it('should reload the portlet that asked on every save', async () => {
                const reloadData = vi.spyOn(dotIframeService, 'reloadData');
                await send(name, data);

                openOptions().saved?.(createFakeContentlet({ inode: 'saved-inode' }));

                expect(reloadData).toHaveBeenCalledWith(PORTLET_ID);
            });

            it('should reload the portlet that asked when the panel closes', async () => {
                const reloadData = vi.spyOn(dotIframeService, 'reloadData');
                await send(name, data);

                openOptions().closed?.();

                expect(reloadData).toHaveBeenCalledWith(PORTLET_ID);
            });

            // The opener writes the panel to the portlet's URL, as on every surface but UVE
            // (#38303, FR-044).
            it('should ask the opener to write the panel to the URL', async () => {
                await send(name, data);

                expect(openOptions().url).toEqual({});
            });
        });

        /**
         * The legacy portlet route opens what a URL names through the same path the events use
         * (#38303, FR-049): `IframePortletLegacyComponent` calls it with the identifier and the
         * language, or the content type and the folder.
         */
        describe('openContent', () => {
            const IDENTIFIER = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';

            const openContent = (request: DotEditContentRequest): Promise<void> => {
                (
                    service as unknown as { openContent: (r: DotEditContentRequest) => void }
                ).openContent(request);

                return vi.dynamicImportSettled();
            };

            it('should be public', () => {
                expect(typeof (service as unknown as { openContent?: unknown }).openContent).toBe(
                    'function'
                );
            });

            it('should open the request for the current portlet, written to the URL', async () => {
                await openContent({ mode: 'edit', identifier: IDENTIFIER, languageId: 2 });

                expect(openedRequest()).toEqual({
                    mode: 'edit',
                    identifier: IDENTIFIER,
                    languageId: 2,
                    portletId: PORTLET_ID
                });
                expect(openOptions().url).toEqual({});
                expect(openOptions().injector === sidePanelHostInjector).toBe(true);
            });

            it('should reload the portlet on every save and when the panel closes', async () => {
                const reloadData = vi.spyOn(dotIframeService, 'reloadData');
                await openContent({
                    mode: 'new',
                    contentType: 'Blog',
                    folderInode: 'folder-inode'
                });

                openOptions().saved?.(createFakeContentlet({ inode: 'saved-inode' }));
                openOptions().closed?.();

                expect(reloadData).toHaveBeenCalledTimes(2);
                expect(reloadData).toHaveBeenCalledWith(PORTLET_ID);
            });

            // The Workflow Tasks detail views its task again once the task list behind it has
            // reloaded (#38303, FR-025a).
            it("should run the caller's saved and closed after reloading the portlet", async () => {
                const reloadData = vi.spyOn(dotIframeService, 'reloadData');
                const saved = vi.fn();
                const closed = vi.fn();
                const contentlet = createFakeContentlet({ inode: 'saved-inode' });

                (
                    service as unknown as {
                        openContent: (
                            r: DotEditContentRequest,
                            after: Pick<DotEditContentOpenOptions, 'saved' | 'closed'>
                        ) => void;
                    }
                ).openContent(
                    { mode: 'edit', inode: '123', contentType: 'Blog' },
                    { saved, closed }
                );
                await vi.dynamicImportSettled();

                openOptions().saved?.(contentlet);
                expect(saved).toHaveBeenCalledWith(contentlet);
                expect(reloadData.mock.invocationCallOrder[0]).toBeLessThan(
                    saved.mock.invocationCallOrder[0]
                );

                openOptions().closed?.();
                expect(closed).toHaveBeenCalledTimes(1);
                expect(reloadData.mock.invocationCallOrder[1]).toBeLessThan(
                    closed.mock.invocationCallOrder[0]
                );
            });
        });

        // The opener picks the editor per content type; the global flag swap is gone.
        it('should never read the content editor flag', async () => {
            await send('edit-contentlet', { inode: '123', contentType: 'Blog' });

            expect(getKeys.mock.calls.length).toBe(0);
        });
    });

    /**
     * The root injector provides no `MessageService`, so a panel opened under it fails inside.
     * `DotSidePanelHostComponent` registers before any portlet can send an event; until it has,
     * nothing opens and the broken invariant is logged.
     */
    describe('before the side panel host registers its injector', () => {
        it('should open nothing and log an error', async () => {
            const open = TestBed.inject(DotEditContentOpenerService).open as Mock;
            open.mockClear();
            const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

            service.handle(
                new CustomEvent('ng-event', {
                    detail: { name: 'edit-contentlet', data: { inode: '123', contentType: 'Blog' } }
                })
            );
            await vi.dynamicImportSettled();

            expect(open.mock.calls.length).toBe(0);
            expect(consoleError).toHaveBeenCalledTimes(1);
            consoleError.mockRestore();
        });
    });
});
