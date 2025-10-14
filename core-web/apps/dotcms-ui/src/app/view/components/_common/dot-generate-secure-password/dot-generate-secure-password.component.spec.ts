/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';

import { DotGenerateSecurePasswordService, DotMessageService } from '@dotcms/data-access';
import {
    DotClipboardUtil,
    DotDialogComponent,
    DotMessagePipe,
    DotSafeHtmlPipe
} from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotGenerateSecurePasswordComponent } from './dot-generate-secure-password.component';

describe('DotGenerateSecurePasswordComponent', () => {
    let spectator: Spectator<DotGenerateSecurePasswordComponent>;
    let dotGenerateSecurePasswordService: DotGenerateSecurePasswordService;
    let dotClipboardUtil: DotClipboardUtil;

    const messageServiceMock = new MockDotMessageService({
        'generate.secure.password': 'Generate Secure Password',
        Copy: 'Copy',
        Copied: 'Copied',
        Close: 'Close',
        hide: 'hide',
        'generate.secure.password.reveal': 'Reveal',
        'generate.secure.password.description': 'Description'
    });

    const passwordGenerateData: { [key: string]: any } = {
        password: '123'
    };

    const clipboardUtilMock = {
        copy: jest.fn()
    };

    const createComponent = createComponentFactory({
        component: DotGenerateSecurePasswordComponent,
        imports: [BrowserAnimationsModule, ButtonModule, DotDialogComponent, DotSafeHtmlPipe, DotMessagePipe],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            DotGenerateSecurePasswordService
        ],
        componentProviders: [
            { provide: DotClipboardUtil, useValue: clipboardUtilMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotGenerateSecurePasswordService = spectator.inject(DotGenerateSecurePasswordService);
        // DotClipboardUtil está en componentProviders, así que obtenemos la referencia del mock
        dotClipboardUtil = clipboardUtilMock as any;
        jest.clearAllMocks();
    });

    describe('dot-dialog', () => {
        let dialog: DotDialogComponent;
        
        beforeEach(() => {
            dialog = spectator.query(DotDialogComponent);
            dotGenerateSecurePasswordService.open(passwordGenerateData);
            spectator.detectChanges();
        });

        it('should set dialog params', () => {
            expect(dialog.visible).toEqual(spectator.component.dialogShow);
            expect(dialog.width).toEqual('34.25rem');
            expect(spectator.component.value).toEqual(passwordGenerateData.password);
            expect(spectator.component.typeInput).toBe('password');
        });

        it('should copy password to clipboard', fakeAsync(() => {
            const copyButton = spectator.query('[data-testId="copyBtn"]') as HTMLButtonElement;
            spectator.click(copyButton);
            spectator.detectChanges();
            
            expect(dotClipboardUtil.copy).toHaveBeenCalledWith(spectator.component.value);
            expect(dotClipboardUtil.copy).toHaveBeenCalledTimes(1);
            expect(copyButton.textContent).toBe('Copied');
            
            tick(2000);
            spectator.detectChanges();
            expect(copyButton.textContent).toBe('Copy');
        }));

        it('should Reveal password', () => {
            const revealButton = spectator.query(
                '.dot-generate-secure-password__reveal-link'
            ) as HTMLAnchorElement;
            
            expect(revealButton.text).toContain('Reveal');
            spectator.click(revealButton);
            spectator.detectChanges();
            
            expect(spectator.component.typeInput).toBe('text');
            expect(revealButton.text).toContain('hide');
        });

        it('should reset on close', () => {
            const revealButton = spectator.query(
                '.dot-generate-secure-password__reveal-link'
            ) as HTMLAnchorElement;
            
            dialog.close();
            spectator.detectChanges();
            
            expect(spectator.component.typeInput).toBe('password');
            expect(spectator.component.value).toBe('');
            expect(spectator.component.dialogShow).toBe(false);
            expect(revealButton.text.trim()).toBe('Reveal');
        });
    });

    afterEach(() => {
        spectator.component.dialogShow = false;
        spectator.detectChanges();
    });
});
