import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DotExperimentOptionContentDirective } from './directives/dot-experiment-option-content.directive';
import { DotExperimentOptionsItemDirective } from './directives/dot-experiment-options-item.directive';
import { DotExperimentOptionsComponent } from './dot-experiment-options.component';

describe('DotExperimentOptionsComponent', () => {
    let spectator: SpectatorHost<DotExperimentOptionsComponent>;
    let form: FormGroup;
    const createHost = createHostFactory({
        component: DotExperimentOptionsComponent,
        imports: [
            ReactiveFormsModule,
            DotExperimentOptionsComponent,
            DotExperimentOptionsItemDirective,
            DotExperimentOptionContentDirective
        ]
    });

    beforeEach(() => {
        form = new FormGroup({
            type: new FormControl<string>('a', { nonNullable: true })
        });
        spectator = createHost(
            `<form [formGroup]="form">
                <dot-experiment-options formControlName="type">
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
                        </dot-experiment-options>
              </form>`,
            { hostProps: { form } }
        );
    });

    it('should have 2 rendered items', () => {
        expect(spectator.queryAll('[data-testId^="dot-options-item_"]').length).toEqual(2);
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

        expect(spectator.query(byTestId('dot-options-item-content_b'))).toHaveClass('pb-4');
        expect(spectator.query(byTestId('dot-options-item-content_b'))).toHaveText(
            'Content of Detail B'
        );
    });
});
