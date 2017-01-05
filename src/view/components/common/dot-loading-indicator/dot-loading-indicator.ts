import {Component, Input, ViewEncapsulation} from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    moduleId: __moduleName, // REQUIRED to use relative path in styleUrls
    selector: 'dot-loading-indicator',
    styleUrls: ['dot-loading-indicator.css'],
    templateUrl: ['dot-loading-indicator.html']
})

export class DotLoadingIndicator {
    @Input() fullscreen: boolean;

    constructor() {
        console.log(this.fullscreen)
    }
}
