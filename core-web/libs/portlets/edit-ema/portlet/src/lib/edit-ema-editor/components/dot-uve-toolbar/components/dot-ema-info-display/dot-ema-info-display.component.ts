import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { InfoOptions } from '../../../../../shared/models';

@Component({
    selector: 'dot-ema-info-display',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './dot-ema-info-display.component.html',
    styleUrls: ['./dot-ema-info-display.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaInfoDisplayComponent {
    // Inputs - data down from container
    $options = input<InfoOptions>(undefined, { alias: 'options' });

    // Outputs - events up to container
    actionClicked = output<string>();

    /**
     * Handle the action by emitting event to parent container
     * Parent will handle navigation or store dispatch based on option ID
     *
     * @public
     * @memberof DotEmaInfoDisplayComponent
     */
    handleAction() {
        const optionId = this.$options()?.id;

        if (optionId) {
            // Emit option ID to let parent handle the action
            this.actionClicked.emit(optionId);
        }
    }
}
