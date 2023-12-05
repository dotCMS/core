import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Listbox, ListboxModule } from 'primeng/listbox';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotPersona } from '@dotcms/dotcms-models';
import { DotAvatarDirective, DotMessagePipe } from '@dotcms/ui';

import { DotPageApiService } from '../../services/dot-page-api.service';
import { catchError, map } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

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
        FormsModule
    ],
    templateUrl: './edit-ema-persona-selector.component.html',
    styleUrls: ['./edit-ema-persona-selector.component.scss']
})
export class EditEmaPersonaSelectorComponent implements OnInit, AfterViewInit {
    @ViewChild('listbox') listbox: Listbox;

    private readonly pageApiService = inject(DotPageApiService);
    personas$: Observable<DotPersona[]>;

    @Input() pageID: string;
    @Input() value: DotPersona;

    @Output() selected: EventEmitter<DotPersona & { pageID: string }> = new EventEmitter();

    ngOnInit(): void {
        this.personas$ = this.pageApiService
            .getPersonas({
                pageID: this.pageID,
                // TODO: when we update to PrimeNG 17 we can do this async
                perPage: 5000
            })
            .pipe(
                map((res) => res.data),
                catchError(() => of([]))
            );
    }

    ngAfterViewInit(): void {
        this.listbox.value = this.value;
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
                pageID: this.pageID
            });
        }
    }
}
