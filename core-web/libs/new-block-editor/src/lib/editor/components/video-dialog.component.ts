import {
    ChangeDetectionStrategy,
    Component,
    NgZone,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { DataViewModule, type DataViewLazyLoadEvent } from 'primeng/dataview';

import { take } from 'rxjs/operators';

import { Editor } from '@tiptap/core';

import { EditorDialogComponent } from './editor-dialog.component';

import { DOT_VIDEO_NODE_NAME } from '../extensions/nodes/video.extension';
import { DotContentletService, type DotContentlet } from '../services/dot-contentlet.service';
import { DotUploadService } from '../services/dot-upload.service';
import { DOT_BASE_URL } from '../services/dot.config';
import { EditorDialogManagerService } from '../services/editor-dialog-manager.service';
import { EditorStore } from '../store/editor.store';

type Tab = 'upload' | 'url' | 'dotcms';

@Component({
    selector: 'dot-video-dialog',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, DataViewModule, EditorDialogComponent],
    template: `
        <dot-editor-dialog dialogId="video">
            <div
                aria-label="Insert video"
                class="w-[32rem] max-w-[calc(100vw-2rem)] overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg">
                <!-- Tabs -->
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
                                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                        </svg>
                        Upload video
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
                        Video URL
                    </button>
                    <button
                        type="button"
                        role="tab"
                        [attr.aria-selected]="activeTab() === 'dotcms'"
                        [class]="tabClass('dotcms')"
                        data-testid="video-dialog-tab-dotcms"
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

                <!-- Upload tab -->
                @if (activeTab() === 'upload') {
                    <div class="p-4">
                        <label
                            [class]="
                                'flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 transition-colors ' +
                                (uploading()
                                    ? 'border-indigo-300 bg-indigo-50 cursor-wait pointer-events-none'
                                    : 'border-gray-300 cursor-pointer hover:border-indigo-400 hover:bg-indigo-50')
                            "
                            for="vid-upload">
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
                                        d="M15 10l4.553-2.277A1 1 0 0121 8.677v6.646a1 1 0 01-1.447.894L15 14M3 8a2 2 0 012-2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2V8z" />
                                </svg>
                                <span class="text-sm text-gray-600">Click to upload</span>
                                <span class="text-xs text-gray-400">MP4, MOV, WebM</span>
                            }
                            <input
                                id="vid-upload"
                                type="file"
                                accept="video/*"
                                class="sr-only"
                                [disabled]="uploading()"
                                (change)="onFileChange($event)" />
                        </label>
                    </div>
                }

                <!-- URL tab -->
                @if (activeTab() === 'url') {
                    <div
                        class="p-4 flex flex-col gap-3"
                        (keydown.enter)="$event.preventDefault(); onInsertUrl()">
                        <div class="flex flex-col gap-1">
                            <label for="vid-url" class="text-sm text-gray-700">URL</label>
                            <input
                                id="vid-url"
                                type="url"
                                [formControl]="urlControl"
                                placeholder="https://example.com/video.mp4"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>
                        <div class="flex flex-col gap-1">
                            <label for="vid-title" class="text-sm text-gray-700">
                                Title
                                <span class="text-gray-400 text-xs">(optional)</span>
                            </label>
                            <input
                                id="vid-title"
                                type="text"
                                [formControl]="titleControl"
                                placeholder="Video title"
                                class="w-full rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                        </div>
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
                            <label
                                for="dotcms-vid-search"
                                class="text-sm font-medium text-gray-700">
                                Search dotCMS videos
                            </label>
                            <div class="flex gap-2">
                                <input
                                    id="dotcms-vid-search"
                                    type="search"
                                    data-testid="dotcms-video-search-input"
                                    [formControl]="dotcmsSearchControl"
                                    placeholder="Filter by name…"
                                    (keydown.enter)="$event.preventDefault(); runDotcmsSearch()"
                                    class="min-w-0 flex-1 rounded border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none" />
                                <button
                                    type="button"
                                    data-testid="dotcms-video-search-btn"
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
                                class="max-h-[24rem] overflow-hidden rounded-lg bg-gray-50/90 ring-1 ring-inset ring-gray-200 dark:bg-gray-900/30 dark:ring-gray-600/60"
                                data-testid="dotcms-video-dataview-wrap">
                                <p-dataview
                                    [value]="dotcmsVideos()"
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
                                    emptyMessage="No videos found."
                                    [style]="{ border: 'none', boxShadow: 'none' }"
                                    styleClass="!border-0 !shadow-none bg-transparent [&_.p-dataview-content]:border-0 [&_.p-dataview-content]:bg-transparent"
                                    data-testid="dotcms-video-dataview"
                                    (onLazyLoad)="onDotcmsLazyLoad($event)">
                                    <ng-template #list let-items>
                                        <div
                                            class="flex flex-col gap-1 p-1"
                                            role="listbox"
                                            aria-label="Video results">
                                            @for (vid of items; track vid.inode) {
                                                <button
                                                    type="button"
                                                    role="option"
                                                    class="flex w-full items-center gap-3 rounded px-2 py-2 text-left hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-indigo-300"
                                                    [attr.data-testid]="
                                                        'dotcms-video-row-' + vid.inode
                                                    "
                                                    (mousedown)="
                                                        $event.preventDefault();
                                                        insertFromDotcms(vid)
                                                    ">
                                                    <video
                                                        [src]="dotcmsVideoPreviewUrl(vid.inode)"
                                                        muted
                                                        playsInline
                                                        preload="metadata"
                                                        class="pointer-events-none h-10 w-16 shrink-0 rounded bg-black object-cover"
                                                        aria-hidden="true"></video>
                                                    <span
                                                        class="min-w-0 flex-1 truncate text-sm font-medium text-gray-900">
                                                        {{ vid.title || vid.identifier }}
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
            </div>
        </dot-editor-dialog>
    `
})
export class VideoDialogComponent {
    readonly editor = input.required<Editor>();
    protected readonly manager = inject(EditorDialogManagerService);
    private readonly zone = inject(NgZone);
    private readonly dotUpload = inject(DotUploadService);
    private readonly dotContentlet = inject(DotContentletService);
    private readonly store = inject(EditorStore);

    protected readonly activeTab = signal<Tab>('url');
    protected readonly uploading = signal(false);
    protected readonly dotcmsVideos = signal<DotContentlet[]>([]);
    protected readonly dotcmsLoading = signal(false);
    protected readonly dotcmsError = signal<string | null>(null);
    protected readonly dotcmsTotalRecords = signal(0);
    protected readonly dotcmsFirst = signal(0);
    protected readonly dotcmsPageSize = signal(8);
    readonly dotcmsRows = 8;
    readonly dotcmsRowsOptions: number[] = [8, 16, 24];

    readonly urlControl = new FormControl<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.pattern(/^https?:\/\/[^\s]+/)]
    });
    readonly titleControl = new FormControl<string>('', { nonNullable: true });
    readonly dotcmsSearchControl = new FormControl<string>('', { nonNullable: true });

    constructor() {
        effect(() => {
            if (!this.manager.isOpen('video')) {
                untracked(() => {
                    this.activeTab.set('url');
                    this.urlControl.reset('');
                    this.titleControl.reset('');
                    this.dotcmsSearchControl.reset('');
                    this.dotcmsVideos.set([]);
                    this.dotcmsError.set(null);
                    this.dotcmsLoading.set(false);
                    this.dotcmsTotalRecords.set(0);
                    this.dotcmsFirst.set(0);
                    this.dotcmsPageSize.set(this.dotcmsRows);
                    this.uploading.set(false);
                });
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

    dotcmsVideoPreviewUrl(inode: string): string {
        return `${DOT_BASE_URL}/dA/${inode}`;
    }

    onSelectDotcmsTab(): void {
        this.activeTab.set('dotcms');
    }

    onDotcmsLazyLoad(event: DataViewLazyLoadEvent): void {
        this.dotcmsPageSize.set(event.rows);
        this.fetchDotcmsVideosPage(event.first, event.rows);
    }

    runDotcmsSearch(): void {
        this.dotcmsFirst.set(0);
        this.fetchDotcmsVideosPage(0, this.dotcmsPageSize());
    }

    private fetchDotcmsVideosPage(first: number, rows: number): void {
        this.dotcmsLoading.set(true);
        this.dotcmsError.set(null);
        this.dotContentlet
            .searchVideos({
                text: this.dotcmsSearchControl.getRawValue(),
                offset: first,
                limit: rows,
                languageId: this.store.languageId()
            })
            .pipe(take(1))
            .subscribe({
                next: ({ contentlets, totalRecords }) => {
                    this.zone.run(() => {
                        this.dotcmsVideos.set(contentlets);
                        this.dotcmsTotalRecords.set(totalRecords);
                        this.dotcmsFirst.set(first);
                        this.dotcmsLoading.set(false);
                    });
                },
                error: () => {
                    this.zone.run(() => {
                        this.dotcmsVideos.set([]);
                        this.dotcmsTotalRecords.set(0);
                        this.dotcmsError.set('Could not load videos from dotCMS.');
                        this.dotcmsLoading.set(false);
                    });
                }
            });
    }

    insertFromDotcms(contentlet: DotContentlet): void {
        const src = `${DOT_BASE_URL}/dA/${contentlet.inode}`;
        const title = contentlet.title || contentlet.identifier || undefined;
        this.editor()
            .chain()
            .focus()
            .insertContent({
                type: DOT_VIDEO_NODE_NAME,
                attrs: {
                    src,
                    title: title ?? null,
                    data: {
                        identifier: contentlet.identifier,
                        inode: contentlet.inode,
                        languageId: contentlet.languageId,
                        title: contentlet.title ?? '',
                        asset: `/dA/${contentlet.inode}`
                    }
                }
            })
            .run();
        this.manager.close();
    }

    async onFileChange(event: Event): Promise<void> {
        const file = (event.target as HTMLInputElement).files?.[0];
        if (!file) return;

        this.uploading.set(true);
        try {
            const { src, data } = await this.dotUpload.uploadVideo(file);
            const title = file.name.replace(/\.[^.]+$/, '');
            this.zone.run(() => {
                this.editor()
                    .chain()
                    .focus()
                    .insertContent({
                        type: DOT_VIDEO_NODE_NAME,
                        attrs: { src, title: title ?? null, data }
                    })
                    .run();
                this.manager.close();
            });
        } catch (err) {
            console.error('Video upload failed', err);
        } finally {
            this.uploading.set(false);
        }
    }

    onInsertUrl(): void {
        if (this.urlControl.invalid) return;
        const title = this.titleControl.getRawValue().trim() || undefined;
        this.editor()
            .chain()
            .focus()
            .insertContent({
                type: DOT_VIDEO_NODE_NAME,
                attrs: { src: this.urlControl.getRawValue(), title: title ?? null, data: null }
            })
            .run();
        this.manager.close();
    }
}
