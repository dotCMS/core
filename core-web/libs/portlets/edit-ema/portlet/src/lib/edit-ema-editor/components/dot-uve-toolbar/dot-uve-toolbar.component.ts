import { ClipboardModule } from '@angular/cdk/clipboard';
import { ChangeDetectionStrategy, Component, inject, ViewChild } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotPersona } from '@dotcms/dotcms-models';

import { DEFAULT_PERSONA } from '../../../shared/consts';
import { UVEStore } from '../../../store/dot-uve.store';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaPersonaSelectorComponent } from '../edit-ema-persona-selector/edit-ema-persona-selector.component';

@Component({
    selector: 'dot-uve-toolbar',
    standalone: true,
    imports: [
        ButtonModule,
        ToolbarModule,
        DotEmaBookmarksComponent,
        DotEmaInfoDisplayComponent,
        DotEmaRunningExperimentComponent,
        EditEmaPersonaSelectorComponent,
        ClipboardModule
    ],
    providers: [DotPersonalizeService],
    templateUrl: './dot-uve-toolbar.component.html',
    styleUrl: './dot-uve-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveToolbarComponent {
    // I tried to use the new viewChild signal API but is not supported on MockComponent
    // Revisit this ticket to see if it's supported already, so we can move on to signals
    // https://github.com/help-me-mom/ng-mocks/issues/8634
    @ViewChild('personaSelector')
    personaSelector!: EditEmaPersonaSelectorComponent;

    #store = inject(UVEStore);

    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #personalizeService = inject(DotPersonalizeService);

    readonly $toolbar = this.#store.$uveToolbar;
    readonly $apiURL = this.#store.$apiURL;
    readonly $personaSelector = this.#store.$personaSelector;

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

    /**
     * Handle the persona selection
     *
     * @param {DotPersona} persona
     * @memberof DotEmaComponent
     */
    onPersonaSelected(persona: DotPersona & { pageId: string }) {
        if (persona.identifier === DEFAULT_PERSONA.identifier || persona.personalized) {
            this.#store.loadPageAsset({
                'com.dotmarketing.persona.id': persona.identifier
            });
        } else {
            this.#confirmationService.confirm({
                header: this.#dotMessageService.get('editpage.personalization.confirm.header'),
                message: this.#dotMessageService.get(
                    'editpage.personalization.confirm.message',
                    persona.name
                ),
                acceptLabel: this.#dotMessageService.get('dot.common.dialog.accept'),
                rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
                accept: () => {
                    this.#personalizeService
                        .personalized(persona.pageId, persona.keyTag)
                        .subscribe(() => {
                            this.#store.loadPageAsset({
                                'com.dotmarketing.persona.id': persona.identifier
                            });

                            this.personaSelector.fetchPersonas();
                        }); // This does a take 1 under the hood
                },
                reject: () => {
                    this.personaSelector.resetValue();
                }
            });
        }
    }

    /**
     * Handle the persona despersonalization
     *
     * @param {(DotPersona & { pageId: string })} persona
     * @memberof EditEmaToolbarComponent
     */
    onDespersonalize(persona: DotPersona & { pageId: string; selected: boolean }) {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('editpage.personalization.delete.confirm.header'),
            message: this.#dotMessageService.get(
                'editpage.personalization.delete.confirm.message',
                persona.name
            ),
            acceptLabel: this.#dotMessageService.get('dot.common.dialog.accept'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            accept: () => {
                this.#personalizeService
                    .despersonalized(persona.pageId, persona.keyTag)
                    .subscribe(() => {
                        this.personaSelector.fetchPersonas();

                        if (persona.selected) {
                            this.#store.loadPageAsset({
                                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                            });
                        }
                    }); // This does a take 1 under the hood
            }
        });
    }
}
