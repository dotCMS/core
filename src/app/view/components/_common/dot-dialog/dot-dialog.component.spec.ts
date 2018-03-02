import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotDialogService } from '../../../../api/services/dot-dialog/dot-dialog.service';
import { DebugElement } from '@angular/core/src/debug/debug_node';
import { LoginServiceMock } from '../../../../test/login-service.mock';
import { LoginService } from 'dotcms-js/dotcms-js';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { DotDialogComponent } from './dot-dialog.component';
import { async, ComponentFixture, fakeAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { tick } from '@angular/core/testing';

describe('DotDialogComponent', () => {
    let component: DotDialogComponent;
    let dialogService: DotDialogService;
    let fixture: ComponentFixture<DotDialogComponent>;
    let de: DebugElement;

    beforeEach(
        async(() => {
            DOTTestBed.configureTestingModule({
                declarations: [DotDialogComponent],
                providers: [
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    },
                    DotDialogService
                ],
                imports: [BrowserAnimationsModule]
            });

            fixture = DOTTestBed.createComponent(DotDialogComponent);
            component = fixture.componentInstance;
            de = fixture.debugElement;
            dialogService = de.injector.get(DotDialogService);
            fixture.detectChanges();
        })
    );

    it('should have confirm and dialog null by default', () => {
        const confirm = de.query(By.css('p-confirmDialog'));
        const alert = de.query(By.css('p-dialog'));
        expect(confirm === null).toBe(true);
        expect(alert === null).toBe(true);
    });

    it('should show confirmation dialog', () => {
        dialogService.confirm({
            header: '',
            message: ''
        });

        fixture.detectChanges();
        const confirm = de.query(By.css('p-confirmDialog'));
        expect(confirm === null).toBe(false);
    });

    it('should have right attr in confirmation dialog', () => {
        dialogService.confirm({
            header: '',
            message: ''
        });

        fixture.detectChanges();
        const confirm = de.query(By.css('p-confirmDialog')).componentInstance;
        expect(confirm.responsive).toBe(true, 'responsive');
        expect(confirm.width).toBe('400', 'width');
        expect(confirm.closable).toBe(false, 'closable');
    });

    it('should show dialog', () => {
        dialogService.alert({
            header: '',
            message: ''
        });

        fixture.detectChanges();
        const confirm = de.query(By.css('p-dialog'));
        expect(confirm === null).toBe(false);
    });

    it('should have right attr in dialog', () => {
        dialogService.alert({
            header: 'Header Test',
            message: ''
        });

        fixture.detectChanges();
        const dialog = de.query(By.css('p-dialog')).componentInstance;

        expect(dialog.closable).toBe(false, 'closable');
        expect(dialog.header).toBe('Header Test', 'header');
        expect(dialog.modal).toBe('modal', 'modal');
        expect(dialog.responsive).toBe(true, 'responsive');
        expect(dialog.visible).toBe(true, 'visible');
        expect(dialog.width).toBe('400', 'width');
    });

    it('should add message to the dialog', () => {
        dialogService.alert({
            header: 'Header Test',
            message: 'Hello world message'
        });

        fixture.detectChanges();
        const message = de.query(By.css('.ui-dialog-content p'));
        expect(message.nativeElement.textContent).toEqual('Hello world message');
    });

    it('should bind ok button in dialog', () => {
        spyOn(dialogService, 'clearAlert');

        dialogService.alert({
            header: '',
            message: '',
        });

        fixture.detectChanges();

        const button = de.query(By.css('p-dialog button'));
        button.nativeElement.click();
        expect(dialogService.clearAlert).toHaveBeenCalledTimes(1);
    });

    it('should bind correctly to confirm dialog buttons', fakeAsync(() => {
        spyOn(component, 'onClickConfirm');

        dialogService.confirm({
            header: '',
            message: '',
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

    it('should handle confirm dialog accept click correctly', fakeAsync(() => {
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

    it('should handle confirm dialog reject click correctly', fakeAsync(() => {
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
