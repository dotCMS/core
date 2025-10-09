import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-spinner',
    templateUrl: './dot-spinner.component.html',
    styleUrls: ['./dot-spinner.component.scss'],
    imports: [CommonModule],
    standalone: true
})
export class DotSpinnerComponent {
    @Input() borderSize = '';
    @Input() size = '';
}
