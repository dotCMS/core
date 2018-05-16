import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By, DomSanitizer } from '@angular/platform-browser';
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

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-iframe-dialog [url]="url"></dot-iframe-dialog>'
})
class TestHostComponent {
    url: string;
}

describe('DotIframeDialogComponent', () => {
    let component: DotIframeDialogComponent;
    let de: DebugElement;
    let dialog: DebugElement;
    let dialogComponent: Dialog;
    let hostComponent: TestHostComponent;
    let hostDe: DebugElement;
    let hostFixture: ComponentFixture<TestHostComponent>;
    let sanitizer: DomSanitizer;
    let dotIframe: DebugElement;
    let dotIframeComponent: IframeComponent;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [DialogModule, BrowserAnimationsModule, IFrameModule, RouterTestingModule],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                }
            ],
            declarations: [DotIframeDialogComponent, TestHostComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(TestHostComponent);
        hostDe = hostFixture.debugElement;
        hostComponent = hostFixture.componentInstance;
        de = hostDe.query(By.css('dot-iframe-dialog'));
        component = de.componentInstance;
        sanitizer = de.injector.get(DomSanitizer);
        spyOn(component, 'closeDialog').and.callThrough();
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
            hostFixture.detectChanges();
            dialog = de.query(By.css('p-dialog'));
            dialogComponent = dialog.componentInstance;
            dotIframe = de.query(By.css('dot-iframe'));
        });

        describe('events', () => {
            it('should emit load', () => {
                spyOn(component.load, 'emit');

                const mockEvent = fakeEvent();
                dotIframe.triggerEventHandler('load', mockEvent);
                expect(component.load.emit).toHaveBeenCalledWith(mockEvent);
            });

            it('should emit close', () => {
                spyOn(component.close, 'emit');

                dialog.triggerEventHandler('onHide', {});
                expect(component.close.emit).toHaveBeenCalledTimes(1);
            });

            it('should emit keydown', () => {
                spyOn(component.keydown, 'emit');

                dotIframe.triggerEventHandler('keydown', { hello: 'world' });
                expect(component.keydown.emit).toHaveBeenCalledWith({ hello: 'world' });
            });

            it('should emit custom', () => {
                spyOn(component.custom, 'emit');

                dotIframe.triggerEventHandler('custom', { hello: 'world' });
                expect(component.custom.emit).toHaveBeenCalledWith({ hello: 'world' });
            });
        });

        describe('dialog', () => {
            it('should have', () => {
                expect(dialog).toBeTruthy();
            });

            it('should have the right attrs', () => {
                expect(dialogComponent.visible).toEqual(true, 'visible');
                expect(dialogComponent.draggable).toEqual(false, 'draggable');
                expect(dialogComponent.dismissableMask).toEqual(true, 'dismissableMask');
                expect(dialogComponent.modal).toEqual(true, 'modal');
            });

            it('should focus in the iframe window', () => {
                const mockEvent = fakeEvent();
                dotIframe.triggerEventHandler('load', { ...mockEvent });
                expect(mockEvent.target.contentWindow.focus).toHaveBeenCalledTimes(1);
            });

            it('should close dialog on esc key', () => {
                dotIframe.triggerEventHandler('keydown', {
                    key: 'Escape'
                });
                expect(component.closeDialog).toHaveBeenCalledTimes(1);
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
