import { tapResponse } from '@ngrx/operators';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import {
    DotContentletLockerService,
    DotMessageService,
    DotPersonalizeService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotDevice,
    DotExperimentStatus,
    DotPersona
} from '@dotcms/dotcms-models';
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { DotPageApiParams } from '../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../shared/consts';
import { EDITOR_MODE, EDITOR_STATE } from '../../../shared/enums';
import { UVEStore } from '../../../store/dot-uve.store';
import { DotEditEmaWorkflowActionsComponent } from '../dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaLanguageSelectorComponent } from '../edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from '../edit-ema-persona-selector/edit-ema-persona-selector.component';

@Component({
    selector: 'dot-edit-ema-toolbar',
    standalone: true,
    imports: [
        CommonModule,
        MenuModule,
        ButtonModule,
        ToolbarModule,
        DotDeviceSelectorSeoComponent,
        DotMessagePipe,
        DotEmaBookmarksComponent,
        DotEmaRunningExperimentComponent,
        EditEmaPersonaSelectorComponent,
        EditEmaLanguageSelectorComponent,
        DotEmaInfoDisplayComponent,
        DotEditEmaWorkflowActionsComponent,
        ClipboardModule
    ],
    providers: [DotPersonalizeService],
    templateUrl: './edit-ema-toolbar.component.html',
    styleUrls: ['./edit-ema-toolbar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class EditEmaToolbarComponent {
    @Output() readonly editUrlContentMap = new EventEmitter<DotCMSContentlet>();

    @ViewChild('personaSelector')
    personaSelector!: EditEmaPersonaSelectorComponent;

    private readonly store = inject(EditEmaStore);

    private readonly messageService = inject(MessageService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly router = inject(Router);
    private readonly dotContentletLockerService = inject(DotContentletLockerService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly personalizeService = inject(DotPersonalizeService);
    private readonly activatedRouter = inject(ActivatedRoute);

    readonly editorState = EDITOR_STATE;
    readonly editorMode = EDITOR_MODE;
    readonly experimentStatus = DotExperimentStatus;
    readonly uveStore = inject(UVEStore);

    get queryParams(): DotPageApiParams {
        return this.activatedRouter.snapshot.queryParams as DotPageApiParams;
    }

    /**
     * Update the current device
     *
     * @param {DotDevice} [device]
     * @memberof EditEmaToolbarComponent
     */
    updateCurrentDevice(device: DotDevice & { icon?: string }) {
        // DELETE WHEN THE UVE STORE IS DONE
        this.store.setDevice(device);

        this.uveStore.setDevice(device);
    }

    /**
     * Preview RRSS media cards
     *
     * @param {string} seoMedia
     * @memberof EditEmaToolbarComponent
     */
    onSeoMediaChange(seoMedia: string) {
        // DELETE WHEN THE UVE STORE IS DONE
        this.store.setSocialMedia(seoMedia);

        this.uveStore.setSocialMedia(seoMedia);
    }

    /**
     * Handle the copy URL action
     *
     * @memberof EditEmaToolbarComponent
     */
    triggerCopyToast() {
        this.messageService.add({
            severity: 'success',
            summary: this.dotMessageService.get('Copied'),
            life: 3000
        });
    }

    /**
     * Handle the language selection
     *
     * @param {number} language_id
     * @memberof DotEmaComponent
     */
    onLanguageSelected(language_id: number) {
        this.updateQueryParams({
            language_id
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
            this.updateQueryParams({
                'com.dotmarketing.persona.id': persona.identifier
            });
        } else {
            this.confirmationService.confirm({
                header: this.dotMessageService.get('editpage.personalization.confirm.header'),
                message: this.dotMessageService.get(
                    'editpage.personalization.confirm.message',
                    persona.name
                ),
                acceptLabel: this.dotMessageService.get('dot.common.dialog.accept'),
                rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
                accept: () => {
                    this.personalizeService
                        .personalized(persona.pageId, persona.keyTag)
                        .subscribe(() => {
                            this.updateQueryParams({
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
        this.confirmationService.confirm({
            header: this.dotMessageService.get('editpage.personalization.delete.confirm.header'),
            message: this.dotMessageService.get(
                'editpage.personalization.delete.confirm.message',
                persona.name
            ),
            acceptLabel: this.dotMessageService.get('dot.common.dialog.accept'),
            rejectLabel: this.dotMessageService.get('dot.common.dialog.reject'),
            accept: () => {
                this.personalizeService
                    .despersonalized(persona.pageId, persona.keyTag)
                    .subscribe(() => {
                        this.personaSelector.fetchPersonas();

                        if (persona.selected) {
                            this.updateQueryParams({
                                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
                            });
                        }
                    }); // This does a take 1 under the hood
            }
        });
    }

    /**
     * Handle a new page event. This event is triggered when the page changes for a Workflow Action
     * Update the query params if the url or the language id changed
     *
     * @param {DotCMSContentlet} page
     * @memberof EditEmaToolbarComponent
     */
    handleNewPage(page: DotCMSContentlet): void {
        const { pageURI, url, languageId } = page;
        const params = {
            ...this.updateQueryParams,
            url: pageURI ?? url,
            language_id: languageId?.toString()
        };

        if (this.shouldReload(params)) {
            this.updateQueryParams(params);
        }
    }

    /**
     * Unlocks a page with the specified inode.
     *
     * @param {string} inode
     * @memberof EditEmaToolbarComponent
     */
    unlockPage(inode: string) {
        // DELETE WHEN THE UVE STORE IS DONE
        this.store.unlockPage(inode); // This action does unnatural things, like modifying the page and then reloading the page afterwards

        this.messageService.add({
            severity: 'info',
            summary: 'Page Unlock',
            detail: 'Page is being unlocked'
        });

        this.dotContentletLockerService.unlock(inode).pipe(
            tapResponse({
                next: () => {
                    this.messageService.add({
                        severity: 'success',
                        summary: 'Page Unlock',
                        detail: 'Page is unlocked'
                    });

                    const params = this.uveStore.params();

                    this.uveStore.reload(params);
                },
                error: () => {
                    this.messageService.add({
                        severity: 'error',
                        summary: 'Page Unlock',
                        detail: 'Page could not be unlocked'
                    });
                }
            })
        );
    }

    private updateQueryParams(params: Params) {
        this.router.navigate([], {
            queryParams: params,
            queryParamsHandling: 'merge'
        });
    }

    private shouldReload(params: Params): boolean {
        const { url: newUrl, language_id: newLanguageId } = params;
        const { url, language_id } = this.queryParams;

        return newUrl != url || newLanguageId != language_id;
    }
}
