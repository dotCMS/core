import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { Drawer, DrawerModule } from 'primeng/drawer';

import { map, take } from 'rxjs/operators';

import { JSONContent } from '@tiptap/core';

import { BlockEditorModule } from '@dotcms/block-editor';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotCMSInlineEditingPayload } from '@dotcms/types';
import { DotMessagePipe } from '@dotcms/ui';

export interface BlockEditorData {
    inode: string;
    fieldName: string;
    language: number;
    content: JSONContent;
    field: DotCMSContentTypeField;
}

export const INLINE_EDIT_BLOCK_EDITOR_EVENT = 'edit-block-editor';

@Component({
    selector: 'dot-block-editor-sidebar',
    templateUrl: './dot-block-editor-sidebar.component.html',
    styleUrls: ['./dot-block-editor-sidebar.component.scss'],
    imports: [
        FormsModule,
        BlockEditorModule,
        DrawerModule,
        DotMessagePipe,
        ButtonModule,
        ConfirmDialogModule
    ]
})
export class DotBlockEditorSidebarComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotAlertConfirmService = inject(DotAlertConfirmService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    readonly drawerRef = viewChild<Drawer>('drawerRef');

    protected readonly contentlet = signal<BlockEditorData>(null);
    protected readonly value = signal<JSONContent>(null);
    protected readonly loading = signal<boolean>(false);

    onSaved = output();
    onClose = output();

    /**
     * Open the sidebar with the block editor content
     *
     * @param {DotCMSInlineEditingPayload} payload
     * @memberof DotBlockEditorSidebarComponent
     */
    open({ inode, content, language, fieldName, contentType }: DotCMSInlineEditingPayload): void {
        this.#getEditorField({ fieldName, contentType }).subscribe({
            next: (field) =>
                this.contentlet.set({
                    inode,
                    field,
                    content,
                    language,
                    fieldName
                }),
            error: (err) => console.error('Error getting contentlet ', err)
        });
    }

    /**
     * Close the drawer using PrimeNG's close method to properly
     * run the leave animation and remove the overlay mask.
     *
     * @param {Event} event
     * @memberof DotBlockEditorSidebarComponent
     */
    closeCallback(event: Event): void {
        this.drawerRef()?.close(event);
    }

    /**
     * Handle the drawer's onHide event — clean up state after the close animation completes.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    onDrawerHide(): void {
        this.value.set(null);
        this.loading.set(false);
        this.contentlet.set(null);
        this.onClose.emit();
    }

    /**
     * Execute the workflow to save the editor changes and then close the sidebar.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    protected saveEditorChanges(): void {
        const { fieldName, inode } = this.contentlet();
        this.loading.set(true);
        this.#dotWorkflowActionsFireService
            .saveContentlet({
                inode,
                indexPolicy: 'WAIT_FOR',
                [fieldName]: JSON.stringify(this.value())
            })
            .pipe(take(1))
            .subscribe({
                next: () => {
                    this.onSaved.emit();
                    this.closeCallback(new Event('close'));
                },
                error: ({ error }: HttpErrorResponse) => {
                    this.#dotAlertConfirmService.alert({
                        accept: () => this.closeCallback(new Event('close')),
                        header: this.#dotMessageService.get('error'),
                        message:
                            error?.message || this.#dotMessageService.get('editpage.inline.error')
                    });
                }
            });
    }

    #getEditorField({ fieldName, contentType }: DOMStringMap): Observable<DotCMSContentTypeField> {
        return this.#dotContentTypeService
            .getContentType(contentType)
            .pipe(map(({ fields }) => fields.find(({ variable }) => variable === fieldName)));
    }
}
