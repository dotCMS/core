import { describe, beforeEach, afterEach } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { DotContentDriveSearchInputComponent } from './dot-content-drive-search-input.component';

describe('DotContentDriveSearchInputComponent', () => {
    let spectator: Spectator<DotContentDriveSearchInputComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveSearchInputComponent,
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('should render the search input', () => {
        spectator.detectChanges();
        const input = spectator.query('[data-testid="search-input"]');
        expect(input).toBeTruthy();
    });
});
