import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { Skeleton } from 'primeng/skeleton';

import { DotExperimentsUiHeaderComponent } from './dot-experiments-ui-header.component';

describe('ExperimentsHeaderComponent', () => {
    let spectator: Spectator<DotExperimentsUiHeaderComponent>;

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

    it('should emit goBack output when icon is clicked. ', () => {
        let output;
        spectator.output('goBack').subscribe((result) => (output = result));
        const goBackButton = spectator.query(byTestId('goback-link')) as HTMLAnchorElement;
        spectator.click(goBackButton);
        expect(output).toBeTrue();
    });

    it('should show the skeleton component if isLoading true ', () => {
        spectator.setInput({
            isLoading: true
        });

        expect(spectator.query(Skeleton)).toExist();
    });
});
