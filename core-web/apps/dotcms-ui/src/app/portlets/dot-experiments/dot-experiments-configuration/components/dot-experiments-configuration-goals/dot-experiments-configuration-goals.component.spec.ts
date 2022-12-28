import { DotExperimentsConfigurationGoalsComponent } from './dot-experiments-configuration-goals.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessageService } from '@dotcms/data-access';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.name': 'Goals'
});
describe('DotExperimentsConfigurationGoalsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationGoalsComponent>;
    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationGoalsComponent,
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

    it('should render the card', () => {
        expect(spectator.queryAll(Card).length).toEqual(1);
        expect(spectator.query(byTestId('goals-card-name'))).toHaveText('Goals');
        expect(spectator.query(byTestId('goals-add-button'))).toExist();
    });
});
