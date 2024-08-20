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
            `<dot-binary-field-ui-message [uiMessage]="uiMessage">
                <button data-testId="choose-file-btn">Choose File</button>
            </dot-binary-field-ui-message>`,
            {
                hostProps: {
                    uiMessage: {
                        message: 'Drag and Drop File',
                        icon: 'pi pi-upload',
                        severity: 'info'
                    }
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
});
