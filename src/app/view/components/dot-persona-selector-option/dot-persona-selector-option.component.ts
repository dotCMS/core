import { Component, EventEmitter, Input, OnInit, Output, HostListener } from '@angular/core';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';

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

    messagesKey: { [key: string]: string } = {};

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['modes.persona.personalized'])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
    }

    @HostListener('click', ['$event'])
    onClick($event: MouseEvent) {
        $event.stopPropagation();
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
