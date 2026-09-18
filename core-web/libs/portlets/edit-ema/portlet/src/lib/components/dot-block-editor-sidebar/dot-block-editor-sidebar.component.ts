import { Observable } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    inject,
    input,
    output,
    signal,
    viewChild,
    ChangeDetectionStrategy
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
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
    DotPropertiesService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSContentTypeField, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotCMSEditorComponent } from '@dotcms/new-block-editor';
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
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [
        FormsModule,
        DotCMSEditorComponent,
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
    readonly #dotPropertiesService = inject(DotPropertiesService);

    /**
     * Resolves the `FEATURE_FLAG_NEW_BLOCK_EDITOR` flag — `undefined` while the HTTP request
     * is in flight, then `true` / `false` once it returns. Per the project-wide rule, a missing
     * flag resolves to `true` (`getFeatureFlag`), so the new editor renders unless the flag is
     * explicitly `false`. The template's truthy check still keeps the legacy editor in-flight.
     */
    readonly isNewBlockEditorEnabled = toSignal(
        this.#dotPropertiesService.getFeatureFlag(FeaturedFlags.FEATURE_FLAG_NEW_BLOCK_EDITOR)
    );

    readonly drawerRef = viewChild<Drawer>('drawerRef');

    variantName = input<string>(DEFAULT_VARIANT_ID);

    /**
     * Both are `| null` because both are null while the sidebar is closed: that is the seed value
     * here and what `onDrawerHide` sets them back to once the close animation finishes.
     */
    protected readonly contentlet = signal<BlockEditorData | null>(null);
    protected readonly value = signal<JSONContent | null>(null);
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
            next: (field) => {
                // The lookup misses when the payload names a field the content type does not
                // have. Opening the sidebar against no field renders an editor with no
                // configuration, so it is reported the same way as a failed request instead.
                if (!field) {
                    console.error(
                        `Field "${fieldName}" not found on content type "${contentType}"`
                    );

                    return;
                }

                this.contentlet.set({
                    inode,
                    field,
                    content,
                    language,
                    fieldName
                });
            },
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
        const contentlet = this.contentlet();

        // The save button lives in the drawer, which only renders with a contentlet set — but the
        // signal is null whenever the drawer is closed, so the read has to say so.
        if (!contentlet) {
            return;
        }

        const { fieldName, inode } = contentlet;
        this.loading.set(true);
        this.#dotWorkflowActionsFireService
            .saveContentlet({
                inode,
                indexPolicy: 'WAIT_FOR',
                variantName: this.variantName(),
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

    /**
     * `DOMStringMap` was structurally close enough to compile — two optional string keys — but it
     * made both values `string | undefined` when the payload declares them as required strings, and
     * `find` returning `undefined` was never in the signature at all.
     */
    #getEditorField({
        fieldName,
        contentType
    }: Pick<DotCMSInlineEditingPayload, 'fieldName' | 'contentType'>): Observable<
        DotCMSContentTypeField | undefined
    > {
        return this.#dotContentTypeService
            .getContentType(contentType)
            .pipe(map(({ fields }) => fields.find(({ variable }) => variable === fieldName)));
    }
}
