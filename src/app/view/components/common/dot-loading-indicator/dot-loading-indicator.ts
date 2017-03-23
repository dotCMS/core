import {Component, Input, ViewEncapsulation} from '@angular/core';
import {LoggerService} from '../../../../api/services/logger.service';

@Component({
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-loading-indicator',
    styleUrls: ['dot-loading-indicator.css'],
    templateUrl: ['dot-loading-indicator.html']
})

export class DotLoadingIndicator {
    @Input() fullscreen: boolean;

    constructor(private loggerService: LoggerService) {

        this.loggerService.debug(this.fullscreen)
    }
}
