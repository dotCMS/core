import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotcmsEventsService, DotEventData } from 'dotcms-js';
import { map, takeUntil, filter } from 'rxjs/operators';
import { DotRouterService } from '@services/dot-router/dot-router.service';


/**
 *Handle message send by the Backend, this message are sended as Event through the {@link DotcmsEventsService}
 *
 * @export
 * @class DotMessageDisplayService
 */
@Injectable()
export class DotMessageDisplayService {

    private messages$: Observable<Dot.Message>;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        dotcmsEventsService: DotcmsEventsService,
        private dotRouterService: DotRouterService) {

        this.messages$ = dotcmsEventsService.subscribeTo('MESSAGE').pipe(
            takeUntil(this.destroy$),
            map((messageEvent: DotEventData) => <Dot.Message>messageEvent.data),
            filter((dotMessage: Dot.Message) => this.hasPortletIdList(dotMessage))
        );
    }

    /**
     *Allow subscribe to recive new message
     *
     * @returns {Observable<DotMessage>}
     * @memberof DotMessageDisplayService
     */
    messages(): Observable<Dot.Message> {
        return this.messages$;
    }


    /**
     *Unsubscribe to service's Observable
     *
     * @memberof DotMessageDisplayService
     */
    unsubscribe(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    private hasPortletIdList(dotMessage: Dot.Message): boolean {
        return !dotMessage.portletIdList || !dotMessage.portletIdList.length ||
            dotMessage.portletIdList.includes(this.dotRouterService.currentPortlet.id);
    }
}
