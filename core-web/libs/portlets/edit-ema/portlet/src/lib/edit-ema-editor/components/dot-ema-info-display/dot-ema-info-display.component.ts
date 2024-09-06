import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-ema-info-display',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-ema-info-display.component.html',
    styleUrls: ['./dot-ema-info-display.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaInfoDisplayComponent {
    protected readonly uveStore = inject(UVEStore);
    protected readonly router = inject(Router);

    protected readonly $options = this.uveStore.$infoDisplayOptions;

    /**
     * Handle the action based on the options
     *
     * @protected
     * @param {InfoOptions} options
     * @memberof DotEmaInfoDisplayComponent
     */
    protected handleAction() {
        const options = this.$options();

        if (options.id === 'device' || options.id === 'socialMedia') {
            this.uveStore.clearDeviceAndSocialMedia();
        } else if (options.id === 'variant') {
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
}
