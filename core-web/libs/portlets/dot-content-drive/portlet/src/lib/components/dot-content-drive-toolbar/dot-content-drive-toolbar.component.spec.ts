import { it, describe, expect, beforeEach, afterEach } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { DotContentDriveToolbarComponent } from './dot-content-drive-toolbar.component';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

describe('DotContentDriveToolbarComponent', () => {
    let spectator: Spectator<DotContentDriveToolbarComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveToolbarComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                isTreeExpanded: jest.fn().mockReturnValue(true),
                setIsTreeExpanded: jest.fn()
            })
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render toolbar container', () => {
        spectator.detectChanges();
        const toolbar = spectator.query('.dot-content-drive-toolbar');
        expect(toolbar).toBeTruthy();
    });

    it('should render the tree toggler', () => {
        spectator.detectChanges();
        const toggler = spectator.query('[data-testid="tree-toggler"]');
        expect(toggler).toBeTruthy();
    });

    it('should render the Add New button', () => {
        spectator.detectChanges();
        const button = spectator.query('[data-testid="add-new-button"]');
        expect(button).toBeTruthy();
    });

    it('should render start and end groups', () => {
        spectator.detectChanges();
        expect(spectator.query('.p-toolbar-group-start')).toBeTruthy();
        expect(spectator.query('.p-toolbar-group-end')).toBeTruthy();
    });
});
