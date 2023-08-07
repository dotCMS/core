import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { DotExperimentOptionsComponent } from './dot-experiment-options.component';
import { DotExperimentsOptionsModule } from './dot-experiments-options.module';

describe('DotExperimentOptionsComponent', () => {
    let spectator: SpectatorHost<DotExperimentOptionsComponent>;
    const createHost = createHostFactory({
        component: DotExperimentOptionsComponent,
        imports: [DotExperimentsOptionsModule]
    });

    beforeEach(() => {
        spectator = createHost(
            `<dot-experiment-options formControlName="type">
                         <dot-experiment-options-item
                                      value="a"
                                      detail="Detail A"
                                      title="Title A"
                                      icon="test-icon">
                         </dot-experiment-options-item>
                         <dot-experiment-options-item
                                      value="b"
                                      detail="Detail B"
                                      title="Title B">
                                      
                          <ng-template dotOptionContent>
                Content of Detail B
              </ng-template>
                         </dot-experiment-options-item>
                        </dot-experiment-options>`
        );
    });

    it('should have 2 rendered items', () => {
        expect(spectator.queryAll('.dot-options__item').length).toEqual(2);
    });

    it('should have 2 rendered items with title and detail', () => {
        const headers = spectator.queryAll(byTestId('dot-options-item-header'));

        expect(headers[0].querySelector('h2')).toHaveText('Title A');
        expect(headers[0].querySelector('p')).toHaveText('Detail A');
        expect(headers[0].querySelector('i')).toHaveClass('test-icon');

        expect(headers[1].querySelector('h2')).toHaveText('Title B');
        expect(headers[1].querySelector('p')).toHaveText('Detail B');
        expect(headers[1].querySelector('i')).not.toExist();
    });

    it('should add the class expand to an option clicked that contains content', () => {
        const reachPageOption = spectator.queryLast(byTestId('dot-options-item-header'));

        spectator.click(reachPageOption);
        spectator.detectComponentChanges();

        expect(spectator.query(byTestId('dot-options-item-content'))).toHaveClass('expanded');
        expect(spectator.query(byTestId('dot-options-item-content'))).toHaveText(
            'Content of Detail B'
        );
    });
});
