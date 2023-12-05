import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotPageApiService } from '../../services/dot-page-api.service';

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
    styleUrls: ['./edit-ema-persona-selector.component.scss']
})
export class EditEmaPersonaSelectorComponent implements OnInit {
    @ViewChild('autoComplete') autoComplete: AutoComplete;
    private readonly pageApiService = inject(DotPageApiService);
    private personas: DotPersona[];

    @Input() pageID: string;
    @Input() value: DotPersona;

    @Output() selected: EventEmitter<DotPersona & { pageID: string }> = new EventEmitter();

    filteredPersonas: DotPersona[] = [];

    ngOnInit(): void {
        this.pageApiService
            .getPersonas({
                pageID: this.pageID,
                // TODO: when we update to PrimeNG 17 we can do this async
                perPage: 5000
            })
            .subscribe(
                (res) => {
                    this.personas = res.data;
                },
                () => {
                    this.personas = [];
                }
            );
    }

    /**
     * Handle the change of the persona
     *
     * @param {DotPersona} value
     * @memberof EditEmaPersonaSelectorComponent
     */
    onSelect(value: DotPersona) {
        if (value.identifier !== this.value.identifier) {
            this.selected.emit({
                ...value,
                pageID: this.pageID
            });
        }
    }

    /**
     * Filter the personas by the query
     *
     * @param {{ query: string }} { query }
     * @memberof EditEmaPersonaSelectorComponent
     */
    onFilter({ query }: { query: string }) {
        if (!query.length) {
            this.filteredPersonas = [...this.personas];
        } else {
            this.filteredPersonas = this.personas.filter((persona) =>
                persona.title.toLowerCase().includes(query.toLowerCase())
            );
        }
    }

    /**
     * Reset the value of the autocomplete
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    resetValue() {
        this.autoComplete.selectItem(this.value);
    }
}
