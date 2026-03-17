/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';

import { DotGenerateSecurePasswordService, DotMessageService } from '@dotcms/data-access';
import { DotClipboardUtil, DotMessagePipe } from '@dotcms/ui';
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
        imports: [NoopAnimationsModule, ButtonModule, DialogModule, DotMessagePipe],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            DotGenerateSecurePasswordService
        ],
        componentProviders: [{ provide: DotClipboardUtil, useValue: clipboardUtilMock }]
    });

    beforeEach(() => {
        spectator = createComponent();
        dotGenerateSecurePasswordService = spectator.inject(DotGenerateSecurePasswordService);
        // DotClipboardUtil está en componentProviders, así que obtenemos la referencia del mock
        dotClipboardUtil = clipboardUtilMock as any;
        jest.clearAllMocks();
    });

    describe('dot-dialog', () => {
        beforeEach(() => {
            dotGenerateSecurePasswordService.open(passwordGenerateData);
            spectator.fixture.detectChanges(false);
        });

        it('should set dialog params', () => {
            const dialogEl = spectator.query('p-dialog');
            expect(dialogEl).toBeTruthy();
            expect(spectator.component.dialogShow).toBe(true);
            expect(spectator.component.value).toEqual(passwordGenerateData.password);
            expect(spectator.component.typeInput).toBe('password');
        });

        it('should copy password to clipboard', fakeAsync(() => {
            spectator.component.copyToClipboard();
            expect(dotClipboardUtil.copy).toHaveBeenCalledWith(spectator.component.value);
            expect(dotClipboardUtil.copy).toHaveBeenCalledTimes(1);
            expect(spectator.component.copyBtnLabel).toBe('Copied');

            tick(2000);
            spectator.fixture.detectChanges(false);
            expect(spectator.component.copyBtnLabel).toBe('Copy');
        }));

        it('should Reveal password', () => {
            expect(spectator.component.revealBtnLabel).toContain('Reveal');
            const mockEvent = {
                stopPropagation: jest.fn(),
                preventDefault: jest.fn()
            } as unknown as MouseEvent;
            spectator.component.revealPassword(mockEvent);
            spectator.fixture.detectChanges(false);

            expect(spectator.component.typeInput).toBe('text');
            expect(spectator.component.revealBtnLabel).toContain('hide');
        });

        it('should reset on close', () => {
            spectator.component.close();
            spectator.fixture.detectChanges(false);

            expect(spectator.component.typeInput).toBe('password');
            expect(spectator.component.value).toBe('');
            expect(spectator.component.dialogShow).toBe(false);
            expect(spectator.component.revealBtnLabel).toContain('Reveal');
        });
    });

    afterEach(() => {
        if (spectator) {
            spectator.component.dialogShow = false;
            spectator.fixture.detectChanges(false);
        }
    });
});
