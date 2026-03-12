import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { DotHttpErrorManagerService, DotMessageService, DotOsgiService } from '@dotcms/data-access';

import { DotPluginsListComponent } from './dot-plugins-list.component';
import { DotPluginsListStore } from './store/dot-plugins-list.store';

describe('DotPluginsListComponent', () => {
    let spectator: Spectator<DotPluginsListComponent>;

    const createComponent = createComponentFactory({
        component: DotPluginsListComponent,
        providers: [
            DotPluginsListStore,
            mockProvider(DotMessageService, { get: (key: string) => key }),
            mockProvider(DotOsgiService, {
                getInstalledBundles: jest.fn().mockReturnValue(of({ entity: [] })),
                getAvailablePlugins: jest.fn().mockReturnValue(of({ entity: [] }))
            }),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ConfirmationService),
            mockProvider(MessageService, { add: jest.fn() })
        ],
        shallow: true
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    it('should have toolbar with upload, refresh and restart buttons', () => {
        expect(spectator.query('[data-testid="plugins-upload-btn"]')).toBeTruthy();
        expect(spectator.query('[data-testid="plugins-refresh-btn"]')).toBeTruthy();
        expect(spectator.query('[data-testid="plugins-restart-btn"]')).toBeTruthy();
    });

    it('should have plugins table', () => {
        expect(spectator.query('[data-testid="plugins-table"]')).toBeTruthy();
    });
});
