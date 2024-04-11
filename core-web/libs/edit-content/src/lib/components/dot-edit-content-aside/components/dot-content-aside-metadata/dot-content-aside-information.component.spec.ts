import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotContentAsideInformationComponent } from './dot-content-aside-information.component';

describe('DotContentAsideInformationComponent', () => {
    let spectator: Spectator<DotContentAsideInformationComponent>;
    const createComponent = createComponentFactory(DotContentAsideInformationComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
