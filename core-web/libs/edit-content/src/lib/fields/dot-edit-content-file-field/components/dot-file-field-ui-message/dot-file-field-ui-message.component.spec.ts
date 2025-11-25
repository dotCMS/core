import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

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
        imports: [DotMessagePipe],
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
});
