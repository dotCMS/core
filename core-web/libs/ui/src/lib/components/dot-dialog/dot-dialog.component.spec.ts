import { byTestId, createHostFactory, SpectatorHost } from '@openng/spectator/jest';

import { DotDialogContentComponent } from './components/dot-dialog-content/dot-dialog-content.component';
import { DotDialogFooterComponent } from './components/dot-dialog-footer/dot-dialog-footer.component';
import { DotDialogHeaderComponent } from './components/dot-dialog-header/dot-dialog-header.component';
import { DotDialogComponent } from './dot-dialog.component';

describe('DotDialogComponent', () => {
    let spectator: SpectatorHost<DotDialogComponent>;

    const createHost = createHostFactory({
        component: DotDialogComponent,
        imports: [DotDialogHeaderComponent, DotDialogContentComponent, DotDialogFooterComponent]
    });

    const slotTags = () => Array.from(spectator.element.children).map((child) => child.tagName);

    describe('slot order', () => {
        // Declared footer → content → header on purpose: the shell's template, not the consumer's
        // markup, is what decides where each slot lands.
        beforeEach(() => {
            spectator = createHost(`
                <dot-dialog>
                    <dot-dialog-footer><span data-testid="footer-child"></span></dot-dialog-footer>
                    <dot-dialog-content><span data-testid="content-child"></span></dot-dialog-content>
                    <dot-dialog-header title="Add File" />
                </dot-dialog>
            `);
        });

        it('should render the slots as header, content, footer regardless of the authored order', () => {
            expect(slotTags()).toEqual([
                'DOT-DIALOG-HEADER',
                'DOT-DIALOG-CONTENT',
                'DOT-DIALOG-FOOTER'
            ]);
        });

        it('should project the children of each slot', () => {
            expect(spectator.query(byTestId('content-child'))).toExist();
            expect(spectator.query(byTestId('footer-child'))).toExist();
            expect(spectator.query(byTestId('dialog-title'))?.textContent?.trim()).toBe('Add File');
        });
    });

    it('should lay itself out as a full-height column that clips its overflow', () => {
        spectator = createHost(`
            <dot-dialog>
                <dot-dialog-content></dot-dialog-content>
            </dot-dialog>
        `);

        expect(spectator.element).toHaveClass([
            'flex',
            'h-full',
            'min-h-0',
            'flex-col',
            'overflow-hidden'
        ]);
    });

    // Documented behavior, not an accident: there is no default slot, so overlays and hidden inputs
    // have to stay siblings of <dot-dialog>. A silent drop is easy to miss in review, so it is
    // pinned down here.
    it('should not render a child that matches no slot', () => {
        spectator = createHost(`
            <dot-dialog>
                <dot-dialog-content></dot-dialog-content>
                <span data-testid="orphan"></span>
            </dot-dialog>
        `);

        expect(spectator.query(byTestId('orphan'))).not.toExist();
    });

    it('should still place a conditionally rendered footer in the footer slot', () => {
        spectator = createHost(
            `
            <dot-dialog>
                <dot-dialog-content></dot-dialog-content>
                @if (showFooter) {
                    <dot-dialog-footer><span data-testid="footer-child"></span></dot-dialog-footer>
                }
            </dot-dialog>
        `,
            { hostProps: { showFooter: false } }
        );

        expect(slotTags()).toEqual(['DOT-DIALOG-CONTENT']);

        spectator.setHostInput({ showFooter: true });
        spectator.detectChanges();

        expect(slotTags()).toEqual(['DOT-DIALOG-CONTENT', 'DOT-DIALOG-FOOTER']);
        expect(spectator.query(byTestId('footer-child'))).toExist();
    });
});
