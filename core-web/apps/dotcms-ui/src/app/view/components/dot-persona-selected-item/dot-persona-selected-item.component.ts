import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-persona-selected-item',
    templateUrl: './dot-persona-selected-item.component.html',
    styleUrls: ['./dot-persona-selected-item.component.scss'],
    imports: [AvatarModule, BadgeModule, TooltipModule, DotMessagePipe]
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
