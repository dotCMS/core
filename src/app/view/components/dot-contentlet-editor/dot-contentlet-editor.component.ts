import { Component, EventEmitter, Output } from '@angular/core';

@Component({
    selector: 'dot-contentlet-editor',
    templateUrl: './dot-contentlet-editor.component.html',
    styleUrls: ['./dot-contentlet-editor.component.scss']
})
export class DotContentletEditorComponent {
    @Output() close: EventEmitter<any> = new EventEmitter();

    constructor() {}
}
