import { Injectable, Injector, inject } from '@angular/core';

import {
    DotEventsService,
    DotGenerateSecurePasswordService,
    DotIframeService,
    DotLicenseService,
    DotNavLogoService,
    DotRouterService,
    DotUiColorsService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotPushPublishDialogService, DotUiColors } from '@dotcms/dotcms-js';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';
import type { DotEditContentRequest } from '@dotcms/edit-content';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { openEditContentPanel } from '../../util/open-edit-content-panel';
import { DotDownloadBundleDialogService } from '../dot-download-bundle-dialog/dot-download-bundle-dialog.service';

export const COMPARE_CUSTOM_EVENT = 'compare-contentlet';

/** The legacy page editor's `edit-page` event. */
export interface DotCMSEditPageEvent {
    name: string;
    data: {
        url: string;
        languageId: string;
        hostId: string;
    };
}

/**
 * Handle Custom events
 *
 * @export
 * @class DotCustomEventHandlerService
 */
@Injectable({
    providedIn: 'root'
})
export class DotCustomEventHandlerService {
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private dotRouterService = inject(DotRouterService);
    private dotUiColorsService = inject(DotUiColorsService);
    private dotNavLogoService = inject(DotNavLogoService);
    private dotIframeService = inject(DotIframeService);
    private dotPushPublishDialogService = inject(DotPushPublishDialogService);
    private dotDownloadBundleDialogService = inject(DotDownloadBundleDialogService);
    private dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    private dotGenerateSecurePasswordService = inject(DotGenerateSecurePasswordService);
    private dotEventsService = inject(DotEventsService);
    private dotLicenseService = inject(DotLicenseService);

    /** This service's own injector: the fallback until the admin root registers its own. */
    readonly #injector = inject(Injector);

    /** The admin root's injector, which the side panels this service opens live under. */
    #panelInjector: Injector | null = null;

    private handlers: Record<string, ($event: CustomEvent) => void> = {
        'edit-page': this.goToEditPage.bind(this),
        'edit-contentlet': this.editContentlet.bind(this),
        'edit-task': this.editTaskLegacy.bind(this),
        'create-contentlet': this.createContentlet.bind(this),
        'company-info-updated': this.setPersonalization.bind(this),
        'push-publish': this.pushPublishDialog.bind(this),
        'download-bundle': this.downloadBundleDialog.bind(this),
        'workflow-wizard': this.executeWorkflowWizard.bind(this),
        'generate-secure-password': this.generateSecurePassword.bind(this),
        'compare-contentlet': this.openCompareDialog.bind(this),
        'license-changed': this.updateLicense.bind(this),
        'edit-host': this.editHost.bind(this),
        'create-host': this.createHost.bind(this)
    };

    /**
     * Handle custom events from the iframe portlets
     *
     * @param CustomEvent event
     * @memberof DotCustomEventHandlerService
     */
    handle(event: CustomEvent): void {
        if (event?.detail?.name && this.handlers?.[event.detail.name]) {
            this.handlers[event.detail.name](event);
        }
    }

    /**
     * Sets the injector the side panels this service opens live under (#38303).
     * `MainComponentLegacyComponent` registers its own, so a panel finds the `MessageService` and
     * the `<p-toast>` the admin root provides; the root injector has neither.
     *
     * @param injector The admin root's injector.
     */
    setInjector(injector: Injector): void {
        this.#panelInjector = injector;
    }

    private generateSecurePassword($event: CustomEvent): void {
        this.dotGenerateSecurePasswordService.open($event.detail.data);
    }

    private goToEditPage($event: CustomEvent<DotCMSEditPageEvent>): void {
        this.dotLoadingIndicatorService.show();
        this.dotRouterService.goToEditPage({
            url: $event.detail.data.url,
            language_id: $event.detail.data.languageId,
            host_id: $event.detail.data.hostId
        });
    }

    /**
     * A legacy portlet asked to edit a contentlet: it opens in the side panel, in whichever editor
     * its content type uses.
     */
    private editContentlet($event: CustomEvent): void {
        const { inode, contentType } = $event.detail.data;

        this.#openInSidePanel({ mode: 'edit', inode, contentType });
    }

    /**
     * The Sites portlet asked to edit a site. The event names only the inode; the opener fetches
     * the contentlet to learn its type.
     */
    private editHost($event: CustomEvent): void {
        this.#openInSidePanel({ mode: 'edit', inode: $event.detail.data.inode });
    }

    /**
     * A legacy portlet asked to create content of a type, maybe in a folder. The JSP's `url` is
     * ignored: the opener resolves the create screen itself.
     */
    private createContentlet($event: CustomEvent): void {
        const { contentType, folderPath } = $event.detail.data;

        this.#openInSidePanel({ mode: 'new', contentType, folderPath });
    }

    /** The Sites portlet asked to create a site. The JSP's `url` is ignored. */
    private createHost(): void {
        this.#openInSidePanel({ mode: 'new', contentType: 'Host' });
    }

    /**
     * Opens a request in the side panel for the portlet that asked (#38303, R11). The portlet is
     * the one current when the event arrived: the request names it, and every save and the close
     * reload its list.
     *
     * @param request What to open, without the portlet.
     */
    #openInSidePanel(request: DotEditContentRequest): void {
        const portletId = this.dotRouterService.currentPortlet.id;
        const reload = () => {
            if (portletId) {
                this.dotIframeService.reloadData(portletId);
            }
        };

        void openEditContentPanel(
            { ...request, portletId },
            {
                injector: this.#panelInjector ?? this.#injector,
                saved: reload,
                closed: reload
            }
        );
    }

    private editTaskLegacy($event: CustomEvent): void {
        this.dotRouterService.goToEditTask($event.detail.data.inode);
    }

    private setPersonalization($event: CustomEvent): void {
        this.dotNavLogoService.setLogo($event.detail.payload.navBarLogo);

        this.dotUiColorsService.setColors(
            document.querySelector('html'),
            <DotUiColors>$event.detail.payload.colors
        );
        this.dotIframeService.reloadColors();
    }

    private pushPublishDialog($event: CustomEvent): void {
        this.dotPushPublishDialogService.open($event.detail.data);
    }

    private downloadBundleDialog($event: CustomEvent): void {
        this.dotDownloadBundleDialogService.open($event.detail.data);
    }

    private executeWorkflowWizard($event: CustomEvent): void {
        this.dotWorkflowEventHandlerService.open($event.detail.data);
    }

    private openCompareDialog($event: CustomEvent): void {
        this.dotEventsService.notify<DotContentCompareEvent>(
            COMPARE_CUSTOM_EVENT,
            $event.detail.data
        );
    }

    /**
     * Update license
     *
     * @private
     * @memberof DotCustomEventHandlerService
     */
    private updateLicense(): void {
        this.dotLicenseService.updateLicense();
    }
}
