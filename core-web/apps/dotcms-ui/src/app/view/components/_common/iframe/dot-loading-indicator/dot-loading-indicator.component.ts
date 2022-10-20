import { Component, Input, ViewEncapsulation } from '@angular/core';
import { DotLoadingIndicatorService } from './dot-loading-indicator.service';
import { LoadingState } from '@portlets/shared/models/shared-models';

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
