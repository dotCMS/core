import { NgStyle } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-spinner',
    templateUrl: './dot-spinner.component.html',
    styleUrls: ['./dot-spinner.component.scss'],
    imports: [NgStyle]
})
export class DotSpinnerComponent {
    @Input() borderSize = '';
    @Input() size = '';
}
