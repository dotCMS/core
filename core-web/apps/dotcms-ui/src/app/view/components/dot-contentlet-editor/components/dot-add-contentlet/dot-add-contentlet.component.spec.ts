import { By } from '@angular/platform-browser';
import { ComponentFixture, waitForAsync, TestBed } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';
import { DotAddContentletComponent } from './dot-add-contentlet.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '@services/dot-menu.service';
import {
    ApiRoot,
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils,
    UserModel
} from '@dotcms/dotcms-js';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { ConfirmationService } from 'primeng/api';
import { DotFormatDateService } from '@services/dot-format-date-service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import cleanUpDialog from '@tests/clean-up-dialog';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

describe('DotAddContentletComponent', () => {
    let component: DotAddContentletComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotAddContentletComponent>;
    let dotAddContentletWrapper: DebugElement;
    let dotAddContentletWrapperComponent: DotContentletWrapperComponent;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotAddContentletComponent, DotContentletWrapperComponent],
                providers: [
                    DotContentletEditorService,
                    DotMenuService,
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    },
                    DotAlertConfirmService,
                    ConfirmationService,
                    DotFormatDateService,
                    DotHttpErrorManagerService,
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    { provide: DotRouterService, useClass: MockDotRouterService },
                    ApiRoot,
                    DotIframeService,
                    { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                    DotcmsEventsService,
                    DotEventsSocket,
                    { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                    DotcmsConfigService,
                    LoggerService,
                    StringUtils,
                    UserModel
                ],
                imports: [
                    DotIframeDialogModule,
                    BrowserAnimationsModule,
                    RouterTestingModule,
                    HttpClientTestingModule
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotAddContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotContentletEditorService = de.injector.get(DotContentletEditorService);

        spyOn(component.shutdown, 'emit');
        spyOn(component.custom, 'emit');

        fixture.detectChanges();

        dotAddContentletWrapper = de.query(By.css('dot-contentlet-wrapper'));
        dotAddContentletWrapperComponent = dotAddContentletWrapper.componentInstance;
    });

    describe('default', () => {
        it('should have dot-contentlet-wrapper', () => {
            expect(dotAddContentletWrapper).toBeTruthy();
        });

        it('should emit close', () => {
            dotAddContentletWrapper.triggerEventHandler('shutdown', {});
            expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
        });

        it('should emit custom', () => {
            dotAddContentletWrapper.triggerEventHandler('custom', {});
            expect(component.custom.emit).toHaveBeenCalledTimes(1);
        });

        it('should have url in null', () => {
            expect(dotAddContentletWrapperComponent.url).toEqual(null);
        });

        it('should set url', () => {
            dotContentletEditorService.add({
                header: 'Add some content',
                data: {
                    container: '123',
                    baseTypes: 'content,form'
                },
                events: {
                    load: jasmine.createSpy('load'),
                    keyDown: jasmine.createSpy('keyDown')
                }
            });

            fixture.detectChanges();

            expect(dotAddContentletWrapperComponent.url).toEqual(
                '/html/ng-contentlet-selector.jsp?ng=true&container_id=123&add=content,form'
            );

            expect(dotAddContentletWrapperComponent.header).toEqual('Add some content');
        });
    });

    afterEach(() => {
        cleanUpDialog(fixture);
    });
});
