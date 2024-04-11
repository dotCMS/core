import { Spectator, createComponentFactory } from '@ngneat/spectator';

import { DotContentAsideWorkflowComponent } from './dot-content-aside-workflow.component';

describe('DotContentAsideWorkflowComponent', () => {
    let spectator: Spectator<DotContentAsideWorkflowComponent>;
    const createComponent = createComponentFactory(DotContentAsideWorkflowComponent);

    it('should create', () => {
        spectator = createComponent();

        expect(spectator.component).toBeTruthy();
    });
});
