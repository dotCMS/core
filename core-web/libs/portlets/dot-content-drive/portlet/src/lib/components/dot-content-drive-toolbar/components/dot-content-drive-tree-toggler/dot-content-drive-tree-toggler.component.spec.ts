import { it, describe, expect, beforeEach, afterEach } from '@jest/globals';
import { byTestId, Spectator, createComponentFactory, mockProvider } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveTreeTogglerComponent } from './dot-content-drive-tree-toggler.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveTreeTogglerComponent', () => {
    let spectator: Spectator<DotContentDriveTreeTogglerComponent>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;

    /** The real `<button>` PrimeNG renders, which is what the user actually interacts with. */
    const button = () =>
        spectator.query(byTestId('tree-toggler-button'))?.querySelector('button') as HTMLElement;

    const createComponent = createComponentFactory({
        component: DotContentDriveTreeTogglerComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                isTreeExpanded: jest.fn().mockReturnValue(true),
                isTreeVisuallyExpanded: jest.fn().mockReturnValue(true),
                isTreeForceCollapsed: jest.fn().mockReturnValue(false),
                setIsTreeExpanded: jest.fn()
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.tree.collapse': 'Collapse folder tree',
                    'content-drive.tree.expand': 'Expand folder tree'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(DotContentDriveStore, true);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render the dock glyph for a left-hand panel', () => {
        spectator.detectChanges();

        const icon = spectator.query(byTestId('tree-toggle-icon'));

        // The ligature text IS the icon, so the exact string matters — a typo renders as plain words.
        expect(icon?.textContent?.trim()).toBe('dock_to_right');
        expect(icon?.classList.contains('material-symbols-outlined')).toBe(true);
        // Decorative: the accessible name lives on the button.
        expect(icon?.getAttribute('aria-hidden')).toBe('true');

        // The previous two-image (open/close SVG) markup is gone.
        expect(spectator.query(byTestId('tree-toggle-open-icon'))).toBeFalsy();
        expect(spectator.query(byTestId('tree-toggle-close-icon'))).toBeFalsy();
    });

    it('should keep the same glyph whether the tree is expanded or collapsed', () => {
        // The icon names where the panel docks, not what the click does, so it must not react to the
        // state — swapping it made one control read as two different things.
        store.isTreeExpanded.mockReturnValue(true);
        store.isTreeVisuallyExpanded.mockReturnValue(true);
        spectator.detectChanges();
        const whenExpanded = spectator.query(byTestId('tree-toggle-icon'))?.textContent?.trim();

        store.isTreeExpanded.mockReturnValue(false);
        store.isTreeVisuallyExpanded.mockReturnValue(false);
        spectator.detectChanges();

        expect(spectator.query(byTestId('tree-toggle-icon'))?.textContent?.trim()).toBe(
            whenExpanded
        );
    });

    it('should collapse the tree when clicked and currently expanded', () => {
        store.isTreeExpanded.mockReturnValue(true);
        spectator.detectChanges();

        spectator.click(button());

        expect(store.setIsTreeExpanded).toHaveBeenCalledWith(false);
    });

    it('should expand the tree when clicked and currently collapsed', () => {
        store.isTreeExpanded.mockReturnValue(false);
        spectator.detectChanges();

        spectator.click(button());

        expect(store.setIsTreeExpanded).toHaveBeenCalledWith(true);
    });

    describe('accessibility', () => {
        it('should expose a focusable button naming the action it performs', () => {
            store.isTreeVisuallyExpanded.mockReturnValue(true);
            spectator.detectChanges();

            expect(button()).toBeTruthy();
            expect(button().getAttribute('aria-label')).toBe('Collapse folder tree');
        });

        it('should name the opposite action once the tree is collapsed', () => {
            store.isTreeVisuallyExpanded.mockReturnValue(false);
            spectator.detectChanges();

            expect(button().getAttribute('aria-label')).toBe('Expand folder tree');
        });
    });

    describe('when the side panel is forcing the tree collapsed', () => {
        beforeEach(() => {
            store.isTreeForceCollapsed.mockReturnValue(false);
        });

        it('should disable the button, since toggling the preference would move nothing', () => {
            store.isTreeForceCollapsed.mockReturnValue(true);
            spectator.detectChanges();

            expect(button().hasAttribute('disabled')).toBe(true);
        });

        it('should not touch the stored preference while disabled', () => {
            store.isTreeForceCollapsed.mockReturnValue(true);
            spectator.detectChanges();

            spectator.click(button());

            expect(store.setIsTreeExpanded).not.toHaveBeenCalled();
        });

        it('should stay enabled when the panel is not forcing a collapse', () => {
            spectator.detectChanges();

            expect(button().hasAttribute('disabled')).toBe(false);
        });
    });
});
