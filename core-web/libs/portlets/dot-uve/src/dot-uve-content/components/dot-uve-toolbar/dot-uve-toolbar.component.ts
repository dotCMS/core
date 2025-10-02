import { ClipboardModule } from '@angular/cdk/clipboard';
import { NgClass } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { UVEStore } from '../../../store/dot-uve.store';
import { PERSONA_KEY } from '../../../store/model';
import { buildUVEQueryParams, getUserParams } from '../../../utils';
import { DotUVEBookmarkComponent } from '../dot-uve-bookmark/dot-uve-bookmark.component';
import { DotUVEDeviceSelectorComponent } from '../dot-uve-device-selector/dot-uve-device-selector.component';
import { DotUVELanguageSelectorComponent } from '../dot-uve-language-selector/dot-uve-language-selector.component';
import { DotUVEModeSelectorComponent } from '../dot-uve-mode-selector/dot-uve-mode-selector.component';
import { DotUVEPersonaSelectorComponent } from '../dot-uve-persona-selector/dot-uve-persona-selector.component';
import { DotUVEWorkflowActionsComponent } from '../dot-uve-workflow-actions/dot-uve-workflow-actions.component';

@Component({
    selector: 'dot-uve-toolbar',
    imports: [
        NgClass,
        ToolbarModule,
        ButtonModule,
        TooltipModule,
        ClipboardModule,
        DotMessagePipe,
        DotUVEBookmarkComponent,
        DotUVEModeSelectorComponent,
        DotUVEDeviceSelectorComponent,
        DotUVEWorkflowActionsComponent,
        DotUVEPersonaSelectorComponent,
        DotUVELanguageSelectorComponent
    ],
    providers: [MessageService],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUVEToolbarComponent {
    readonly #store = inject(UVEStore);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    readonly $apiURL = computed(() => {
        const url = this.#store.url();
        const params = buildUVEQueryParams(this.#store.configuration());
        return `/api/v1/page/json${url}?${params}`;
    });

    readonly $copyUrl = computed(() => {
        const url = this.#store.url();
        const personId = this.#store.configuration[PERSONA_KEY]();
        const languageId = this.#store.configuration.language_id();
        const origin = window.location.origin;
        // TODO: Maybe `getUserParams` and `buildUVEQueryParams` should be merged into a single function receiving a partial UVEConfiguration
        const userParams = getUserParams();
        // TODO: Double check if this is correct beacuse I'm just sending the language and persona id rest of the params seems to irrelavant for the copy url
        const params = buildUVEQueryParams({ language_id: languageId, [PERSONA_KEY]: personId });
        return `${origin}${url}?${params}${userParams ? `&${userParams}` : ''}`;
    });

    /**
     * Trigger the copy toasts
     *
     * @memberof DotUveToolbarComponent
     */
    protected copyURLToast() {
        this.#messageService.add({
            severity: 'success',
            summary: this.#dotMessageService.get('Copied'),
            life: 3000
        });
    }
}
