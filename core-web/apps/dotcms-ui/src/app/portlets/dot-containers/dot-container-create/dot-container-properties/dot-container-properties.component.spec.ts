import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { of } from 'rxjs';
import { DotContainerPropertiesComponent } from './dot-container-properties.component';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { LoggerService } from '@dotcms/dotcms-js';
import { DotSiteBrowserService } from '@services/dot-site-browser/dot-site-browser.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { CoreWebService } from '@dotcms/dotcms-js';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ActivatedRoute } from '@angular/router';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotEventsSocketURL } from '@dotcms/dotcms-js';
import { dotEventSocketURLFactory } from '@tests/dot-test-bed';
import { StringUtils } from '@dotcms/dotcms-js';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ConfirmationService, SharedModule } from 'primeng/api';
import { LoginService } from '@dotcms/dotcms-js';
import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotEventsSocket } from '@dotcms/dotcms-js';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { DotFormatDateServiceMock } from '@tests/format-date-service.mock';
import { CommonModule } from '@angular/common';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { CheckboxModule } from 'primeng/checkbox';
import { MenuModule } from 'primeng/menu';
import { ButtonModule } from 'primeng/button';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotActionMenuButtonModule } from '@components/_common/dot-action-menu-button/dot-action-menu-button.module';
import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { DotContainersService } from '@services/dot-containers/dot-containers.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotEventsService } from '@dotcms/app/api/services/dot-events/dot-events.service';
import { DotContentTypeService } from '@dotcms/app/api/services/dot-content-type';
import { InplaceModule } from 'primeng/inplace';
import { ReactiveFormsModule } from '@angular/forms';
import { DotTextareaContentModule } from '@components/_common/dot-textarea-content/dot-textarea-content.module';
import { By } from '@angular/platform-browser';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';

@Component({
    selector: 'dot-container-code',
    template: '<div></div>'
})
export class DotContentEditorComponent {}

@Component({
    selector: 'dot-loop-editor',
    template: '<div></div>'
})
export class DotLoopEditorComponent {}

const messages = {
    'message.containers.create.click_to_edit': 'Click to Edit',
    'message.containers.create.description': 'description',
    'message.containers.create.max_contents': 'Max Contents',
    'message.containers.create.clear': 'clear',
    'message.containers.create.content_type_code': 'Code'
};

describe('DotContainerPropertiesComponent', () => {
    let fixture: ComponentFixture<DotContainerPropertiesComponent>;
    let de: DebugElement;
    let coreWebService: CoreWebService;

    const messageServiceMock = new MockDotMessageService(messages);

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [
                DotContainerPropertiesComponent,
                DotContentEditorComponent,
                DotLoopEditorComponent
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({})
                    }
                },
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jasmine.createSpy(),
                        goToEditContainer: jasmine.createSpy(),
                        goToSiteBrowser: jasmine.createSpy()
                    }
                },
                StringUtils,
                DotHttpErrorManagerService,
                DotContentTypeService,
                DotAlertConfirmService,
                ConfirmationService,
                LoginService,
                DotcmsEventsService,
                DotEventsSocket,
                DotcmsConfigService,
                DotMessageDisplayService,
                DialogService,
                DotSiteBrowserService,
                DotContainersService,
                DotGlobalMessageService,
                DotEventsService,
                DotHttpErrorManagerService,
                LoggerService,
                { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
            ],
            imports: [
                CommonModule,
                DotMessagePipeModule,
                SharedModule,
                DotTextareaContentModule,
                CheckboxModule,
                InplaceModule,
                ReactiveFormsModule,
                MenuModule,
                ButtonModule,
                DotActionButtonModule,
                DotActionMenuButtonModule,
                DotAddToBundleModule,
                HttpClientTestingModule,
                DynamicDialogModule,
                DotAutofocusModule,
                BrowserAnimationsModule
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
        fixture = TestBed.createComponent(DotContainerPropertiesComponent);
        de = fixture.debugElement;
        coreWebService = TestBed.inject(CoreWebService);
    });

    describe('with data', () => {
        beforeEach(() => {
            spyOn<CoreWebService>(coreWebService, 'requestView').and.returnValue(
                of({
                    entity: [],
                    header: (type) => (type === 'Link' ? 'test;test=test' : '10')
                })
            );
            fixture.detectChanges();
        });

        it('should focus on title field', async () => {
            const inplace = de.query(By.css('[data-testId="inplace"]'));
            const title = de.query(By.css('[data-testId="title"]'));

            expect(inplace.componentInstance.active).toBe(true);
            expect(title.attributes.dotAutofocus).toBeDefined();
        });
    });
});
