import { mockProvider } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, DebugElement, Input } from '@angular/core';
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

const createFeatureFlagResponse = (enabled = 'NOT_FOUND') => ({
    [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: enabled
});

describe('MainLegacyComponent', () => {
    let fixture: ComponentFixture<MainComponentLegacyComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                RouterTestingModule,
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
                // Stands in for a MessageService a route provides, as UVE's does.
                MessageService,
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
     * The admin root is an element ancestor of every routed portlet, and Angular resolves element
     * injectors before a route's providers. A MessageService provided here shadowed the one UVE
     * provides on its route, so UVE's toast and the admin root's both rendered every UVE message
     * (#38303). The side panels and their toast live in `dot-side-panel-host`, a sibling of the
     * router outlet, instead.
     */
    describe('side panel toasts', () => {
        it('should not provide a MessageService of its own', () => {
            // Compared by identity: printing a MessageService on failure floods the output.
            expect(de.injector.get(MessageService) === TestBed.inject(MessageService)).toBe(true);
        });

        it('should render the side panel host', () => {
            expect(de.query(By.css('dot-side-panel-host'))).not.toBeNull();
        });

        it('should render no toast or side panel outlet of its own', () => {
            const host: HTMLElement = fixture.nativeElement;

            expect(host.querySelector(':scope > p-toast')).toBeNull();
            expect(host.querySelector(':scope > dot-side-panel-outlet')).toBeNull();
        });
    });
});
