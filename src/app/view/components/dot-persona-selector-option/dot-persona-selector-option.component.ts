import { Component, EventEmitter, Input, OnInit, Output, HostListener } from '@angular/core';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { DotMessageService } from '@services/dot-messages-service';
import { take, map } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Component({
    selector: 'dot-persona-selector-option',
    templateUrl: './dot-persona-selector-option.component.html',
    styleUrls: ['./dot-persona-selector-option.component.scss']
})
export class DotPersonaSelectorOptionComponent implements OnInit {
    @Input()
    persona: DotPersona;

    @Input()
    selected: boolean;

    @Output()
    change = new EventEmitter<DotPersona>();

    @Output()
    delete = new EventEmitter<DotPersona>();

    deleteLabel$: Observable<string>;

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit() {
        /*
            Looks like because we're passing this as a template the requets to get
            the message key is not happening as expected, setTimeout hack it to work.
        */
        setTimeout(() => {
            this.deleteLabel$ = this.dotMessageService
                .getMessages(['modes.persona.personalized'])
                .pipe(
                    take(1),
                    map(
                        (messages: { [key: string]: string }) =>
                            messages['modes.persona.personalized']
                    )
                );
        }, 10);
    }

    @HostListener('click', ['$event'])
    onClick(_$event: MouseEvent) {
        this.change.emit(this.persona);
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
