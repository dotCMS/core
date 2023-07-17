import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { byTestId } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent } from '@dotcms/ui';

const BUTTON_LABEL = 'Amazing Label';
const TEXT_INPUT = 'My title';
const SUBTITLE_INPUT = 'My subtitle';
const ICON_PRIMENG_CLASS = 'icon-class';

describe('DotEmptyContainerComponent', () => {
    let spectator: Spectator<DotEmptyContainerComponent>;

    const createComponent = createComponentFactory({
        component: DotEmptyContainerComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('Only Principal message', () => {
        beforeEach(() => {
            spectator.setInput('hideContactUsLink', true);
        });
        it('should has only a principal message and `title` Input', () => {
            spectator.setInput('title', TEXT_INPUT);

            expect(spectator.query(byTestId('message-principal'))).toExist();
            expect(spectator.query(byTestId('message-extra'))).not.toExist();

            expect(spectator.query(byTestId('message-title'))).toContainText(TEXT_INPUT);
        });

        it('should has icon when you send a `icon` Input', () => {
            expect(spectator.query(byTestId('message-icon'))).not.toExist();

            spectator.setInput('icon', ICON_PRIMENG_CLASS);

            expect(spectator.query(byTestId('message-icon'))).toExist();
            expect(spectator.query(byTestId('message-icon'))).toHaveClass(ICON_PRIMENG_CLASS);
        });

        it('should has subtitle when you send a `subtitle` Input', () => {
            expect(spectator.query(byTestId('message-subtitle'))).not.toExist();

            spectator.setInput('subtitle', SUBTITLE_INPUT);

            expect(spectator.query(byTestId('message-subtitle'))).toExist();
            expect(spectator.query(byTestId('message-subtitle'))).toContainText(SUBTITLE_INPUT);
        });
    });
    describe('With extra message', () => {
        it('should has extra message', () => {
            expect(spectator.query(byTestId('message-principal'))).toExist();
            expect(spectator.query(byTestId('message-extra'))).toExist();
        });

        it('should has button if `buttonLabel` Input are send', () => {
            expect(spectator.query(byTestId('message-button'))).not.toExist();

            spectator.setInput('buttonLabel', BUTTON_LABEL);

            expect(spectator.query(byTestId('message-button'))).toExist();
            expect(spectator.query(byTestId('message-button'))).toContainText(BUTTON_LABEL);
        });
        it('should has contact link', () => {
            expect(spectator.query(byTestId('message-contact-link'))).toExist();

            spectator.setInput('hideContactUsLink', true);
            expect(spectator.query(byTestId('message-contact-link'))).not.toExist();
        });
    });
});
