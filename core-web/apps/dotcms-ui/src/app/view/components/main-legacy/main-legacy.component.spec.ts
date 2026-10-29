/* eslint-disable @typescript-eslint/no-explicit-any */

import { mockProvider } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, Injector, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';

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
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotSystemConfigService,
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
import { LoginServiceMock, MockDotRouterService } from '@dotcms/utils-testing';

import { MainComponentLegacyComponent } from './main-legacy.component';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotDownloadBundleDialogService } from '../../../api/services/dot-download-bundle-dialog/dot-download-bundle-dialog.service';
import { DotMenuService } from '../../../api/services/dot-menu.service';
import { NotificationsService } from '../../../api/services/notifications-service';
import { LOCATION_TOKEN } from '../../../providers';
import { MockDotUiColorsService } from '../../../test/dot-test-bed';
import { DotDownloadBundleDialogComponent } from '../_common/dot-download-bundle-dialog/dot-download-bundle-dialog.component';
import { DotWizardComponent } from '../_common/dot-wizard/dot-wizard.component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';
// import { DotContentletEditorModule } from '../dot-contentlet-editor/dot-contentlet-editor.module';

@Component({
    selector: 'dot-alert-confirm',
    template: '',
    standalone: false
})
class MockDotDialogComponent {}

@Component({
    selector: 'dot-toolbar',
    template: '',
    standalone: false
})
class MockDotToolbarComponent {
    @Input() collapsed: boolean;
}

@Component({
    selector: 'dot-generate-secure-password',
    template: '',
    standalone: false
})
class MockDotGenerateSecurePasswordComponent {}

@Component({
    selector: 'dot-main-nav',
    template: '',
    standalone: false
})
class MockDotMainNavComponent {
    @Input() collapsed: boolean;
}

@Component({
    selector: 'dot-message-display',
    template: '',
    standalone: false
})
class MockDotMessageDisplayComponent {}

@Component({
    selector: 'dot-large-message-display',
    template: '',
    standalone: false
})
class MockDotLargeMessageDisplayComponent {}

@Component({
    selector: 'dot-push-publish-dialog',
    template: '',
    standalone: false
})
class MockDotPushPublishDialogComponent {}

/** The handler once it can be told which injector its panels live under (#38303). */
type HandlerWithInjector = DotCustomEventHandlerService & {
    setInjector?: (injector: Injector) => void;
};

const createFeatureFlagResponse = (enabled = 'NOT_FOUND') => ({
    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: enabled
});

describe('MainLegacyComponent', () => {
    let fixture: ComponentFixture<MainComponentLegacyComponent>;
    let de: DebugElement;
    let dotCustomEventHandlerService: DotCustomEventHandlerService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule,
                // DotContentletEditorModule,
                DotDownloadBundleDialogComponent,
                DotWizardComponent,
                MainComponentLegacyComponent
            ],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: LoginService, useClass: LoginServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                DotMenuService,
                DotCustomEventHandlerService,
                DotLicenseService,
                DotIframeService,
                DotFormatDateService,
                DotAlertConfirmService,
                ConfirmationService,
                DotcmsConfigService,
                LoggerService,
                StringUtils,

                DotWorkflowEventHandlerService,
                ApiRoot,
                UserModel,
                DotMessageDisplayService,
                DotHttpErrorManagerService,
                DotWorkflowActionsFireService,
                DotGlobalMessageService,
                DotEventsService,
                DotGenerateSecurePasswordService,
                mockProvider(DotContentTypeService),
                // Add missing services for standalone components
                DotDownloadBundleDialogService,
                DotPushPublishDialogService,
                PushPublishService,
                DotCurrentUserService,
                DotWorkflowEventHandlerService,
                DotNavigationService,
                IframeOverlayService,
                DotSystemConfigService,
                NotificationsService,
                {
                    provide: DotPropertiesService,
                    useValue: {
                        getKeys: () => of(createFeatureFlagResponse()),
                        getFeatureFlag: vi.fn().mockReturnValue(of(true))
                    }
                },
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        reload: vi.fn()
                    }
                }
            ],
            declarations: [
                // MainComponentLegacyComponent, // Moved to imports as standalone
                MockDotDialogComponent,
                MockDotMainNavComponent,
                MockDotToolbarComponent,
                MockDotGenerateSecurePasswordComponent,
                MockDotMessageDisplayComponent,
                MockDotLargeMessageDisplayComponent,
                MockDotPushPublishDialogComponent
            ]
        });
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(MainComponentLegacyComponent);
        de = fixture.debugElement;
        dotCustomEventHandlerService = de.injector.get(DotCustomEventHandlerService);
        fixture.detectChanges();
    });
    it('should have basic layout elements', () => {
        // No dot-alert-confirm: app.component.html renders one at the root for every route.
        // Having a second host here made every alert and confirm render twice.
        expect(de.query(By.css('dot-alert-confirm'))).toBeNull();
        expect(de.query(By.css('dot-toolbar')) !== null).toBe(true);
        expect(de.query(By.css('dot-main-nav')) !== null).toBe(true);
        expect(de.query(By.css('router-outlet')) !== null).toBe(true);
        expect(de.query(By.css('dot-push-publish-dialog')) !== null).toBe(true);
        expect(de.query(By.css('dot-download-bundle-dialog')) !== null).toBe(true);
        expect(de.query(By.css('dot-generate-secure-password')) !== null).toBe(true);
        expect(de.query(By.css('dot-wizard')) !== null).toBe(true);
    });

    it('should have messages components', () => {
        expect(de.query(By.css('dot-large-message-display')) !== null).toBe(true);
        expect(de.query(By.css('dot-large-message-display')) !== null).toBe(true);
    });

    /**
     * Panels opened by root services (the legacy JSP portlets' events, the deep links) resolve
     * their toasts through the root injector (#38303, FR-037). Nothing provided a PrimeNG
     * MessageService or a toast at the admin root before, so their messages had nowhere to go.
     */
    describe('side panel toasts', () => {
        it('should provide a MessageService and a toast beside the side panel outlet', () => {
            expect(de.injector.get(MessageService)).toBeDefined();
            expect(de.query(By.css('p-toast'))).not.toBeNull();
            expect(de.query(By.css('dot-side-panel-outlet'))).not.toBeNull();
        });

        /**
         * The legacy portlets' events reach the root `DotCustomEventHandlerService`, whose panels
         * must live under this component so they find the MessageService above, not the root
         * injector, where nothing provides one (#38303, T090).
         */
        it('should register its own injector with the custom-event handler', () => {
            const handler = TestBed.inject(DotCustomEventHandlerService) as HandlerWithInjector;
            expect(typeof handler.setInjector, 'the handler accepts an injector').toBe('function');
            const setInjector = vi.spyOn(handler as Required<HandlerWithInjector>, 'setInjector');

            const mounted = TestBed.createComponent(MainComponentLegacyComponent);
            mounted.detectChanges();

            expect(setInjector.mock.calls.length).toBe(1);
            const [registered] = setInjector.mock.calls[0];
            // Compared by identity: printing an injector runs the worker out of memory.
            expect(
                registered.get(MessageService, null) ===
                    mounted.debugElement.injector.get(MessageService)
            ).toBe(true);
        });
    });

    describe('Create Contentlet', () => {
        let createContentlet: DebugElement;
        beforeEach(() => {
            createContentlet = de.query(By.css('dot-create-contentlet'));
        });

        it('should call dotCustomEventHandlerService on customEvent', () => {
            vi.spyOn(dotCustomEventHandlerService, 'handle');
            const mockEvent = {
                detail: {
                    name: 'create-contentlet',
                    data: 'test'
                }
            };
            createContentlet.triggerEventHandler('custom', mockEvent);

            expect<any>(dotCustomEventHandlerService.handle).toHaveBeenCalledWith(mockEvent);
        });
    });
});
