import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotPublishingQueueToolbarComponent } from '../components/dot-publishing-queue-toolbar/dot-publishing-queue-toolbar.component';
import { DotPublishingQueuePageComponent } from '../dot-publishing-queue-page/dot-publishing-queue-page.component';
import { DotPublishingQueueStore } from '../dot-publishing-queue-page/store/dot-publishing-queue.store';

@Component({
    selector: 'dot-publishing-queue-shell',
    standalone: true,
    imports: [DotPublishingQueueToolbarComponent, DotPublishingQueuePageComponent],
    providers: [DotPublishingQueueStore, DialogService],
    template: `
        <dot-publishing-queue-toolbar />
        <dot-publishing-queue-page />
    `,
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0 block' }
})
export class DotPublishingQueueShellComponent {}
