import { Component, Input, ViewEncapsulation } from '@angular/core';
import { DotLoadingIndicatorService } from './dot-loading-indicator.service';

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
}
