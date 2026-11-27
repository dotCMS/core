import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { finalize } from 'rxjs/operators';

import {
    DotContentDriveExportRequest,
    DotContentDriveExportService,
    DotContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';

import {
    ERROR_MESSAGE_LIFE,
    MAX_SELECTION_ITEMS,
    SUCCESS_MESSAGE_LIFE,
    WARNING_MESSAGE_LIFE
} from '../../../../shared/constants';
import { DotContentDriveStatus } from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import { excludeFolders, toFolderAssetPaths } from '../../../../utils/action-center';
import { isFolder } from '../../../../utils/functions';

/**
 * The toolbar's Export button. With nothing selected it downloads the listing as a CSV when the
 * listing is filtered to one content type (filter mode); with anything selected it downloads the
 * selected contentlets as a zip with one CSV per content type (selection mode).
 *
 * It reads the store and keeps its own in-flight state; it adds nothing to the store, which is at
 * the `signalStore` feature limit.
 */
@Component({
    selector: 'dot-content-drive-export-button',
    imports: [ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-content-drive-export-button.component.html'
})
export class DotContentDriveExportButtonComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #exportService = inject(DotContentDriveExportService);
    readonly #contentTypeService = inject(DotContentTypeService);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);

    /** True from the click until the export answers, so a second click cannot start another. */
    protected readonly $exporting = signal(false);

    /**
     * Selection mode applies whenever anything is selected, whatever the listing is filtered to:
     * the Form rule is filter mode's, and the server drops Form contentlets from a selection.
     */
    protected readonly $selectionMode = computed<boolean>(
        () => this.#store.selectedItems().length > 0
    );

    /** More items are selected than the server accepts in one export. */
    readonly #selectionTooLarge = computed<boolean>(
        () => this.#store.selectedItems().length > MAX_SELECTION_ITEMS
    );

    /** The one content type variable the listing is filtered to, or `undefined` for none or many. */
    readonly #filteredVariable = computed<string | undefined>(() => {
        const contentTypes = this.#store.filters()?.contentType ?? [];

        return contentTypes.length === 1 ? contentTypes[0] : undefined;
    });

    /**
     * The filtered type, fetched to rule out Forms: they export through the Forms portlet, not
     * here. Nothing is fetched unless exactly one type is filtered.
     */
    readonly #contentType = rxResource({
        params: () => this.#filteredVariable(),
        stream: ({ params }) => this.#contentTypeService.getContentType(params)
    });

    /**
     * Filter mode is offered when nothing is selected and the listing is filtered to exactly one
     * type that is not a Form. It stays hidden until that type has been fetched, and while a
     * previous type's answer is still the one held.
     */
    protected readonly $filterModeVisible = computed<boolean>(() => {
        const variable = this.#filteredVariable();

        if (this.#store.selectedItems().length > 0 || !variable || !this.#contentType.hasValue()) {
            return false;
        }

        const contentType = this.#contentType.value();

        return (
            contentType.variable === variable &&
            contentType.baseType !== DotCMSBaseTypesContentTypes.FORM
        );
    });

    /**
     * Disabled while an export runs, and when the loaded listing has nothing to export: it holds no
     * contentlet and no loaded page reports more content. A listing still loading keeps it enabled,
     * since its items are not known yet.
     */
    protected readonly $filterModeDisabled = computed<boolean>(() => {
        if (this.$exporting()) {
            return true;
        }

        if (this.#store.status() !== DotContentDriveStatus.LOADED) {
            return false;
        }

        const hasContentlet = this.#store.items().some((item) => !isFolder(item));
        const hasMoreContent = this.#store.pages().some((page) => page.hasMoreContent);

        return !hasContentlet && !hasMoreContent;
    });

    /** Whether the button shows: in selection mode always, in filter mode on its own rule. */
    protected readonly $visible = computed<boolean>(
        () => this.$selectionMode() || this.$filterModeVisible()
    );

    /**
     * Disabled while an export runs. In selection mode, also when more items are selected than the
     * server accepts; in filter mode, when the loaded listing has nothing to export.
     */
    protected readonly $disabled = computed<boolean>(() => {
        if (!this.$selectionMode()) {
            return this.$filterModeDisabled();
        }

        return this.$exporting() || this.#selectionTooLarge();
    });

    /**
     * The message key that says why the button is disabled, or an empty string when there is
     * nothing to say. Only the selection limit is explained: the other disabled states are either
     * brief (an export running) or visible in the listing itself (nothing to export).
     */
    protected readonly $tooltip = computed<string>(() =>
        this.$selectionMode() && this.#selectionTooLarge()
            ? 'content-drive.export.tooltip.max-items'
            : ''
    );

    /**
     * Exports what the button stands for and downloads the file under the name the server gave it,
     * then confirms with a toast that carries the number of exported items and files. When nothing
     * readable matched, a warning toast says so; when the export fails, an error toast shows the
     * server's message. Neither downloads anything.
     *
     * In selection mode the request carries the browsed site and the selected folders and contentlets; in
     * filter mode, the listing request the store holds.
     *
     * The request is not tied to this component's lifetime: the user may change the selection
     * while the file is being built, which can remove the button, and the download should still
     * arrive.
     */
    protected onExport(): void {
        if (this.$disabled()) {
            return;
        }

        const body = this.#buildRequest();

        this.$exporting.set(true);

        this.#exportService
            .export(body)
            .pipe(finalize(() => this.$exporting.set(false)))
            .subscribe({
                next: (result) => {
                    if (result.kind !== 'file') {
                        this.#messageService.add({
                            severity: 'warn',
                            summary: this.#dotMessageService.get(
                                'content-drive.toast.export-empty'
                            ),
                            life: WARNING_MESSAGE_LIFE
                        });

                        return;
                    }

                    getDownloadLink(result.blob, result.fileName).click();

                    this.#messageService.add({
                        severity: 'success',
                        summary: this.#dotMessageService.get('content-drive.toast.export-success'),
                        detail: this.#dotMessageService.get(
                            'content-drive.toast.export-success-detail',
                            String(result.contentlets),
                            String(result.files)
                        ),
                        life: SUCCESS_MESSAGE_LIFE
                    });
                },
                // The service already turned the response into the server's message
                error: (error: Error) => {
                    this.#messageService.add({
                        severity: 'error',
                        summary: this.#dotMessageService.get('content-drive.toast.export-error'),
                        detail: error.message,
                        life: ERROR_MESSAGE_LIFE
                    });
                }
            });
    }

    /**
     * The export body for the current mode. Selection mode sends the selected folders as asset
     * paths on the browsed site, such as `//demo.dotcms.com/blog/`, so each exports everything
     * below it, and the selected contentlets with one entry per selected language version.
     */
    #buildRequest(): DotContentDriveExportRequest {
        if (!this.$selectionMode()) {
            return { query: this.#store.$request() };
        }

        const site = this.#store.currentSite();
        const selectedItems = this.#store.selectedItems();

        return {
            siteId: site?.identifier ?? '',
            selection: {
                folders: toFolderAssetPaths(selectedItems, site?.hostname ?? ''),
                contentlets: excludeFolders(selectedItems).map(({ identifier, languageId }) => ({
                    identifier,
                    languageId
                }))
            }
        };
    }
}
