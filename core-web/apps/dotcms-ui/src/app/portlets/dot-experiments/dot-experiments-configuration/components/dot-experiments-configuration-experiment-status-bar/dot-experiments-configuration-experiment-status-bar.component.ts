import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotExperimentStatusList } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-experiments-configuration-experiment-status-bar',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-experiments-configuration-experiment-status-bar.component.html',
    styleUrls: ['./dot-experiments-configuration-experiment-status-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationExperimentStatusBarComponent {
    @Input()
    status: DotExperimentStatusList;
}
