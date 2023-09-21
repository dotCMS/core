import { SpectatorHost, byTestId, createHostFactory } from '@ngneat/spectator';

import { CommonModule } from '@angular/common';

import { DotBinaryFieldUiMessageComponent } from './dot-binary-field-ui-message.component';

describe('DotBinaryFieldUiMessageComponent', () => {
    let spectator: SpectatorHost<DotBinaryFieldUiMessageComponent>;

    const createHost = createHostFactory({
        component: DotBinaryFieldUiMessageComponent,
        imports: [CommonModule],
        providers: []
    });

    beforeEach(async () => {
        spectator = createHost(
            `<dot-binary-field-ui-message [message]="message" [icon]="icon" [severity]="severity">
                <button data-testId="choose-file-btn">Choose File</button>
            </dot-binary-field-ui-message>`,
            {
                hostProps: {
                    message: 'Drag and Drop File',
                    icon: 'pi pi-upload',
                    severity: 'info'
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
        expect(messageIconClass).toBe('pi pi-upload');
        expect(messageIconContainer).toBe('icon-container info');
    });

    it('should have a button', () => {
        const button = spectator.query(byTestId('choose-file-btn'));
        expect(button).toBeTruthy();
    });
});
