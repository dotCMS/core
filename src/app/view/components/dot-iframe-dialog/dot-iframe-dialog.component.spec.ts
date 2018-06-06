import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement, Component } from '@angular/core';
import { async, ComponentFixture } from '@angular/core/testing';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DialogModule, Dialog } from 'primeng/primeng';
import { DotIframeDialogComponent } from './dot-iframe-dialog.component';
import { IFrameModule } from '../_common/iframe';
import { LoginService } from 'dotcms-js/dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { IframeComponent } from '../_common/iframe/iframe-component';

let component: DotIframeDialogComponent;
let de: DebugElement;
let dialog: DebugElement;
let dialogComponent: Dialog;
let hostDe: DebugElement;
let dotIframe: DebugElement;
let dotIframeComponent: IframeComponent;
let closeButton: DebugElement;

const getTestConfig = (hostComponent) => {
    return {
        imports: [DialogModule, BrowserAnimationsModule, IFrameModule, RouterTestingModule],
        providers: [
            {
                provide: LoginService,
                useClass: LoginServiceMock
            }
        ],
        declarations: [DotIframeDialogComponent, hostComponent]
    };
};

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-iframe-dialog [url]="url" [header]="header"></dot-iframe-dialog>'
})
class TestHostComponent {
    url: string;
    header: string;
}

describe('DotIframeDialogComponent', () => {
    let hostComponent: TestHostComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule(getTestConfig(TestHostComponent)).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(TestHostComponent);
        hostDe = hostFixture.debugElement;
        hostComponent = hostFixture.componentInstance;
        de = hostDe.query(By.css('dot-iframe-dialog'));
        component = de.componentInstance;
    });

    describe('default', () => {
        beforeEach(() => {
            hostFixture.detectChanges();
            dialog = de.query(By.css('p-dialog'));
            dotIframe = de.query(By.css('dot-iframe'));
        });

        describe('dialog', () => {
            it('should have', () => {
                expect(dialog).toBeTruthy();
            });
        });

        describe('iframe', () => {
            it('should not have', () => {
                expect(dotIframe === null).toBe(true);
            });
        });
    });

    describe('show', () => {
        const fakeEvent = () => {
            return {
                target: {
                    contentWindow: {
                        focus: jasmine.createSpy()
                    }
                }
            };
        };

        beforeEach(() => {
            hostComponent.url = 'hello/world';
            hostComponent.header = 'This is a header';
            hostFixture.detectChanges();
            dialog = de.query(By.css('p-dialog'));
            dialogComponent = dialog.componentInstance;
            dotIframe = de.query(By.css('dot-iframe'));
            closeButton = de.query(By.css('.ui-dialog-titlebar-icon.ui-dialog-titlebar-close.dialog__close'));
        });

        describe('events', () => {
            it('should emit load', () => {
                spyOn(component.load, 'emit');

                const mockEvent = fakeEvent();
                dotIframe.triggerEventHandler('load', mockEvent);
                expect(component.load.emit).toHaveBeenCalledWith(mockEvent);
            });

            describe('hide dialog', () => {
                beforeEach(() => {
                    spyOn(component.close, 'emit');
                    spyOn(component.beforeClose, 'emit');

                    component.url = 'hello.world.com';
                    component.show = true;
                    component.header = 'Header';
                });

                it('should hide and emit close when click close button', () => {
                    closeButton.triggerEventHandler('click', { preventDefault: () => {} });

                    expect(component.url).toBe(null);
                    expect(component.show).toBe(false);
                    expect(component.header).toBe('');
                    expect(component.close.emit).toHaveBeenCalledTimes(1);
                    expect(component.beforeClose.emit).not.toHaveBeenCalled();
                });

                it('should close ', () => {
                    document.dispatchEvent(new KeyboardEvent('keydown', {
                        key: 'Escape'
                    }));
                    expect(component.url).toBe(null);
                    expect(component.show).toBe(false);
                    expect(component.header).toBe('');
                    expect(component.close.emit).toHaveBeenCalledTimes(1);
                    expect(component.beforeClose.emit).not.toHaveBeenCalled();
                });
            });

            it('should emit keydown', () => {
                spyOn(component.keydown, 'emit');

                dotIframe.triggerEventHandler('keydown', { hello: 'world' });
                expect(component.keydown.emit).toHaveBeenCalledWith({ hello: 'world' });
            });

            it('should emit custom', () => {
                spyOn(component.custom, 'emit');

                dotIframe.triggerEventHandler('custom', {
                    detail: {
                        name: 'Hello World'
                    }
                });
                expect(component.custom.emit).toHaveBeenCalledWith({
                    detail: {
                        name: 'Hello World'
                    }
                });
            });
        });

        describe('dialog', () => {
            it('should have', () => {
                expect(dialog).toBeTruthy();
            });

            it('should have the right attrs', () => {
                expect(dialogComponent.visible).toEqual(true, 'visible');
                expect(dialogComponent.draggable).toEqual(false, 'draggable');
                expect(dialogComponent.dismissableMask).toEqual(false, 'dismissableMask');
                expect(dialogComponent.closable).toEqual(false, 'closable');
                expect(dialogComponent.modal).toEqual(true, 'modal');
                expect(dialogComponent.header).toBe('This is a header');
            });

            it('it should have custom close button', () => {
                expect(closeButton === null).toBe(false);
            });

            it('should focus in the iframe window', () => {
                const mockEvent = fakeEvent();
                dotIframe.triggerEventHandler('load', { ...mockEvent });
                expect(mockEvent.target.contentWindow.focus).toHaveBeenCalledTimes(1);
            });

            it('should call onClose on esc key', () => {
                spyOn(component, 'onClose');
                dotIframe.triggerEventHandler('keydown', {
                    key: 'Escape'
                });
                expect(component.onClose).toHaveBeenCalledTimes(1);
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

            it('should have the right attrs', () => {
                expect(dotIframeComponent.src).toBe('hello/world');
            });
        });
    });

    describe('show/hide', () => {
        beforeEach(() => {
            hostComponent.url = 'hello/world';
            hostFixture.detectChanges();
            dialog = de.query(By.css('p-dialog'));
            dotIframe = de.query(By.css('dot-iframe'));
        });

        it('should update', () => {
            expect(dialog).toBeTruthy();
            expect(dotIframe).toBeTruthy();

            hostComponent.url = null;
            hostFixture.detectChanges();
            dialog = de.query(By.css('p-dialog'));
            dotIframe = de.query(By.css('dot-iframe'));

            expect(dialog).toBeTruthy();
            expect(dotIframe === null).toBe(true);
        });
    });
});

@Component({
    selector: 'dot-test-host-2-component',
    template: '<dot-iframe-dialog [url]="url" [header]="header" (beforeClose)="onBeforeClose($event)"></dot-iframe-dialog>'
})
class TestHost2Component {
    url: string;
    header: string;

    onBeforeClose(_$event: { originalEvent: MouseEvent | KeyboardEvent; close: () => {} }) {}
}

describe('DotIframeDialogComponent with onBeforeClose event', () => {
    let hostFixture: ComponentFixture<TestHost2Component>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule(getTestConfig(TestHost2Component)).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(TestHost2Component);
        hostDe = hostFixture.debugElement;
        de = hostDe.query(By.css('dot-iframe-dialog'));
        component = de.componentInstance;
        closeButton = de.query(By.css('.ui-dialog-titlebar-icon.ui-dialog-titlebar-close.dialog__close'));
    });

    it('should emit beforeClose', () => {
        spyOn(component.beforeClose, 'emit');
        spyOn(component.close, 'emit');
        closeButton.triggerEventHandler('click', { preventDefault: () => {} });

        expect(component.beforeClose.emit).toHaveBeenCalledWith({
            originalEvent: { preventDefault: jasmine.any(Function) },
            close: jasmine.any(Function)
        });
        expect(component.close.emit).not.toHaveBeenCalled();
    });

    it('should close when callback is called', () => {
        spyOn(component.close, 'emit');

        component.beforeClose.subscribe(($event: { originalEvent: MouseEvent | KeyboardEvent; close: () => {} }) => {
            $event.close();

            expect(component.url).toBe(null);
            expect(component.show).toBe(false);
            expect(component.header).toBe('');
            expect(component.close.emit).toHaveBeenCalledTimes(1);
        });

        closeButton.triggerEventHandler('click', { preventDefault: () => {} });
    });
});
