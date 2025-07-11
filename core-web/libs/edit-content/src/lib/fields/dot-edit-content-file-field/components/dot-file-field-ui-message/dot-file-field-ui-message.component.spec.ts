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
            spectator = createComponent({
                props: {
                    uiMessage: getUiMessage('DEFAULT')
                } as unknown
            });
        });

        it('should be created', () => {
            spectator.detectChanges();
            expect(spectator.component).toBeTruthy();
        });

        it('should have proper data', () => {
            spectator.detectChanges();

            const expectMessage = getUiMessage('DEFAULT');

            const severity = spectator.query(byTestId('ui-message-icon-container'));
            const messageIcon = spectator.query(byTestId('ui-message-icon'));
            const messageText = spectator.query(byTestId('ui-message-span'));

            expect(severity).toHaveClass(expectMessage.severity);
            expect(messageIcon).toHaveClass(expectMessage.icon);
            expect(messageText).toContainText('Drag and Drop File');
        });
    });

    describe('when disabled', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    uiMessage: getUiMessage('DEFAULT'),
                    disabled: true
                } as unknown
            });
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
            const expectMessage = getUiMessage('DEFAULT');

            const severity = spectator.query(byTestId('ui-message-icon-container'));
            const messageIcon = spectator.query(byTestId('ui-message-icon'));
            const messageText = spectator.query(byTestId('ui-message-span'));

            expect(severity).toHaveClass(expectMessage.severity);
            expect(severity).toHaveClass('disabled');
            expect(messageIcon).toHaveClass(expectMessage.icon);
            expect(messageText).toContainText('Drag and Drop File');
        });
    });
});
