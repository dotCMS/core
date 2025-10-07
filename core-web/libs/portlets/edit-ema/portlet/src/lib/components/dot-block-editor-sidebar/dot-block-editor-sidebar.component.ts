import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SidebarModule } from 'primeng/sidebar';

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
        CommonModule,
        SidebarModule,
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

    protected readonly contentlet = signal<BlockEditorData>(null);
    protected readonly value = signal<JSONContent>(null);
    protected readonly loading = signal<boolean>(false);

    /**
     * Emit when the editor changes are saved
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    onSaved = output();

    /**
     * Emit when the sidebar is closed
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    onClose = output();

    /**
     * Open the sidebar with the block editor content
     *
     * @param {InlineEditorData} { fieldName, contentType, inode, language, blockEditorContent }
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
     * Remove the contentlet data and close the sidebar
     *
     * @protected
     * @memberof DotBlockEditorSidebarComponent
     */
    close() {
        this.resetState();
        this.onClose.emit();
    }

    /**
     * Reset the state of the sidebar
     *
     * @protected
     * @memberof DotBlockEditorSidebarComponent
     */
    protected resetState(): void {
        this.value.set(null);
        this.loading.set(false);
        this.contentlet.set(null);
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
            .subscribe(
                () => {
                    this.onSaved.emit();
                    this.close();
                },
                ({ error }: HttpErrorResponse) => {
                    this.#dotAlertConfirmService.alert({
                        accept: () => this.close(),
                        header: this.#dotMessageService.get('error'),
                        message:
                            error?.message || this.#dotMessageService.get('editpage.inline.error')
                    });
                },
                () => this.close()
            );
    }

    /**
     * Get the field that belongs to the editor content
     *
     * @private
     * @param {DOMStringMap} { fieldName, contentType }
     * @return {*}  {Observable<DotCMSContentTypeField>}
     * @memberof DotBlockEditorSidebarComponent
     */
    #getEditorField({ fieldName, contentType }: DOMStringMap): Observable<DotCMSContentTypeField> {
        return this.#dotContentTypeService
            .getContentType(contentType)
            .pipe(map(({ fields }) => fields.find(({ variable }) => variable === fieldName)));
    }
}
