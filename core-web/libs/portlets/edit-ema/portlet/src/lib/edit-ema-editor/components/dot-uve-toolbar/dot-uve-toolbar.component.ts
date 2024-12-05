import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { ChipModule } from 'primeng/chip';
import { DividerModule } from 'primeng/divider';
import { SplitButtonModule } from 'primeng/splitbutton';
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
        NgTemplateOutlet,
        ButtonModule,
        ToolbarModule,
        DotEmaBookmarksComponent,
        DotEmaInfoDisplayComponent,
        DotEmaRunningExperimentComponent,
        ClipboardModule,
        CalendarModule,
        SplitButtonModule,
        DividerModule,
        FormsModule,
        ReactiveFormsModule,
        ChipModule
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
    readonly $isPreviewMode = this.#store.$isPreviewMode;
    readonly $apiURL = this.#store.$apiURL;

    readonly $styleToolbarClass = computed(() => {
        if (!this.$isPreviewMode()) {
            return 'uve-toolbar';
        }

        return 'uve-toolbar uve-toolbar-preview';
    });

    protected readonly date = new Date();

    /**
     * Set the preview mode
     *
     * @memberof DotUveToolbarComponent
     */
    protected setPreviewMode() {
        this.#store.loadPageAsset({ preview: 'true' });
    }

    /**
     * Set the edit mode
     *
     * @memberof DotUveToolbarComponent
     */
    protected setEditMode() {
        this.#store.loadPageAsset({ preview: null });
    }

    triggerCopyToast() {
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('Copied'),
            life: 3000
        });
    }
}
