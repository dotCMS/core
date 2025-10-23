import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotLicenseService,
    DotMessageService,
    DotPageStateService,
    DotPersonalizeService
} from '@dotcms/data-access';
import {
    CustomIframeDialogEvent,
    DotDevice,
    DotLanguage,
    DotPageMode,
    DotPageRenderState,
    DotPersona,
    DotVariantData
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotIframeDialogComponent } from '../../../../../view/components/dot-iframe-dialog/dot-iframe-dialog.component';
import { DotLanguageSelectorComponent } from '../../../../../view/components/dot-language-selector/dot-language-selector.component';
import { DotPersonaSelectorComponent } from '../../../../../view/components/dot-persona-selector/dot-persona-selector.component';

@Component({
    selector: 'dot-edit-page-view-as-controller-seo',
    templateUrl: './dot-edit-page-view-as-controller-seo.component.html',
    styleUrls: ['./dot-edit-page-view-as-controller-seo.component.scss'],
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        TooltipModule,
        DotPersonaSelectorComponent,
        DotLanguageSelectorComponent,
        CheckboxModule,
        ConfirmDialogModule,
        DotIframeDialogComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditPageViewAsControllerSeoComponent implements OnInit {
    private dotAlertConfirmService = inject(DotAlertConfirmService);
    private dotMessageService = inject(DotMessageService);
    private dotLicenseService = inject(DotLicenseService);
    private dotPersonalizeService = inject(DotPersonalizeService);
    private router = inject(Router);

    isEnterpriseLicense$: Observable<boolean>;
    showEditJSPDialog = signal(false);
    urlEditPageIframeDialog = signal('');

    @Input() pageState: DotPageRenderState;
    @Input() variant: DotVariantData | null = null;
    private confirmationService = inject(ConfirmationService);

    private readonly customEventsHandler;
    dotPageStateService = inject(DotPageStateService);

    constructor() {
        this.customEventsHandler = {
            close: ({ detail: { data } }: CustomEvent) => {
                this.showEditJSPDialog.set(false);
                const queryparams = {
                    url: data.redirectUrl,
                    language_id: data.languageId
                };

                this.router.navigate(['/edit-page/content'], {
                    queryParams: { ...queryparams },
                    queryParamsHandling: 'merge'
                });
            }
        };
    }

    ngOnInit(): void {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
    }

    /**
     * Handle the changes in Persona Selector.
     *
     * @memberof DotEditPageViewAsControllerComponent
     * @param persona
     */
    changePersonaHandler(persona: DotPersona): void {
        this.dotPageStateService.setPersona(persona);
    }

    /**
     * Handle changes in Language Selector.
     *
     * @memberof DotEditPageViewAsControllerComponent
     * @param language
     */
    changeLanguageHandler(language: DotLanguage): void {
        if (language.translated) {
            this.dotPageStateService.setLanguage(language.id);
        } else {
            this.askForCreateNewTranslation(language);
        }
    }

    /**
     * Handle changes in Device Selector.
     *
     * @memberof DotEditPageViewAsControllerComponent
     * @param device
     */
    changeDeviceHandler(device: DotDevice): void {
        this.dotPageStateService.setDevice(device);
    }

    /**
     * Remove personalization for the current page and set the new state to the page
     *
     * @param {DotPersona} persona
     * @memberof DotEditPageViewAsControllerComponent
     */
    deletePersonalization(persona: DotPersona): void {
        this.dotAlertConfirmService.confirm({
            header: this.dotMessageService.get('editpage.personalization.delete.confirm.header'),
            message: this.dotMessageService.get(
                'editpage.personalization.delete.confirm.message',
                persona.name
            ),
            accept: () => {
                this.dotPersonalizeService
                    .despersonalized(this.pageState.page.identifier, persona.keyTag)
                    .pipe(take(1))
                    .subscribe(() => {
                        this.dotPageStateService.setLock(
                            {
                                mode: DotPageMode.PREVIEW
                            },
                            false
                        );
                    });
            }
        });
    }

    /**
     * Removes the edit JSP dialog.
     * Close when press the close button in the iframe
     *
     * @return {void}
     */
    removeEditJSPDialog(): void {
        this.showEditJSPDialog.set(false);
        this.router.navigate(['/edit-page/content'], { queryParamsHandling: 'preserve' });
    }

    /**
     * Handle the different custom event sent by the iframe.
     *
     * @param {CustomIframeDialogEvent} $event - The custom iframe dialog event.
     */
    customIframeDialog($event: CustomIframeDialogEvent) {
        if (this.customEventsHandler[$event.detail.name]) {
            this.customEventsHandler[$event.detail.name]($event);
        }
    }

    /**
     * Asks the user for confirmation to create a new translation for a given language.
     *
     * @param {DotLanguage} language - The language to create a new translation for.
     * @private
     *
     * @return {void}
     */

    private askForCreateNewTranslation(language: DotLanguage): void {
        this.confirmationService.confirm({
            key: 'lang-confirm-dialog',
            header: this.dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.header'
            ),
            message: this.dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.message',
                language.language
            ),
            rejectIcon: 'hidden',
            acceptIcon: 'hidden',
            accept: () => {
                this.urlEditPageIframeDialog.set(this.getUrlEditPageJSP(language.id));
                // TODO: Handle the new editor
                this.showEditJSPDialog.set(true);
            },
            reject: () => {
                this.router.navigate(['/edit-page/content'], {
                    queryParamsHandling: 'preserve'
                });
            }
        });
    }

    /**
     * Returns the URL of the edit page in JSP format with the specified new language.
     *
     * @param {number} newLanguage - The new language to use.
     * @returns {string} The URL of the edit page.
     * @private
     */
    private getUrlEditPageJSP(newLanguage: number): string {
        const isLive = this.pageState.page.live;
        const pageLiveInode = this.pageState.page.liveInode;
        const iNode = this.pageState.page.inode;
        const stInode = this.pageState.page.stInode;

        const queryStringParts = [
            'p_p_id=content',
            'p_p_action=1',
            'p_p_state=maximized',
            'angularCurrentPortlet=pages',
            `_content_sibbling=${isLive ? pageLiveInode : iNode}`,
            '_content_cmd=edit',
            `_content_sibblingStructure=${isLive ? pageLiveInode : stInode}`,
            '_content_struts_action=%2Fext%2Fcontentlet%2Fedit_contentlet',
            'inode=',
            `lang=${newLanguage}`,
            'populateaccept=true',
            'reuseLastLang=true'
        ];

        const queryString = queryStringParts.join('&');

        return `/c/portal/layout?${queryString}`;
    }
}
