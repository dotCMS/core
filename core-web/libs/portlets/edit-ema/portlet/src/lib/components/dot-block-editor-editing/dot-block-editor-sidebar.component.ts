import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
// import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, OnInit, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SidebarModule } from 'primeng/sidebar';

import { map, switchMap, take } from 'rxjs/operators';

import { BlockEditorModule, DotBlockEditorComponent } from '@dotcms/block-editor';
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
    contentletIdentifier: string;
    field: DotCMSContentTypeField;
    fieldName: string;
    inode: string;
    languageId: number;
    contentType: string;
}

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
    @ViewChild('blockEditor') blockEditor: DotBlockEditorComponent;

    readonly #destroyRef = inject(DestroyRef);
    readonly #dotEventsService = inject(DotEventsService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotAlertConfirmService = inject(DotAlertConfirmService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);

    protected readonly contentlet = signal<BlockEditorData>(null);

    ngOnInit(): void {
        this.#dotEventsService
            .listen<{ [key: string]: string }>('edit-block-editor')
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                map((event) => event.data),
                switchMap(
                    ({
                        fieldName,
                        contentType,
                        inode,
                        contentletIdentifier,
                        language,
                        blockEditorContent
                    }) => {
                        return this.getEditorField({ fieldName, contentType }).pipe(
                            map((field) => ({
                                inode,
                                field,
                                fieldName,
                                contentType,
                                contentletIdentifier,
                                languageId: parseInt(language),
                                // Add Try/Catch to handle JSON.parse error
                                content: JSON.parse(blockEditorContent)
                            }))
                        );
                    }
                )
            )
            .subscribe({
                next: (contentlet) => this.contentlet.set(contentlet as BlockEditorData),
                error: (err) => console.error('Error processing event contentlet ', err)
            });
    }

    /**
     *  Execute the workflow to save the editor changes and then close the sidebar.
     *
     * @memberof DotBlockEditorSidebarComponent
     */
    saveEditorChanges(): void {
        const { fieldName, inode } = this.contentlet();

        this.#dotWorkflowActionsFireService
            .saveContentlet({
                inode,
                indexPolicy: 'WAIT_FOR',
                [fieldName]: JSON.stringify(this.blockEditor.editor.getJSON())
            })
            .pipe(take(1))
            .subscribe();

        // () => {
        //     this.saving = false;
        //     const customEvent = new CustomEvent('ng-event', {
        //         detail: { name: 'in-iframe' }
        //     });
        //     window.top.document.dispatchEvent(customEvent);
        //     this.closeSidebar();
        // },
        // (e: HttpErrorResponse) => {
        //     this.saving = false;
        //     this.#dotAlertConfirmService.alert({
        //         accept: () => {
        //             this.closeSidebar();
        //         },
        //         header: this.#dotMessageService.get('error'),
        //         message:
        //             e.error?.message || this.#dotMessageService.get('editpage.inline.error')
        //     });
        // }
    }

    /**
     * Get the field that belongs to the editor content
     *
     * @private
     * @param {DOMStringMap} {
     *         fieldName,
     *         contentType
     *     }
     * @return {*}  {Observable<DotCMSContentTypeField>}
     * @memberof DotBlockEditorSidebarComponent
     */
    private getEditorField({
        fieldName,
        contentType
    }: DOMStringMap): Observable<DotCMSContentTypeField> {
        return this.#dotContentTypeService
            .getContentType(contentType)
            .pipe(map(({ fields }) => fields.find(({ variable }) => variable === fieldName)));
    }
}
