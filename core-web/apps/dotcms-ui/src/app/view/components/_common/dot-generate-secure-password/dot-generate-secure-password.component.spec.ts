/* eslint-disable @typescript-eslint/no-explicit-any */

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DotGenerateSecurePasswordService, DotMessageService } from '@dotcms/data-access';
import {
    DotClipboardUtil,
    DotDialogComponent,
    DotDialogModule,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotGenerateSecurePasswordComponent } from './dot-generate-secure-password.component';

@Component({
    selector: 'dot-test-host-component',
    template: '<dot-generate-secure-password></dot-generate-secure-password>',
    standalone: false
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
            imports: [
                BrowserAnimationsModule,
                ButtonModule,
                DotDialogModule,
                DotSafeHtmlPipe,
                DotMessagePipe
            ],
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
            jest.spyOn(dotClipboardUtil, 'copy');
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

        it('should copy password to clipboard', fakeAsync(() => {
            const copyButton = fixture.debugElement.query(By.css('[data-testId="copyBtn"]'));
            copyButton.nativeElement.click();
            fixture.detectChanges();
            expect(dotClipboardUtil.copy).toHaveBeenCalledWith(comp.value);
            expect(copyButton.nativeElement.textContent).toBe('Copied');
            tick(2000);
            fixture.detectChanges();
            expect(copyButton.nativeElement.textContent).toBe('Copy');
        }));

        it('should Reveal password', () => {
            const revealButton = fixture.debugElement.query(
                By.css('.dot-generate-secure-password__reveal-link')
            );
            revealButton.nativeElement.click();
            expect(revealButton.nativeElement.text).toContain('Reveal');
            fixture.detectChanges();
            expect(comp.typeInput).toBe('text');
            expect(revealButton.nativeElement.text).toContain('hide');
        });

        it('should reset on close', () => {
            const revealButton = fixture.debugElement.query(
                By.css('.dot-generate-secure-password__reveal-link')
            );
            dialog.close();
            fixture.detectChanges();
            expect(comp.typeInput).toBe('password');
            expect(comp.value).toBe('');
            expect(comp.dialogShow).toBe(false);
            expect(revealButton.nativeElement.text.trim()).toBe('Reveal');
        });
    });

    afterEach(() => {
        comp.dialogShow = false;
        fixture.detectChanges();
    });
});
