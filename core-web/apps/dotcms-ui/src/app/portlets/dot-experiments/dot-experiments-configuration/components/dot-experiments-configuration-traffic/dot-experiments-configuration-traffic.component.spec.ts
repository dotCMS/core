import { DotExperimentsConfigurationTrafficComponent } from './dot-experiments-configuration-traffic.component';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { DotMessageService } from '@dotcms/data-access';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.traffic.name': 'Traffic',
    'experiments.configure.traffic.split.name': 'Split'
});
describe('DotExperimentsConfigurationTrafficComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTrafficComponent>;
    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationTrafficComponent,
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

    it('should render split and allocation rows', () => {
        expect(spectator.queryAll(Card).length).toEqual(3);
        expect(spectator.query(byTestId('traffic-card-title'))).toHaveText('Traffic');
        expect(spectator.query(byTestId('traffic-allocation-button'))).toExist();
        expect(spectator.query(byTestId('traffic-split-title'))).toHaveText('Split');
        expect(spectator.query(byTestId('traffic-split-change-button'))).toExist();
    });
});
