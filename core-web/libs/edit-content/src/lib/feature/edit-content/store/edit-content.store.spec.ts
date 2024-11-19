import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';

import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotWorkflowsActionsService,
    DotWorkflowService
} from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotEditContentStore } from './edit-content.store';

import { DotEditContentService } from '../../../services/dot-edit-content.service';

describe('DotEditContentStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotEditContentStore>>;
    let store: InstanceType<typeof DotEditContentStore>;

    const createService = createServiceFactory({
        service: DotEditContentStore,
        providers: [
            {
                provide: ActivatedRoute,
                useValue: {
                    get snapshot() {
                        return { params: {} };
                    }
                }
            },
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotEditContentService),
            mockProvider(DotContentTypeService),
            mockProvider(DotWorkflowsActionsService),
            mockProvider(DotWorkflowService),
            mockProvider(DotWorkflowActionsFireService),
            mockProvider(MessageService),
            mockProvider(DotMessageService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should create store with initial state', () => {
        expect(store.state()).toBe(ComponentStatus.INIT);
        expect(store.error()).toBeNull();
    });

    it('should compose with all required features', () => {
        // Verify features are composed into the store
        expect(store.contentType).toBeDefined();
        expect(store.contentlet).toBeDefined();
        expect(store.showSidebar).toBeDefined();
        expect(store.showWorkflowActions).toBeDefined();
        expect(store.information).toBeDefined();
    });
});
