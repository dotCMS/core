import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
    selector: 'dot-loop-editor',
    templateUrl: './dot-loop-editor.component.html',
    styleUrls: ['./dot-loop-editor.component.scss']
})
export class DotLoopEditorComponent {
    @Input() isEditorVisible = false;
    @Output() buttonClick = new EventEmitter();

    constructor() {
        //
    }

    /**
     * This method shows the Loop Editor Input
     *
     * @return void
     * @memberof DotLoopEditorComponent
     */
    handleClick(): void {
        this.buttonClick.emit();
    }
}
