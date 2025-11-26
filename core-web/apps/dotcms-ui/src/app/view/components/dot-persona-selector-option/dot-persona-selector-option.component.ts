import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-persona-selector-option',
    templateUrl: './dot-persona-selector-option.component.html',
    styleUrls: ['./dot-persona-selector-option.component.scss'],
    imports: [
        CommonModule,
        AvatarModule,
        BadgeModule,
        ButtonModule,
        DotMessagePipe,
        DotAvatarDirective
    ]
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
