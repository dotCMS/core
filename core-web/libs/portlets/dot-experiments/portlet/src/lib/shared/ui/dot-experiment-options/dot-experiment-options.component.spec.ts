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
        const headerA = spectator.query(byTestId('dot-options-item-header_a'));
        const headerB = spectator.query(byTestId('dot-options-item-header_b'));

        expect(headerA.querySelector('h2')).toHaveText('Title A');
        expect(headerA.querySelector('p')).toHaveText('Detail A');
        expect(headerA.querySelector('i')).toHaveClass('test-icon');

        expect(headerB.querySelector('h2')).toHaveText('Title B');
        expect(headerB.querySelector('p')).toHaveText('Detail B');
        expect(headerB.querySelector('i')).not.toExist();
    });

    it('should add the class expand to an option clicked that contains content', () => {
        const headerOptionWithContent = spectator.query(byTestId('dot-options-item-header_b'));

        spectator.click(headerOptionWithContent);
        spectator.detectComponentChanges();

        expect(spectator.query(byTestId('dot-options-item-content_b'))).toHaveClass('expanded');
        expect(spectator.query(byTestId('dot-options-item-content_b'))).toHaveText(
            'Content of Detail B'
        );
    });
});
