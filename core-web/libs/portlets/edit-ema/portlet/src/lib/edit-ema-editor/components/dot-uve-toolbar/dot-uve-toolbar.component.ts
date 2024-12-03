import { ClipboardModule } from '@angular/cdk/clipboard';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';

import { UVEStore } from '../../../store/dot-uve.store';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';

@Component({
    selector: 'dot-uve-toolbar',
    standalone: true,
    imports: [
        ButtonModule,
        ToolbarModule,
        DotEmaBookmarksComponent,
        DotEmaInfoDisplayComponent,
        DotEmaRunningExperimentComponent,
        ClipboardModule
    ],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly $toolbar = this.#store.$uveToolbar;

    togglePreviewMode(preview: boolean) {
        this.#store.togglePreviewMode(preview);
    }

    triggerCopyToast() {
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('Copied'),
            life: 3000
        });
    }
}
