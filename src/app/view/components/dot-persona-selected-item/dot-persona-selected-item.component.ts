import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';
import { DotPersona } from '@models/dot-persona/dot-persona.model';

@Component({
    selector: 'dot-persona-selected-item',
    templateUrl: './dot-persona-selected-item.component.html',
    styleUrls: ['./dot-persona-selected-item.component.scss']
})
export class DotPersonaSelectedItemComponent {
    @Input()
    persona: DotPersona;
    @Input()
    label: string;
    @Output()
    selected = new EventEmitter<MouseEvent>();

    constructor() {}

    @HostListener('click', ['$event'])
    onClick($event: MouseEvent) {
        $event.stopPropagation();
        this.selected.emit($event);
    }
}
