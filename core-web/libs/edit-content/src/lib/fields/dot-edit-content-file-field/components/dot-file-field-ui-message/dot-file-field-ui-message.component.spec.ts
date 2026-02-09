import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { CommonModule } from '@angular/common';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotFileFieldUiMessageComponent } from './dot-file-field-ui-message.component';

import { getUiMessage } from '../../utils/messages';

describe('DotFileFieldUiMessageComponent', () => {
    let spectator: Spectator<DotFileFieldUiMessageComponent>;

    const createComponent = createComponentFactory({
        component: DotFileFieldUiMessageComponent,
        detectChanges: false,
        imports: [DotMessagePipe, CommonModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'dot.file.field.drag.and.drop.message': 'Drag and Drop File'
                })
            }
        ]
    });

    describe('default uiMessage', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator.setInput('uiMessage', getUiMessage('DEFAULT'));
        });

        it('should be created', () => {
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });

        it('should have proper data', () => {
            spectator.detectChanges();

            const expectMessage = getUiMessage('DEFAULT');

            const iconContainer = spectator.query(byTestId('ui-message-icon-container'));
            const messageIcon = spectator.query(byTestId('ui-message-icon'));
            const messageText = spectator.query(byTestId('ui-message-span'));

            expect(iconContainer).toHaveStyle({
                color: 'var(--color-palette-primary-500)',
                'background-color': 'var(--color-palette-primary-200)'
            });
            expect(messageIcon).toHaveClass(expectMessage.icon);
            expect(messageText).toContainText('Drag and Drop File');
        });
    });

    describe('when disabled', () => {
        beforeEach(() => {
            spectator = createComponent();
            spectator.setInput('uiMessage', getUiMessage('DEFAULT'));
            spectator.setInput('disabled', true);
            spectator.detectChanges();
        });

        it('should add pointer-events-none class to root element', () => {
            const rootElement = spectator.query(
                '[data-testid="ui-message-icon-container"]'
            )?.parentElement;
            expect(rootElement).toHaveClass('pointer-events-none');
        });

        it('should apply disabled styles to icon container', () => {
            const iconContainer = spectator.query(byTestId('ui-message-icon-container'));
            expect(iconContainer).toHaveStyle({
                color: 'var(--field-disabled-color, #9ca3af)',
                'background-color': 'var(--field-disabled-bgcolor, #f3f4f6)'
            });
        });

        it('should add disabled text color to message text', () => {
            const textElement = spectator.query(byTestId('ui-message-span'))?.parentElement;
            expect(textElement).toHaveClass('text-gray-400');
        });

        it('should still display message content when disabled', () => {
            const expectMessage = getUiMessage('DEFAULT');

            const iconContainer = spectator.query(byTestId('ui-message-icon-container'));
            const messageIcon = spectator.query(byTestId('ui-message-icon'));
            const messageText = spectator.query(byTestId('ui-message-span'));

            expect(iconContainer).toBeTruthy();
            expect(messageIcon).toHaveClass(expectMessage.icon);
            expect(messageText).toContainText('Drag and Drop File');
        });
    });
});
