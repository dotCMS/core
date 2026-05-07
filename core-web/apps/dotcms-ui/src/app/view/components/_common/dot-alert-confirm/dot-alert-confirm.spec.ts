import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { EMPTY } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ChangeDetectorRef, Injectable } from '@angular/core';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ConfirmationService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import { DotAlertConfirmService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '@dotcms/utils-testing';

import { DotAlertConfirmComponent } from './dot-alert-confirm';

/**
 * Service que no emite en confirmDialogOpened$ para evitar timing de focus en tests.
 */
@Injectable()
class DotAlertConfirmServiceTest extends DotAlertConfirmService {
    override get confirmDialogOpened$() {
        return EMPTY;
    }
}

describe('DotAlertConfirmComponent', () => {
    let spectator: Spectator<DotAlertConfirmComponent>;
    let dialogService: DotAlertConfirmServiceTest;

    const createComponent = createComponentFactory({
        component: DotAlertConfirmComponent,
        imports: [BrowserAnimationsModule],
        detectChanges: false,
        providers: [
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: DotAlertConfirmService, useClass: DotAlertConfirmServiceTest },
            ConfirmationService,
            provideHttpClient(),
            provideHttpClientTesting()
        ]
    });

    /**
     * Ejecuta change detection sin checkNoChanges para evitar NG0100 con PrimeNG.
     * Marca el componente para asegurar que se actualice al cambiar el servicio.
     */
    function detectChanges(): void {
        spectator.fixture.componentRef.injector.get(ChangeDetectorRef).markForCheck();
        spectator.fixture.detectChanges(false);
    }

    beforeEach(() => {
        spectator = createComponent();
        detectChanges();
        dialogService = spectator.inject(DotAlertConfirmService) as DotAlertConfirmServiceTest;
    });

    it('should always render p-confirmdialog and not render the standalone alert p-dialog by default', () => {
        // The confirm dialog is always mounted (no `@if` guard) so that
        // PrimeNG's ConfirmationService Subject has a live subscriber when
        // the very first call comes in — including from a route guard.
        // PrimeNG's `<p-confirmDialog>` internally renders a `<p-dialog>`,
        // so we scope the alert assertion to the standalone (top-level)
        // dialog that this template renders for `alert(...)`.
        expect(spectator.debugElement.query(By.css('p-confirmdialog'))).toBeTruthy();
        expect(spectator.query(':scope > p-dialog')).toBeNull();
    });

    describe('confirmation dialog', () => {
        it('should set the model when service.confirm() is called', () => {
            dialogService.confirm({ header: '', message: '' });
            detectChanges();

            expect(dialogService.confirmModel).toBeTruthy();
            expect(spectator.debugElement.query(By.css('p-confirmdialog'))).toBeTruthy();
        });

        it('should have expected attrs', () => {
            dialogService.confirm({ header: '', message: '' });
            detectChanges();

            const el = spectator.debugElement.query(By.css('p-confirmdialog'));
            expect(el?.componentInstance?.style).toEqual({ width: '400px' });
            expect(el?.componentInstance?.closable).toBe(false);
        });

        it('should call onClickConfirm for reject and accept', () => {
            const spy = jest.spyOn(spectator.component, 'onClickConfirm');
            dialogService.confirm({ header: '', message: '' });
            detectChanges();

            spectator.component.onClickConfirm('reject');
            spectator.component.onClickConfirm('accept');

            expect(spy).toHaveBeenCalledWith('reject');
            expect(spy).toHaveBeenCalledWith('accept');
        });

        it('should call model accept and clearConfirm on accept', () => {
            const model = { header: '', message: '', accept: jest.fn(), reject: jest.fn() };
            const clearSpy = jest.spyOn(dialogService, 'clearConfirm');
            dialogService.confirm(model);
            detectChanges();

            spectator.component.onClickConfirm('accept');

            expect(clearSpy).toHaveBeenCalled();
            expect(model.accept).toHaveBeenCalled();
        });

        it('should call model reject and clearConfirm on reject', () => {
            const model = { header: '', message: '', accept: jest.fn(), reject: jest.fn() };
            const clearSpy = jest.spyOn(dialogService, 'clearConfirm');
            dialogService.confirm(model);
            detectChanges();

            spectator.component.onClickConfirm('reject');

            expect(clearSpy).toHaveBeenCalled();
            expect(model.reject).toHaveBeenCalled();
        });
    });

    describe('alert dialog', () => {
        it('should show when service.alert() is called', () => {
            dialogService.alert({ header: '', message: '' });
            detectChanges();

            expect(spectator.debugElement.query(By.css('p-dialog'))).toBeTruthy();
        });

        it('should have expected attrs', () => {
            dialogService.alert({ header: 'Header Test', message: '' });
            detectChanges();

            // Scope to the standalone alert `<p-dialog>` — the confirm
            // dialog (always-mounted) also renders a nested `<p-dialog>`
            // internally that would otherwise match a bare `p-dialog`
            // selector and pull in PrimeNG defaults instead of our config.
            const dialog = spectator.debugElement
                .queryAll(By.css('p-dialog'))
                .find((d) => !d.nativeElement.closest('p-confirmdialog'))
                ?.componentInstance as Dialog;
            expect(dialog?.closable).toBe(false);
            expect(dialog?.draggable).toBe(false);
            expect(dialog?.header).toBe('Header Test');
            expect(dialog?.modal).toBe(true);
            expect(dialog?.visible).toBe(true);
            expect(dialog?.style).toEqual({ width: '400px' });
        });

        it('should show message', () => {
            dialogService.alert({ header: '', message: 'Hello world message' });
            detectChanges();

            const content = spectator.debugElement.query(By.css('.p-dialog-content'));
            expect(content?.nativeElement?.textContent?.trim()).toBe('Hello world message');
        });

        it('should show one button when no reject label', () => {
            dialogService.alert({ header: '', message: '' });
            detectChanges();

            const buttons = spectator.debugElement.queryAll(By.css('p-dialog button'));
            expect(buttons.length).toBe(1);
        });

        it('should show two buttons when footerLabel has accept and reject', () => {
            dialogService.alert({
                header: '',
                message: '',
                footerLabel: { accept: 'Accept', reject: 'Reject' }
            });
            detectChanges();

            const buttons = spectator.debugElement.queryAll(By.css('p-dialog button'));
            expect(buttons.length).toBe(2);
        });

        it('should call alertAccept and alertReject on button clicks', () => {
            const acceptSpy = jest.spyOn(dialogService, 'alertAccept');
            const rejectSpy = jest.spyOn(dialogService, 'alertReject');
            dialogService.alert({
                header: '',
                message: '',
                footerLabel: { accept: 'accept', reject: 'reject' }
            });
            detectChanges();

            const buttons = spectator.debugElement.queryAll(By.css('p-dialog button'));
            buttons[1].nativeElement.click();
            buttons[0].nativeElement.click();

            expect(acceptSpy).toHaveBeenCalled();
            expect(rejectSpy).toHaveBeenCalled();
        });
    });
});
