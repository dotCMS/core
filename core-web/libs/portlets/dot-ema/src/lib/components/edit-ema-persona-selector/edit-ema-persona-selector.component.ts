import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-edit-ema-persona-selector',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        AvatarModule,
        OverlayPanelModule,
        DotAvatarDirective,
        DotMessagePipe,
        ListboxModule,
        BadgeModule
    ],
    templateUrl: './edit-ema-persona-selector.component.html',
    styleUrls: ['./edit-ema-persona-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaPersonaSelectorComponent {
    // I need to check the personalize state to open a confirmation dialog.
    // Do I need to make changes in nextJS to handle the persona?
    // Test the hell out of it to be sure it does works.

    @Input() personas: DotPersona[];
    @Input() personaID: string;

    @Output() selected: EventEmitter<string> = new EventEmitter();

    get selectedPersona() {
        return this.personas.find((persona) => persona.identifier === this.personaID);
    }

    onChange({ value }: { event: Event; value: DotPersona }) {
        this.selected.emit(value.identifier);
    }
}
