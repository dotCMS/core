import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotEventsService,
    DotFormatDateService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageDisplayService,
    DotRouterService,
    DotUiColorsService
} from '@dotcms/data-access';
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
import {
    cleanUpDialog,
    CoreWebServiceMock,
    DotMessageDisplayServiceMock,
    LoginServiceMock,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotAddContentletComponent } from './dot-add-contentlet.component';

import { DotCustomEventHandlerService } from '../../../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';
import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '../../../../../test/dot-test-bed';
import { IframeOverlayService } from '../../../_common/iframe/service/iframe-overlay.service';
import { DotIframeDialogComponent } from '../../../dot-iframe-dialog/dot-iframe-dialog.component';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from '../dot-contentlet-wrapper/dot-contentlet-wrapper.component';

describe('DotAddContentletComponent', () => {
    let component: DotAddContentletComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotAddContentletComponent>;
    let dotAddContentletWrapper: DebugElement;
    let dotAddContentletWrapperComponent: DotContentletWrapperComponent;
    let dotContentletEditorService: DotContentletEditorService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                DotAddContentletComponent,
                DotContentletWrapperComponent,
                DotIframeDialogComponent,
                BrowserAnimationsModule,
                RouterTestingModule,
                HttpClientTestingModule
            ],
            providers: [
                DotContentletEditorService,
                DotMenuService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotAlertConfirmService,
                DotEventsService,
                ConfirmationService,
                DotFormatDateService,
                DotHttpErrorManagerService,
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                ApiRoot,
                DotIframeService,
                { provide: DotUiColorsService, useClass: MockDotUiColorsService },
                {
                    provide: IframeOverlayService,
                    useValue: {
                        overlay: of(false),
                        show: jest.fn(),
                        hide: jest.fn(),
                        toggle: jest.fn()
                    }
                },
                DotcmsEventsService,
                DotEventsSocket,
                { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                DotcmsConfigService,
                LoggerService,
                StringUtils,
                UserModel,
                {
                    provide: DotCustomEventHandlerService,
                    useValue: {
                        handle: jest.fn()
                    }
                }
            ]
        });
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(DotAddContentletComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotContentletEditorService = de.injector.get(DotContentletEditorService);

        jest.spyOn(component.shutdown, 'emit');
        jest.spyOn(component.custom, 'emit');

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
                    load: jest.fn(),
                    keyDown: jest.fn()
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
