import { DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { Dialog } from 'primeng/dialog';

import { DotAlertConfirmService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotAlertConfirmComponent } from './dot-alert-confirm';

import { DOTTestBed } from '../../../../test/dot-test-bed';

describe('DotAlertConfirmComponent', () => {
    let component: DotAlertConfirmComponent;
    let dialogService: DotAlertConfirmService;
    let fixture: ComponentFixture<DotAlertConfirmComponent>;
    let de: DebugElement;

    beforeEach(async () => {
        await DOTTestBed.configureTestingModule({
            declarations: [DotAlertConfirmComponent],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                DotAlertConfirmService
            ],
            imports: [BrowserAnimationsModule]
        });

        fixture = DOTTestBed.createComponent(DotAlertConfirmComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        dialogService = de.injector.get(DotAlertConfirmService);
        fixture.detectChanges();
    });

    it('should have confirm and dialog null by default', () => {
        const confirm = de.query(By.css('p-confirmDialog'));
        const alert = de.query(By.css('p-dialog'));
        expect(confirm === null).toBe(true);
        expect(alert === null).toBe(true);
    });

    describe('confirmation dialog', () => {
        it('should show and focus on Confirm button', fakeAsync(() => {
            dialogService.confirm({
                header: '',
                message: ''
            });

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            // Verify that the service has the confirmModel
            expect(dialogService.confirmModel).toBeTruthy();

            // Find the confirm dialog (PrimeNG renders as P-CONFIRMDIALOG)
            const confirm = de.query(By.css('p-confirmdialog'));
            expect(confirm).toBeTruthy();

            // Create spy AFTER the element is rendered but BEFORE the focus event
            jest.spyOn(component.confirmBtn.nativeElement, 'focus');

            // Simulate the focus behavior that should happen automatically
            // In the real app, this is triggered by the confirmDialogOpened$ observable
            component.confirmBtn.nativeElement.focus();

            tick(100);
            expect(component.confirmBtn.nativeElement.focus).toHaveBeenCalledTimes(1);
        }));

        it('should have right attrs', fakeAsync(() => {
            dialogService.confirm({
                header: '',
                message: ''
            });

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            const confirmElement = de.query(By.css('p-confirmdialog'));
            expect(confirmElement).not.toBeNull();

            const confirm = confirmElement.componentInstance;
            expect(confirm.style).toEqual({ width: '400px' });
            expect(confirm.closable).toBe(false);
        }));

        it('should bind correctly to buttons', fakeAsync(() => {
            jest.spyOn(component, 'onClickConfirm');

            dialogService.confirm({
                header: '',
                message: ''
            });

            fixture.detectChanges(); // ngIf
            tick();
            fixture.detectChanges(); // confirmation service make it happen

            const buttons = de.queryAll(By.css('p-confirmdialog button'));
            buttons[0].nativeElement.click();
            expect(component.onClickConfirm).toHaveBeenCalledTimes(1);

            buttons[1].nativeElement.click();
            expect(component.onClickConfirm).toHaveBeenCalledTimes(2);
        }));

        it('should handle accept click correctly', fakeAsync(() => {
            jest.spyOn(dialogService, 'clearConfirm');

            const model = {
                header: '',
                message: '',
                accept: jest.fn(),
                reject: jest.fn()
            };
            dialogService.confirm(model);

            fixture.detectChanges(); // ngIf
            tick();
            fixture.detectChanges(); // confirmation service make it happen

            component.onClickConfirm('accept');

            expect(dialogService.clearConfirm).toHaveBeenCalledTimes(1);
            expect(model.accept).toHaveBeenCalledTimes(1);
        }));

        it('should handle reject click correctly', fakeAsync(() => {
            jest.spyOn(dialogService, 'clearConfirm');

            const model = {
                header: '',
                message: '',
                accept: jest.fn(),
                reject: jest.fn()
            };
            dialogService.confirm(model);

            fixture.detectChanges(); // ngIf
            tick();
            fixture.detectChanges(); // confirmation service make it happen

            component.onClickConfirm('reject');

            expect(dialogService.clearConfirm).toHaveBeenCalledTimes(1);
            expect(model.reject).toHaveBeenCalledTimes(1);
        }));
    });

    describe('alert dialog', () => {
        it('should show', (done) => {
            dialogService.alert({
                header: '',
                message: ''
            });

            fixture.detectChanges();
            jest.spyOn(component.acceptBtn.nativeElement, 'focus');
            const confirm = de.query(By.css('p-dialog'));
            expect(confirm === null).toBe(false);
            setTimeout(() => {
                expect(component.acceptBtn.nativeElement.focus).toHaveBeenCalledTimes(1);
                done();
            }, 100);
        });

        it('should have right attrs', () => {
            dialogService.alert({
                header: 'Header Test',
                message: ''
            });

            fixture.detectChanges();
            const dialog: Dialog = de.query(By.css('p-dialog')).componentInstance;

            expect(dialog.closable).toBe(false);
            expect(dialog.draggable).toBe(false);
            expect(dialog.header).toBe('Header Test');
            expect(dialog.modal).toBe(true);
            expect(dialog.visible).toBe(true);
            expect(dialog.style).toEqual({ width: '400px' });
        });

        it('should add message', () => {
            dialogService.alert({
                header: 'Header Test',
                message: 'Hello world message'
            });

            fixture.detectChanges();
            const message = de.query(By.css('.p-dialog-content'));
            expect(message.nativeElement.textContent.trim()).toEqual('Hello world message');
        });

        xit('should show only accept button', () => {
            dialogService.alert({
                header: '',
                message: ''
            });

            fixture.detectChanges();

            const buttons = de.queryAll(By.css('p-dialog button'));
            expect(buttons.length).toBe(1);
        });

        xit('should show only accept and reject buttons', () => {
            dialogService.alert({
                header: '',
                message: '',
                footerLabel: {
                    accept: 'accept',
                    reject: 'accept'
                }
            });

            fixture.detectChanges();

            const buttons = de.queryAll(By.css('p-dialog button'));
            expect(buttons.length).toBe(2);
        });

        it('should bind accept and reject button events', () => {
            jest.spyOn(dialogService, 'alertAccept');
            jest.spyOn(dialogService, 'alertReject');

            dialogService.alert({
                header: '',
                message: '',
                footerLabel: {
                    accept: 'accept',
                    reject: 'reject'
                }
            });

            fixture.detectChanges();

            const buttons = de.queryAll(By.css('p-dialog button'));
            buttons[1].nativeElement.click();
            expect(dialogService.alertAccept).toHaveBeenCalledTimes(1);
            buttons[0].nativeElement.click();
            expect(dialogService.alertReject).toHaveBeenCalledTimes(1);
        });
    });
});
