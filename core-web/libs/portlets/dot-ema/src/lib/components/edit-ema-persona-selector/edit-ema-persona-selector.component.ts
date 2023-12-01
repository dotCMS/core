import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DEFAULT_PERSONA_ID } from '../../shared/consts';

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
        BadgeModule,
        ConfirmDialogModule,
        FormsModule
    ],

    templateUrl: './edit-ema-persona-selector.component.html',
    styleUrls: ['./edit-ema-persona-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaPersonaSelectorComponent {
    // I need to trigger a service to update the persona in the backend.
    // Do I need to make changes in nextJS to handle the persona?
    // Test the hell out of it to be sure it does works.

    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly personalizeService = inject(DotPersonalizeService);

    @Input() personas: DotPersona[];
    @Input() pageID: string;
    @Input() selectedPersona: DotPersona;

    @Output() selected: EventEmitter<DotPersona> = new EventEmitter();

    onSelect({ value }: { event: Event; value: DotPersona }) {
        if (value.identifier === this.selectedPersona.identifier) return;

        if (value.identifier === DEFAULT_PERSONA_ID || value.personalized) {
            this.selected.emit(value);
        } else {
            this.confirmationService.confirm({
                header: this.dotMessageService.get('editpage.personalization.confirm.header'),
                message: this.dotMessageService.get(
                    'editpage.personalization.confirm.message',
                    value.name
                ),
                acceptLabel: this.dotMessageService.get('dot.common.dialog.accept'),
                rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
                accept: () => {
                    this.selected.emit(value);
                    this.personalizeService.personalized(this.pageID, value.keyTag).subscribe(); // This does a take 1 under the hood
                },
                reject: () => {
                    this.selected.emit(this.selectedPersona); // The styles are not getting updated
                }
            });
        }
    }
}
