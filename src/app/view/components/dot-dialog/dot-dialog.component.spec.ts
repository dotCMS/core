import { DebugElement, Component } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { By } from '@angular/platform-browser';
import { DotDialogComponent, DotDialogActions } from './dot-dialog.component';
import { DotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';
import { DotIconButtonComponent } from '@components/_common/dot-icon-button/dot-icon-button.component';
import { ButtonModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

const dispatchKeydownEvent = (key: string) => {
    const event = new KeyboardEvent('keydown', {
        'key': key,
        'code': key
    });

    document.dispatchEvent(event);
};

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-dialog
            width="100px"
            height="100px"
            [actions]="actions"
            [cssClass]="cssClass"
            [headerStyle]="{'margin': '0'}"
            [contentStyle]="{'padding': '0'}"
            [header]="header"
            [(visible)]="show"
            [closeable]="closeable"
            [appendToBody]="appendToBody"
            [hideButtons]="hideButtons">
            <b>Dialog content</b>
        </dot-dialog>
    `
})
class TestHostComponent {
    header: string;
    cssClass: string;
    show = false;
    closeable = false;
    actions: DotDialogActions;
    hideButtons =  false;
    appendToBody = false;
}

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-dialog
            (beforeClose)="beforeClose()"
            [(visible)]="show">
            <b>Dialog content</b>
        </dot-dialog>
    `
})
class TestHost2Component {
    show = false;
    beforeClose(): void {}
}

describe('DotDialogComponent', () => {
    describe('regular close', () => {
        let component: DotDialogComponent;
        let de: DebugElement;
        let hostComponent: TestHostComponent;
        let hostDe: DebugElement;
        let hostFixture: ComponentFixture<TestHostComponent>;

        beforeEach(async(() => {
            TestBed.configureTestingModule({
                imports: [DotIconButtonModule, ButtonModule, BrowserAnimationsModule],
                providers: [],
                declarations: [DotDialogComponent, TestHostComponent]
            }).compileComponents();
        }));

        beforeEach(() => {
            hostFixture = TestBed.createComponent(TestHostComponent);
            hostDe = hostFixture.debugElement;
            hostComponent = hostFixture.componentInstance;
        });

        describe('default', () => {
            beforeEach(() => {
                hostFixture.detectChanges();
                de = hostDe.query(By.css('dot-dialog'));
                component = de.componentInstance;
            });

            it('should not show dialog', () => {
                expect(de.nativeElement.classList.contains('active')).toBe(false);
            });

            it('should not show footer buttons', () => {
                const footer = de.query(By.css('.dialog__footer'));
                expect(footer === null).toBe(true);
            });

            it('should not show close button', () => {
                const close: DebugElement = de.query(By.css('dot-icon-button'));
                expect(close === null).toBe(true);
            });

            it('should set appendToBody to false', () => {
                expect(component.appendToBody).toBe(false);
            });
        });

        describe('show', () => {
            let accceptAction: jasmine.Spy;
            let cancelAction: jasmine.Spy;

            beforeEach(() => {
                accceptAction = jasmine.createSpy('ok');
                cancelAction = jasmine.createSpy('cancel');

                hostComponent.closeable = true;
                hostComponent.header = 'Hello World';
                hostComponent.actions = {
                    accept: {
                        label: 'Accept',
                        disabled: true,
                        action: accceptAction
                    },
                    cancel: {
                        label: 'Cancel',
                        disabled: false,
                        action: cancelAction
                    }
                };
                hostComponent.show = true;

                hostFixture.detectChanges();
                de = hostDe.query(By.css('dot-dialog'));
                component = de.componentInstance;
            });

            it('should show dialog', () => {
                expect(de.nativeElement.classList.contains('active')).toBe(true);
            });

            it('should set the header', () => {
                const header: DebugElement = de.query(By.css('.dialog__header h4'));
                expect(header.nativeElement.textContent).toBe('Hello World');
            });

            it('should set the header custom styles', () => {
                const header: DebugElement = de.query(By.css('.dialog__header'));
                expect(header.styles).toEqual({margin: '0'});
            });

            it('should have close button', () => {
                const close: DebugElement = de.query(By.css('dot-icon-button'));
                const closeComponent: DotIconButtonComponent = close.componentInstance;

                expect(closeComponent.icon).toBe('close');
                expect(close.attributes.big).toBeDefined();
            });

            it('should show content', () => {
                const content: DebugElement = de.query(By.css('.dialog__content'));
                expect(content.nativeElement.innerHTML).toBe('<b>Dialog content</b>');
            });

            it('should set the content custom styles', () => {
                const content: DebugElement = de.query(By.css('.dialog__content'));
                expect(content.styles).toEqual({padding: '0'});
            });

            it('should set width and height', () => {
                const dialog: DebugElement = de.query(By.css('.dialog'));
                expect(dialog.styles).toEqual({height: '100px', width: '100px'});
            });

            it('should show footer', () => {
                const footer: DebugElement = de.query(By.css('.dialog__footer'));
                expect(footer === null).toBe(false);
            });

            it('should append component to body', () => {
                hostComponent.show = false;
                hostComponent.appendToBody = true;
                hostFixture.detectChanges();
                hostComponent.show = true;
                hostFixture.detectChanges();

                expect(de.nativeElement.parentNode).toBe(document.body);
            });

            it('should add CSS class', () => {
                hostComponent.cssClass = 'paginator';
                hostFixture.detectChanges();
                const dialog: DebugElement = de.query(By.css('.dialog'));
                expect(dialog.nativeElement.classList.contains('paginator')).toBe(true);
            });

            it('should show action buttons', () => {
                const buttons: DebugElement[] = de.queryAll(By.css('.dialog__footer button'));

                const buttonsElements = buttons.map((button: DebugElement) => button.nativeElement);

                const buttonsComponents = buttonsElements.map(
                    (button: HTMLButtonElement) => button.textContent
                );

                const buttonsAttr = buttonsElements.map((button: HTMLButtonElement) => button.disabled);

                expect(buttonsComponents).toEqual(['Cancel', 'Accept']);
                expect(buttonsAttr).toEqual([false, true]);
            });

            describe('events', () => {
                beforeEach(() => {
                    spyOn(component.hide, 'emit').and.callThrough();
                    spyOn(component.visibleChange, 'emit').and.callThrough();
                });

                it('should close dialog and emit close', () => {
                    hostFixture.whenStable().then(() => {
                        expect(component.visible).toBe(true);

                        const close: DebugElement = de.query(By.css('dot-icon-button'));

                        close.triggerEventHandler('click', {
                            preventDefault: () => {}
                        });

                        hostFixture.detectChanges();
                        expect(component.visibleChange.emit).toHaveBeenCalledTimes(1);
                        expect(component.visible).toBe(false);
                        expect(component.hide.emit).toHaveBeenCalledTimes(1);
                    });
                });

                it('should not close the dialog on overlay click', () => {
                    hostFixture.whenStable().then(() => {
                        de.nativeElement.click();
                        hostFixture.detectChanges();

                        expect(component.visible).toBe(true);
                    });
                });

                it('it should set shadow to header and footer on content scroll', () => {
                    const content: DebugElement = de.query(By.css('.dialog__content'));
                    content.triggerEventHandler('scroll', {
                        target: {
                            click: () => {},
                            scrollTop: 100
                        }
                    });
                    hostFixture.detectChanges();

                    const header: DebugElement = de.query(By.css('.dialog__header'));
                    const footer: DebugElement = de.query(By.css('.dialog__footer'));
                    expect(header.classes['dialog__header--shadowed']).toBe(true);
                    expect(footer.classes['dialog__footer--shadowed']).toBe(true);
                });

                it('it should click target on content scroll', () => {
                    const clickSpy = jasmine.createSpy('clickSpy');
                    const content: DebugElement = de.query(By.css('.dialog__content'));
                    content.triggerEventHandler('scroll', {
                        target: {
                            click: clickSpy,
                            scrollTop: 100
                        }
                    });
                    hostFixture.detectChanges();
                    expect(clickSpy).toHaveBeenCalledTimes(1);
                });


                describe('keyboard events', () => {

                    it('should trigger cancel action and close the dialog on Escape', () => {
                        hostFixture.whenStable().then(() => {
                            expect(component.visible).toBe(true);

                            dispatchKeydownEvent('Escape');

                            hostFixture.detectChanges();

                            expect(cancelAction).toHaveBeenCalledTimes(1);
                            expect(component.visible).toBe(false);
                            expect(component.hide.emit).toHaveBeenCalledTimes(1);
                        });
                    });

                    it('should trigger accept action on Enter and unbind events', () => {
                        hostComponent.actions = {
                            ...hostComponent.actions,
                            accept: {
                                ...hostComponent.actions.accept,
                                disabled: false
                            }
                        };

                        hostFixture.detectChanges();

                        hostFixture.whenStable().then(() => {
                            expect(component.visible).toBe(true);

                            dispatchKeydownEvent('Enter');

                            hostFixture.detectChanges();

                            dispatchKeydownEvent('Enter');

                            hostFixture.detectChanges();

                            expect(accceptAction).toHaveBeenCalledTimes(1);
                        });
                    });
                });

                describe('actions', () => {
                    it('should call accept action', () => {
                        hostComponent.actions = {
                            ...hostComponent.actions,
                            accept: {
                                ...hostComponent.actions.accept,
                                disabled: false
                            }
                        };
                        hostFixture.detectChanges();

                        const accept: DebugElement = de.query(By.css('.dialog__button-accept'));
                        accept.triggerEventHandler('click', {});

                        expect(accceptAction).toHaveBeenCalledTimes(1);
                    });

                    it('should call cancel action and close the dialog', () => {
                        hostFixture.whenStable().then(() => {
                            expect(component.visible).toBe(true);

                            const cancel: DebugElement = de.query(By.css('.dialog__button-cancel'));
                            cancel.triggerEventHandler('click', {});

                            hostFixture.detectChanges();

                            expect(cancelAction).toHaveBeenCalledTimes(1);
                            expect(component.visible).toBe(false);
                            expect(component.hide.emit).toHaveBeenCalledTimes(1);
                        });
                    });
                });
            });

            it('should not show buttons when hideButtons is true', () => {
                hostComponent.hideButtons = true;
                hostFixture.detectChanges();

                expect(hostDe.query(By.css('.dialog__button-cancel')).styles.display).toBe('none');
                expect(hostDe.query(By.css('.dialog__button-accept')).styles.display).toBe('none');
            });
        });
    });

    describe('with before close', () => {
        let component: DotDialogComponent;
        let de: DebugElement;
        let hostComponent: TestHost2Component;
        let hostDe: DebugElement;
        let hostFixture: ComponentFixture<TestHost2Component>;

        beforeEach(async(() => {
            TestBed.configureTestingModule({
                imports: [DotIconButtonModule, ButtonModule, BrowserAnimationsModule],
                providers: [],
                declarations: [DotDialogComponent, TestHost2Component]
            }).compileComponents();
        }));

        beforeEach(() => {
            hostFixture = TestBed.createComponent(TestHost2Component);
            hostDe = hostFixture.debugElement;
            hostComponent = hostFixture.componentInstance;
            hostComponent.show = true;

            hostFixture.detectChanges();
            de = hostDe.query(By.css('dot-dialog'));
            component = de.componentInstance;

            spyOn(component.visibleChange, 'emit').and.callThrough();
            spyOn(component.beforeClose, 'emit').and.callThrough();
        });

        it('should emit beforeClose', () => {
            hostFixture.whenStable().then(() => {
                expect(component.visible).toBe(true);

                const close: DebugElement = de.query(By.css('dot-icon-button'));

                close.triggerEventHandler('click', {
                    preventDefault: () => {}
                });

                hostFixture.detectChanges();
                expect(component.beforeClose.emit).toHaveBeenCalledTimes(1);
                expect(component.visibleChange.emit).not.toHaveBeenCalled();
                expect(component.visible).toBe(true);
            });
        });
    });

});

