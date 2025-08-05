import { merge, Observable, Subject } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { filter, takeUntil } from 'rxjs/operators';

import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotMessage, DotMessageSeverity } from '@dotcms/dotcms-models';

import { DotRouterService } from '../dot-router/dot-router.service';

/**
 * Handle message send by the Backend, this message are sended as Event through the {@link DotcmsEventsService}
 *
 * @export
 * @class DotMessageDisplayService
 */
@Injectable()
export class DotMessageDisplayService {
    private dotRouterService = inject(DotRouterService);
    private dotcmsEventsService = inject(DotcmsEventsService);

    private messages$: Observable<DotMessage>;
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private localMessage$: Subject<DotMessage> = new Subject<DotMessage>();

    constructor() {
        const webSocketMessage = (
            this.dotcmsEventsService.subscribeTo('MESSAGE') as Observable<DotMessage>
        ).pipe(
            takeUntil<DotMessage>(this.destroy$),
            filter((data: DotMessage) => this.hasPortletIdList(data))
        );

        this.messages$ = merge(webSocketMessage, this.localMessage$);
    }

    /**
     * Allow subscribe to recive new message
     *
     * @returns {Observable<DotMessage>}
     * @memberof DotMessageDisplayService
     */
    messages(): Observable<DotMessage> {
        return this.messages$;
    }

    /**
     * Allows to send a new message
     *
     * @param {DotMessage} message
     * @memberof DotMessageDisplayService
     */
    push(message: DotMessage): void {
        /*
            We don't want to show a toast for a loading message for now, we have to come up to a
            better way to show the loading state in our UI.
            https://github.com/dotCMS/core/issues/19107
        */
        if (message.severity !== DotMessageSeverity.LOADING) {
            this.localMessage$.next(message);
        }
    }

    /**
     * Unsubscribe to service's Observable
     *
     * @memberof DotMessageDisplayService
     */
    unsubscribe(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private hasPortletIdList(dotMessage: DotMessage): boolean {
        return (
            !dotMessage.portletIdList?.length ||
            dotMessage.portletIdList.includes(this.dotRouterService.currentPortlet.id ?? '')
        );
    }
}
