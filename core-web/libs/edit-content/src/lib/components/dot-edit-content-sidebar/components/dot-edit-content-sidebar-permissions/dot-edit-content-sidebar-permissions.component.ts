import { ChangeDetectionStrategy, Component, DestroyRef, inject, input } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { CardModule } from 'primeng/card';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotMessagePipe,
    DotPermissionsIframeDialogComponent,
    DotPermissionsIframeDialogData
} from '@dotcms/ui';

export const CONTENTLET_PERMISSIONS_IFRAME_PATH = '/html/portlet/ext/contentlet/permissions.jsp';

/**
 * Tab content component for the Permissions section in the edit content sidebar.
 * Renders a clickable card that opens the permissions modal.
 */
@Component({
    selector: 'dot-edit-content-sidebar-permissions',
    imports: [CardModule, DotMessagePipe],
    templateUrl: './dot-edit-content-sidebar-permissions.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarPermissionsComponent {
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    #permissionsDialogRef: DynamicDialogRef | undefined;

    /**
     * Contentlet identifier for the permissions iframe.
     */
    readonly identifier = input<string>('');

    /**
     * Contentlet language id for the permissions iframe.
     */
    readonly languageId = input<number>(0);

    constructor() {
        this.#destroyRef.onDestroy(() => this.#permissionsDialogRef?.close());
    }

    /**
     * Opens the permissions dialog with an iframe for the current contentlet.
     * Prevents opening multiple instances if the user clicks the card repeatedly.
     */
    openPermissionsDialog(): void {
        if (this.#permissionsDialogRef) return;

        const id = this.identifier();
        const langId = this.languageId();
        if (!id || !langId) return;

        this.#permissionsDialogRef = this.#dialogService.open(DotPermissionsIframeDialogComponent, {
            header: this.#dotMessageService.get('edit.content.sidebar.permissions.title'),
            width: 'min(92vw, 75rem)',
            contentStyle: { overflow: 'hidden' },
            data: {
                url: this.#buildPermissionsUrl(id, langId)
            } satisfies DotPermissionsIframeDialogData,
            transitionOptions: null,
            modal: true,
            appendTo: 'body',
            closeOnEscape: false,
            closable: true,
            draggable: false,
            resizable: false,
            position: 'center'
        });

        this.#permissionsDialogRef.onClose
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                this.#permissionsDialogRef = undefined;
            });
    }

    #buildPermissionsUrl(identifier: string, languageId: number): string {
        const params = new URLSearchParams({
            contentletId: identifier,
            languageId: String(languageId),
            popup: 'true'
        });
        return `${CONTENTLET_PERMISSIONS_IFRAME_PATH}?${params.toString()}`;
    }
}
