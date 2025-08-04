import { Component, Input, ViewEncapsulation, inject } from '@angular/core';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-loading-indicator',
    styleUrls: ['./dot-loading-indicator.component.scss'],
    templateUrl: 'dot-loading-indicator.component.html',
    standalone: false
})
export class DotLoadingIndicatorComponent {
    dotLoadingIndicatorService = inject(DotLoadingIndicatorService);

    @Input()
    fullscreen: boolean;

    @Input()
    set show(status: ComponentStatus) {
        if (status === ComponentStatus.LOADING || status === ComponentStatus.INIT) {
            this.dotLoadingIndicatorService.show();
        } else {
            this.dotLoadingIndicatorService.hide();
        }
    }
}
