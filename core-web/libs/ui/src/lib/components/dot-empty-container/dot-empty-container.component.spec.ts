import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { byTestId } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotEmptyContainerComponent, PrincipalConfiguration } from '@dotcms/ui';

const BUTTON_LABEL = 'Amazing Label';

const BASIC_CONFIGURATION: PrincipalConfiguration = {
    title: 'My title',
    icon: 'icon-class',
    subtitle: 'My subtitle'
};

describe('DotEmptyContainerComponent', () => {
    let spectator: Spectator<DotEmptyContainerComponent>;

    const createComponent = createComponentFactory({
        component: DotEmptyContainerComponent,
        providers: [mockProvider(DotMessageService)]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                configuration: BASIC_CONFIGURATION
            }
        });
        spectator.detectChanges();
    });

    describe('Only Principal message', () => {
        beforeEach(() => {
            spectator.setInput('hideContactUsLink', true);
        });
        it('should has only a principal empty message with, title, icon and subtitle', () => {
            expect(spectator.query(byTestId('message-principal'))).toExist();
            expect(spectator.query(byTestId('message-extra'))).not.toExist();

            expect(spectator.query(byTestId('message-title'))).toContainText(
                BASIC_CONFIGURATION.title
            );

            expect(spectator.query(byTestId('message-icon'))).toExist();
            expect(spectator.query(byTestId('message-icon'))).toHaveClass(BASIC_CONFIGURATION.icon);

            expect(spectator.query(byTestId('message-subtitle'))).toExist();
            expect(spectator.query(byTestId('message-subtitle'))).toContainText(
                BASIC_CONFIGURATION.subtitle
            );
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
        it('should has secondary button class', () => {
            spectator.setInput('buttonLabel', BUTTON_LABEL);
            spectator.setInput('secondaryButton', true);

            const button = spectator.query(byTestId('message-button'));

            expect(button.classList.contains('p-button-outlined')).toBe(true);
        });
    });
});
