import { Component, computed, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { finalize } from 'rxjs/operators';

import {
    DotContentDriveExportService,
    DotContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { getDownloadLink } from '@dotcms/utils';

import {
    ERROR_MESSAGE_LIFE,
    SUCCESS_MESSAGE_LIFE,
    WARNING_MESSAGE_LIFE
} from '../../../../shared/constants';
import { DotContentDriveStatus } from '../../../../shared/models';
import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';
import { isFolder } from '../../../../utils/functions';

/**
 * The toolbar's Export button: downloads the listing as a CSV when it is filtered to one content
 * type and nothing is selected (filter mode).
 *
 * It reads the store and keeps its own in-flight state; it adds nothing to the store, which is at
 * the `signalStore` feature limit.
 */
@Component({
    selector: 'dot-content-drive-export-button',
    imports: [ButtonModule, DotMessagePipe],
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

    /**
     * Exports the listing request the store holds and downloads the file under the name the server
     * gave it, then confirms with a toast that carries the number of exported items. When nothing
     * readable matched, a warning toast says so; when the export fails, an error toast shows the
     * server's message. Neither downloads anything.
     *
     * The request is not tied to this component's lifetime: the user may select a row while the
     * file is being built, which removes the button, and the download should still arrive.
     */
    protected onExport(): void {
        if (this.$exporting()) {
            return;
        }

        this.$exporting.set(true);

        this.#exportService
            .export({ query: this.#store.$request() })
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
}
