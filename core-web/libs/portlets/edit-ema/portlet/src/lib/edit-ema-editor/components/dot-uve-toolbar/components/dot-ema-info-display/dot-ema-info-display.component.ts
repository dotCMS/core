import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { InfoOptions } from '../../../../../shared/models';
import { UVEStore } from '../../../../../store/dot-uve.store';

@Component({
    selector: 'dot-ema-info-display',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-ema-info-display.component.html',
    styleUrls: ['./dot-ema-info-display.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaInfoDisplayComponent {
    protected readonly uveStore = inject(UVEStore);
    protected readonly router = inject(Router);

    $options = input<InfoOptions>(undefined, { alias: 'options' });

    /**
     * Handle the action based on the options
     *
     * @protected
     * @param {InfoOptions} options
     * @memberof DotEmaInfoDisplayComponent
     */
    protected handleAction() {
        const optionId = this.$options().id;

        if (optionId === 'device' || optionId === 'socialMedia') {
            this.uveStore.clearDeviceAndSocialMedia();

            return;
        }

        const currentExperiment = this.uveStore.experiment();

        this.router.navigate(
            [
                '/edit-page/experiments/',
                currentExperiment.pageId,
                currentExperiment.id,
                'configuration'
            ],
            {
                queryParams: {
                    mode: null,
                    variantName: null,
                    experimentId: null
                },
                queryParamsHandling: 'merge'
            }
        );
    }
}
