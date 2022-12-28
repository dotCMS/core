import { DotExperimentsConfigurationSchedulingComponent } from './dot-experiments-configuration-scheduling.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'Scheduling',
    'experiments.configure.scheduling.start': 'When the experiment start'
});
describe('DotExperimentsConfigurationSchedulingComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationSchedulingComponent>;
    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationSchedulingComponent,
        componentProviders: [],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the card and split and allocation rows', () => {
        expect(spectator.queryAll(Card).length).toEqual(2);
        expect(spectator.query(byTestId('scheduling-card-name'))).toHaveText('Scheduling');
        expect(spectator.query(byTestId('scheduling-card-title-row'))).toHaveText(
            'When the experiment start'
        );
        expect(spectator.query(byTestId('scheduling-setup-button'))).toExist();
    });
});
