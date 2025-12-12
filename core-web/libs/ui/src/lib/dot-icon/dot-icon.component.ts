import { Component, Input } from '@angular/core';

/**
 * The DotIconComponent uses google material design icons
 * https://material.io/tools/icons
 * @export
 * @class DotIconComponent
 * @deprecated
 */
@Component({
    selector: 'dot-icon',
    styleUrls: ['./dot-icon.component.scss'],
    templateUrl: './dot-icon.component.html'
})
export class DotIconComponent {
    @Input() name: string;
    @Input() size: number;
}
