import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-spinner',
    templateUrl: './dot-spinner.component.html',
    styleUrls: ['./dot-spinner.component.scss']
})
export class DotSpinnerComponent {
    @Input() borderSize = '';
    @Input() size = '';

    constructor() {}
}
