import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Listbox, ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { catchError, map } from 'rxjs/operators';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotPageApiService } from '../../../services/dot-page-api.service';

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
        ConfirmDialogModule,
        FormsModule,
        ChipModule
    ],
    templateUrl: './edit-ema-persona-selector.component.html',
    styleUrls: ['./edit-ema-persona-selector.component.scss']
})
export class EditEmaPersonaSelectorComponent implements OnInit, AfterViewInit, OnChanges {
    @ViewChild('listbox') listbox: Listbox;

    private readonly pageApiService = inject(DotPageApiService);
    personas: DotPersona[];

    @Input() pageId: string;
    @Input() value: DotPersona;

    @Output() selected: EventEmitter<DotPersona & { pageId: string }> = new EventEmitter();
    @Output() despersonalize: EventEmitter<DotPersona & { pageId: string; selected: boolean }> =
        new EventEmitter();

    ngOnInit(): void {
        this.fetchPersonas();
    }

    ngOnChanges(): void {
        // To select the correct persona when the page is reloaded with no queryParams
        if (this.listbox) {
            this.resetValue();
        }
    }

    ngAfterViewInit(): void {
        this.resetValue();
    }

    /**
     * Handle the change of the persona
     *
     * @param {{ value: DotPersona }} { value }
     * @memberof EditEmaPersonaSelectorComponent
     */
    onSelect({ value }: { value: DotPersona }) {
        if (value.identifier !== this.value.identifier) {
            this.selected.emit({
                ...value,
                pageId: this.pageId
            });
        }
    }

    /**
     * Reset the value of the listbox
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    resetValue(): void {
        this.listbox.value = this.value;
        this.listbox.cd.detectChanges();
    }

    /**
     * Handle the remove of the persona
     *
     * @param {MouseEvent} event
     * @param {DotPersona} persona
     * @memberof EditEmaPersonaSelectorComponent
     */
    onRemove(event: MouseEvent, persona: DotPersona, selected: boolean) {
        event.stopPropagation();

        this.despersonalize.emit({
            ...persona,
            selected,
            pageId: this.pageId
        });
    }

    /**
     * Fetch personas from the API
     *
     * @memberof EditEmaPersonaSelectorComponent
     */
    fetchPersonas() {
        this.pageApiService
            .getPersonas({
                pageId: this.pageId,
                perPage: 5000
            })
            .pipe(
                map((res) => res.data),
                catchError(() => of([]))
            )
            .subscribe((personas) => (this.personas = personas));
    }
}
