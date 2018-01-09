import { Injectable } from '@angular/core';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotEventsService } from '../../../../api/services/dot-events/dot-events.service';

/**
 * Service to provide configurations for Global Messages.
 * @export
 * @class DotGlobalMessageService
 */
@Injectable()
export class DotGlobalMessageService {
    constructor(public dotMessageService: DotMessageService, private dotEventsService: DotEventsService) {
        this.dotMessageService
            .getMessages(['dot.common.message.loading', 'dot.common.message.loaded', 'dot.common.message.error'])
            .subscribe();
    }

    /**
     * Display text messages.
     * @param {string} message
     */
    display(message?: string): void {
        this.dotEventsService.notify('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.loaded'),
            life: 3000
        });
    }

    /**
     * Display text messages with a loading indicator.
     * @param {string} message
     */
    loading(message?: string): void {
        this.dotEventsService.notify('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.loading'),
            type: 'loading'
        });
    }

    /**
     * Display text messages with error configuration.
     * @param {string} message
     */
    error(message?: string): void {
        // TODO: Define the behaior of error messages.
        this.dotEventsService.notify('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.error'),
            life: 3000
        });
    }
}
