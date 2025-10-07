import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

import { DotPersona } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-persona-selector-option',
    templateUrl: './dot-persona-selector-option.component.html',
    styleUrls: ['./dot-persona-selector-option.component.scss'],
    standalone: false
})
export class DotPersonaSelectorOptionComponent {
    @Input() canDespersonalize = true;

    @Input() persona: DotPersona;

    @Input() selected: boolean;

    @Output() switch = new EventEmitter<DotPersona>();

    @Output() delete = new EventEmitter<DotPersona>();

    @HostListener('click', ['$event'])
    onClick(_$event: MouseEvent) {
        this.switch.emit(this.persona);
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
