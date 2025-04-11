import { Injectable } from '@angular/core';

import { NG_CUSTOM_EVENTS } from '../../shared/enums';

@Injectable({
    providedIn: 'root'
})
export class DotNgEvenHandlerService {
    private readonly store = inject(EditEmaStore);
    private readonly router = inject(Router);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dialog = inject(DotEmaDialogComponent);

    handleEvent({ event }: { event: CustomEvent }) {
        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.DIALOG_CLOSED: {
                if (!this.$didTranslate()) {
                    this.navigate({
                        language_id: 1 // We navigate to the default language if the user didn't translate
                    });
                } else {
                    this.$didTranslate.set(false);
                    this.reloadFromDialog();
                }

                break;
            }

            case NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED: {
                // We need to check when the contentlet is updated, to know if we need to reload the page
                this.$didTranslate.set(true);
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                this.$didTranslate.set(true);
                const url = event.detail.payload.htmlPageReferer.split('?')[0].replace('/', '');

                if (this.queryParams.url !== url) {
                    this.navigate({
                        url
                    });

                    return;
                }

                if (this.#currentComponent instanceof EditEmaEditorComponent) {
                    this.#currentComponent.reloadIframe();
                }

                this.#activatedRoute.data.pipe(take(1)).subscribe(({ data }) => {
                    this.store.load({
                        ...this.queryParams,
                        clientHost: this.queryParams.clientHost ?? data?.url
                    });
                });
                break;
            }
        }
    }

    /*
     * Reloads the component from the dialog.
     */
    reloadFromDialog() {
        this.store.reload({ params: this.queryParams });
    }

    private navigate(queryParams) {
        this.#router.navigate([], {
            queryParams,
            queryParamsHandling: 'merge'
        });
    }

    /**
     * Check if the clientHost is in the whitelist provided by the app
     *
     * @private
     * @param {string} clientHost
     * @param {*} [devURLWhitelist=[]]
     * @return {*}
     * @memberof DotEmaShellComponent
     */
    private checkClientHostAccess(clientHost: string, devURLWhitelist: string[] = []): boolean {
        // If we don't have a whitelist or a clientHost we can't access it
        if (!clientHost || !Array.isArray(devURLWhitelist) || !devURLWhitelist.length) {
            return false;
        }

        // Most IDEs and terminals add a / at the end of the URL, so we need to sanitize it
        const sanitizedClientHost = clientHost.endsWith('/') ? clientHost.slice(0, -1) : clientHost;

        // We need to sanitize the whitelist as well
        const sanitizedDevURLWhitelist = devURLWhitelist.map((url) =>
            url.endsWith('/') ? url.slice(0, -1) : url
        );

        // If the clientHost is in the whitelist we can access it
        return sanitizedDevURLWhitelist.includes(sanitizedClientHost);
    }

    /**
     * Asks the user for confirmation to create a new translation for a given language.
     *
     * @param {DotLanguage} language - The language to create a new translation for.
     * @private
     *
     * @return {void}
     */
    private createNewTranslation(language: DotLanguage, page: DotPage): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.header'
            ),
            message: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.message',
                language.language
            ),
            rejectIcon: 'hidden',
            acceptIcon: 'hidden',
            key: 'shell-confirm-dialog',
            accept: () => {
                this.dialog.translatePage({
                    page,
                    newLanguage: language.id
                });
            },
            reject: () => {
                this.navigate({
                    language_id: 1
                });
            }
        });
    }
}
