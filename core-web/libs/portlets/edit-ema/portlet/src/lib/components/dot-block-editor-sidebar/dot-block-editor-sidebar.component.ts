import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SidebarModule } from 'primeng/sidebar';

import { map, switchMap, take } from 'rxjs/operators';

import { JSONContent } from '@tiptap/core';

import { BlockEditorModule } from '@dotcms/block-editor';
import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotEventsService,
    DotMessageService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

export interface BlockEditorData {
    content: { [key: string]: string };
    field: DotCMSContentTypeField;
    fieldName: string;
    inode: string;
    languageId: number;
}

export const INLINE_EDIT_BLOCK_EDITOR_EVENT = 'edit-block-editor';

@Component({
    selector: 'dot-block-editor-editing',
    standalone: true,
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
export class DotBlockEditorSidebarComponent implements OnInit {
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotEventsService = inject(DotEventsService);
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

    ngOnInit(): void {
        this.#dotEventsService
            .listen<{ [key: string]: string }>(INLINE_EDIT_BLOCK_EDITOR_EVENT)
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                map((event) => event.data),
                switchMap(({ fieldName, contentType, inode, language, blockEditorContent }) => {
                    return this.#getEditorField({ fieldName, contentType }).pipe(
                        map((field) => ({
                            inode,
                            field,
                            fieldName,
                            languageId: parseInt(language),
                            content: this.#getJsonContent(blockEditorContent)
                        }))
                    );
                })
            )
            .subscribe({
                next: (contentlet) => this.contentlet.set(contentlet as BlockEditorData),
                error: (err) => console.error('Error processing event contentlet ', err)
            });
    }

    /**
     * Execute the workflow to save the editor changes and then close the sidebar.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    saveEditorChanges(): void {
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
     * Remove the contentlet data and close the sidebar
     *
     * @protected
     * @memberof DotBlockEditorSidebarComponent
     */
    protected close() {
        this.contentlet.set(null);
        this.value.set(null);
        this.loading.set(false);
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

    /**
     * Parse the content to JSON
     *
     * @param {string} content
     * @return {*}  {JSONContent}
     * @memberof DotBlockEditorSidebarComponent
     */
    #getJsonContent(content: string): JSONContent {
        try {
            return JSON.parse(content);
        } catch (e) {
            console.error('Error parsing JSON content ', e);

            return {};
        }
    }
}
