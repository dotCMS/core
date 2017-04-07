import {Component, Input, ViewEncapsulation} from '@angular/core';
import {LoggerService} from '../../../../api/services/logger.service';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-loading-indicator',
    styles: [require('./dot-loading-indicator.scss')],
    templateUrl: 'dot-loading-indicator.html'
})

export class DotLoadingIndicator {
    @Input() fullscreen: boolean;

    constructor(private loggerService: LoggerService) {

        this.loggerService.debug(this.fullscreen);
    }
}
