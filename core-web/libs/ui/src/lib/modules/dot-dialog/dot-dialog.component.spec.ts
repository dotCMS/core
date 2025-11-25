import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DotDialogActions } from '@dotcms/dotcms-models';
import { DotDialogComponent } from '@dotcms/ui';

const dispatchKeydownEvent = (key: string, meta = false, alt = false) => {
    const event = new KeyboardEvent('keydown', {
        key: key,
        code: key,
        metaKey: meta,
        altKey: alt
    });

    document.dispatchEvent(event);
};

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-dialog
            [(visible)]="show"
            [actions]="actions"
            [cssClass]="cssClass"
            [headerStyle]="{ margin: '0' }"
            [contentStyle]="{ padding: '0' }"
            [header]="header"
            [closeable]="closeable"
            [appendToBody]="appendToBody"
            [hideButtons]="hideButtons"
            [bindEvents]="bindEvents"
            [isSaving]="isSaving"
            width="100px"
            height="100px">
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
    hideButtons = false;
    appendToBody = false;
    bindEvents = true;
    isSaving = false;
}

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-dialog (beforeClose)="beforeClose($event)" [(visible)]="show">
            <b>Dialog content</b>
        </dot-dialog>
    `
})
class TestHost2Component {
    show = false;

    beforeClose({ close }): void {
        close();
    }
}

describe('DotDialogComponent', () => {
    describe('regular close', () => {
        let component: DotDialogComponent;
        let de: DebugElement;
        let hostComponent: TestHostComponent;
        let hostDe: DebugElement;
        let hostFixture: ComponentFixture<TestHostComponent>;
        let accceptAction: jasmine.Spy;
        let cancelAction: jasmine.Spy;

        beforeEach(waitForAsync(() => {
            TestBed.configureTestingModule({
                imports: [ButtonModule, BrowserAnimationsModule],
                providers: [],
                declarations: [DotDialogComponent, TestHostComponent]
            }).compileComponents();
        }));

        beforeEach(() => {
            hostFixture = TestBed.createComponent(TestHostComponent);
            hostDe = hostFixture.debugElement;
            hostComponent = hostFixture.componentInstance;
            accceptAction = jasmine.createSpy('ok');
            cancelAction = jasmine.createSpy('cancel');
        });

        describe('default', () => {
            beforeEach(() => {
                hostFixture.detectChanges();
                de = hostDe.query(By.css('dot-dialog'));
                component = de.componentInstance;
            });

            afterEach(() => {
                component.close();
                hostFixture.detectChanges();
            });

            it('should not show dialog', () => {
                expect(de.nativeElement.classList.contains('active')).toBe(false);
            });

            it('should not show footer buttons', () => {
                const footer = de.query(By.css('.dialog__footer'));
                expect(footer === null).toBe(true);
            });

            it('should not show close button', () => {
                const close: DebugElement = de.query(By.css('[data-testId="close-button"]'));
                expect(close === null).toBe(true);
            });

            it('should set appendToBody to false', () => {
                expect(component.appendToBody).toBe(false);
            });
        });

        describe('show', () => {
            beforeEach(() => {
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

            afterEach(() => {
                component.close();
                hostFixture.detectChanges();
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
                expect(header.styles.cssText).toEqual('margin: 0px;');
            });

            it('should have close button', () => {
                const close: DebugElement = de.query(By.css('[data-testId="close-button"]'));
                const closeComponent = close.componentInstance;

                expect(closeComponent.icon).toBe('pi pi-times');
            });

            it('should show content', () => {
                const content: DebugElement = de.query(By.css('.dialog__content'));
                expect(content.nativeElement.innerHTML).toBe('<b>Dialog content</b>');
            });

            it('should set the content custom styles', () => {
                const content: DebugElement = de.query(By.css('.dialog__content'));
                expect(content.styles.cssText).toEqual('padding: 0px;');
            });

            it('should set width and height', () => {
                const dialog: DebugElement = de.query(By.css('.dialog'));
                expect(dialog.styles.cssText).toEqual('width: 100px; height: 100px;');
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

                const buttonsAttr = buttonsElements.map(
                    (button: HTMLButtonElement) => button.disabled
                );

                expect(buttonsComponents).toEqual(['Cancel', 'Accept']);
                expect(buttonsAttr).toEqual([false, true]);
            });

            describe('events', () => {
                beforeEach(() => {
                    spyOn(component.hide, 'emit').and.callThrough();
                    spyOn(component.visibleChange, 'emit').and.callThrough();
                });

                it('should close dialog and emit close', () => {
                    expect(component.visible).toBe(true);

                    const close: DebugElement = de.query(By.css('[data-testId="close-button"]'));

                    close.triggerEventHandler('click', {
                        preventDefault: () => {
                            //
                        }
                    });

                    hostFixture.detectChanges();
                    expect(component.visibleChange.emit).toHaveBeenCalledTimes(1);
                    expect(component.visible).toBe(false);
                    expect(component.hide.emit).toHaveBeenCalledTimes(1);
                });

                it('should not close the dialog on overlay click', () => {
                    hostFixture.whenStable();
                    de.nativeElement.click();
                    hostFixture.detectChanges();

                    expect(component.visible).toBe(true);
                });

                it('it should set shadow to header and footer on content scroll', () => {
                    const content: DebugElement = de.query(By.css('.dialog__content'));
                    content.triggerEventHandler('scroll', {
                        target: {
                            click: () => {
                                //
                            },
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
                        expect(component.visible).toBe(true);

                        dispatchKeydownEvent('Escape');

                        hostFixture.detectChanges();

                        expect(cancelAction).toHaveBeenCalledTimes(1);
                        expect(component.hide.emit).not.toHaveBeenCalled();
                    });

                    it('should trigger cancel action and close the dialog on Escape', () => {
                        hostComponent.actions = {
                            ...hostComponent.actions,
                            cancel: {
                                ...hostComponent.actions.cancel,
                                action: null
                            }
                        };
                        hostFixture.detectChanges();

                        expect(component.visible).toBe(true);

                        dispatchKeydownEvent('Escape');
                        hostFixture.detectChanges();

                        expect(component.visible).toBe(false);
                        expect(component.hide.emit).toHaveBeenCalledTimes(1);
                    });

                    it('should trigger accept action on CMD + Enter and unbind events', () => {
                        hostComponent.actions = {
                            ...hostComponent.actions,
                            accept: {
                                ...hostComponent.actions.accept,
                                disabled: false
                            }
                        };

                        hostFixture.detectChanges();

                        expect(component.visible).toBe(true);

                        dispatchKeydownEvent('Enter', true);

                        hostFixture.detectChanges();

                        expect(accceptAction).toHaveBeenCalledTimes(1);
                    });

                    it('should trigger accept action on ALT + Enter and unbind events', () => {
                        hostComponent.actions = {
                            ...hostComponent.actions,
                            accept: {
                                ...hostComponent.actions.accept,
                                disabled: false
                            }
                        };

                        hostFixture.detectChanges();

                        expect(component.visible).toBe(true);

                        dispatchKeydownEvent('Enter', false, true);

                        hostFixture.detectChanges();

                        expect(accceptAction).toHaveBeenCalledTimes(1);
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

                    it('should show loading indicator when is saving', () => {
                        hostComponent.isSaving = true;
                        hostFixture.detectChanges();

                        expect(component.isSaving).toEqual(true);
                    });

                    it("shouldn't show loading indicator when is not saving", () => {
                        hostComponent.isSaving = false;
                        hostFixture.detectChanges();
                        expect(component.isSaving).toEqual(false);
                    });

                    it('should call cancel action when is set', () => {
                        expect(component.visible).toBe(true);

                        const cancel: DebugElement = de.query(By.css('.dialog__button-cancel'));
                        cancel.triggerEventHandler('click', {});

                        hostFixture.detectChanges();

                        expect(cancelAction).toHaveBeenCalledTimes(1);
                        expect(component.hide.emit).not.toHaveBeenCalled();
                    });

                    it('should close the dialog', () => {
                        hostComponent.actions = {
                            ...hostComponent.actions,
                            cancel: {
                                ...hostComponent.actions.cancel,
                                action: null
                            }
                        };
                        hostFixture.detectChanges();
                        expect(component.visible).toBe(true);

                        const cancel: DebugElement = de.query(By.css('.dialog__button-cancel'));
                        cancel.triggerEventHandler('click', {});

                        hostFixture.detectChanges();

                        expect(component.visible).toBe(false);
                        expect(component.hide.emit).toHaveBeenCalledTimes(1);
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

        describe('show with no bindEvents', () => {
            beforeEach(() => {
                hostComponent.bindEvents = false;
                hostComponent.actions = {
                    accept: {
                        label: 'Accept',
                        action: accceptAction
                    },
                    cancel: {
                        label: 'Cancel',
                        action: cancelAction
                    }
                };
                hostComponent.show = true;
                hostFixture.detectChanges();
                de = hostDe.query(By.css('dot-dialog'));
                component = de.componentInstance;
            });

            afterEach(() => {
                component.close();
                hostFixture.detectChanges();
            });

            it('should not bind keyboard events', () => {
                dispatchKeydownEvent('Escape');
                dispatchKeydownEvent('Enter');
                hostFixture.detectChanges();
                expect(accceptAction).not.toHaveBeenCalled();
                expect(cancelAction).not.toHaveBeenCalled();
            });
        });
    });

    describe('with before close', () => {
        let component: DotDialogComponent;
        let de: DebugElement;
        let hostComponent: TestHost2Component;
        let hostDe: DebugElement;
        let hostFixture: ComponentFixture<TestHost2Component>;

        beforeEach(waitForAsync(() => {
            TestBed.configureTestingModule({
                imports: [ButtonModule, BrowserAnimationsModule],
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
            expect(component.visible).toBe(true);

            const close: DebugElement = de.query(By.css('[data-testId="close-button"]'));

            close.triggerEventHandler('click', {
                preventDefault: () => {
                    //
                }
            });

            hostFixture.detectChanges();
            expect(component.beforeClose.emit).toHaveBeenCalledTimes(1);
        });
    });
});
