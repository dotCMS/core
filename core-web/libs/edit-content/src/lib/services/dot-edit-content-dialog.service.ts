import { Observable } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { filter, take } from 'rxjs/operators';

import { DotCMSContentlet, DotContentletDepths } from '@dotcms/dotcms-models';

import {
    DotEditContentDialogComponent,
    EditContentDialogData
} from '../components/dot-create-content-dialog/dot-create-content-dialog.component';

/**
 * Service for managing edit content dialog instances.
 * This service provides convenience methods for opening edit content dialogs
 * for both new content creation and existing content editing.
 */
@Injectable({
    providedIn: 'root'
})
export class DotEditContentDialogService {
    readonly #dialogService = inject(DialogService);

    /**
     * Opens a dialog for creating new content
     *
     * @param contentTypeId - The content type variable name or ID
     * @param options - Additional configuration options
     * @returns Observable that emits the created contentlet or null if cancelled
     */
    openNewContentDialog(
        contentTypeId: string,
        options: {
            header?: string;
            width?: string;
            height?: string;
            relationshipInfo?: EditContentDialogData['relationshipInfo'];
            onContentSaved?: (contentlet: DotCMSContentlet) => void;
            onCancel?: () => void;
        } = {}
    ): Observable<DotCMSContentlet | null> {
        const dialogData: EditContentDialogData = {
            mode: 'new',
            contentTypeId,
            relationshipInfo: options.relationshipInfo,
            onContentSaved: options.onContentSaved,
            onCancel: options.onCancel
        };

        console.log(
            '🎬 [DotEditContentDialogService] Opening new content dialog for:',
            contentTypeId
        );

        const dialogRef = this.#dialogService.open(DotEditContentDialogComponent, {
            data: dialogData,
            header: options.header || `Create ${contentTypeId}`,
            width: options.width || '95%',
            height: options.height || '95%',
            style: { 'max-width': '1400px', 'max-height': '900px' },
            appendTo: 'body',
            closeOnEscape: true,
            draggable: false,
            keepInViewport: false,
            modal: true,
            resizable: true,
            position: 'center',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-edit-content'
        });

        return dialogRef.onClose.pipe(take(1));
    }

    /**
     * Opens a dialog for editing existing content
     *
     * @param contentletInode - The inode of the content to edit
     * @param options - Additional configuration options
     * @returns Observable that emits the updated contentlet or null if cancelled
     */
    openEditContentDialog(
        contentletInode: string,
        options: {
            header?: string;
            width?: string;
            height?: string;
            depth?: DotContentletDepths;
            onContentSaved?: (contentlet: DotCMSContentlet) => void;
            onCancel?: () => void;
        } = {}
    ): Observable<DotCMSContentlet | null> {
        const dialogData: EditContentDialogData = {
            mode: 'edit',
            contentletInode,
            depth: (options.depth as any) || DotContentletDepths.TWO,
            onContentSaved: options.onContentSaved,
            onCancel: options.onCancel
        };

        const dialogRef = this.#dialogService.open(DotEditContentDialogComponent, {
            data: dialogData,
            header: options.header || 'Edit Content',
            width: options.width || '95%',
            height: options.height || '95%',
            style: { 'max-width': '1400px', 'max-height': '900px' },
            appendTo: 'body',
            closeOnEscape: true,
            draggable: false,
            keepInViewport: false,
            modal: true,
            resizable: true,
            position: 'center',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-edit-content'
        });

        return dialogRef.onClose.pipe(take(1));
    }

    /**
     * Generic method for opening edit content dialog with full control
     *
     * @param data - Complete dialog data configuration
     * @param dialogOptions - PrimeNG dialog options
     * @returns Observable that emits the result contentlet or null if cancelled
     */
    openDialog(
        data: EditContentDialogData,
        dialogOptions: {
            header?: string;
            width?: string;
            height?: string;
            [key: string]: any;
        } = {}
    ): Observable<DotCMSContentlet | null> {
        const defaultOptions = {
            width: '95%',
            height: '95%',
            style: { 'max-width': '1400px', 'max-height': '900px' },
            appendTo: 'body',
            closeOnEscape: true,
            draggable: false,
            keepInViewport: false,
            modal: true,
            resizable: true,
            position: 'center',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-edit-content'
        };

        const dialogRef = this.#dialogService.open(DotEditContentDialogComponent, {
            data,
            ...defaultOptions,
            ...dialogOptions
        });

        return dialogRef.onClose.pipe(take(1));
    }
}
