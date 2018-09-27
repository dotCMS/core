import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { DotPersonasService } from '@services/dot-personas/dot-personas.service';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { DotMessageService } from '@services/dot-messages-service';
import { map, take } from 'rxjs/operators';

@Component({
    selector: 'dot-persona-selector',
    templateUrl: './dot-persona-selector.component.html',
    styleUrls: ['./dot-persona-selector.component.scss']
})
export class DotPersonaSelectorComponent implements OnInit {
    @Input()
    value: DotPersona;
    @Output()
    selected = new EventEmitter<DotPersona>();

    options: DotPersona[];

    constructor(
        private dotPersonasService: DotPersonasService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['modes.persona.no.persona'])
            .pipe(take(1))
            .subscribe(() => {
                this.dotPersonasService
                    .get()
                    .pipe(
                        map((personas: DotPersona[]) =>
                            this.setOptions(
                                this.dotMessageService.get('modes.persona.no.persona'),
                                personas
                            )
                        )
                    )
                    .subscribe((personas: DotPersona[]) => {
                        this.options = personas;
                    });
            });
    }

    /**
     * Track changes in the dropwdow
     * @param {DotPersona} persona
     */
    change(persona: DotPersona) {
        this.selected.emit(persona);
    }

    private setOptions(message: string, personas: DotPersona[]): DotPersona[] {
        return [{ name: message, identifier: '0' }, ...personas];
    }
}
