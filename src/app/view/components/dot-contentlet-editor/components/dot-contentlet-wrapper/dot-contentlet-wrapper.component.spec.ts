import { of as observableOf } from 'rxjs';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture } from '@angular/core/testing';

import { LoginService } from 'dotcms-js';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotContentletEditorService } from '../../services/dot-contentlet-editor.service';
import { DotContentletWrapperComponent } from './dot-contentlet-wrapper.component';
import { DotIframeDialogModule } from '../../../dot-iframe-dialog/dot-iframe-dialog.module';
import { DotMenuService } from '@services/dot-menu.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { LoginServiceMock } from '../../../../../test/login-service.mock';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotRouterService } from '@services/dot-router/dot-router.service';

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

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotContentletWrapperComponent],
            providers: [
                DotContentletEditorService,
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMenuService,
                    useValue: {
                        getDotMenuId() {
                            return observableOf('999');
                        }
                    }
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [DotIframeDialogModule, RouterTestingModule, BrowserAnimationsModule]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = DOTTestBed.createComponent(DotContentletWrapperComponent);
        de = fixture.debugElement;
        component = de.componentInstance;
        dotAddContentletService = de.injector.get(DotContentletEditorService);
        dotAlertConfirmService = de.injector.get(DotAlertConfirmService);
        dotRouterService = de.injector.get(DotRouterService);

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

            it('should close the dialog', () => {
                component.header = 'header';

                dotIframeDialog.triggerEventHandler('custom', {
                    detail: {
                        name: 'close'
                    }
                });
                expect(dotAddContentletService.clear).toHaveBeenCalledTimes(1);
                expect(component.header).toBe('');
                expect(component.custom.emit).toHaveBeenCalledTimes(1);
                expect(component.close.emit).toHaveBeenCalledTimes(1);
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

                    expect(dotAlertConfirmService.confirm).toHaveBeenCalledWith({
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

                    expect(dotAlertConfirmService.confirm).toHaveBeenCalledWith({
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
                });
            });
        });
    });
});
