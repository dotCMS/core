import { DotExperimentsConfigurationTargetingComponent } from './dot-experiments-configuration-targeting.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.targeting.name': 'Targeting'
});
describe('DotExperimentsConfigurationTargetingComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTargetingComponent>;
    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationTargetingComponent,
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
        expect(spectator.query(byTestId('targeting-card-name'))).toHaveText('Targeting');
        expect(spectator.query(byTestId('targeting-add-button'))).toExist();
    });
});
