import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputNumber, InputNumberModule } from 'primeng/inputnumber';
import { RadioButton, RadioButtonModule } from 'primeng/radiobutton';
import { Sidebar } from 'primeng/sidebar';

import { DotMessageService } from '@dotcms/data-access';
import { ExperimentSteps, TrafficProportionTypes } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationTrafficSplitAddComponent } from './dot-experiments-configuration-traffic-split-add.component';

const messageServiceMock = new MockDotMessageService({
    Done: 'Done',
    'experiments.configure.traffic.split.variants.error':
        'The total sum of the weights of the variables must be 100.'
});

const EXPERIMENT_MOCK = getExperimentMock(1);

describe('DotExperimentsConfigurationTrafficSplitAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTrafficSplitAddComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Sidebar;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, RadioButtonModule, InputNumberModule],
        component: DotExperimentsConfigurationTrafficSplitAddComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false
        });
        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.returnValue(of({ ...EXPERIMENT_MOCK }));
        store.loadExperiment(EXPERIMENT_MOCK.id);
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.TRAFFIC,
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
        expect(variantsName[0]).toContainText('DEFAULT');
        expect(variantsName[1]).toContainText('variant a');
    });

    it('should save form when is valid ', () => {
        spyOn(store, 'setSelectedTrafficProportion');
        const submitButton = spectator.query(
            byTestId('add-traffic-split-button')
        ) as HTMLButtonElement;

        expect(submitButton.disabled).toBeFalse();
        expect(submitButton).toContainText('Done');
        expect(spectator.component.form.valid).toBeTrue();
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
        expect(spectator.component.form.valid).toBeFalse();
    });

    it('should close sidebar ', () => {
        spyOn(store, 'closeSidebar');
        sidebar = spectator.query(Sidebar);
        sidebar.hide();

        expect(store.closeSidebar).toHaveBeenCalledTimes(1);
    });
});
