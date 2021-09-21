import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DebugElement, Component } from '@angular/core';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotDialogComponent } from '@components/dot-dialog/dot-dialog.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotGenerateSecurePasswordComponent } from './dot-generate-secure-password.component';
import { DotGenerateSecurePasswordService } from '@services/dot-generate-secure-password/dot-generate-secure-password.service';
import { DotClipboardUtil } from '@dotcms/app/api/util/clipboard/ClipboardUtil';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-generate-secure-password></dot-generate-secure-password>'
})
class TestHostComponent {}

describe('DotGenerateSecurePasswordComponent', () => {
    let comp: DotGenerateSecurePasswordComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;
    let dotGenerateSecurePasswordService: DotGenerateSecurePasswordService;
    let dotClipboardUtil: DotClipboardUtil;

    const messageServiceMock = new MockDotMessageService({
        'generate.secure.password': 'Generate Secure Password',
        Copy: 'Copy',
        'generate.secure.password.reveal': 'Reveal',
        'generate.secure.password.description': 'Description'
    });

    const passwordGenerateData: { [key: string]: any } = {
        password: '123'
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotGenerateSecurePasswordComponent, TestHostComponent],
            imports: [BrowserAnimationsModule, ButtonModule, DotDialogModule, DotPipesModule],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                DotGenerateSecurePasswordService,
                DotClipboardUtil
            ]
        });

        fixture = TestBed.createComponent(TestHostComponent);
        de = fixture.debugElement.query(By.css('dot-generate-secure-password'));
        comp = de.componentInstance;
        dotGenerateSecurePasswordService = TestBed.inject(DotGenerateSecurePasswordService);
        dotClipboardUtil = TestBed.inject(DotClipboardUtil);
        fixture.detectChanges();
    });

    describe('dot-dialog', () => {
        let dialog: DotDialogComponent;
        beforeEach(() => {
            spyOn(dotClipboardUtil, 'copy');
            dialog = fixture.debugElement.query(By.css('dot-dialog')).componentInstance;
            dotGenerateSecurePasswordService.open(passwordGenerateData);
            fixture.detectChanges();
        });

        it('should set dialog params', () => {
            expect(dialog.visible).toEqual(comp.dialogShow);
            expect(dialog.width).toEqual('34.25rem');
            expect(comp.value).toEqual(passwordGenerateData.password);
            expect(comp.typeInput).toBe('password');
        });

        it('should copy password to clipboard', () => {
            const copyButton = fixture.debugElement.query(By.css('[data-testId="copyBtn"]'));
            copyButton.triggerEventHandler('click', null);
            expect(dotClipboardUtil.copy).toHaveBeenCalledWith(comp.value);
        });

        it('should Reveal password', () => {
            const revealButton = fixture.debugElement.query(
                By.css('.dot-generate-secure-password__reveal-link')
            );
            revealButton.nativeElement.click();
            fixture.detectChanges();
            expect(comp.typeInput).toBe('text');
        });
    });

    afterEach(() => {
        comp.dialogShow = false;
        fixture.detectChanges();
    });
});
