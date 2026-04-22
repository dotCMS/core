import { computePosition, flip, shift } from '@floating-ui/dom';

import { DOCUMENT } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    NgZone,
    afterRenderEffect,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { DataViewModule, type DataViewLazyLoadEvent } from 'primeng/dataview';

import { take } from 'rxjs/operators';

import { ImageDialogService } from './image-dialog.service';

import {
    DotCmsContentletService,
    type DotCmsContentlet
} from '../../services/dot-cms-contentlet.service';
import { DotCmsUploadService } from '../../services/dot-cms-upload.service';
import { DOT_CMS_BASE_URL } from '../../services/dot-cms.config';
import { EditorStore } from '../../store/editor.store';

type Tab = 'upload' | 'url' | 'dotcms';

@Component({
    selector: 'dot-block-editor-image-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, DataViewModule],
    host: {
        '[attr.aria-label]': 'isEditing() ? "Edit image" : "Insert image"',
        class: 'absolute z-50 w-[32rem] max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg',
        '[style.display]': 'service.isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: `
        @if (isEditing()) {
            <!-- Edit mode: link + caption + description, no tabs -->
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
                        (mousedown)="$event.preventDefault(); service.close()"
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
            <!-- Create mode: tabs (upload / URL) -->
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
                        [class]="
                            'flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 transition-colors ' +
                            (uploading()
                                ? 'border-indigo-300 bg-indigo-50 cursor-wait pointer-events-none'
                                : 'border-gray-300 cursor-pointer hover:border-indigo-400 hover:bg-indigo-50')
                        "
                        for="img-upload">
                        @if (uploading()) {
                            <svg
                                class="h-8 w-8 animate-spin text-indigo-400"
                                xmlns="http://www.w3.org/2000/svg"
                                fill="none"
                                viewBox="0 0 24 24"
                                aria-hidden="true">
                                <circle
                                    class="opacity-25"
                                    cx="12"
                                    cy="12"
                                    r="10"
                                    stroke="currentColor"
                                    stroke-width="4" />
                                <path
                                    class="opacity-75"
                                    fill="currentColor"
                                    d="M4 12a8 8 0 018-8v8z" />
                            </svg>
                            <span class="text-sm text-indigo-600">Uploading…</span>
                        } @else {
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
                        }
                        <input
                            id="img-upload"
                            type="file"
                            accept="image/*"
                            class="sr-only"
                            [disabled]="uploading()"
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
                <div class="flex flex-col gap-3 p-4">
                    <div class="flex flex-col gap-1">
                        <label for="dotcms-img-search" class="text-sm font-medium text-gray-700">
                            Search dotCMS images
                        </label>
                        <div class="flex gap-2">
                            <input
                                id="dotcms-img-search"
                                type="search"
                                data-testid="dotcms-image-search-input"
                                [formControl]="dotcmsSearchControl"
                                placeholder="Filter by name…"
                                (keydown.enter)="$event.preventDefault(); runDotcmsSearch()"
                                class="min-w-0 flex-1 rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                            <button
                                type="button"
                                data-testid="dotcms-image-search-btn"
                                (mousedown)="$event.preventDefault(); runDotcmsSearch()"
                                class="shrink-0 rounded border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-300">
                                Search
                            </button>
                        </div>
                    </div>

                    @if (dotcmsError()) {
                        <p class="text-sm text-red-600" role="alert">{{ dotcmsError() }}</p>
                    } @else {
                        <div
                            class="overflow-hidden rounded-lg bg-gray-50/90 ring-1 ring-inset ring-gray-200 dark:bg-gray-900/30 dark:ring-gray-600/60"
                            data-testid="dotcms-image-dataview-wrap">
                            <p-dataview
                                [value]="dotcmsImages()"
                                [lazy]="true"
                                [lazyLoadOnInit]="true"
                                [loading]="dotcmsLoading()"
                                [paginator]="dotcmsTotalRecords() > dotcmsRows"
                                [rows]="dotcmsRows"
                                [rowsPerPageOptions]="dotcmsRowsOptions"
                                [totalRecords]="dotcmsTotalRecords()"
                                [first]="dotcmsFirst()"
                                [pageLinks]="3"
                                paginatorPosition="bottom"
                                [showCurrentPageReport]="true"
                                currentPageReportTemplate="{first} – {last} of {totalRecords}"
                                layout="list"
                                emptyMessage="No images found."
                                [style]="{ border: 'none', boxShadow: 'none' }"
                                styleClass="!border-0 !shadow-none bg-transparent [&_.p-dataview-content]:border-0 [&_.p-dataview-content]:bg-transparent"
                                data-testid="dotcms-image-dataview"
                                (onLazyLoad)="onDotcmsLazyLoad($event)">
                                <ng-template #list let-items>
                                    <div
                                        class="flex flex-col gap-1 p-1"
                                        role="listbox"
                                        aria-label="Image results">
                                        @for (img of items; track img.inode) {
                                            <button
                                                type="button"
                                                role="option"
                                                class="group flex w-full items-center gap-3 rounded px-2 py-2 text-left hover:bg-gray-100 hover:ring-1 hover:ring-inset hover:ring-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-300"
                                                [attr.data-testid]="'dotcms-image-row-' + img.inode"
                                                (mousedown)="
                                                    $event.preventDefault(); insertFromDotcms(img)
                                                ">
                                                <img
                                                    [src]="dotcmsThumbUrl(img.inode)"
                                                    alt=""
                                                    width="40"
                                                    height="40"
                                                    loading="lazy"
                                                    class="h-10 w-10 shrink-0 rounded bg-gray-100 object-cover group-hover:ring-2 group-hover:ring-indigo-400 group-hover:ring-offset-1" />
                                                <span
                                                    class="min-w-0 flex-1 truncate text-sm font-medium text-gray-900">
                                                    {{ img.title || img.identifier }}
                                                </span>
                                            </button>
                                        }
                                    </div>
                                </ng-template>
                            </p-dataview>
                        </div>
                    }
                </div>
            }
        }
    `
})
export class ImageDialogComponent {
    protected readonly service = inject(ImageDialogService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly document = inject(DOCUMENT);
    private readonly dotCmsUpload = inject(DotCmsUploadService);
    private readonly dotCmsContentlet = inject(DotCmsContentletService);
    private readonly store = inject(EditorStore);

    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);
    protected readonly activeTab = signal<Tab>('url');
    protected readonly isEditing = computed(() => this.service.initialValues() !== null);
    protected readonly uploading = signal(false);
    protected readonly dotcmsImages = signal<DotCmsContentlet[]>([]);
    protected readonly dotcmsLoading = signal(false);
    protected readonly dotcmsError = signal<string | null>(null);
    protected readonly dotcmsTotalRecords = signal(0);
    protected readonly dotcmsFirst = signal(0);
    /** Last page size from DataView (rows per page); kept for “Search” reset. */
    protected readonly dotcmsPageSize = signal(8);
    readonly dotcmsRows = 8;
    readonly dotcmsRowsOptions: number[] = [8, 16, 24];

    private previouslyFocused: HTMLElement | null = null;

    readonly urlControl = new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
    });

    readonly dotcmsSearchControl = new FormControl<string>('', { nonNullable: true });

    readonly editForm = new FormGroup({
        src: new FormControl<string>('', { nonNullable: true, validators: [Validators.required] }),
        title: new FormControl<string>('', { nonNullable: true }),
        alt: new FormControl<string>('', { nonNullable: true })
    });

    constructor() {
        effect(() => {
            const values = this.service.initialValues();
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

        effect((onCleanup) => {
            if (!this.service.isOpen()) return;

            this.previouslyFocused = this.document.activeElement as HTMLElement | null;

            const handleKeyDown = (event: KeyboardEvent) => {
                if (event.key === 'Escape') this.zone.run(() => this.service.close());
            };
            const handleMouseDown = (event: MouseEvent) => {
                if (!this.el.nativeElement.contains(event.target as Node)) {
                    this.zone.run(() => this.service.close());
                }
            };

            this.document.addEventListener('keydown', handleKeyDown);
            this.document.addEventListener('mousedown', handleMouseDown);
            onCleanup(() => {
                this.document.removeEventListener('keydown', handleKeyDown);
                this.document.removeEventListener('mousedown', handleMouseDown);
                this.previouslyFocused?.focus({ preventScroll: true });
                this.previouslyFocused = null;
            });
        });

        afterRenderEffect(() => {
            const isOpen = this.service.isOpen();
            const clientRectFn = this.service.clientRectFn();

            if (!isOpen || !clientRectFn) {
                untracked(() => {
                    this.positioned.set(false);
                    this.activeTab.set('url');
                    this.urlControl.reset('');
                    this.dotcmsSearchControl.reset('');
                    this.dotcmsImages.set([]);
                    this.dotcmsError.set(null);
                    this.dotcmsLoading.set(false);
                    this.dotcmsTotalRecords.set(0);
                    this.dotcmsFirst.set(0);
                    this.dotcmsPageSize.set(this.dotcmsRows);
                    this.editForm.reset({ src: '', title: '', alt: '' });
                });
                return;
            }

            const virtualRef = {
                getBoundingClientRect: () => clientRectFn() ?? new DOMRect()
            };

            computePosition(virtualRef, this.el.nativeElement, {
                placement: 'bottom-start',
                strategy: 'absolute',
                middleware: [flip(), shift({ padding: 8 })]
            }).then(({ x, y }) => {
                this.zone.run(() => {
                    untracked(() => {
                        this.floatX.set(x);
                        this.floatY.set(y);
                        this.positioned.set(true);
                    });
                });
                setTimeout(() => {
                    const firstInput = this.el.nativeElement.querySelector(
                        'input:not([type="file"]):not([type="checkbox"])'
                    ) as HTMLElement | null;
                    firstInput?.focus();
                }, 0);
            });
        });
    }

    tabClass(tab: Tab): string {
        const base =
            'flex min-w-0 flex-1 items-center justify-center gap-1.5 px-2 py-2.5 text-xs font-medium border-b-2 transition-colors sm:gap-2 sm:px-3 sm:text-sm';
        return this.activeTab() === tab
            ? `${base} border-indigo-500 text-indigo-600 bg-white`
            : `${base} border-transparent text-gray-500 hover:text-gray-700 bg-gray-50`;
    }

    dotcmsThumbUrl(inode: string): string {
        return `${DOT_CMS_BASE_URL}/dA/${inode}/120/max`;
    }

    onSelectDotcmsTab(): void {
        this.activeTab.set('dotcms');
    }

    onDotcmsLazyLoad(event: DataViewLazyLoadEvent): void {
        this.dotcmsPageSize.set(event.rows);
        this.fetchDotcmsImagesPage(event.first, event.rows);
    }

    /** New search/filter: reset to first page (keeps current rows-per-page). */
    runDotcmsSearch(): void {
        this.dotcmsFirst.set(0);
        this.fetchDotcmsImagesPage(0, this.dotcmsPageSize());
    }

    private fetchDotcmsImagesPage(first: number, rows: number): void {
        this.dotcmsLoading.set(true);
        this.dotcmsError.set(null);
        this.dotCmsContentlet
            .searchImages({
                text: this.dotcmsSearchControl.getRawValue(),
                offset: first,
                limit: rows,
                languageId: this.store.languageId()
            })
            .pipe(take(1))
            .subscribe({
                next: ({ contentlets, totalRecords }) => {
                    this.zone.run(() => {
                        this.dotcmsImages.set(contentlets);
                        this.dotcmsTotalRecords.set(totalRecords);
                        this.dotcmsFirst.set(first);
                        this.dotcmsLoading.set(false);
                    });
                },
                error: () => {
                    this.zone.run(() => {
                        this.dotcmsImages.set([]);
                        this.dotcmsTotalRecords.set(0);
                        this.dotcmsError.set('Could not load images from dotCMS.');
                        this.dotcmsLoading.set(false);
                    });
                }
            });
    }

    insertFromDotcms(contentlet: DotCmsContentlet): void {
        const src = `${DOT_CMS_BASE_URL}/dA/${contentlet.inode}`;
        const label = contentlet.title || contentlet.identifier;
        this.service.insert(src, label || undefined, label || undefined);
    }

    async onFileChange(event: Event): Promise<void> {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (!file) return;

        this.uploading.set(true);
        try {
            const src = await this.dotCmsUpload.uploadImage(file);
            this.zone.run(() => this.service.insert(src, undefined, file.name));
        } catch (err) {
            console.error('Image upload failed', err);
        } finally {
            this.uploading.set(false);
        }
    }

    onInsertUrl(): void {
        if (this.urlControl.invalid) return;
        this.service.insert(this.urlControl.getRawValue());
    }

    onApplyEdit(): void {
        if (this.editForm.controls.src.invalid) return;
        const { src, title, alt } = this.editForm.getRawValue();
        this.service.insert(src, title || undefined, alt || undefined);
    }
}
