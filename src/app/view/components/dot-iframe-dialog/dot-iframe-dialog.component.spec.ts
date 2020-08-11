import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DebugElement, Component } from '@angular/core';
import { async, ComponentFixture } from '@angular/core/testing';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotIframeDialogComponent } from './dot-iframe-dialog.component';
import { IFrameModule } from '../_common/iframe';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '../../../test/login-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { IframeComponent } from '../_common/iframe/iframe-component';
import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';

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
            DotDialogModule,
            BrowserAnimationsModule,
            IFrameModule,
            RouterTestingModule,
            DotIconButtonModule
        ],
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

@Component({
    selector: 'dot-test-host2-component',
    template:
        '<dot-iframe-dialog [url]="url" [header]="header" (beforeClose)="onBeforeClose()"></dot-iframe-dialog>'
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
                focus: jasmine.createSpy('focus')
            }
        }
    };
};

describe('DotIframeDialogComponent', () => {
    describe('no beforeClose set', () => {
        let hostComponent: TestHostComponent;
        let hostFixture: ComponentFixture<TestHostComponent>;

        beforeEach(async(() => {
            DOTTestBed.configureTestingModule(getTestConfig(TestHostComponent));
        }));

        beforeEach(() => {
            hostFixture = DOTTestBed.createComponent(TestHostComponent);
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
                    dotIframe.triggerEventHandler('load', { ...mockEvent });
                    expect(mockEvent.target.contentWindow.focus).toHaveBeenCalledTimes(1);
                });
            });

            describe('events', () => {
                beforeEach(() => {
                    spyOn(component.beforeClose, 'emit');
                    spyOn(component.close, 'emit');
                    spyOn(component.custom, 'emit');
                    spyOn(component.keydown, 'emit');
                    spyOn(component.load, 'emit');
                    spyOn(dialog.componentInstance, 'close');
                });

                describe('dot-iframe', () => {
                    it('should emit events from dot-iframe', () => {
                        const mockEvent = fakeEvent();
                        dotIframe.triggerEventHandler('load', mockEvent);

                        dotIframe.triggerEventHandler('keydown', { hello: 'world' });

                        dotIframe.triggerEventHandler('custom', {
                            detail: {
                                name: 'Hello World'
                            }
                        });

                        expect(component.load.emit).toHaveBeenCalledWith(mockEvent);
                        expect(component.keydown.emit).toHaveBeenCalledWith({ hello: 'world' });
                        expect(component.custom.emit).toHaveBeenCalledWith({
                            detail: {
                                name: 'Hello World'
                            }
                        });
                    });

                    it('should call close method on dot-dialog on dot-iframe escape key', () => {
                        dotIframe.triggerEventHandler('keydown', {
                            key: 'Escape'
                        });

                        expect(component.keydown.emit).toHaveBeenCalledTimes(1);
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
                        expect(component.close.emit).toHaveBeenCalledTimes(1);
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

        beforeEach(async(() => {
            DOTTestBed.configureTestingModule(
                getTestConfig(TestHost2Component)
            );
        }));

        beforeEach(() => {
            hostFixture = DOTTestBed.createComponent(TestHost2Component);
            hostDe = hostFixture.debugElement;
            hostComponent = hostFixture.componentInstance;
            de = hostDe.query(By.css('dot-iframe-dialog'));
            component = de.componentInstance;
            hostComponent.url = 'hello/world';
            hostFixture.detectChanges();
            dialog = de.query(By.css('dot-dialog'));
            dialogComponent = dialog.componentInstance;
            spyOn(component.beforeClose, 'emit');
        });

        it('should emit beforeClose when a observer is set', () => {
            dialogComponent.beforeClose.emit({
                close: () => {}
            });
            expect(component.beforeClose.emit).toHaveBeenCalledTimes(1);
        });

        it('should NOT emit beforeClose when dialog is hidden', () => {
            hostComponent.url = null;
            hostFixture.detectChanges();

            dialogComponent.beforeClose.emit({
                close: () => {}
            });
            expect(component.beforeClose.emit).not.toHaveBeenCalled();
        });
    });
});
