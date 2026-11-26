import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { signal } from '@angular/core';

import { MessageService } from 'primeng/api';
import { Toast } from 'primeng/toast';

import { DotSidePanelOutletComponent, DotSidePanelService } from '@dotcms/ui';

import { DotSidePanelHostComponent } from './dot-side-panel-host.component';

import { DotCustomEventHandlerService } from '../../../api/services/dot-custom-event-handler/dot-custom-event-handler.service';

/**
 * The side panels opened from the admin root (the legacy portlets' events, the deep links) and
 * the toasts they raise live here, in a sibling of the routed portlets rather than on the admin
 * root itself. A `MessageService` provided on the admin root would shadow the one UVE provides on
 * its route, and every UVE toast would render twice (#38303).
 */
describe('DotSidePanelHostComponent', () => {
    let spectator: Spectator<DotSidePanelHostComponent>;

    const createComponent = createComponentFactory({
        component: DotSidePanelHostComponent,
        providers: [
            // Stands in for a MessageService an ancestor or the root would provide.
            MessageService,
            mockProvider(DotCustomEventHandlerService),
            mockProvider(DotSidePanelService, { panels: signal([]) })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the side panel outlet', () => {
        expect(spectator.query(DotSidePanelOutletComponent)).not.toBeNull();
    });

    it('should render a toast', () => {
        expect(spectator.query(Toast)).not.toBeNull();
    });

    it('should provide a MessageService of its own', () => {
        const own = spectator.inject(MessageService, true);
        const root = spectator.inject(MessageService);

        // Compared by identity: printing a MessageService on failure floods the output.
        expect(own === root).toBe(false);
    });

    /**
     * The legacy portlets' events reach the root `DotCustomEventHandlerService`, whose panels must
     * live under this component so they find its MessageService and its toast.
     */
    it('should register an injector that resolves its own MessageService with the custom-event handler', () => {
        const setInjector = vi.mocked(spectator.inject(DotCustomEventHandlerService).setInjector);

        expect(setInjector).toHaveBeenCalledTimes(1);
        const [registered] = setInjector.mock.calls[0];
        // Compared by identity: printing an injector runs the worker out of memory.
        expect(
            registered.get(MessageService, null) === spectator.inject(MessageService, true)
        ).toBe(true);
    });
});
