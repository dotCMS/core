import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';

import { DotPersona } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-persona-selected-item',
    templateUrl: './dot-persona-selected-item.component.html',
    styleUrls: ['./dot-persona-selected-item.component.scss'],
    standalone: false
})
export class DotPersonaSelectedItemComponent {
    @Input() persona: DotPersona;

    @Input() isEditMode = false;
    @Input() readonly = false;

    @Input()
    @HostBinding('class.disabled')
    disabled: boolean;

    @Output() selected = new EventEmitter<MouseEvent>();
}
