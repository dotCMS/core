import { Component, Input, ViewEncapsulation } from '@angular/core';
import { LoadingState } from '@dotcms/dotcms-models';
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

    @Input()
    set show(status: LoadingState) {
        if (status === LoadingState.LOADING || status === LoadingState.INIT) {
            this.dotLoadingIndicatorService.show();
        } else {
            this.dotLoadingIndicatorService.hide();
        }
    }

    constructor(public dotLoadingIndicatorService: DotLoadingIndicatorService) {}
}
