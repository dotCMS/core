import { Injectable, Injector, inject } from '@angular/core';

import {
    DotContentEditor,
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

/** What a screen runs when a side panel {@link DotCustomEventHandlerService} opened reports. */
export interface DotPortletPanelEvents {
    /** On every save, with the panel still open. */
    saved: () => void;
    /** On a close after the legacy editor, once the portlet's list has reloaded. */
    closed: () => void;
}

/**
 * Handles the `ng-event` custom events the legacy JSP screens send to the admin. Two senders: the
 * legacy portlets (`IframePortletLegacyComponent`), and the Workflow Tasks detail dialog
 * (`DotWorkflowTaskComponent`), which passes on every event but its own close events and
 * `edit-contentlet`. Its "Edit Content" sends that one, and the detail opens it itself through
 * {@link openContent}, so it can view its task again after the panel (#38303, FR-025a).
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

    /**
     * `DotSidePanelHostComponent`'s injector: the side panels this service opens live under it.
     * There is no fallback: the root injector has no `MessageService`, so a panel opened under it
     * would fail inside.
     */
    #panelInjector: Injector | null = null;

    /** What runs when a side panel this service opened saves or closes; see {@link onPanelEvents}. */
    #panelEvents: DotPortletPanelEvents | null = null;

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
     * `DotSidePanelHostComponent` registers its own, so a panel finds the `MessageService` and the
     * `<p-toast>` that component provides; the root injector has neither.
     *
     * @param injector `DotSidePanelHostComponent`'s injector.
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

        this.openContent({ mode: 'edit', inode, contentType });
    }

    /**
     * The Sites portlet asked to edit a site. The event names only the inode; the opener fetches
     * the contentlet to learn its type.
     */
    private editHost($event: CustomEvent): void {
        this.openContent({ mode: 'edit', inode: $event.detail.data.inode });
    }

    /**
     * A legacy portlet asked to create content of a type, maybe in a folder. The JSP's `url` is
     * read for its `lang`, the language the content search or the Site Browser shows, and its
     * `folder`, the folder inode the Site Browser names for the legacy editor, which reads no
     * `folderPath`; the opener resolves the create screen itself.
     */
    private createContentlet($event: CustomEvent): void {
        const { url, contentType, folderPath } = $event.detail.data;
        const languageId = languageOf(url);
        const folderInode = paramOf(url, 'folder');

        this.openContent({
            mode: 'new',
            contentType,
            folderPath,
            ...(folderInode ? { folderInode } : {}),
            ...(languageId ? { languageId } : {})
        });
    }

    /** The Sites portlet asked to create a site. The JSP's `url` is ignored. */
    private createHost(): void {
        this.openContent({ mode: 'new', contentType: 'Host' });
    }

    /**
     * Registers what runs when a side panel this service opened saves or closes: the Workflow
     * Tasks detail views its task again, so it shows the content's current title and step,
     * whichever path opened the panel (its "Edit Content", or the task's URL, which the portlet
     * behind reopens) (#38303, FR-025a). `saved` runs on every save, with the panel still open.
     * `closed` runs after the portlet's list reloads, and only when the panel showed the legacy
     * editor, whose workflow actions report nothing; the new editor reports each one as a save.
     * One registration at a time; the last one wins.
     *
     * @param handlers What to run on a save and on a close.
     * @returns Releases the handlers; call it when the registering screen goes away.
     */
    onPanelEvents(handlers: DotPortletPanelEvents): () => void {
        this.#panelEvents = handlers;

        return () => {
            if (this.#panelEvents === handlers) {
                this.#panelEvents = null;
            }
        };
    }

    /**
     * Opens a request in the side panel for the current legacy portlet (#38303, R11): for the
     * portlet's events, for what the portlet's URL names on load (`IframePortletLegacyComponent`,
     * FR-049), and for the Workflow Tasks detail's "Edit Content" (`DotWorkflowTaskComponent`,
     * FR-025a). The request names the portlet current now, the close reloads its list, and the
     * panel is written to its URL like on every surface but the page editor (FR-044). A save does
     * not reload the list here: the portlet, while it is shown, reloads it on every save and delete
     * in any panel (`IframePortletLegacyComponent`, T174).
     *
     * Nothing opens until `DotSidePanelHostComponent` has registered its injector, which it does
     * before any portlet can send an event; a request before then is logged as an error.
     *
     * A save runs the `saved` handler {@link onPanelEvents} registered. A close runs its `closed`
     * handler after the portlet's list reloads, when the panel showed the legacy editor.
     *
     * @param request What to open, without the portlet.
     */
    openContent(request: DotEditContentRequest): void {
        const injector = this.#panelInjector;

        if (!injector) {
            console.error(
                'DotCustomEventHandlerService: no side panel host has registered its injector, so the panel did not open (#38303)'
            );

            return;
        }

        const portletId = this.dotRouterService.currentPortlet.id;
        let editor: DotContentEditor | null = null;

        // The handlers are read when the panel reports, not when it opened: a panel the URL
        // reopens opens before the Workflow Tasks detail that registers them exists.
        void openEditContentPanel(
            { ...request, portletId },
            {
                injector,
                editorResolved: (resolved) => (editor = resolved),
                saved: () => this.#panelEvents?.saved(),
                closed: () => {
                    if (portletId) {
                        this.dotIframeService.reloadData(portletId);
                    }

                    if (editor !== 'new') {
                        this.#panelEvents?.closed();
                    }
                },
                url: {}
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

/**
 * The language a legacy create URL names: the content search and the Site Browser append
 * `&lang=<id>` for the language the portlet shows (`view_contentlets_js_inc.jsp`,
 * `view_browser_js_inc.jsp`).
 *
 * @param url The `url` of a `create-contentlet` event, if it has one.
 * @returns The language id, or `undefined` when the URL names none.
 */
function languageOf(url: unknown): number | undefined {
    const languageId = Number(paramOf(url, 'lang'));

    return Number.isInteger(languageId) && languageId > 0 ? languageId : undefined;
}

/**
 * A plain (not portlet-namespaced) query param of a legacy create URL, such as the `lang` and the
 * `folder` the JSPs append after the portlet's action URL.
 *
 * @param url The `url` of a `create-contentlet` event, if it has one.
 * @param name The param to read.
 * @returns Its value, or `undefined` when the URL is missing, unparseable or has it empty.
 */
function paramOf(url: unknown, name: string): string | undefined {
    if (typeof url !== 'string' || !url) {
        return undefined;
    }

    try {
        return new URL(url, window.location.origin).searchParams.get(name) || undefined;
    } catch {
        return undefined;
    }
}
