import { Component, EventEmitter, Input, Output, HostListener } from '@angular/core';
import { DotPersona } from '@models/dot-persona/dot-persona.model';

@Component({
    selector: 'dot-persona-selector-option',
    templateUrl: './dot-persona-selector-option.component.html',
    styleUrls: ['./dot-persona-selector-option.component.scss']
})
export class DotPersonaSelectorOptionComponent {
    @Input() canDespersonalize = true;

    @Input() persona: DotPersona;

    @Input() selected: boolean;

    @Output() change = new EventEmitter<DotPersona>();

    @Output() delete = new EventEmitter<DotPersona>();

    constructor() {}

    @HostListener('click', ['$event'])
    onClick(_$event: MouseEvent) {
        this.change.emit(this.persona);
    }

    /**
     * Emit DotPersona field to be deleted
     * @param {MouseEvent} $event
     * @memberof DotPersonaSelectorOptionComponent
     */
    deletePersonalized($event: MouseEvent) {
        $event.stopPropagation();
        this.delete.emit(this.persona);
    }
}
