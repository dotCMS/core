import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
    selector: 'dot-layout-designer',
    templateUrl: './dot-layout-designer.component.html',
    styleUrls: ['./dot-layout-designer.component.scss']
})
export class DotLayoutDesignerComponent {
    @Input() group: FormGroup;

    constructor() {}
}
