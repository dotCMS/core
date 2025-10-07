import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';
import { MockComponent } from 'ng-mocks';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DotMessagePipe } from '@dotcms/ui';

import { DotBinaryFieldUiMessageComponent } from './dot-binary-field-ui-message.component';

import { DotBinaryFieldEditorComponent } from '../dot-binary-field-editor/dot-binary-field-editor.component';

describe('DotBinaryFieldUiMessageComponent', () => {
    let spectator: SpectatorHost<DotBinaryFieldUiMessageComponent>;

    const createHost = createHostFactory({
        component: DotBinaryFieldUiMessageComponent,
        imports: [
            CommonModule,
            DotMessagePipe,
            HttpClientTestingModule,
            MockComponent(DotBinaryFieldEditorComponent)
        ],
        providers: [mockProvider(DotMessagePipe)]
    });

    beforeEach(async () => {
        spectator = createHost(
            `<dot-binary-field-ui-message [uiMessage]="uiMessage" [disabled]="disabled">
                <button data-testId="choose-file-btn">Choose File</button>
            </dot-binary-field-ui-message>`,
            {
                hostProps: {
                    uiMessage: {
                        message: 'Drag and Drop File',
                        icon: 'pi pi-upload',
                        severity: 'info'
                    },
                    disabled: false
                }
            }
        );
        spectator.detectChanges();
        await spectator.fixture.whenStable();
    });

    it('should have a message, icon, and serverity', () => {
        const messageText = spectator.query(byTestId('ui-message-span')).innerHTML;
        const messageIconClass = spectator.query(byTestId('ui-message-icon')).className;
        const messageIconContainer = spectator.query(
            byTestId('ui-message-icon-container')
        ).className;

        expect(messageText).toBe('Drag and Drop File');
        expect(messageIconClass).toBe('icon pi pi-upload');
        expect(messageIconContainer).toBe('icon-container info');
    });

    it('should have a button', () => {
        const button = spectator.query(byTestId('choose-file-btn'));
        expect(button).toBeTruthy();
    });

    describe('when disabled', () => {
        beforeEach(() => {
            spectator.setHostInput({ disabled: true });
            spectator.detectChanges();
        });

        it('should add disabled class to host element', () => {
            expect(spectator.element).toHaveClass('disabled');
        });

        it('should add disabled class to icon container', () => {
            const iconContainer = spectator.query(byTestId('ui-message-icon-container'));
            expect(iconContainer).toHaveClass('disabled');
        });

        it('should add disabled class to text element', () => {
            const textElement = spectator.query('.text');
            expect(textElement).toHaveClass('disabled');
        });

        it('should still display message content when disabled', () => {
            const messageText = spectator.query(byTestId('ui-message-span')).innerHTML;
            const messageIconClass = spectator.query(byTestId('ui-message-icon')).className;
            const button = spectator.query(byTestId('choose-file-btn'));

            expect(messageText).toBe('Drag and Drop File');
            expect(messageIconClass).toBe('icon pi pi-upload');
            expect(button).toBeTruthy();
        });
    });
});
