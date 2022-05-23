import { Component, Input } from '@angular/core';

@Component({
    selector: 'dot-field-helper',
    templateUrl: './dot-field-helper.component.html',
    styleUrls: ['./dot-field-helper.component.scss']
})
export class DotFieldHelperComponent {
    @Input() message: string;
}
