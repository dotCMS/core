import { byTestId, createHostFactory, SpectatorHost } from '@openng/spectator/jest';

import { DotDialogContentComponent } from './dot-dialog-content.component';

describe('DotDialogContentComponent', () => {
    let spectator: SpectatorHost<DotDialogContentComponent>;

    const createHost = createHostFactory(DotDialogContentComponent);

    beforeEach(() => {
        spectator = createHost(
            `<dot-dialog-content [scroll]="scroll" [padded]="padded">
                <span data-testid="body"></span>
            </dot-dialog-content>`,
            { hostProps: { scroll: false, padded: false } }
        );
    });

    it('should project its content', () => {
        expect(spectator.query(byTestId('body'))).toExist();
    });

    // `min-h-0 flex-1` is what lets an inner region scroll instead of stretching the dialog.
    it('should always take the remaining space and allow shrinking', () => {
        expect(spectator.element).toHaveClass(['block', 'min-h-0', 'min-w-0', 'flex-1']);
    });

    it('should not scroll or pad by default', () => {
        expect(spectator.element).not.toHaveClass('overflow-y-auto');
        expect(spectator.element).not.toHaveClass('p-4');
    });

    it('should scroll when asked to', () => {
        spectator.setHostInput({ scroll: true });

        expect(spectator.element).toHaveClass('overflow-y-auto');
    });

    it('should pad when asked to', () => {
        spectator.setHostInput({ padded: true });

        expect(spectator.element).toHaveClass('p-4');
    });
});
