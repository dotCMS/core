import { Injectable, inject, OnDestroy } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    DotLegacyImageEditorDialogComponent,
    DotLegacyImageEditorDialogData
} from './dot-legacy-image-editor-dialog.component';

const POST_MESSAGE_SOURCE = 'dot-image-editor';

/** Payload sent from the legacy image editor iframe via `postMessage`. */
interface DotImageEditorPostMessage {
    source: typeof POST_MESSAGE_SOURCE;
    type: 'tempfile' | 'close';
    tempFile?: DotCMSTempFile;
}

/** Detail of the `binaryField-open-image-editor-{variable}` custom event. */
interface ImageEditorOpenDetail {
    inode?: string;
    tempId?: string;
    variable: string;
}

/**
 * Bridges the Angular binary field with the legacy Dojo image editor.
 *
 * Listens for open events dispatched by the binary field web component, opens a
 * modal dialog with the editor iframe, and relays `postMessage` results back as
 * `binaryField-tempfile-{variable}` and `binaryField-close-image-editor-{variable}` events.
 */
@Injectable()
export class DotLegacyImageEditorLauncherService implements OnDestroy {
    readonly #dialogService = inject(DialogService);

    #dialogRef: DynamicDialogRef | null = null;
    #openEventHandler: ((event: Event) => void) | null = null;
    #messageHandler: ((event: MessageEvent) => void) | null = null;
    #variable: string | null = null;

    /**
     * Registers listeners for the given binary field variable.
     *
     * @param variable - Content type field variable that scopes editor events.
     */
    listen(variable: string): void {
        this.stopListening();

        this.#variable = variable;
        const openEventName = `binaryField-open-image-editor-${variable}`;

        this.#openEventHandler = (event: Event) => {
            const {
                inode,
                tempId,
                variable: fieldVariable
            } = (event as CustomEvent<ImageEditorOpenDetail>).detail;

            this.openDialog({ inode, tempId, variable: fieldVariable });
        };

        document.addEventListener(openEventName, this.#openEventHandler);
        this.#registerMessageListener();
    }

    /**
     * Removes event listeners, closes any open dialog, and clears the active field scope.
     */
    stopListening(): void {
        if (this.#variable && this.#openEventHandler) {
            document.removeEventListener(
                `binaryField-open-image-editor-${this.#variable}`,
                this.#openEventHandler
            );
        }

        this.#openEventHandler = null;
        this.#variable = null;
        this.#unregisterMessageListener();
        this.#closeDialog();
    }

    ngOnDestroy(): void {
        this.stopListening();
    }

    /**
     * Opens the legacy image editor dialog for the given asset and field.
     *
     * @param inode - Content inode when editing a published asset.
     * @param tempId - Temporary file id when editing an unsaved upload.
     * @param variable - Binary field variable used for editor event routing.
     */
    private openDialog({ inode, tempId, variable }: DotLegacyImageEditorDialogData): void {
        this.#closeDialog();

        this.#dialogRef = this.#dialogService.open(DotLegacyImageEditorDialogComponent, {
            appendTo: 'body',
            closable: false,
            closeOnEscape: false,
            draggable: false,
            keepInViewport: false,
            modal: true,
            resizable: false,
            showHeader: false,
            width: '90%',
            height: '90%',
            style: { maxWidth: 'none' },
            contentStyle: { height: '100%', overflow: 'hidden', padding: 0 },
            data: {
                inode,
                tempId,
                variable
            } satisfies DotLegacyImageEditorDialogData
        });
    }

    #registerMessageListener(): void {
        if (this.#messageHandler) {
            return;
        }

        this.#messageHandler = (event: MessageEvent) => {
            if (event.origin !== window.location.origin) {
                return;
            }

            const data = event.data as DotImageEditorPostMessage | undefined;

            if (!data || data.source !== POST_MESSAGE_SOURCE) {
                return;
            }

            const variable = this.#variable;

            if (!variable) {
                return;
            }

            if (data.type === 'tempfile' && data.tempFile) {
                document.dispatchEvent(
                    new CustomEvent(`binaryField-tempfile-${variable}`, {
                        detail: { tempFile: data.tempFile }
                    })
                );
                this.#closeDialog();
            }

            if (data.type === 'close') {
                document.dispatchEvent(
                    new CustomEvent(`binaryField-close-image-editor-${variable}`, {})
                );
                this.#closeDialog();
            }
        };

        window.addEventListener('message', this.#messageHandler);
    }

    #unregisterMessageListener(): void {
        if (this.#messageHandler) {
            window.removeEventListener('message', this.#messageHandler);
            this.#messageHandler = null;
        }
    }

    #closeDialog(): void {
        this.#dialogRef?.close();
        this.#dialogRef = null;
    }
}
