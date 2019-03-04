import { DotCMSEventsParams } from '../models';

/**
 * Emit custom events for the DotCMS Edit Mode
 *
 */
export class DotApiEvent {
    emit({ name, data }: DotCMSEventsParams): void {
        const customEvent = window.top.document.createEvent('CustomEvent');
        customEvent.initCustomEvent('ng-event', false, false, {
            name: name,
            data: data
        });
        window.top.document.dispatchEvent(customEvent);
    }
}
