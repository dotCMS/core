import {
    Component,
    Input,
    ViewEncapsulation,
    inject,
    ChangeDetectionStrategy
} from '@angular/core';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotSpinnerComponent } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-loading-indicator',
    styleUrls: ['./dot-loading-indicator.component.scss'],
    templateUrl: 'dot-loading-indicator.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [DotSpinnerComponent]
})
export class DotLoadingIndicatorComponent {
    dotLoadingIndicatorService = inject(DotLoadingIndicatorService);

    @Input()
    fullscreen!: boolean;

    @Input()
    set show(status: ComponentStatus) {
        if (status === ComponentStatus.LOADING || status === ComponentStatus.INIT) {
            this.dotLoadingIndicatorService.show();
        } else {
            this.dotLoadingIndicatorService.hide();
        }
    }
}
