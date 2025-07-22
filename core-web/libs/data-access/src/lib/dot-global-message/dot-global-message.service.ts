import { Injectable, inject } from '@angular/core';

import { DotGlobalMessage } from '@dotcms/dotcms-models';

import { DotEventsService } from '../dot-events/dot-events.service';
import { DotMessageService } from '../dot-messages/dot-messages.service';

/**
 * Service to provide configurations for Global Messages.
 * @export
 * @class DotGlobalMessageService
 */
@Injectable()
export class DotGlobalMessageService {
    private dotMessageService = inject(DotMessageService);
    private dotEventsService = inject(DotEventsService);

    private messageLife = 3000;

    /**
     * Display text messages.
     * @param string message
     */
    display(message?: string): void {
        this.dotEventsService.notify<DotGlobalMessage>('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.loaded'),
            life: this.messageLife
        });
    }

    /**
     * Display text messages with custom time.
     * @param string message
     * @param number [time]
     * @memberof DotGlobalMessageService
     */
    customDisplay(message: string, time?: number) {
        this.dotEventsService.notify<DotGlobalMessage>('dot-global-message', {
            value: message,
            life: time
        });
    }

    /**
     * Display text messages with a loading indicator.
     * @param string message
     */
    loading(message?: string): void {
        this.dotEventsService.notify<DotGlobalMessage>('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.loading'),
            type: 'loading'
        });
    }

    /**
     * Display text messages with success configuration.
     * @param string message
     */
    success(message?: string): void {
        this.dotEventsService.notify<DotGlobalMessage>('dot-global-message', {
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
        this.dotEventsService.notify<DotGlobalMessage>('dot-global-message', {
            value: message ? message : this.dotMessageService.get('dot.common.message.error'),
            type: 'error',
            life: this.messageLife
        });
    }
}
