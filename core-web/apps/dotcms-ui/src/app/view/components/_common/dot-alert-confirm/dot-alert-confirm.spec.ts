import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DebugElement } from '@angular/core';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { LoginService } from '@dotcms/dotcms-js';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotAlertConfirmComponent } from './dot-alert-confirm';
import { ComponentFixture, fakeAsync, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { tick } from '@angular/core/testing';

describe('DotAlertConfirmComponent', () => {
    let component: DotAlertConfirmComponent;
    let dialogService: DotAlertConfirmService;
    let fixture: ComponentFixture<DotAlertConfirmComponent>;
    let de: DebugElement;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
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
    }));

    it('should have confirm and dialog null by default', () => {
        const confirm = de.query(By.css('p-confirmDialog'));
        const alert = de.query(By.css('p-dialog'));
        expect(confirm === null).toBe(true);
        expect(alert === null).toBe(true);
    });

    describe('confirmation dialog', () => {
        it('should show and focus on Confirm button', (done) => {
            dialogService.confirm({
                header: '',
                message: ''
            });

            fixture.detectChanges();
            spyOn(component.confirmBtn.nativeElement, 'focus');
            const confirm = de.query(By.css('p-confirmDialog'));
            expect(confirm === null).toBe(false);
            setTimeout(() => {
                expect(component.confirmBtn.nativeElement.focus).toHaveBeenCalledTimes(1);
                done();
            }, 100);
        });

        it('should have right attrs', () => {
            dialogService.confirm({
                header: '',
                message: ''
            });

            fixture.detectChanges();
            const confirm = de.query(By.css('p-confirmDialog')).componentInstance;
            expect(confirm.style).toEqual({ width: '400px' }, 'width');
            expect(confirm.closable).toBe(false, 'closable');
        });

        it('should bind correctly to buttons', fakeAsync(() => {
            spyOn(component, 'onClickConfirm');

            dialogService.confirm({
                header: '',
                message: ''
            });

            fixture.detectChanges(); // ngIf
            tick();
            fixture.detectChanges(); // confirmation service make it happen

            const buttons = de.queryAll(By.css('p-confirmDialog button'));
            buttons[0].nativeElement.click();
            expect(component.onClickConfirm).toHaveBeenCalledTimes(1);

            buttons[1].nativeElement.click();
            expect(component.onClickConfirm).toHaveBeenCalledTimes(2);
        }));

        it('should handle accept click correctly', fakeAsync(() => {
            spyOn(dialogService, 'clearConfirm');

            const model = {
                header: '',
                message: '',
                accept: jasmine.createSpy('accept'),
                reject: jasmine.createSpy('reject')
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
            spyOn(dialogService, 'clearConfirm');

            const model = {
                header: '',
                message: '',
                accept: jasmine.createSpy('accept'),
                reject: jasmine.createSpy('reject')
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
            spyOn(component.acceptBtn.nativeElement, 'focus');
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
            const dialog = de.query(By.css('p-dialog')).componentInstance;

            expect(dialog.closable).toBe(false, 'closable');
            expect(dialog.draggable).toBe(false, 'draggable');
            expect(dialog.header).toBe('Header Test', 'header');
            expect(dialog.modal).toBe('modal', 'modal');
            expect(dialog.visible).toBe(true, 'visible');
            expect(dialog.style).toEqual({ width: '400px' }, 'width');
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
            spyOn(dialogService, 'alertAccept');
            spyOn(dialogService, 'alertReject');

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
