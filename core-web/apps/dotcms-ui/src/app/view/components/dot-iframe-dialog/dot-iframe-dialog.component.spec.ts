/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Component, DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

import { DotIframeService, DotRouterService, DotUiColorsService } from '@dotcms/data-access';
import {
    CoreWebService,
    DotcmsEventsService,
    DotEventsSocket,
    DotEventsSocketURL,
    LoggerService,
    LoginService,
    StringUtils
} from '@dotcms/dotcms-js';
import { DotLoadingIndicatorService } from '@dotcms/utils';
import { CoreWebServiceMock, LoginServiceMock } from '@dotcms/utils-testing';

import { DotIframeDialogComponent } from './dot-iframe-dialog.component';

import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

@Component({
    selector: 'dot-test-host-component',
    template:
        '<dot-iframe-dialog [url]="url" [header]="header" (beforeClose)="onBeforeClose($event)"></dot-iframe-dialog>',
    standalone: true,
    imports: [DotIframeDialogComponent]
})
class TestHostComponent {
    url: string;
    header: string;
    onBeforeClose = jest.fn();
}

@Component({
    selector: 'dot-test-host2-component',
    template:
        '<dot-iframe-dialog [url]="url" [header]="header" (beforeClose)="onBeforeClose($event)"></dot-iframe-dialog>',
    standalone: true,
    imports: [DotIframeDialogComponent]
})
class TestHost2Component {
    url: string;
    header: string;
    onBeforeClose = jest.fn();
}

const fakeEvent = () => ({
    target: {
        contentWindow: {
            focus: jest.fn()
        }
    }
});

describe('DotIframeDialogComponent', () => {
    const defaultProviders = [
        { provide: LoginService, useClass: LoginServiceMock },
        { provide: CoreWebService, useClass: CoreWebServiceMock },
        DotIframeService,
        DotRouterService,
        DotUiColorsService,
        DotcmsEventsService,
        DotEventsSocket,
        {
            provide: DotEventsSocketURL,
            useFactory: () =>
                new DotEventsSocketURL(
                    `${typeof window !== 'undefined' ? window.location.hostname : ''}:${typeof window !== 'undefined' ? window.location.port : ''}/api/ws/v1/system/events`,
                    typeof window !== 'undefined' && window.location.protocol === 'https:'
                )
        },
        DotLoadingIndicatorService,
        LoggerService,
        StringUtils,
        IframeOverlayService
    ];

    describe('no beforeClose set', () => {
        let spectator: Spectator<TestHostComponent>;
        let component: DotIframeDialogComponent;
        let hostComponent: TestHostComponent;
        let de: DebugElement;
        let dialogDe: DebugElement;
        let dotIframeDe: DebugElement;

        const createHost = createComponentFactory({
            component: TestHostComponent,
            imports: [BrowserAnimationsModule, RouterTestingModule, HttpClientTestingModule],
            providers: defaultProviders,
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createHost();
            hostComponent = spectator.component;
            de = spectator.debugElement.query(By.css('dot-iframe-dialog'));
            component = de?.componentInstance ?? null;
        });

        describe('hidden', () => {
            beforeEach(() => {
                spectator.detectChanges();
                dialogDe = de?.query(By.css('p-dialog')) ?? null;
                dotIframeDe = de?.query(By.css('dot-iframe')) ?? null;
            });

            it('should have', () => {
                expect(dialogDe).toBeTruthy();
            });

            it('should have the right attrs', () => {
                const dialog = dialogDe?.componentInstance;
                expect(dialog).toBeTruthy();
                expect(component.show).toBeFalsy();
                expect(component.header).toBeUndefined();
            });
        });

        describe('show', () => {
            beforeEach(() => {
                hostComponent.url = 'hello/world';
                hostComponent.header = 'This is a header';
                spectator.detectChanges();
                dialogDe = de?.query(By.css('p-dialog')) ?? null;
                dotIframeDe = de?.query(By.css('dot-iframe')) ?? null;
            });

            describe('p-dialog', () => {
                it('should have', () => {
                    expect(dialogDe).toBeTruthy();
                });

                it('should set visible attr', () => {
                    expect(component.show).toBe(true);
                });

                it('should set header attr', () => {
                    expect(component.header).toContain('This is a header');
                });

                it('should set width and height att', () => {
                    const dialog = dialogDe?.componentInstance as {
                        style?: Record<string, string>;
                    };
                    expect(dialog?.style).toBeDefined();
                    expect(component.show).toBe(true);
                });
            });

            describe('dot-iframe', () => {
                let dotIframeComponent: { src?: string };

                beforeEach(() => {
                    dotIframeDe = de?.query(By.css('dot-iframe')) ?? null;
                    dotIframeComponent = dotIframeDe?.componentInstance ?? ({} as { src?: string });
                });

                it('should have', () => {
                    expect(dotIframeDe).toBeTruthy();
                });

                it('should set src attr', () => {
                    expect(dotIframeComponent.src).toBe('hello/world');
                });

                it('should focus in the iframe window on dot-iframe load', () => {
                    const mockEvent = fakeEvent();
                    dotIframeDe?.triggerEventHandler('charge', { ...mockEvent });
                    expect(mockEvent.target.contentWindow.focus).toHaveBeenCalledTimes(1);
                });
            });

            describe('events', () => {
                let dialogDeEvents: DebugElement;

                beforeEach(() => {
                    jest.spyOn(component.beforeClose, 'emit');
                    jest.spyOn(component.shutdown, 'emit');
                    jest.spyOn(component.custom, 'emit');
                    jest.spyOn(component.keyWasDown, 'emit');
                    jest.spyOn(component.charge, 'emit');
                    dialogDeEvents = de?.query(By.css('p-dialog')) ?? null;
                    if (
                        dialogDeEvents?.componentInstance &&
                        typeof (dialogDeEvents.componentInstance as { close?: () => void })
                            .close === 'function'
                    ) {
                        jest.spyOn(
                            dialogDeEvents.componentInstance as { close: () => void },
                            'close'
                        );
                    }
                });

                describe('dot-iframe', () => {
                    it('should emit events from dot-iframe', () => {
                        const mockEvent = fakeEvent();
                        dotIframeDe?.triggerEventHandler('charge', mockEvent);
                        dotIframeDe?.triggerEventHandler('keyWasDown', { hello: 'world' });
                        dotIframeDe?.triggerEventHandler('custom', {
                            detail: { name: 'Hello World' }
                        });

                        expect(component.charge.emit).toHaveBeenCalledWith(mockEvent);
                        expect(component.charge.emit).toHaveBeenCalledTimes(1);
                        expect<any>(component.keyWasDown.emit).toHaveBeenCalledWith({
                            hello: 'world'
                        });
                        expect<any>(component.custom.emit).toHaveBeenCalledWith({
                            detail: { name: 'Hello World' }
                        });
                    });

                    it('should call close method on dot-dialog on dot-iframe escape key', () => {
                        dotIframeDe?.triggerEventHandler('keyWasDown', { key: 'Escape' });
                        expect(component.keyWasDown.emit).toHaveBeenCalledTimes(1);
                    });
                });

                describe('p-dialog', () => {
                    beforeEach(() => {
                        component.show = true;
                    });

                    it('should handle hide', () => {
                        component.onDialogHide();
                        expect(component.url).toBe(null);
                        expect(component.show).toBe(false);
                        expect(component.header).toBe('');
                        expect(component.shutdown.emit).toHaveBeenCalledTimes(1);
                        expect(component.beforeClose.emit).not.toHaveBeenCalled();
                    });

                    it('should NOT emit beforeClose when no observer is set', () => {
                        dialogDe?.triggerEventHandler('beforeClose', {});
                        expect(component.beforeClose.emit).not.toHaveBeenCalled();
                    });
                });
            });
        });
    });

    describe('beforeClose set', () => {
        let spectator: Spectator<TestHost2Component>;
        let component: DotIframeDialogComponent;
        let hostComponent: TestHost2Component;
        let de: DebugElement;

        const createHost2 = createComponentFactory({
            component: TestHost2Component,
            imports: [BrowserAnimationsModule, RouterTestingModule, HttpClientTestingModule],
            providers: defaultProviders,
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createHost2();
            hostComponent = spectator.component;
            de = spectator.debugElement.query(By.css('dot-iframe-dialog'));
            component = de?.componentInstance ?? null;
            hostComponent.url = 'hello/world';
            spectator.detectChanges();
            jest.spyOn(component.beforeClose, 'emit');
        });

        it('should emit beforeClose when a observer is set', () => {
            component.beforeClose.emit({ close: () => {} });
            expect(hostComponent.onBeforeClose).toHaveBeenCalledTimes(1);
            expect(hostComponent.onBeforeClose).toHaveBeenCalledWith({
                close: expect.any(Function)
            });
        });

        it('should NOT emit beforeClose when dialog is hidden', () => {
            hostComponent.url = null;
            spectator.detectChanges();
            expect(component.beforeClose.emit).not.toHaveBeenCalled();
        });
    });
});
