import { createHostFactory, SpectatorHost } from '@openng/spectator/jest';

import { DotDialogFooterComponent } from './dot-dialog-footer.component';

describe('DotDialogFooterComponent', () => {
    let spectator: SpectatorHost<DotDialogFooterComponent>;

    const createHost = createHostFactory(DotDialogFooterComponent);

    beforeEach(() => {
        spectator = createHost(`
            <dot-dialog-footer>
                <span data-testid="first"></span>
                <span data-testid="second"></span>
            </dot-dialog-footer>
        `);
    });

    it('should project its actions in source order', () => {
        const projected = Array.from(spectator.element.children).map((child) =>
            child.getAttribute('data-testid')
        );

        expect(projected).toEqual(['first', 'second']);
    });

    // `flex-wrap` is also what lets a `w-full` child (an error message) take its own line above the
    // actions, which is why it is asserted here rather than left to the consumer.
    it('should align its actions to the right under a divider', () => {
        expect(spectator.element).toHaveClass([
            'flex',
            'flex-none',
            'flex-wrap',
            'items-center',
            'justify-end',
            'gap-2',
            'border-t',
            'border-gray-200',
            'px-4',
            'py-3'
        ]);
    });
});
