import { Component, Input, ViewEncapsulation } from '@angular/core';

import { ComponentStatus } from '@dotcms/dotcms-models';
import { DotLoadingIndicatorService } from '@dotcms/utils';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-loading-indicator',
    styleUrls: ['./dot-loading-indicator.component.scss'],
    templateUrl: 'dot-loading-indicator.component.html'
})
export class DotLoadingIndicatorComponent {
    @Input()
    fullscreen: boolean;

    constructor(public dotLoadingIndicatorService: DotLoadingIndicatorService) {}

    @Input()
    set show(status: ComponentStatus) {
        if (status === ComponentStatus.LOADING || status === ComponentStatus.INIT) {
            this.dotLoadingIndicatorService.show();
        } else {
            this.dotLoadingIndicatorService.hide();
        }
    }
}
