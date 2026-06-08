import { createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotPublishingQueueService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotPublishingQueueShellComponent } from './dot-publishing-queue-shell.component';

describe('DotPublishingQueueShellComponent', () => {
    let spectator: Spectator<DotPublishingQueueShellComponent>;

    const createComponent = createComponentFactory({
        component: DotPublishingQueueShellComponent,
        providers: [
            provideHttpClient(),
            mockProvider(DotPublishingQueueService, {
                listPublishingJobs: jest
                    .fn()
                    .mockReturnValue(
                        of({
                            entity: [],
                            pagination: { currentPage: 1, perPage: 10, totalEntries: 0 }
                        })
                    ),
                getBundleAssets: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotHttpErrorManagerService),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('renders toolbar and page', () => {
        expect(spectator.query('dot-publishing-queue-toolbar')).toBeTruthy();
        expect(spectator.query('dot-publishing-queue-page')).toBeTruthy();
    });
});
