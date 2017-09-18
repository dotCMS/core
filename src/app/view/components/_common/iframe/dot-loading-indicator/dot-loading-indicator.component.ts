import { Component, Input, ViewEncapsulation } from '@angular/core';

@Component({
    encapsulation: ViewEncapsulation.Emulated,
    selector: 'dot-loading-indicator',
    styleUrls: ['./dot-loading-indicator.component.scss'],
    templateUrl: 'dot-loading-indicator.component.html'
})
export class DotLoadingIndicatorComponent {
    @Input() fullscreen: boolean;

    constructor() {}
}
