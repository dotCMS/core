import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'dot-spinner',
    templateUrl: './dot-spinner.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrls: ['./dot-spinner.component.scss']
})
export class DotSpinnerComponent {
    @Input() borderSize = '';
    @Input() size = '';
}
