import { Component, Injector, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';

import { DotSidePanelOutletComponent } from '@dotcms/ui';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';

/**
 * Where the admin's side panels render, with the toast for the panels the legacy portlets' events
 * open. It sits beside the router outlet, not above it.
 *
 * It provides PrimeNG's `MessageService` itself instead of the admin root because a component's
 * provider wins over a route's for everything rendered under that component. On the admin root,
 * an ancestor of every routed portlet, it hid the one the page editor (UVE) provides on its route,
 * and UVE's toast and the admin root's both showed every UVE message (#38303).
 */
@Component({
    selector: 'dot-side-panel-host',
    imports: [DotSidePanelOutletComponent, ToastModule],
    providers: [MessageService],
    templateUrl: './dot-side-panel-host.component.html'
})
export class DotSidePanelHostComponent {
    /**
     * Hands the custom-event handler this component's injector, so the panels the legacy portlets'
     * events open live under it and toast here.
     */
    constructor() {
        inject(DotCustomEventHandlerService).setInjector(inject(Injector));
    }
}
