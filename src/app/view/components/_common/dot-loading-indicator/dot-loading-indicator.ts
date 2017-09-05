import {Component, Input, ViewEncapsulation} from '@angular/core';
import {LoggerService} from 'dotcms-js/dotcms-js';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-loading-indicator',
    styleUrls: ['./dot-loading-indicator.scss'],
    templateUrl: 'dot-loading-indicator.html'
})

export class DotLoadingIndicator {
    @Input() fullscreen: boolean;

    constructor(private loggerService: LoggerService) {

        this.loggerService.debug(this.fullscreen);
    }
}
