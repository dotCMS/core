import { DotExperimentsConfigurationVariantsComponent } from './dot-experiments-configuration-variants.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { TrafficProportion } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { TrafficProportionTypes } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';

const messageServiceMock = new MockDotMessageService({
    'experiments.list.name': 'Name'
});
describe('DotExperimentsConfigurationVariantsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationVariantsComponent>;
    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationVariantsComponent,
        componentProviders: [],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
    });

    it('should show the variants sent', () => {
        const trafficProportion: TrafficProportion = {
            type: TrafficProportionTypes.SPLIT_EVENLY,
            variants: [
                {
                    id: '111',
                    name: 'my variant',
                    weight: 100
                }
            ]
        };
        spectator.setInput({
            trafficProportion
        });

        spectator.detectChanges();

        expect(spectator.query(byTestId('variant-name'))).toHaveText(
            trafficProportion.variants[0].name
        );
        expect(spectator.query(byTestId('variant-weight'))).toHaveText(
            trafficProportion.variants[0].weight + '%'
        );
        expect(spectator.query(byTestId('variant-view-button'))).toExist();
    });
});
