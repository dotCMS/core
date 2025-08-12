import { it, describe, expect, beforeEach, afterEach } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotContentDriveTreeTogglerComponent } from './dot-content-drive-tree-toggler.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

describe('DotContentDriveTreeTogglerComponent', () => {
    let spectator: Spectator<DotContentDriveTreeTogglerComponent>;
    let store: jest.Mocked<InstanceType<typeof DotContentDriveStore>>;

    const createComponent = createComponentFactory({
        component: DotContentDriveTreeTogglerComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                treeExpanded: jest.fn().mockReturnValue(true),
                setTreeExpanded: jest.fn()
            })
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

    it('should render the tree toggle icon', () => {
        spectator.detectChanges();
        const icon = spectator.query('[data-testid="tree-toggle-icon"]');
        expect(icon).toBeTruthy();
    });

    it('should have active class when tree is expanded', () => {
        store.treeExpanded.mockReturnValue(true);
        spectator.detectChanges();
        expect(spectator.element.classList.contains('active')).toBe(true);
    });

    it('should not have active class when tree is collapsed', () => {
        store.treeExpanded.mockReturnValue(false);
        spectator.detectChanges();
        expect(spectator.element.classList.contains('active')).toBe(false);
    });

    it('should collapse the tree when clicked and currently expanded', () => {
        store.treeExpanded.mockReturnValue(true);
        spectator.detectChanges();

        spectator.click(spectator.element);

        expect(store.setTreeExpanded).toHaveBeenCalledWith(false);
    });

    it('should expand the tree when clicked and currently collapsed', () => {
        store.treeExpanded.mockReturnValue(false);
        spectator.detectChanges();

        spectator.click(spectator.element);

        expect(store.setTreeExpanded).toHaveBeenCalledWith(true);
    });
});
