import { Component } from '@angular/core';
import { ControlContainer } from '@angular/forms';

@Component({
    selector: 'dot-layout-designer',
    templateUrl: './dot-layout-designer.component.html',
    styleUrls: ['./dot-layout-designer.component.scss']
})
export class DotLayoutDesignerComponent {
    constructor(public group: ControlContainer) {}
}
