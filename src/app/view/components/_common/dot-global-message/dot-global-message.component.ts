import { filter } from 'rxjs/operators';
import { Component, HostBinding, OnInit } from '@angular/core';
import { DotGlobalMessage } from '@models/dot-global-message/dot-global-message.model';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotEvent } from '@models/dot-event/dot-event';

/**
 * Set a listener to display Global Messages in the main top toolbar
 * and hold icon classes to display them with the message.
 * @export
 * @class DotGlobalMessageComponent
 */
@Component({
    selector: 'dot-global-message',
    templateUrl: './dot-global-message.component.html',
    styleUrls: ['./dot-global-message.component.scss']
})
export class DotGlobalMessageComponent implements OnInit {
    @HostBinding('class')
    get classes(): string {
        return `${this.visibility ? 'dot-global-message--visible' : ''} ${this.message.type}`;
    }

    message: DotGlobalMessage = { value: '' };

    private visibility = false;
    private icons = {
        loading: 'loading',
        success: 'check_circle',
        error: 'error',
        warning: 'warning'
    };

    constructor(private dotEventsService: DotEventsService) {}

    ngOnInit() {
        this.dotEventsService
            .listen('dot-global-message')
            .pipe(filter((event: DotEvent) => !!event.data))
            .subscribe((event: DotEvent) => {
                this.message = event.data;
                this.visibility = true;
                this.message.type = this.icons[this.message.type] || '';
                if (this.message.life) {
                    setTimeout(() => {
                        this.visibility = false;
                    }, this.message.life);
                }
            });
    }
}
