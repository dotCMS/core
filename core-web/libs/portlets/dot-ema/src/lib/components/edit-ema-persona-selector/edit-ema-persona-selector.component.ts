import { tapResponse } from '@ngrx/component-store';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotPageApiService } from '../../services/dot-page-api.service';
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
        FormsModule,
        AutoCompleteModule,
        FormsModule
    ],

    templateUrl: './edit-ema-persona-selector.component.html',
    styleUrls: ['./edit-ema-persona-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaPersonaSelectorComponent implements OnInit {
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly personalizeService = inject(DotPersonalizeService);
    private readonly pageApiService = inject(DotPageApiService);
    private personas: DotPersona[];

    @Input() pageID: string;
    @Input() selectedPersona: DotPersona;

    @Output() selected: EventEmitter<DotPersona> = new EventEmitter();

    filteredPersonas: DotPersona[] = [];

    ngOnInit(): void {
        this.pageApiService
            .getPersonas({
                pageID: this.pageID,
                // TODO: when we update to PrimeNG 17 we can do this async
                perPage: 5000
            })
            .pipe(
                tapResponse(
                    (res) => {
                        this.personas = res.data;
                    },
                    () => {
                        this.personas = [];
                    }
                )
            )
            .subscribe();
    }

    onSelect(value: DotPersona) {
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
                    this.selected.emit(this.selectedPersona);
                }
            });
        }
    }

    onFilter({ query }: { query: string }) {
        if (!query.length) this.filteredPersonas = [...this.personas];
        else
            this.filteredPersonas = this.personas.filter((persona) =>
                persona.title.toLowerCase().includes(query.toLowerCase())
            );
    }
}
