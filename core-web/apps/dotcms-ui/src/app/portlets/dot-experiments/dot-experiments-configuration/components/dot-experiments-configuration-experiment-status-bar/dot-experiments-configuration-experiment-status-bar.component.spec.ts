import { DotExperimentsConfigurationExperimentStatusBarComponent } from './dot-experiments-configuration-experiment-status-bar.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { DotExperimentStatusList } from '@dotcms/dotcms-models';

describe('DotExperimentsConfigurationExperimentStatusBarComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationExperimentStatusBarComponent>;
    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurationExperimentStatusBarComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should show the current status', () => {
        spectator.setInput({
            status: DotExperimentStatusList.DRAFT
        });

        expect(spectator.query(byTestId('status-bar-current-status'))).toContainText(
            DotExperimentStatusList.DRAFT
        );
    });
});
