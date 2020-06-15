import { Component, Input, Output, EventEmitter, HostBinding } from '@angular/core';
import { DotPersona } from '@models/dot-persona/dot-persona.model';

@Component({
    selector: 'dot-persona-selected-item',
    templateUrl: './dot-persona-selected-item.component.html',
    styleUrls: ['./dot-persona-selected-item.component.scss']
})
export class DotPersonaSelectedItemComponent {
    @Input() persona: DotPersona;

    @Input() isEditMode = false;

    @Input()
    @HostBinding('class.disabled')
    disabled: boolean;

    @Output() selected = new EventEmitter<MouseEvent>();

    constructor() {}
}
