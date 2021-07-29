import { of } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import {
    CoreWebService,
    DotcmsConfigService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';

import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from './dot-contentlet-wrapper.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '@services/dot-menu.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ConfirmationService } from 'primeng/api';
import { MockDotRouterService } from '@tests/dot-router-service.mock';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { dotEventSocketURLFactory, MockDotUiColorsService } from '@tests/dot-test-bed';

const messageServiceMock = new MockDotMessageService({
    'editcontentlet.lose.dialog.header': 'Header',
    'editcontentlet.lose.dialog.message': 'Message',
    'editcontentlet.lose.dialog.accept': 'Accept'
});

describe('DotContentletWrapperComponent', () => {
    let component: DotContentletWrapperComponent;
    let de: DebugElement;
    let fixture: ComponentFixture<DotContentletWrapperComponent>;
    let dotIframeDialog: DebugElement;
    let dotAddContentletService: DotContentletEditorService;
    let dotAlertConfirmService: DotAlertConfirmService;
    let dotRouterService: DotRouterService;
    let dotIframeService: DotIframeService;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                declarations: [DotContentletWrapperComponent],
                providers: [
                    DotContentletEditorService,
                    DotIframeService,
                    DotAlertConfirmService,
                    ConfirmationService,
                    DotcmsEventsService,
                    DotEventsSocket,
                    DotcmsConfigService,
                    LoggerService,
                    StringUtils,
                    { provide: DotEventsSocketURL, useFactory: dotEventSocketURLFactory },
                    {
                        provide: DotHttpErrorManagerService,
                        useValue: {
                            handle: jasmine.createSpy().and.returnValue(of({}))
                        }
                    },
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    },
                    {
                        provide: DotMenuService,
                        useValue: {
                            getDotMenuId() {
                                return of('999');
                            }
                        }
                    },
                    {
                        provide: DotMessageService,
                        useValue: messageServiceMock
                    },
                    {
                        provide: CoreWebService,
                        useClass: CoreWebServiceMock
                    },
                    { provide: DotRouterService, useClass: MockDotRouterService },
                    { provide: DotUiColorsService, useClass: MockDotUiColorsService }
                ],
                imports: [
                    DotIframeDialogModule,
                    RouterTestingModule,
                    BrowserAnimationsModule,
                    HttpClientTestingModule
                ]
            });
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotContentletWrapperComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotAddContentletService = de.injector.get(DotContentletEditorService);
        dotAlertConfirmService = de.injector.get(DotAlertConfirmService);
        dotRouterService = de.injector.get(DotRouterService);
        dotIframeService = de.injector.get(DotIframeService);

        spyOn(dotIframeService, 'reload');
        spyOn(dotAddContentletService, 'clear');
        spyOn(dotAddContentletService, 'load');
        spyOn(dotAddContentletService, 'keyDown');
        spyOn(component.close, 'emit');
        spyOn(component.custom, 'emit');
    });

    it('should show dot-iframe-dialog', () => {
        fixture.detectChanges();
        dotIframeDialog = de.query(By.css('dot-iframe-dialog'));

        expect(dotIframeDialog).not.toBe(null);
    });

    describe('with data', () => {
        beforeEach(() => {
            component.url = 'hello.world.com';
            fixture.detectChanges();
            dotIframeDialog = de.query(By.css('dot-iframe-dialog'));
        });

        it('should have dot-iframe-dialog', () => {
            expect(dotIframeDialog).toBeDefined();
        });

        describe('events', () => {
            it('should call load', () => {
                dotIframeDialog.triggerEventHandler('load', { hello: 'world' });
                expect(dotAddContentletService.load).toHaveBeenCalledWith({ hello: 'world' });
            });

            it('should close the dialog and redirect to Edit Page', () => {
                component.header = 'header';

                dotIframeDialog.triggerEventHandler('custom', {
                    detail: {
                        name: 'close',
                        data: {
                            redirectUrl: 'testUrl',
                            languageId: '1'
                        }
                    }
                });
                expect(dotAddContentletService.clear).toHaveBeenCalledTimes(1);
                expect(component.header).toBe('');
                expect(component.custom.emit).toHaveBeenCalledTimes(1);
                expect(component.close.emit).toHaveBeenCalledTimes(1);
                expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                    url: 'testUrl',
                    language_id: '1'
                });
            });

            it('should close the dialog and do not redirect to Edit Page', () => {
                dotIframeDialog.triggerEventHandler('custom', {
                    detail: {
                        name: 'close'
                    }
                });
                expect(dotRouterService.goToEditPage).not.toHaveBeenCalled();
            });

            it('should called goToEdit', () => {
                dotIframeDialog.triggerEventHandler('custom', {
                    detail: {
                        name: 'edit-page',
                        data: {
                            url: 'some/fake/url',
                            languageId: '1',
                            hostId: '123'
                        }
                    }
                });

                expect(dotRouterService.goToEditPage).toHaveBeenCalledWith({
                    url: 'some/fake/url',
                    language_id: '1',
                    host_id: '123'
                });
            });

            describe('beforeClose', () => {
                it('should close without confirmation dialog', () => {
                    dotIframeDialog.triggerEventHandler('beforeClose', {
                        close: () => {
                            dotIframeDialog.triggerEventHandler('close', {});
                        }
                    });
                    expect(dotAddContentletService.clear).toHaveBeenCalledTimes(1);
                    expect(component.close.emit).toHaveBeenCalledTimes(1);
                });

                it('should show confirmation dialog and handle accept', () => {
                    spyOn(dotAlertConfirmService, 'confirm').and.callFake((conf) => {
                        conf.accept();
                    });

                    dotIframeDialog.triggerEventHandler('custom', {
                        detail: {
                            name: 'edit-contentlet-data-updated',
                            payload: true
                        }
                    });

                    dotIframeDialog.triggerEventHandler('beforeClose', {
                        close: () => {
                            dotIframeDialog.triggerEventHandler('close', {});
                        }
                    });

                    expect<any>(dotAlertConfirmService.confirm).toHaveBeenCalledWith({
                        accept: jasmine.any(Function),
                        reject: jasmine.any(Function),
                        header: 'Header',
                        message: 'Message',
                        footerLabel: {
                            accept: 'Accept'
                        }
                    });
                    expect(component.close.emit).toHaveBeenCalledTimes(1);
                    expect(component.custom.emit).toHaveBeenCalledTimes(1);
                    expect(dotAddContentletService.clear).toHaveBeenCalledTimes(1);
                });

                it('should show confirmation dialog and handle reject', () => {
                    spyOn(dotAlertConfirmService, 'confirm').and.callFake((conf) => {
                        conf.reject();
                    });

                    dotIframeDialog.triggerEventHandler('custom', {
                        detail: {
                            name: 'edit-contentlet-data-updated',
                            payload: true
                        }
                    });

                    dotIframeDialog.triggerEventHandler('beforeClose', {
                        close: () => {}
                    });

                    expect<any>(dotAlertConfirmService.confirm).toHaveBeenCalledWith({
                        accept: jasmine.any(Function),
                        reject: jasmine.any(Function),
                        header: 'Header',
                        message: 'Message',
                        footerLabel: {
                            accept: 'Accept'
                        }
                    });
                    expect(component.close.emit).not.toHaveBeenCalled();
                    expect(dotAddContentletService.clear).not.toHaveBeenCalled();
                });

                it('should emit custom evt with params', () => {
                    const params = {
                        detail: {
                            name: 'save-page',
                            payload: {
                                hello: 'world'
                            }
                        }
                    };
                    dotIframeDialog.triggerEventHandler('custom', params);
                    expect(component.custom.emit).toHaveBeenCalledWith(params);
                    expect(dotIframeService.reload).toHaveBeenCalledTimes(1);
                });
            });
        });
    });
});
