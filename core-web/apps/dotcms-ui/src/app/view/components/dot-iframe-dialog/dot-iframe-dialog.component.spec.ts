/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotDialogComponent } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

import { IframeComponent } from '../_common/iframe/iframe-component';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

let component: DotIframeDialogComponent;
let de: DebugElement;
let dialog: DebugElement;
let dialogComponent: DotDialogComponent;
let hostDe: DebugElement;
let dotIframe: DebugElement;
let dotIframeComponent: IframeComponent;

const getTestConfig = (hostComponent) => {
    return {
        imports: [
            DotDialogComponent,
            BrowserAnimationsModule,
            IframeComponent,
            RouterTestingModule,
            HttpClientTestingModule
        ],
        providers: [
            {
                provide: LoginService,
                useClass: LoginServiceMock
            },
            {
                provide: CoreWebService,
                useClass: CoreWebServiceMock
            },
            DotIframeService,
            DotRouterService,
            DotUiColorsService,
            DotcmsEventsService,
            DotEventsSocket,
            {
                provide: DotEventsSocketURL,
                useFactory: () =>
                    new DotEventsSocketURL(
                        `${window.location.hostname}:${window.location.port}/api/ws/v1/system/events`,
                        window.location.protocol === 'https:'
                    )
            },
            DotLoadingIndicatorService,
            LoggerService,
            StringUtils,
            IframeOverlayService
        ],
        declarations: [DotIframeDialogComponent, hostComponent]
    };
};

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-iframe-dialog [url]="url" [header]="header"></dot-iframe-dialog>',
    standalone: false
})
class TestHostComponent {
    url: string;
    header: string;
}

@Component({
    selector: 'dot-test-host2-component',
    template:
        '<dot-iframe-dialog [url]="url" [header]="header" (beforeClose)="onBeforeClose()"></dot-iframe-dialog>',
    standalone: false
})
class TestHost2Component {
    url: string;
    header: string;

    onBeforeClose(): void {}
}

const fakeEvent = () => {
    return {
        target: {
            contentWindow: {
                focus: jest.fn()
            }
        }
    };
};

describe('DotIframeDialogComponent', () => {
    describe('no beforeClose set', () => {
        let hostComponent: TestHostComponent;
        let hostFixture: ComponentFixture<TestHostComponent>;

        beforeEach(waitForAsync(() => {
            TestBed.configureTestingModule(getTestConfig(TestHostComponent));
        }));

        beforeEach(() => {
            hostFixture = TestBed.createComponent(TestHostComponent);
            hostDe = hostFixture.debugElement;
            hostComponent = hostFixture.componentInstance;
            de = hostDe.query(By.css('dot-iframe-dialog'));
            component = de.componentInstance;
        });

        describe('hidden', () => {
            beforeEach(() => {
                hostFixture.detectChanges();
                dialog = de.query(By.css('dot-dialog'));
                dialogComponent = dialog.componentInstance;
                dotIframe = de.query(By.css('dot-iframe'));
            });

            it('should have', () => {
                expect(dialog).toBeTruthy();
            });

            it('should have the right attrs', () => {
                expect(dialogComponent.visible).toEqual(false, 'hidden');
                expect(dialogComponent.header).toBeUndefined();
                expect(dialogComponent.contentStyle).toEqual({ padding: '0' });
                expect(dialogComponent.headerStyle).toEqual({ 'background-color': '#f1f3f4' });
            });
        });

        describe('show', () => {
            beforeEach(() => {
                hostComponent.url = 'hello/world';
                hostComponent.header = 'This is a header';
                hostFixture.detectChanges();
                dialog = de.query(By.css('dot-dialog'));
                dialogComponent = dialog.componentInstance;
                dotIframe = de.query(By.css('dot-iframe'));
            });

            describe('dot-dialog', () => {
                it('should have', () => {
                    expect(dialog).toBeTruthy();
                });

                it('should set visible attr', () => {
                    expect(dialogComponent.visible).toEqual(true, 'visible');
                });

                it('should set header attr', () => {
                    expect(dialogComponent.header).toContain('This is a header');
                });

                it('should set width and height att', () => {
                    expect(dialogComponent.width).toEqual('90vw');
                    expect(dialogComponent.height).toEqual('90vh');
                });
            });

            describe('dot-iframe', () => {
                beforeEach(() => {
                    dotIframe = de.query(By.css('dot-iframe'));
                    dotIframeComponent = dotIframe.componentInstance;
                });

                it('should have', () => {
                    expect(dotIframe).toBeTruthy();
                });

                it('should set src attr', () => {
                    expect(dotIframeComponent.src).toBe('hello/world');
                });

                it('should focus in the iframe window on dot-iframe load', () => {
                    const mockEvent = fakeEvent();
                    dotIframe.triggerEventHandler('charge', { ...mockEvent });
                    expect(mockEvent.target.contentWindow.focus).toHaveBeenCalledTimes(1);
                });
            });

            describe('events', () => {
                beforeEach(() => {
                    jest.spyOn(component.beforeClose, 'emit');
                    jest.spyOn(component.shutdown, 'emit');
                    jest.spyOn(component.custom, 'emit');
                    jest.spyOn(component.keyWasDown, 'emit');
                    jest.spyOn(component.charge, 'emit');
                    jest.spyOn(dialog.componentInstance, 'close');
                });

                describe('dot-iframe', () => {
                    it('should emit events from dot-iframe', () => {
                        const mockEvent = fakeEvent();
                        dotIframe.triggerEventHandler('charge', mockEvent);

                        dotIframe.triggerEventHandler('keyWasDown', { hello: 'world' });

                        dotIframe.triggerEventHandler('custom', {
                            detail: {
                                name: 'Hello World'
                            }
                        });

                        expect(component.charge.emit).toHaveBeenCalledWith(mockEvent);
                        expect(component.charge.emit).toHaveBeenCalledTimes(1);
                        expect<any>(component.keyWasDown.emit).toHaveBeenCalledWith({
                            hello: 'world'
                        });
                        expect<any>(component.custom.emit).toHaveBeenCalledWith({
                            detail: {
                                name: 'Hello World'
                            }
                        });
                    });

                    it('should call close method on dot-dialog on dot-iframe escape key', () => {
                        dotIframe.triggerEventHandler('keyWasDown', {
                            key: 'Escape'
                        });

                        expect(component.keyWasDown.emit).toHaveBeenCalledTimes(1);
                    });
                });

                describe('dot-dialog', () => {
                    beforeEach(() => {
                        component.show = true;
                    });

                    it('should handle hide', () => {
                        dialog.triggerEventHandler('hide', {});

                        expect(component.url).toBe(null);
                        expect(component.show).toBe(false);
                        expect(component.header).toBe('');
                        expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
                        expect(component.beforeClose.emit).not.toHaveBeenCalled();
                    });

                    it('should NOT emit beforeClose when no observer is set', () => {
                        dialog.triggerEventHandler('beforeClose', {});
                        expect(component.beforeClose.emit).not.toHaveBeenCalled();
                    });
                });
            });
        });
    });

    describe('beforeClose set', () => {
        let hostFixture: ComponentFixture<TestHost2Component>;
        let hostComponent: TestHostComponent;

        beforeEach(waitForAsync(() => {
            TestBed.configureTestingModule(getTestConfig(TestHost2Component));
        }));

        beforeEach(() => {
            hostFixture = TestBed.createComponent(TestHost2Component);
            hostDe = hostFixture.debugElement;
            hostComponent = hostFixture.componentInstance;
            de = hostDe.query(By.css('dot-iframe-dialog'));
            component = de.componentInstance;
            hostComponent.url = 'hello/world';
            hostFixture.detectChanges();
            dialog = de.query(By.css('dot-dialog'));
            dialogComponent = dialog.componentInstance;
            jest.spyOn(component.beforeClose, 'emit');
        });

        it('should emit beforeClose when a observer is set', () => {
            dialogComponent.beforeClose.emit({
                close: () => {
                    //
                }
            });
            expect(component.beforeClose.emit).toHaveBeenCalledTimes(1);
        });

        it('should NOT emit beforeClose when dialog is hidden', () => {
            hostComponent.url = null;
            hostFixture.detectChanges();

            dialogComponent.beforeClose.emit({
                close: () => {
                    //
                }
            });
            expect(component.beforeClose.emit).not.toHaveBeenCalled();
        });
    });
});
