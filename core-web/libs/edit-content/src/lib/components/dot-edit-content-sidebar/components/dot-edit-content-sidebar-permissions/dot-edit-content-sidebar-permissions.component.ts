import { ChangeDetectionStrategy, Component, inject, input, OnDestroy } from '@angular/core';

import { CardModule } from 'primeng/card';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotPermissionsDialogComponent } from './components/permissions-dialog/permissions-dialog.component';

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
export class DotEditContentSidebarPermissionsComponent implements OnDestroy {
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);

    #permissionsDialogRef: DynamicDialogRef | undefined;

    /**
     * Contentlet identifier for the permissions iframe.
     */
    readonly identifier = input<string>('');

    /**
     * Contentlet language id for the permissions iframe.
     */
    readonly languageId = input<number>(0);

    ngOnDestroy(): void {
        this.#permissionsDialogRef?.close();
    }

    /**
     * Opens the permissions dialog with an iframe for the current contentlet.
     */
    openPermissionsDialog(): void {
        const id = this.identifier();
        const langId = this.languageId();
        if (!id || !langId) return;

        const header = this.#dotMessageService.get('edit.content.sidebar.permissions.title');
        this.#permissionsDialogRef = this.#dialogService.open(DotPermissionsDialogComponent, {
            header,
            width: 'min(92vw, 75rem)',
            contentStyle: { overflow: 'hidden' },
            data: { identifier: id, languageId: langId },
            transitionOptions: null,
            modal: true,
            appendTo: 'body',
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            maskStyleClass: 'p-dialog-mask-dynamic',
            resizable: false,
            position: 'center'
        });
        this.#permissionsDialogRef.onClose.subscribe({
            next: () => {
                this.#permissionsDialogRef = undefined;
            },
            error: (error) => {
                console.error('Error closing permissions dialog', error);
                this.#permissionsDialogRef = undefined;
            }
        });
    }
}
