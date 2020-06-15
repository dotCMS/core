import { Injectable } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { DotEventsService } from '@services/dot-events/dot-events.service';

/**
 * Service to provide configurations for Global Messages.
 * @export
 * @class DotGlobalMessageService
 */
@Injectable()
export class DotGlobalMessageService {
    private messageLife = 3000;

    constructor(
        private dotMessageService: DotMessageService,
        private dotEventsService: DotEventsService
    ) {}

    /**
     * Display text messages.
     * @param string message
     */
    display(message?: string): void {
        this.dotEventsService.notify('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.loaded'),
            life: this.messageLife
        });
    }

    /**
     * Display text messages with a loading indicator.
     * @param string message
     */
    loading(message?: string): void {
        this.dotEventsService.notify('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.loading'),
            type: 'loading'
        });
    }

    /**
     * Display text messages with success configuration.
     * @param string message
     */
    success(message?: string): void {
        this.dotEventsService.notify('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.saved'),
            type: 'success',
            life: this.messageLife
        });
    }

    /**
     * Display text messages with error configuration.
     * @param string message
     */
    error(message?: string): void {
        // TODO: Define the behaior of error messages.
        this.dotEventsService.notify('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.error'),
            type: 'error',
            life: this.messageLife
        });
    }
}
