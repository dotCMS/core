import { byTestId, createHostFactory, SpectatorHost } from '@openng/spectator/jest';

import { DotDialogHeaderComponent } from './dot-dialog-header.component';

describe('DotDialogHeaderComponent', () => {
    let spectator: SpectatorHost<DotDialogHeaderComponent>;

    const createHost = createHostFactory(DotDialogHeaderComponent);

    const clickClose = () => {
        const button = spectator.query(byTestId('dialog-close-btn'))?.querySelector('button');
        spectator.click(button as HTMLElement);
    };

    beforeEach(() => {
        spectator = createHost(
            `<dot-dialog-header [title]="title" [closable]="closable" [closeLabel]="closeLabel">
                <span data-testid="badge"></span>
                <span dialogHeaderActions data-testid="custom-action"></span>
            </dot-dialog-header>`,
            { hostProps: { title: 'Add Image', closable: true, closeLabel: '' } }
        );
    });

    it('should render the title it is given', () => {
        expect(spectator.query(byTestId('dialog-title'))?.textContent?.trim()).toBe('Add Image');
    });

    it('should render a title the consumer changes later', () => {
        spectator.setHostInput({ title: 'Add File' });

        expect(spectator.query(byTestId('dialog-title'))?.textContent?.trim()).toBe('Add File');
    });

    describe('close button', () => {
        it('should emit close when clicked', () => {
            const spy = jest.spyOn(spectator.component.close, 'emit');

            clickClose();

            expect(spy).toHaveBeenCalledTimes(1);
        });

        it('should be rendered by default', () => {
            expect(spectator.query(byTestId('dialog-close-btn'))).toExist();
        });

        it('should be dropped when the dialog is not closable', () => {
            spectator.setHostInput({ closable: false });

            expect(spectator.query(byTestId('dialog-close-btn'))).not.toExist();
        });

        // The label arrives already translated, so an unset one degrades to no accessible name
        // rather than to a raw i18n key leaking into the UI.
        it('should take its accessible name from closeLabel', () => {
            spectator.setHostInput({ closeLabel: 'Close' });

            expect(spectator.query(byTestId('dialog-close-btn'))?.getAttribute('aria-label')).toBe(
                'Close'
            );
        });

        it('should carry no accessible name when closeLabel is unset', () => {
            expect(spectator.query(byTestId('dialog-close-btn'))?.hasAttribute('aria-label')).toBe(
                false
            );
        });
    });

    describe('projected content', () => {
        it('should place the actions slot before the close button', () => {
            const action = spectator.query(byTestId('custom-action'));
            const close = spectator.query(byTestId('dialog-close-btn'));

            expect(action).toExist();
            expect(action?.compareDocumentPosition(close as Node)).toBe(
                Node.DOCUMENT_POSITION_FOLLOWING
            );
        });

        it('should render default content next to the title', () => {
            const badge = spectator.query(byTestId('badge'));
            const title = spectator.query(byTestId('dialog-title'));

            expect(badge?.parentElement).toBe(title?.parentElement);
        });
    });
});
