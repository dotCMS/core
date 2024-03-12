import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotTableComponent } from './dot-table.component';

describe('DotTableComponent', () => {
    let spectator: Spectator<DotTableComponent>;
    const createComponent = createComponentFactory(DotTableComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
