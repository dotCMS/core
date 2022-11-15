import { DotExperimentsUiHeaderComponent } from './dot-experiments-ui-header.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { DotIconComponent } from '@dotcms/ui';

describe('ExperimentsHeaderComponent', () => {
    let spectator: Spectator<DotExperimentsUiHeaderComponent>;
    let dotIconComponent: DotIconComponent | null;

    const createComponent = createComponentFactory({
        component: DotExperimentsUiHeaderComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should has a title rendered', () => {
        const title = 'My title';
        spectator.setInput('title', title);
        expect(spectator.query(byTestId('title'))).toHaveText(title);
    });

    it('should has a dotIcon rendered', () => {
        dotIconComponent = spectator.query(DotIconComponent);

        expect(dotIconComponent).toExist();
    });
});
