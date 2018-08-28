import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DebugElement, Component } from '@angular/core';
import { async, ComponentFixture, tick, fakeAsync } from '@angular/core/testing';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DialogModule, Dialog } from 'primeng/primeng';
import { By } from '@angular/platform-browser';
import { DotDialogComponent, DotDialogAction } from './dot-dialog.component';
import { DotIconButtonModule } from '../_common/dot-icon-button/dot-icon-button.module';

@Component({
    selector: 'dot-test-host-component',
    template: `<dot-dialog [header]="header" [show]="show" [ok]="ok" [cancel]="cancel">
                    <b>Dialog content</b>
                </dot-dialog>`
})
class TestHostComponent {
    header: string;
    show: boolean;

    ok: DotDialogAction;
    cancel: DotDialogAction;
}

describe('DotDialogComponent', () => {
    let component: DotDialogComponent;
    let de: DebugElement;
    let dialog: DebugElement;
    let dialogComponent: Dialog;
    let hostComponent: TestHostComponent;
    let hostFixture: ComponentFixture<TestHostComponent>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [DialogModule, BrowserAnimationsModule, DotIconButtonModule],
            providers: [],
            declarations: [DotDialogComponent, TestHostComponent]
        }).compileComponents();
    }));

    beforeEach(() => {
        hostFixture = DOTTestBed.createComponent(TestHostComponent);
        const hostDe: DebugElement = hostFixture.debugElement;
        hostComponent = hostFixture.componentInstance;
        de = hostDe.query(By.css('dot-dialog'));
        component = de.componentInstance;

        dialog = de.query(By.css('p-dialog'));
        dialogComponent = dialog.componentInstance;
    });

    describe('dialog', () => {
        beforeEach(() => {
            hostComponent.header = 'This is a header';
            hostFixture.detectChanges();
        });

        it('should have', () => {
            expect(dialog).toBeTruthy();
        });

        it('should have the right attrs', () => {
            expect(dialogComponent.draggable).toEqual(false, 'draggable');
            expect(dialogComponent.dismissableMask).toEqual(true, 'dismissableMask');
            expect(dialogComponent.modal).toEqual(true, 'modal');
            expect(component.header).toBe('This is a header');
        });
    });

    describe('events', () => {
        it('header "x" button should trigger the close action', () => {
            hostFixture.detectChanges();

            spyOn(component.close, 'emit');
            const closeButton = dialog.query(By.css('p-header dot-icon-button'));
            closeButton.nativeElement.click();
            expect(component.close.emit).toHaveBeenCalledTimes(1);
        });

        it('should emit close', () => {
            hostComponent.show = true;
            hostFixture.detectChanges();

            spyOn(component.close, 'emit');

            dialog.triggerEventHandler('onHide', {});
            expect(component.close.emit).toHaveBeenCalledTimes(1);
            expect(component.show).toBe(false);

            hostFixture.detectChanges();
            expect(dialogComponent.visible).toBe(false);
        });
    });

    describe('show/hide', () => {
        beforeEach(() => {
            hostComponent.show = true;
            hostFixture.detectChanges();
        });

        it('should show', () => {
            expect(dialogComponent.visible).toBe(true);
        });

        it('should hide', () => {
            spyOn(component.close, 'emit');

            hostComponent.show = false;
            hostFixture.detectChanges();

            expect(dialogComponent.visible).toBe(false);
            expect(component.close.emit).toHaveBeenCalledTimes(1);
        });
    });

    describe('body content', () => {
        beforeEach(() => {
            hostFixture.detectChanges();
        });

        it('should show tag body into the dialog', () => {
            const content = dialog.query(By.css('b'));

            expect(content).not.toBeNull('must have content');
            expect(content.nativeElement.innerHTML).toBe('Dialog content');
        });
    });

    describe('actions', () => {
        it('should not have footer', () => {
            hostFixture.detectChanges();

            const footer = dialog.query(By.css('p-footer'));

            expect(footer).toBeNull('must have not footer');

            const buttons = dialog.queryAll(By.css('ui-dialog-footer button'));
            expect(buttons.length).toBe(0, 'must have not buttons');
        });

        describe('re-center', () => {
            it('should have ok button', fakeAsync(() => {
                spyOn(dialogComponent, 'center');
                component.reRecenter();
                tick();
                expect(dialogComponent.center).toHaveBeenCalled();
            }));
        });

        describe('ok button', () => {
            it('should have ok button', () => {
                hostComponent.ok = {
                    label: 'Ok',
                    disabled: true,
                    action: jasmine.createSpy('ok')
                };

                hostFixture.detectChanges();
                const footer = dialog.query(By.css('p-footer'));
                expect(footer).not.toBeNull('must have footer');

                const buttons = footer.queryAll(By.css('button'));
                expect(buttons.length).toBe(1, 'should have ok button');

                expect(buttons[0].nativeElement.className).toContain('dot-dialog__ok', 'should have the right class');
                expect(buttons[0].properties.disabled).toBe(true, 'should be disabled');
                // expect(buttons[0].componentInstance.label).toBe('Ok', 'should have the right label');
            });

            it('should trigger the right action', () => {
                hostComponent.ok = {
                    label: 'Ok',
                    action: jasmine.createSpy('ok')
                };

                hostFixture.detectChanges();

                const footer = dialog.query(By.css('p-footer'));
                const buttons = footer.queryAll(By.css('button'));

                buttons[0].triggerEventHandler('click', null);
                expect(hostComponent.ok.action).toHaveBeenCalled();
            });
        });

        describe('cancel button', () => {
            beforeEach(() => {
                hostComponent.cancel = {
                    label: 'Cancel',
                    action: jasmine.createSpy('cancel')
                };

                hostFixture.detectChanges();
            });

            it('shouls have cancel button', () => {
                const footer = dialog.query(By.css('p-footer'));
                expect(footer).not.toBeNull('must have footer');

                const buttons = footer.queryAll(By.css('button'));
                expect(buttons.length).toBe(1, 'should have cancel button');

                expect(buttons[0].nativeElement.className).toContain('dot-dialog__cancel', 'should have the right class');
                // expect(buttons[0].componentInstance.label).toBe('Cancel', 'should have the right label');
            });

            it('shouls trigger the right action', () => {
                const footer = dialog.query(By.css('p-footer'));
                const buttons = footer.queryAll(By.css('button'));
                spyOn(component, 'closeDialog');

                buttons[0].triggerEventHandler('click', null);
                expect(component.closeDialog).toHaveBeenCalled();
                expect(hostComponent.cancel.action).toHaveBeenCalled();
            });
        });

        describe('both buttons', () => {
            beforeEach(() => {
                hostComponent.cancel = {
                    label: 'Cancel',
                    action: jasmine.createSpy('cancel')
                };

                hostComponent.ok = {
                    label: 'Ok',
                    action: jasmine.createSpy('ok')
                };

                hostFixture.detectChanges();
            });

            it('shouls have both buttons', () => {
                const footer = dialog.query(By.css('p-footer'));
                expect(footer).not.toBeNull('must have footer');

                const buttons = footer.queryAll(By.css('button'));
                expect(buttons.length).toBe(2, 'should have both button');
            });
        });
    });
});
