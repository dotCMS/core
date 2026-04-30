import {
    ChangeDetectionStrategy,
    Component,
    NgZone,
    OnDestroy,
    computed,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotBrowserSelectorComponent } from '@dotcms/ui';

import { EditorDialogComponent } from './editor-dialog.component';

import { buildBrowserSelectorConfig } from '../config.utils';
import { insertDotImageFromContentlet } from '../editor.utils';
import { DOT_IMAGE_NODE_NAME } from '../extensions/nodes/image.extension';
import {
    insertUploadPlaceholders,
    replacePlaceholder,
    removePlaceholder
} from '../extensions/nodes/upload-placeholder.extension';
import { DotUploadService } from '../services/dot-upload.service';
import { EditorDialogManagerService } from '../services/editor-dialog-manager.service';

type Tab = 'upload' | 'url' | 'dotcms';

@Component({
    selector: 'dot-image-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, EditorDialogComponent],
    template: `
        <dot-editor-dialog dialogId="image">
            <div
                [attr.aria-label]="isEditing() ? 'Edit image' : 'Insert image'"
                class="w-[32rem] max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                @if (isEditing()) {
                    <!-- Edit mode: src + tooltip + alt, no tabs -->
                    <div
                        class="p-4 flex flex-col gap-3"
                        (keydown.enter)="$event.preventDefault(); onApplyEdit()">
                        <div class="flex flex-col gap-1">
                            <label for="edit-img-url" class="text-sm font-medium text-gray-700">
                                Image URL
                                <span class="text-red-500" aria-hidden="true">*</span>
                            </label>
                            <input
                                id="edit-img-url"
                                type="text"
                                [formControl]="editForm.controls.src"
                                placeholder="https://example.com/image.png"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>

                        <div class="flex flex-col gap-1">
                            <label for="edit-img-title" class="text-sm font-medium text-gray-700">
                                Tooltip
                            </label>
                            <p id="edit-img-title-hint" class="text-xs text-gray-400 -mt-0.5">
                                Text shown when hovering over the image
                            </p>
                            <input
                                id="edit-img-title"
                                type="text"
                                [formControl]="editForm.controls.title"
                                placeholder="Add a caption…"
                                aria-describedby="edit-img-title-hint"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>

                        <div class="flex flex-col gap-1">
                            <label for="edit-img-alt" class="text-sm font-medium text-gray-700">
                                Alt text
                            </label>
                            <p id="edit-img-alt-hint" class="text-xs text-gray-400 -mt-0.5">
                                Read aloud by screen readers; improves accessibility
                            </p>
                            <input
                                id="edit-img-alt"
                                type="text"
                                [formControl]="editForm.controls.alt"
                                placeholder="Describe what's in the image…"
                                aria-describedby="edit-img-alt-hint"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>

                        <div class="flex justify-end gap-2">
                            <button
                                type="button"
                                (mousedown)="$event.preventDefault(); manager.close()"
                                class="rounded border border-gray-300 px-4 py-1.5 text-sm text-gray-600 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-300">
                                Cancel
                            </button>
                            <button
                                type="button"
                                (mousedown)="$event.preventDefault(); onApplyEdit()"
                                [disabled]="editForm.controls.src.invalid"
                                class="rounded bg-indigo-500 px-4 py-1.5 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                                Save
                            </button>
                        </div>
                    </div>
                } @else {
                    <!-- Create mode: tabs (upload / URL / dotCMS) -->
                    <div class="flex border-b border-gray-200" role="tablist">
                        <button
                            type="button"
                            role="tab"
                            [attr.aria-selected]="activeTab() === 'upload'"
                            [class]="tabClass('upload')"
                            (mousedown)="$event.preventDefault(); activeTab.set('upload')">
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                class="h-4 w-4"
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                                aria-hidden="true">
                                <path
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    stroke-width="2"
                                    d="M3 7h4l2-3h6l2 3h4a1 1 0 011 1v11a1 1 0 01-1 1H3a1 1 0 01-1-1V8a1 1 0 011-1z" />
                                <circle
                                    cx="12"
                                    cy="13"
                                    r="3"
                                    stroke="currentColor"
                                    stroke-width="2"
                                    fill="none" />
                            </svg>
                            Upload image
                        </button>
                        <button
                            type="button"
                            role="tab"
                            [attr.aria-selected]="activeTab() === 'url'"
                            [class]="tabClass('url')"
                            (mousedown)="$event.preventDefault(); activeTab.set('url')">
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                class="h-4 w-4"
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                                aria-hidden="true">
                                <path
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    stroke-width="2"
                                    d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                            </svg>
                            Image URL
                        </button>
                        <button
                            type="button"
                            role="tab"
                            [attr.aria-selected]="activeTab() === 'dotcms'"
                            [class]="tabClass('dotcms')"
                            data-testid="image-dialog-tab-dotcms"
                            (mousedown)="$event.preventDefault(); onSelectDotcmsTab()">
                            <svg
                                xmlns="http://www.w3.org/2000/svg"
                                class="h-4 w-4 shrink-0"
                                fill="none"
                                viewBox="0 0 24 24"
                                stroke="currentColor"
                                aria-hidden="true">
                                <path
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    stroke-width="2"
                                    d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                            </svg>
                            dotCMS
                        </button>
                    </div>

                    @if (activeTab() === 'upload') {
                        <div class="p-4">
                            <label
                                class="flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-gray-300 p-8 transition-colors cursor-pointer hover:border-indigo-400 hover:bg-indigo-50"
                                for="img-upload">
                                <svg
                                    xmlns="http://www.w3.org/2000/svg"
                                    class="h-8 w-8 text-gray-400"
                                    fill="none"
                                    viewBox="0 0 24 24"
                                    stroke="currentColor"
                                    aria-hidden="true">
                                    <path
                                        stroke-linecap="round"
                                        stroke-linejoin="round"
                                        stroke-width="1.5"
                                        d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                                </svg>
                                <span class="text-sm text-gray-600">Click to upload</span>
                                <span class="text-xs text-gray-400">PNG, JPG, GIF, WebP</span>
                                <input
                                    id="img-upload"
                                    type="file"
                                    accept="image/*"
                                    class="sr-only"
                                    (change)="onFileChange($event)" />
                            </label>
                        </div>
                    }

                    @if (activeTab() === 'url') {
                        <div
                            class="p-4 flex flex-col gap-3"
                            (keydown.enter)="$event.preventDefault(); onInsertUrl()">
                            <label for="img-url" class="text-sm text-gray-700">Image URL</label>
                            <input
                                id="img-url"
                                type="url"
                                [formControl]="urlControl"
                                placeholder="https://example.com/image.png"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                            <div class="flex justify-end">
                                <button
                                    type="button"
                                    (mousedown)="$event.preventDefault(); onInsertUrl()"
                                    [disabled]="urlControl.invalid"
                                    class="rounded bg-indigo-500 px-4 py-1.5 text-sm text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400 disabled:opacity-50 disabled:cursor-not-allowed">
                                    Insert
                                </button>
                            </div>
                        </div>
                    }

                    @if (activeTab() === 'dotcms') {
                        <div class="flex flex-col items-center gap-4 p-6">
                            <p class="text-center text-sm text-gray-600">
                                Browse images stored in dotCMS — folders, filters, and previews.
                            </p>
                            <button
                                type="button"
                                data-testid="dotcms-image-open-browser"
                                (mousedown)="$event.preventDefault(); openDotcmsBrowser()"
                                class="rounded bg-indigo-500 px-4 py-1.5 text-sm font-medium text-white hover:bg-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-400">
                                Open dotCMS image picker
                            </button>
                        </div>
                    }
                }
            </div>
        </dot-editor-dialog>
    `
})
export class ImageDialogComponent implements OnDestroy {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorDialogManagerService);
    private readonly zone = inject(NgZone);
    private readonly dotUpload = inject(DotUploadService);
    private readonly dialogService = inject(DialogService);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly activeTab = signal<Tab>('url');
    protected readonly isEditing = computed(
        () => this.manager.imagePayload()?.initialValues != null
    );

    /** Live PrimeNG dialog ref for the dotCMS image browser-selector; cleared on close / destroy. */
    private dotcmsImagePickerRef: DynamicDialogRef | null = null;

    readonly urlControl = new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
    });
    readonly editForm = new FormGroup({
        src: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
        title: new FormControl<string>('', { nonNullable: true }),
        alt: new FormControl<string>('', { nonNullable: true })
    });

    constructor() {
        // Pre-populate the edit form when opened in edit mode.
        effect(() => {
            const values = this.manager.imagePayload()?.initialValues;
            if (values) {
                untracked(() =>
                    this.editForm.setValue({
                        src: values.src,
                        title: values.title,
                        alt: values.alt
                    })
                );
            }
        });

        // Reset dialog UI state when the dialog closes.
        effect(() => {
            if (!this.manager.isOpen('image')) {
                untracked(() => this.resetDialogUi());
            }
        });
    }

    tabClass(tab: Tab): string {
        const base =
            'flex min-w-0 flex-1 items-center justify-center gap-1.5 px-2 py-2.5 text-xs font-medium border-b-2 transition-colors sm:gap-2 sm:px-3 sm:text-sm';
        return this.activeTab() === tab
            ? `${base} border-indigo-500 text-indigo-600 bg-white`
            : `${base} border-transparent text-gray-500 hover:text-gray-700 bg-gray-50`;
    }

    onSelectDotcmsTab(): void {
        this.activeTab.set('dotcms');
    }

    /**
     * Closes this caret-anchored popover and hands off to the centered dotCMS
     * browser-selector ({@link DotBrowserSelectorComponent} from `@dotcms/ui`) — the same
     * picker the file-field uses. Configured for image-mime contentlets and dotAssets
     * (no folders, no archived). On accept, inserts the picked contentlet as a `dotImage`
     * node via {@link insertDotImageFromContentlet}.
     */
    openDotcmsBrowser(): void {
        if (this.dotcmsImagePickerRef) return;

        // Dismiss the popover-anchored image dialog before opening the centered modal so
        // they don't stack on top of each other.
        this.manager.close();

        const editor = this.editor();
        this.dotcmsImagePickerRef = this.dialogService.open(
            DotBrowserSelectorComponent,
            buildBrowserSelectorConfig({
                header: this.dotMessageService.get(
                    'block-editor.extension.image.dotcms.dialog-title'
                ),
                mimeTypes: ['image']
            })
        );

        this.dotcmsImagePickerRef.onClose.subscribe((contentlet?: DotCMSContentlet) => {
            if (contentlet) {
                this.zone.run(() => insertDotImageFromContentlet(editor, contentlet));
            }
            this.dotcmsImagePickerRef = null;
        });
    }

    ngOnDestroy(): void {
        this.dotcmsImagePickerRef?.close();
        this.dotcmsImagePickerRef = null;
    }

    /**
     * Picks a file, inserts a placeholder immediately (dialog closes), uploads in the background,
     * then replaces the placeholder with the real image node (with full DotImageData).
     */
    async onFileChange(event: Event): Promise<void> {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (!file) return;

        const editor = this.editor();
        const pos = editor.state.selection.from;
        const id = `img-upload-${Date.now()}`;
        insertUploadPlaceholders(editor, pos, [{ id, mediaType: 'image' }]);
        this.manager.close();

        try {
            const { src, data } = await this.dotUpload.uploadImage(file);
            this.zone.run(() =>
                replacePlaceholder(editor, id, {
                    type: DOT_IMAGE_NODE_NAME,
                    attrs: { src, alt: file.name, data, title: null }
                })
            );
        } catch (err) {
            console.error('Image upload failed', err);
            removePlaceholder(editor, id);
        }
    }

    onInsertUrl(): void {
        if (this.urlControl.invalid) return;
        this.editor()
            .chain()
            .focus()
            .insertContent({
                type: DOT_IMAGE_NODE_NAME,
                attrs: {
                    src: this.urlControl.getRawValue(),
                    title: null,
                    alt: null,
                    data: null
                }
            })
            .run();
        this.manager.close();
    }

    onApplyEdit(): void {
        if (this.editForm.controls.src.invalid) return;
        const { src, title, alt } = this.editForm.getRawValue();
        this.editor()
            .chain()
            .focus()
            .updateAttributes('dotImage', {
                src,
                title: title || null,
                alt: alt || null
            })
            .run();
        this.manager.close();
    }

    private resetDialogUi(): void {
        this.activeTab.set('url');
        this.urlControl.reset('');
        this.editForm.reset({ src: '', title: '', alt: '' });
    }
}
