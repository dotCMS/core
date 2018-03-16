import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotPersonasService } from '../../../api/services/dot-personas/dot-personas.service';
import { DotPersona } from '../../../shared/models/dot-persona/dot-persona.model';
import { Observable } from 'rxjs/Observable';
import { DotMessageService } from '../../../api/services/dot-messages-service';

@Component({
    selector: 'dot-persona-selector',
    templateUrl: './dot-persona-selector.component.html',
    styleUrls: ['./dot-persona-selector.component.scss']
})
export class DotPersonaSelectorComponent implements OnInit {
    @Input() value: DotPersona;
    @Output() selected = new EventEmitter<DotPersona>();

    personasOptions: Observable<DotPersona[]>;

    constructor(private dotPersonasService: DotPersonasService, private dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.personasOptions = this.dotMessageService.getMessages(['modes.persona.no.persona'])
            .mergeMap((messages: string[]) =>
                this.dotPersonasService.get()
                    .map((personas: DotPersona[]) => [
                        { name: messages['modes.persona.no.persona'], identifier: '0' },
                        ...personas
                    ])
            );
    }

    /**
     * Track changes in the dropwdow
     * @param {DotPersona} persona
     */
    change(persona: DotPersona) {
        this.selected.emit(persona);
    }
}
