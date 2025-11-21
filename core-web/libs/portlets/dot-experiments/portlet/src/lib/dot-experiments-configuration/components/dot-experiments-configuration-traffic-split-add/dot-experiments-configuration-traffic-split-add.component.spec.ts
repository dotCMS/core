import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { Drawer } from 'primeng/drawer';
import { InputNumber, InputNumberModule } from 'primeng/inputnumber';
import { RadioButton, RadioButtonModule } from 'primeng/radiobutton';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DEFAULT_VARIANT_NAME,
    ExperimentSteps,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationTrafficSplitAddComponent } from './dot-experiments-configuration-traffic-split-add.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

const messageServiceMock = new MockDotMessageService({
    Done: 'Done',
    'experiments.action.stop.confirm-message': 'stop',
    'experiments.configure.traffic.split.variants.error':
        'The total sum of the weights of the variables must be 100.'
});

const EXPERIMENT_MOCK = getExperimentMock(1);

describe('DotExperimentsConfigurationTrafficSplitAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTrafficSplitAddComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Drawer;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, RadioButtonModule, InputNumberModule],
        component: DotExperimentsConfigurationTrafficSplitAddComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(ConfirmationService)
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false
        });
        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of({ ...EXPERIMENT_MOCK }));
        dotExperimentsService.setTrafficProportion.mockReturnValue(of({ ...EXPERIMENT_MOCK }));
        store.loadExperiment(EXPERIMENT_MOCK.id);
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.TRAFFICS_SPLIT,
            isOpen: true
        });
        spectator.detectChanges();
    });

    it('should load split value', () => {
        const radioButton = spectator.query(RadioButton);
        const variantsWeight = spectator.queryAll(byTestId('variant-weight'));
        const variantsName = spectator.queryAll(byTestId('variant-name'));

        expect(radioButton.checked).toEqual(true);
        expect(spectator.queryAll(InputNumber).length).toEqual(0);
        expect(variantsWeight[0]).toContainText('50');
        expect(variantsWeight[1]).toContainText('50');
        expect(variantsName[0]).toContainText(DEFAULT_VARIANT_NAME);
        expect(variantsName[1]).toContainText('variant a');
    });

    it('should save form when is valid ', () => {
        jest.spyOn(store, 'setSelectedTrafficProportion');
        const submitButton = spectator.query(
            byTestId('add-traffic-split-button')
        ) as HTMLButtonElement;

        expect(submitButton.disabled).toEqual(false);
        expect(submitButton).toContainText('Done');
        expect(spectator.component.form.valid).toEqual(true);
        expect(spectator.query(byTestId('dotErrorMsg'))).toBeNull();

        spectator.click(submitButton);
        expect(store.setSelectedTrafficProportion).toHaveBeenCalledWith({
            trafficProportion: EXPERIMENT_MOCK.trafficProportion,
            experimentId: EXPERIMENT_MOCK.id
        });
    });

    it('should display inputs when is Custom split ', () => {
        spectator.component.form.get('type').setValue(TrafficProportionTypes.CUSTOM_PERCENTAGES);
        spectator.detectChanges();

        expect(spectator.queryAll(InputNumber).length).toEqual(2);
    });

    it('should display error and disable form when custom split is different than 100', () => {
        spectator.component.form.get('type').setValue(TrafficProportionTypes.CUSTOM_PERCENTAGES);
        const variants = spectator.component.form.get('variants').value;
        variants[0].weight = 90;
        spectator.component.form.get('variants').setValue(variants);

        spectator.detectChanges();

        expect(spectator.query(byTestId('dotErrorMsg'))).toContainText(
            'The total sum of the weights of the variables must be 100.'
        );
        expect(spectator.component.form.valid).toEqual(false);
    });

    it('should close sidebar ', () => {
        jest.spyOn(store, 'closeSidebar');
        sidebar = spectator.query(Drawer);
        sidebar.hide();

        expect(store.closeSidebar).toHaveBeenCalledTimes(1);
    });
});
